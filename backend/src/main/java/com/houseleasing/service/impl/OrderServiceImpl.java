package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.dto.OrderReviewRequest;
import com.houseleasing.dto.ReviewRecordResponse;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.Review;
import com.houseleasing.entity.User;
import com.houseleasing.entity.Contract;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.ReviewMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.MessageService;
import com.houseleasing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 实现订单相关的所有业务逻辑，包括创建意向订单和预约订单、
 *              订单审批、取消、完成等操作，通过 RabbitMQ 发送状态变更通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final ContractMapper contractMapper;
    private final MessageProducer messageProducer;
    private final MessageService messageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_ONE_DAY_SCRIPT = buildIncrWithExpireOneDayScript();
    private static final long CANCEL_COUNT_DEDUCT_THRESHOLD = 11L;
    private static final String TENANT_CANCEL_SELF_MESSAGE = "您已取消该预约订单";
    private static final String TENANT_CANCEL_NOTIFY_LANDLORD_MESSAGE = "租客已取消预约订单";
    private static final String LANDLORD_CANCEL_SELF_MESSAGE = "您已取消该预约订单";
    private static final String LANDLORD_CANCEL_NOTIFY_TENANT_MESSAGE = "房东已取消预约订单";
    private static final int REVIEW_MIN_RATING = 1;
    private static final int REVIEW_MAX_RATING = 5;
    private static final int REVIEW_MIDDLE_RATING = 3;
    private static final int REVIEW_GOOD_RATING = 4;
    private static final int CREDIT_DELTA_LOW_RATING = -10;
    private static final int CREDIT_DELTA_THREE_STARS = 3;
    private static final int CREDIT_DELTA_FOUR_STARS = 4;
    private static final int CREDIT_DELTA_FIVE_STARS = 5;
    private static final int DEFAULT_CREDIT_SCORE = 100;
    private static final String REVIEW_NOTIFY_TO_LANDLORD_TEMPLATE = "租客已完成评价：%d星";
    private static final String REVIEW_NOTIFY_TO_TENANT = "您的评价已提交，感谢反馈";

    /**
     * 创建意向订单：租客表达租房意向，通知房东
     *
     * @param tenantId 租客用户 ID
     * @param houseId  目标房源 ID
     * @param remark   备注信息
     * @return 创建的意向订单对象
     */
    @Override
    @Transactional
    public Order createIntent(Long tenantId, Long houseId, String remark) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        User tenant = userMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!Boolean.TRUE.equals(tenant.getIsRealNameAuth())) {
            throw new BusinessException(403, "请先完成实名认证后再预约看房");
        }
        // 信用分 <= 0 时禁止发起预约（包括意向预约）
        if (tenant.getCreditScore() == null || tenant.getCreditScore() <= 0) {
            throw new BusinessException(403, "当前信用分过低，暂不可发起预约房源");
        }
        Order order = new Order();
        // 押金金额 = 押金月数 × 月租金（houses.deposit 存储的是月数，需乘以月租金得到实际金额）
        BigDecimal depositAmount = (house.getDeposit() != null && house.getPrice() != null)
                ? house.getDeposit().multiply(house.getPrice())
                : BigDecimal.ZERO;
        order.setHouseId(houseId);
        order.setTenantId(tenantId);
        order.setLandlordId(house.getOwnerId());
        order.setOrderNo(generateOrderNo("INT")); // INT 前缀表示意向订单
        order.setStatus("PENDING");
        order.setMonthlyRent(house.getPrice());
        order.setDeposit(depositAmount); // 存储实际押金金额（元），而非月数
        order.setRemark(remark);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        // 通知房东有新意向订单
        messageProducer.sendOrderStatusChange(house.getOwnerId(), "新的意向订单");
        return order;
    }

    /**
     * 创建预约看房订单，同时通知租客和房东
     *
     * @param request  预约订单请求参数
     * @param tenantId 租客用户 ID
     * @return 创建的预约订单对象
     */
    @Override
    @Transactional
    public Order createAppointment(OrderCreateRequest request, Long tenantId) {
        House house = houseMapper.selectById(request.getHouseId());
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        User tenant = userMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!Boolean.TRUE.equals(tenant.getIsRealNameAuth())) {
            throw new BusinessException(403, "请先完成实名认证后再预约看房");
        }
        // 信用分 <= 0 时禁止发起预约（包括标准预约）
        if (tenant.getCreditScore() == null || tenant.getCreditScore() <= 0) {
            throw new BusinessException(403, "当前信用分过低，暂不可发起预约房源");
        }
        Order order = new Order();
        // 押金金额 = 押金月数 × 月租金（houses.deposit 存储的是月数，需乘以月租金得到实际金额）
        BigDecimal depositAmount = (house.getDeposit() != null && house.getPrice() != null)
                ? house.getDeposit().multiply(house.getPrice())
                : BigDecimal.ZERO;
        order.setHouseId(request.getHouseId());
        order.setTenantId(tenantId);
        order.setLandlordId(house.getOwnerId());
        order.setOrderNo(generateOrderNo("APT")); // APT 前缀表示预约订单
        order.setStatus("PENDING");
        order.setAppointmentTime(request.getAppointmentTime());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        order.setMonthlyRent(house.getPrice());
        order.setDeposit(depositAmount); // 存储实际押金金额（元），而非月数
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        // 通知租客预约已提交，通知房东有新预约订单
        messageProducer.sendAppointmentConfirmation(tenantId, house.getTitle());
        messageProducer.sendOrderStatusChange(house.getOwnerId(), "新的预约订单");
        return order;
    }

    /**
     * 房东审批订单，通知租客审批结果
     *
     * @param orderId    订单 ID
     * @param approved   是否批准
     * @param landlordId 房东用户 ID（用于权限验证）
     */
    @Override
    @Transactional
    public void approveOrder(Long orderId, boolean approved, Long landlordId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 验证操作人是否为该订单的房东
        if (!order.getLandlordId().equals(landlordId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        order.setStatus(approved ? "APPROVED" : "REJECTED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 通知租客审批结果
        messageProducer.sendOrderStatusChange(order.getTenantId(),
                approved ? "您的订单已被批准" : "您的订单已被拒绝");
    }

    /**
     * 取消订单，验证操作人是否为订单的租客或房东
     *
     * @param orderId 订单 ID
     * @param userId  操作人用户 ID
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 验证操作人是租客或房东
        if (!Objects.equals(order.getTenantId(), userId) && !Objects.equals(order.getLandlordId(), userId)) {
            throw new BusinessException(403, "没有操作权限");
        }

        // 仅当“租客本人”取消时触发信用分惩罚逻辑（Redis按日计数）：
        // 第 11 次取消触发一次扣分，计数 key 过期时间固定 1 天。
        boolean tenantCancelling = Objects.equals(userId, order.getTenantId());
        boolean shouldDeductCredit = false;
        if (tenantCancelling) {
            String day = LocalDate.now().toString();
            String cancelCountKey = "order:cancel:" + order.getTenantId() + ":" + order.getHouseId() + ":" + day;
            Long cancelledCount = redisTemplate.execute(
                    INCR_WITH_EXPIRE_ONE_DAY_SCRIPT,
                    java.util.Collections.singletonList(cancelCountKey)
            );
            shouldDeductCredit = cancelledCount != null && cancelledCount == CANCEL_COUNT_DEDUCT_THRESHOLD;
        }

        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 订单取消属于关键业务事件：同时通知租客与房东，确保双方都能第一时间在消息中心看到状态变化。
        // 这里统一复用“订单状态通知”渠道，消息内容根据操作人身份区分，便于双方理解取消原因。
        if (tenantCancelling) {
            messageProducer.sendOrderStatusChange(order.getTenantId(), TENANT_CANCEL_SELF_MESSAGE);
            messageProducer.sendOrderStatusChange(order.getLandlordId(), TENANT_CANCEL_NOTIFY_LANDLORD_MESSAGE);
        } else {
            messageProducer.sendOrderStatusChange(order.getLandlordId(), LANDLORD_CANCEL_SELF_MESSAGE);
            messageProducer.sendOrderStatusChange(order.getTenantId(), LANDLORD_CANCEL_NOTIFY_TENANT_MESSAGE);
        }

        // 满足扣分条件时执行信用分扣减（下限为 0）
        if (shouldDeductCredit) {
            User tenant = userMapper.selectById(order.getTenantId());
            if (tenant != null) {
                int currentScore = tenant.getCreditScore() == null ? 0 : tenant.getCreditScore();
                tenant.setCreditScore(Math.max(0, currentScore - 10));
                tenant.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(tenant);
            }
        }
    }

    /**
     * 根据 ID 查询订单详情，同时关联填充房源、租客和房东信息
     *
     * @param id 订单 ID
     * @return 订单详情对象（含关联信息）
     */
    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 关联填充合同状态与可支付标记：
        // 仅当存在对应合同且状态为 FULLY_SIGNED 时，前端才应展示“待支付”按钮。
        fillContractPaymentAbility(order);
        // 关联填充房源信息
        if (order.getHouseId() != null) {
            House house = houseMapper.selectById(order.getHouseId());
            order.setHouse(house);
        }
        // 关联填充租客信息（隐去敏感字段）
        if (order.getTenantId() != null) {
            User tenant = userMapper.selectById(order.getTenantId());
            if (tenant != null) {
                tenant.setPassword(null);
                tenant.setIdCard(null);
                order.setTenant(tenant);
            }
        }
        // 关联填充房东信息（隐去敏感字段）
        if (order.getLandlordId() != null) {
            User landlord = userMapper.selectById(order.getLandlordId());
            if (landlord != null) {
                landlord.setPassword(null);
                landlord.setIdCard(null);
                order.setLandlord(landlord);
            }
        }
        return order;
    }

    /**
     * 查询租客的所有订单（分页，按创建时间降序）
     *
     * @param tenantId 租客用户 ID
     * @param page     当前页码
     * @param size     每页大小
     * @return 分页订单列表
     */
    @Override
    public PageResult<Order> listTenantOrders(Long tenantId, int page, int size) {
        Page<Order> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getTenantId, tenantId);
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> result = orderMapper.selectPage(pageObj, wrapper);
        // 批量回填合同状态、房源信息与评价标记，避免前端为每条订单额外请求详情接口。
        result.getRecords().forEach(order -> {
            fillContractPaymentAbility(order);
            fillOrderHouse(order);
            fillOrderReviewFlag(order);
        });
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    /**
     * 查询房东的所有订单（分页，按创建时间降序）
     *
     * @param landlordId 房东用户 ID
     * @param page       当前页码
     * @param size       每页大小
     * @return 分页订单列表
     */
    @Override
    public PageResult<Order> listLandlordOrders(Long landlordId, int page, int size) {
        Page<Order> pageObj = new Page<>(page, size);

        // 兼容历史数据：landlord_id 直接命中，或订单房源归属当前房东（JOIN houses.owner_id）
        Page<Order> result = orderMapper.selectLandlordOrdersPage(pageObj, landlordId);
        result.getRecords().forEach(order -> {
            fillContractPaymentAbility(order);
            if (order.getTenantId() != null) {
                User tenant = userMapper.selectById(order.getTenantId());
                if (tenant != null) {
                    tenant.setPassword(null);
                    tenant.setIdCard(null);
                    order.setTenant(tenant);
                }
            }
            fillOrderHouse(order);
            // 房东列表也回填 reviewed 字段，保持订单返回结构一致，避免前端空字段分支判断。
            fillOrderReviewFlag(order);
        });
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    /**
     * 将订单状态标记为已完成
     *
     * @param orderId 订单 ID
     */
    @Override
    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        order.setStatus("COMPLETED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 租客支付订单：
     * 1) 校验订单存在与租客身份；
     * 2) 校验订单必须处于 APPROVED 且未支付；
     * 3) 校验合同必须双方已签（FULLY_SIGNED）；
     * 4) 更新订单状态与支付状态，并通知双方。
     */
    @Override
    @Transactional
    public void payOrder(Long orderId, Long tenantId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new BusinessException(400, "仅已批准订单可支付");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "订单已支付，请勿重复支付");
        }
        if ("REFUNDED".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "订单已退款，无法再次支付");
        }
        Contract contract = findLatestContractByOrderId(order.getId());
        if (contract == null || !"FULLY_SIGNED".equals(contract.getStatus())) {
            throw new BusinessException(400, "合同双方签署完成后方可支付");
        }
        order.setPaymentStatus("PAID");
        order.setStatus("COMPLETED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        messageProducer.sendOrderStatusChange(order.getTenantId(), "订单支付成功，订单已完成");
        messageProducer.sendOrderStatusChange(order.getLandlordId(), "租客已完成支付，订单已完成");
    }

    /**
     * 租客退款订单：
     * 1) 校验订单存在与租客身份；
     * 2) 仅允许对已支付订单发起退款；
     * 3) 退款后订单状态标记为已取消，支付状态标记为已退款；
     * 4) 同步发送双方消息通知。
     */
    @Override
    @Transactional
    public void refundOrder(Long orderId, Long tenantId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "仅已支付订单可退款");
        }
        order.setPaymentStatus("REFUNDED");
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        messageProducer.sendOrderStatusChange(order.getTenantId(), "订单退款成功，订单已取消");
        messageProducer.sendOrderStatusChange(order.getLandlordId(), "租客已退款，订单已取消");
    }

    /**
     * 租客评价已完成订单，并根据评分调整房东信用分：
     * <3 星扣 10 分；3 星 +3；4 星 +4；5 星 +5。
     */
    @Override
    @Transactional
    public void reviewOrder(Long orderId, Long tenantId, OrderReviewRequest request) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException(400, "仅已完成订单可评价");
        }
        if (request == null || request.getRating() == null) {
            throw new BusinessException(400, "评分不能为空");
        }
        int rating = request.getRating();
        if (rating < REVIEW_MIN_RATING || rating > REVIEW_MAX_RATING) {
            throw new BusinessException(400, "评分必须在1到5之间");
        }

        LambdaQueryWrapper<Review> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(Review::getOrderId, orderId).eq(Review::getUserId, tenantId);
        if (reviewMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(400, "该订单已评价，请勿重复提交");
        }

        Review review = new Review();
        review.setHouseId(order.getHouseId());
        review.setOrderId(orderId);
        review.setUserId(tenantId);
        review.setRating(rating);
        review.setContent(request.getContent());
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        User landlord = userMapper.selectById(order.getLandlordId());
        if (landlord != null) {
            int currentScore = landlord.getCreditScore() == null ? DEFAULT_CREDIT_SCORE : landlord.getCreditScore();
            int delta;
            if (rating < REVIEW_MIDDLE_RATING) {
                delta = CREDIT_DELTA_LOW_RATING;
            } else if (rating == REVIEW_MIDDLE_RATING) {
                delta = CREDIT_DELTA_THREE_STARS;
            } else if (rating == REVIEW_GOOD_RATING) {
                delta = CREDIT_DELTA_FOUR_STARS;
            } else {
                delta = CREDIT_DELTA_FIVE_STARS;
            }
            landlord.setCreditScore(currentScore + delta);
            landlord.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(landlord);
        }
        // 评价属于关键闭环事件：通知房东“收到评价”、通知租客“提交成功”，便于双方在消息中心追踪。
        messageProducer.sendOrderStatusChange(order.getLandlordId(), String.format(REVIEW_NOTIFY_TO_LANDLORD_TEMPLATE, rating));
        messageProducer.sendOrderStatusChange(order.getTenantId(), REVIEW_NOTIFY_TO_TENANT);
    }

    /**
     * 查询租客自己提交过的评价记录（分页）。
     */
    @Override
    public PageResult<ReviewRecordResponse> listTenantReviewRecords(Long tenantId, int page, int size) {
        Page<Review> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, tenantId);
        wrapper.orderByDesc(Review::getCreateTime);
        Page<Review> result = reviewMapper.selectPage(pageObj, wrapper);
        List<ReviewRecordResponse> records = toReviewRecordResponses(result.getRecords());
        return PageResult.of(result.getTotal(), records, page, size);
    }

    /**
     * 查询房东收到的评价记录（分页）：
     * 按“房源归属房东”作为主口径查询，确保房东能看到自己房源上的评价。
     *
     * 背景说明：
     * - 评价数据本身只存了 houseId / orderId / tenantId；
     * - 本次业务要求明确为“根据房源 id 关联房源表再查房东 id”，
     *   因此 Mapper 中将 houses.owner_id 设为主过滤条件；
     * - 同时保留 orders.landlord_id 兼容口径，防止历史数据字段差异导致漏数。
     */
    @Override
    public PageResult<ReviewRecordResponse> listLandlordReviewRecords(Long landlordId, int page, int size) {
        Page<Review> pageObj = new Page<>(page, size);
        Page<Review> result = reviewMapper.selectLandlordReviewPage(pageObj, landlordId);
        List<ReviewRecordResponse> records = toReviewRecordResponses(result.getRecords());
        return PageResult.of(result.getTotal(), records, page, size);
    }

    /**
     * 回填订单的合同状态与可支付标记，供前端“待支付/退款”按钮与支付状态展示使用。
     */
    private void fillContractPaymentAbility(Order order) {
        Contract contract = findLatestContractByOrderId(order.getId());
        String contractStatus = contract != null ? contract.getStatus() : null;
        order.setContractStatus(contractStatus);
        order.setCanPay("FULLY_SIGNED".equals(contractStatus));
        // 同步回填合同主键信息，前端可在订单列表/详情直接跳转到对应合同，避免额外查询。
        order.setContractId(contract != null ? contract.getId() : null);
        order.setContractNo(contract != null ? contract.getContractNo() : null);
    }

    /**
     * 按订单 ID 查询用于支付判断的合同：
     * 正常业务下同一订单只应存在一份合同；若历史上出现多份合同，
     * 统一按 create_time 最新的一份作为“当前有效合同”参与支付资格判断，
     * 以保证前后端状态判断口径一致并避免读取过期合同状态。
     */
    private Contract findLatestContractByOrderId(Long orderId) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getOrderId, orderId);
        wrapper.orderByDesc(Contract::getCreateTime);
        wrapper.last("LIMIT 1");
        return contractMapper.selectOne(wrapper);
    }

    /**
     * 回填订单房源对象，保证订单列表可以直接展示“房源标题”而不是仅显示房源 ID。
     */
    private void fillOrderHouse(Order order) {
        if (order.getHouseId() == null) {
            return;
        }
        order.setHouse(houseMapper.selectById(order.getHouseId()));
    }

    /**
     * 回填订单评价标记：
     * - 仅当订单已完成且当前订单租客已提交评价时标记为 true；
     * - 其他状态统一为 false，前端据此决定是否展示“去评价”按钮。
     */
    private void fillOrderReviewFlag(Order order) {
        if (!"COMPLETED".equals(order.getStatus()) || order.getTenantId() == null) {
            order.setReviewed(false);
            return;
        }
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getOrderId, order.getId()).eq(Review::getUserId, order.getTenantId());
        wrapper.last("LIMIT 1");
        order.setReviewed(reviewMapper.selectOne(wrapper) != null);
    }

    /**
     * Review -> ReviewRecordResponse 组装：
     * 补齐房源标题、租客名、房东名，前端可直接渲染“评价管理”列表。
     */
    private List<ReviewRecordResponse> toReviewRecordResponses(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return List.of();
        }

        Set<Long> houseIds = reviews.stream()
                .map(Review::getHouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 为评价列表补齐 order_no：reviews 仅存 orderId，需要批量回查 orders 获取业务单号。
        Set<Long> orderIds = reviews.stream()
                .map(Review::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, House> houseMap = new HashMap<>();
        if (!houseIds.isEmpty()) {
            houseMap = houseMapper.selectBatchIds(houseIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(House::getId, house -> house));
        }
        Map<Long, Order> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orderMap = orderMapper.selectBatchIds(orderIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Order::getId, order -> order));
        }

        Set<Long> tenantIds = reviews.stream()
                .map(Review::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> landlordIds = houseMap.values().stream()
                .map(House::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = java.util.stream.Stream.concat(tenantIds.stream(), landlordIds.stream())
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(User::getId, user -> user));
        }

        Map<Long, House> finalHouseMap = houseMap;
        Map<Long, Order> finalOrderMap = orderMap;
        Map<Long, User> finalUserMap = userMap;
        return reviews.stream().map(review -> {
            ReviewRecordResponse response = new ReviewRecordResponse();
            response.setId(review.getId());
            response.setOrderId(review.getOrderId());
            // 评价管理优先展示 order_no；若历史数据缺失该字段，前端仍可回退展示 orderId。
            Order order = review.getOrderId() != null ? finalOrderMap.get(review.getOrderId()) : null;
            if (order != null) {
                response.setOrderNo(order.getOrderNo());
            }
            response.setHouseId(review.getHouseId());
            response.setTenantId(review.getUserId());
            response.setRating(review.getRating());
            response.setContent(review.getContent());
            response.setCreateTime(review.getCreateTime());

            House house = review.getHouseId() != null ? finalHouseMap.get(review.getHouseId()) : null;
            if (house != null) {
                response.setHouseTitle(house.getTitle());
                response.setLandlordId(house.getOwnerId());
                User landlord = finalUserMap.get(house.getOwnerId());
                if (landlord != null) {
                    response.setLandlordName(landlord.getRealName() != null ? landlord.getRealName() : landlord.getUsername());
                }
            }

            User tenant = review.getUserId() != null ? finalUserMap.get(review.getUserId()) : null;
            if (tenant != null) {
                response.setTenantName(tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername());
            }
            return response;
        }).toList();
    }

    /**
     * 生成唯一订单编号
     * 格式：{前缀}{yyyyMMddHHmmss}{4位随机数}
     *
     * @param prefix 订单类型前缀（INT/APT）
     * @return 生成的订单编号字符串
     */
    private String generateOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + timestamp + random;
    }

    private static DefaultRedisScript<Long> buildIncrWithExpireOneDayScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local cnt = redis.call('INCR', KEYS[1])
                if cnt == 1 then
                  redis.call('EXPIRE', KEYS[1], 86400)
                end
                return cnt
                """);
        script.setResultType(Long.class);
        return script;
    }
}
