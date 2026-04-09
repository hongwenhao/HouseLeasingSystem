package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.utils.OrderStatusUtil;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.dto.OrderReviewRequest;
import com.houseleasing.dto.ReviewRecordResponse;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.Review;
import com.houseleasing.entity.User;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.UserBehavior;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.ReviewMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.UserBehaviorMapper;
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
import java.math.RoundingMode;
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
 * @author hongwenhao
 * @description 实现订单相关的所有业务逻辑，包括创建意向订单和预约订单、
 *              订单审批、取消、完成等操作，通过 RabbitMQ 发送状态变更通知
 */
@Slf4j
@Service // 声明为订单业务服务
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService { // 订单主流程实现：创建、审批、支付、退款、评价

    private final OrderMapper orderMapper; // 订单表访问组件
    private final HouseMapper houseMapper; // 房源表访问组件
    private final UserMapper userMapper; // 用户表访问组件
    private final ReviewMapper reviewMapper; // 评价表访问组件
    private final ContractMapper contractMapper; // 合同表访问组件
    private final UserBehaviorMapper userBehaviorMapper; // 用户行为表访问组件
    private final MessageProducer messageProducer; // MQ 消息发送组件
    private final MessageService messageService; // 站内消息服务
    private final RedisTemplate<String, Object> redisTemplate; // Redis 组件（计数与去重）
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_ONE_DAY_SCRIPT = buildIncrWithExpireOneDayScript(); // Redis 原子脚本：递增并设置 1 天过期
    private static final long CANCEL_COUNT_DEDUCT_THRESHOLD = 11L; // 超过该取消次数后触发信用分扣减
    private static final String TENANT_CANCEL_SELF_MESSAGE = "您已取消该预约订单"; // 租客取消后发给租客自己的通知文案
    private static final String TENANT_CANCEL_NOTIFY_LANDLORD_MESSAGE = "租客已取消预约订单"; // 租客取消后发给房东的通知文案
    private static final String LANDLORD_CANCEL_SELF_MESSAGE = "您已取消该预约订单"; // 房东取消后发给房东自己的通知文案
    private static final String LANDLORD_CANCEL_NOTIFY_TENANT_MESSAGE = "房东已取消预约订单"; // 房东取消后发给租客的通知文案
    private static final int REVIEW_MIN_RATING = 1; // 评价最小星级
    private static final int REVIEW_MAX_RATING = 5; // 评价最大星级
    private static final int REVIEW_MIDDLE_RATING = 3; // 中评分界线（3星）
    private static final int REVIEW_GOOD_RATING = 4; // 好评分界线（4星）
    private static final int CREDIT_DELTA_LOW_RATING = -10; // 低分评价触发的信用分扣减值
    private static final int CREDIT_DELTA_THREE_STARS = 3; // 3 星评价对应信用分加分值
    private static final int CREDIT_DELTA_FOUR_STARS = 4; // 4 星评价对应信用分加分值
    private static final int CREDIT_DELTA_FIVE_STARS = 5; // 5 星评价对应信用分加分值
    private static final int DEFAULT_CREDIT_SCORE = 100; // 用户信用分兜底默认值
    private static final String REVIEW_NOTIFY_TO_LANDLORD_TEMPLATE = "租客已完成评价：%d星"; // 通知房东评价结果的模板
    private static final String REVIEW_NOTIFY_TO_TENANT = "您的评价已提交，感谢反馈"; // 通知租客评价提交成功的文案
    /** 订单行为埋点类型（仅保留 VIEW/COLLECT/ORDER）。 */
    private static final String BEHAVIOR_ORDER = "ORDER";
    /** ORDER 行为推荐权重：下单行为代表强意向。 */
    private static final BigDecimal BEHAVIOR_ORDER_SCORE = new BigDecimal("5.0");

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
    public Order createIntent(Long tenantId, Long houseId, String remark) { // 创建意向订单
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
        // 统一落库订单总金额，避免 total_amount 长期为空导致支付/对账口径分裂。
        order.setTotalAmount(calculateOrderTotalAmount(house.getPrice(), depositAmount));
        order.setRemark(remark);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        // 通知房东有新意向订单，关联当前订单 ID 方便房东在消息中心直接跳转查看
        messageProducer.sendOrderStatusChange(house.getOwnerId(), "新的意向订单", order.getId());
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
    public Order createAppointment(OrderCreateRequest request, Long tenantId) { // 创建预约看房订单
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
        // 与意向订单保持一致：创建时即写入 total_amount（默认=月租+押金）。
        order.setTotalAmount(calculateOrderTotalAmount(house.getPrice(), depositAmount));
        order.setRemark(request.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        // 通知租客预约已提交，通知房东有新预约订单，均关联订单 ID 方便跳转
        messageProducer.sendAppointmentConfirmation(tenantId, house.getTitle(), order.getId());
        messageProducer.sendOrderStatusChange(house.getOwnerId(), "新的预约订单", order.getId());
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
    public void approveOrder(Long orderId, boolean approved, Long landlordId) { // 房东审批订单
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
        // 通知租客审批结果，关联订单 ID 方便跳转
        messageProducer.sendOrderStatusChange(order.getTenantId(),
                approved ? "您的订单已被批准" : "您的订单已被拒绝", order.getId());
    }

    /**
     * 取消订单，验证操作人是否为订单的租客或房东
     *
     * @param orderId 订单 ID
     * @param userId  操作人用户 ID
     */
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) { // 取消订单并处理信用分与通知
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
        // 关键一致性规则（本次需求）：
        // 当租客或房东取消订单时，订单绑定的“当前生效合同”也必须同步置为 CANCELLED，
        // 避免出现“订单已取消但合同仍可继续签署/展示为有效”的状态分裂。
        // 与订单更新处于同一事务中，任一步骤失败都会整体回滚，保证跨表状态原子性。
        cancelLatestContractForOrder(orderId);
        // 订单取消属于关键业务事件：同时通知租客与房东，确保双方都能第一时间在消息中心看到状态变化。
        // 这里统一复用“订单状态通知”渠道，消息内容根据操作人身份区分，便于双方理解取消原因。
        // 关联 orderId 方便接收方在消息中心直接跳转到对应订单详情页
        if (tenantCancelling) {
            messageProducer.sendOrderStatusChange(order.getTenantId(), TENANT_CANCEL_SELF_MESSAGE, orderId);
            messageProducer.sendOrderStatusChange(order.getLandlordId(), TENANT_CANCEL_NOTIFY_LANDLORD_MESSAGE, orderId);
        } else {
            messageProducer.sendOrderStatusChange(order.getLandlordId(), LANDLORD_CANCEL_SELF_MESSAGE, orderId);
            messageProducer.sendOrderStatusChange(order.getTenantId(), LANDLORD_CANCEL_NOTIFY_TENANT_MESSAGE, orderId);
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
    public Order getOrderById(Long id) { // 查询订单详情
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
    public PageResult<Order> listTenantOrders(Long tenantId, int page, int size) { // 分页查询租客订单
        Page<Order> pageObj = new Page<>(page, size); // 创建分页对象，定义当前页和每页条数
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>(); // 构建“只查当前租客”的查询条件
        wrapper.eq(Order::getTenantId, tenantId); // 过滤出该租客自己的订单，避免越权读取他人数据
        wrapper.orderByDesc(Order::getCreateTime); // 按创建时间倒序，确保最新订单优先展示
        Page<Order> result = orderMapper.selectPage(pageObj, wrapper); // 执行分页查询，拿到当前页订单记录
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
    public PageResult<Order> listLandlordOrders(Long landlordId, int page, int size) { // 分页查询房东订单
        Page<Order> pageObj = new Page<>(page, size); // 创建分页对象，控制房东订单列表分页范围

        // 兼容历史数据：landlord_id 直接命中，或订单房源归属当前房东（JOIN houses.owner_id）
        Page<Order> result = orderMapper.selectLandlordOrdersPage(pageObj, landlordId); // 查询房东可见订单（兼容新旧数据口径）
        result.getRecords().forEach(order -> {
            fillContractPaymentAbility(order); // 回填“是否允许支付”的合同能力标记，减少前端二次请求
            if (order.getTenantId() != null) {
                User tenant = userMapper.selectById(order.getTenantId()); // 按租客ID加载基础资料用于展示
                if (tenant != null) {
                    tenant.setPassword(null); // 清空密码字段，防止敏感信息进入接口响应
                    tenant.setIdCard(null); // 清空身份证字段，降低个人隐私泄露风险
                    order.setTenant(tenant); // 将脱敏后的租客信息挂载到订单对象
                }
            }
            fillOrderHouse(order); // 回填房源关键信息，方便列表直接显示房屋标题/封面等
            // 房东列表也回填 reviewed 字段，保持订单返回结构一致，避免前端空字段分支判断。
            fillOrderReviewFlag(order); // 回填评价状态，前端可据此展示“已评价/去评价”按钮
        });
        return PageResult.of(result.getTotal(), result.getRecords(), page, size); // 按统一分页结构返回给前端
    }

    /**
     * 将订单状态标记为已完成
     *
     * @param orderId 订单 ID
     */
    @Override
    @Transactional
    public void completeOrder(Long orderId) { // 手动标记订单完成
        Order order = orderMapper.selectById(orderId); // 先按ID读取订单，确认目标数据存在
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        order.setStatus("COMPLETED"); // 把业务状态改为“已完成”
        order.setUpdateTime(LocalDateTime.now()); // 记录本次状态变更时间，便于后续审计追踪
        orderMapper.updateById(order); // 将最新状态持久化到数据库
    }

    /**
     * 租客支付订单：
     * 1) 校验订单存在与租客身份；
     * 2) 校验订单必须处于 APPROVED（兼容历史）或 SIGNED（双方签约后）且未支付；
     * 3) 校验合同必须双方已签（FULLY_SIGNED）；
     * 4) 更新订单状态与支付状态，并通知双方。
     */
    @Override
    @Transactional
    public void payOrder(Long orderId, Long tenantId) { // 支付订单
        Order order = orderMapper.selectById(orderId); // 加载订单主记录，后续做权限与状态校验
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        // 支付入口兼容两种状态：
        // - APPROVED：历史流程中，签约前后可能未拆分状态；
        // - SIGNED：当前流程中，双方签约完成后会显式写入该状态。
        // 这样既满足“签约后订单=SIGNED”的新口径，也不破坏历史数据可支付能力。
        if (!OrderStatusUtil.isPayableStatus(order.getStatus())) {
            throw new BusinessException(400, "仅已确认或已签约订单可支付");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "订单已支付，请勿重复支付");
        }
        if ("REFUNDED".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "订单已退款，无法再次支付");
        }
        Contract contract = findLatestContractByOrderId(order.getId()); // 获取该订单最新合同，确认签约流程已经完成
        if (contract == null || !"FULLY_SIGNED".equals(contract.getStatus())) {
            throw new BusinessException(400, "合同双方签署完成后方可支付");
        }
        order.setPaymentStatus("PAID"); // 标记支付状态为“已支付”
        order.setStatus("COMPLETED"); // 支付完成即闭环，订单业务状态同步置为“已完成”
        order.setUpdateTime(LocalDateTime.now()); // 更新时间戳，确保时间线反映最新处理动作
        orderMapper.updateById(order); // 落库保存支付后的订单状态
        messageProducer.sendOrderStatusChange(order.getTenantId(), "订单支付成功，订单已完成", order.getId()); // 通知租客支付成功
        messageProducer.sendOrderStatusChange(order.getLandlordId(), "租客已完成支付，订单已完成", order.getId()); // 通知房东订单已收款完成
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
    public void refundOrder(Long orderId, Long tenantId) { // 退款订单
        Order order = orderMapper.selectById(orderId); // 加载订单，确认退款目标存在且可操作
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "仅已支付订单可退款");
        }
        order.setPaymentStatus("REFUNDED"); // 支付状态改为“已退款”，防止后续重复支付或重复退款
        order.setStatus("CANCELLED"); // 退款后交易闭环终止，订单状态同步改为“已取消”
        order.setUpdateTime(LocalDateTime.now()); // 记录退款处理时间点
        orderMapper.updateById(order); // 把退款结果写回数据库
        // 退款即撤销交易闭环（本次需求）：
        // 租客退款后，订单状态已变为 CANCELLED，对应合同也必须联动取消，
        // 防止合同仍停留在 FULLY_SIGNED 等可执行状态，造成前后端口径不一致。
        // 这里复用统一的“按订单定位最新合同并取消”逻辑，保持取消链路行为一致。
        cancelLatestContractForOrder(orderId); // 联动取消该订单最新合同，避免合同状态与订单状态脱节
        messageProducer.sendOrderStatusChange(order.getTenantId(), "订单退款成功，订单已取消", order.getId()); // 通知租客退款完成
        messageProducer.sendOrderStatusChange(order.getLandlordId(), "租客已退款，订单已取消", order.getId()); // 通知房东订单已退款取消
    }

    /**
     * 租客评价已完成订单，并根据评分调整房东信用分：
     * <3 星扣 10 分；3 星 +3；4 星 +4；5 星 +5。
     */
    @Override
    @Transactional
    public void reviewOrder(Long orderId, Long tenantId, OrderReviewRequest request) { // 提交订单评价并调整信用分
        Order order = orderMapper.selectById(orderId); // 查询订单，确认评价对象存在
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
        int rating = request.getRating(); // 取出评分值，后续用于范围校验与信用分增减
        if (rating < REVIEW_MIN_RATING || rating > REVIEW_MAX_RATING) {
            throw new BusinessException(400, "评分必须在1到5之间");
        }

        LambdaQueryWrapper<Review> existsWrapper = new LambdaQueryWrapper<>(); // 构建“该租客对该订单是否已评价”的查询条件
        existsWrapper.eq(Review::getOrderId, orderId).eq(Review::getUserId, tenantId); // 限定订单ID+用户ID组合，确保幂等校验准确
        if (reviewMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(400, "该订单已评价，请勿重复提交");
        }

        Review review = new Review(); // 创建评价实体，准备落库保存
        review.setHouseId(order.getHouseId()); // 记录关联房源，便于后续按房源汇总评分
        review.setOrderId(orderId); // 记录关联订单，保证评价与交易链路可追溯
        review.setUserId(tenantId); // 记录评价人，支持“我的评价”场景查询
        review.setRating(rating); // 保存星级评分（1-5）
        review.setContent(request.getContent()); // 保存评价文本内容，供前端展示
        review.setCreateTime(LocalDateTime.now()); // 记录评价提交时间
        reviewMapper.insert(review); // 将评价持久化到数据库
        // 评价成功后记录 ORDER 行为，确保推荐系统行为枚举口径一致。
        upsertOrderBehavior(tenantId, order.getHouseId()); // 更新用户行为轨迹，帮助推荐系统感知真实成交偏好

        User landlord = userMapper.selectById(order.getLandlordId()); // 读取房东信息，用于按评分调整信用分
        if (landlord != null) {
            int currentScore = landlord.getCreditScore() == null ? DEFAULT_CREDIT_SCORE : landlord.getCreditScore(); // 空值场景回退默认信用分
            int delta; // 记录本次评分对应的信用分变化量
            if (rating < REVIEW_MIDDLE_RATING) {
                delta = CREDIT_DELTA_LOW_RATING; // 低分评价触发扣分
            } else if (rating == REVIEW_MIDDLE_RATING) {
                delta = CREDIT_DELTA_THREE_STARS; // 3 星给小幅正向加分
            } else if (rating == REVIEW_GOOD_RATING) {
                delta = CREDIT_DELTA_FOUR_STARS; // 4 星给中等正向加分
            } else {
                delta = CREDIT_DELTA_FIVE_STARS; // 5 星给最高档正向加分
            }
            landlord.setCreditScore(currentScore + delta);
            landlord.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(landlord);
        }
        // 评价属于关键闭环事件：通知房东“收到评价”、通知租客“提交成功”，便于双方在消息中心追踪。
        messageProducer.sendOrderStatusChange(order.getLandlordId(), String.format(REVIEW_NOTIFY_TO_LANDLORD_TEMPLATE, rating), order.getId());
        messageProducer.sendOrderStatusChange(order.getTenantId(), REVIEW_NOTIFY_TO_TENANT, order.getId());
    }

    /**
     * 查询租客自己提交过的评价记录（分页）。
     */
    @Override
    public PageResult<ReviewRecordResponse> listTenantReviewRecords(Long tenantId, int page, int size) { // 分页查询租客评价记录
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
    public PageResult<ReviewRecordResponse> listLandlordReviewRecords(Long landlordId, int page, int size) { // 分页查询房东收到的评价
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
     * 将订单关联的“最新合同”状态同步取消（若存在且尚未取消）。
     *
     * 说明：
     * 1) 统一服务于“租客/房东取消订单”与“租客退款”两类入口，避免多处复制状态联动代码；
     * 2) 仅处理最新合同，和订单详情页展示口径保持一致，规避历史多合同数据导致的歧义；
     * 3) 已是 CANCELLED 时直接返回，保证幂等；
     * 4) 本方法不抛业务异常（除数据库异常外），让上层事务可按真实失败原因回滚。
     */
    private void cancelLatestContractForOrder(Long orderId) {
        Contract contract = findLatestContractByOrderId(orderId);
        if (contract == null || "CANCELLED".equals(contract.getStatus())) {
            return;
        }
        contract.setStatus("CANCELLED");
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(contract);
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

    /**
     * 计算订单总金额：
     * - 当前业务口径为“首期应付 = 月租 + 押金”；
     * - 空值按 0 处理，避免空指针；
     * - 保留两位小数，保证与支付网关金额口径一致。
     */
    private BigDecimal calculateOrderTotalAmount(BigDecimal monthlyRent, BigDecimal deposit) {
        BigDecimal safeMonthlyRent = monthlyRent == null ? BigDecimal.ZERO : monthlyRent;
        BigDecimal safeDeposit = deposit == null ? BigDecimal.ZERO : deposit;
        return safeMonthlyRent.add(safeDeposit).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 记录下单行为（幂等更新）：
     * - 若该用户-房源已存在 ORDER 行为，则仅刷新时间和分值；
     * - 否则新增一条 ORDER 行为记录。
     *
     * 实现说明：复用 create_time 作为“最近一次下单行为时间”字段，
     * 便于推荐系统按最近行为排序；在当前数据结构下不新增额外字段。
     */
    private void upsertOrderBehavior(Long userId, Long houseId) {
        if (userId == null || houseId == null) {
            return;
        }
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getHouseId, houseId)
                .eq(UserBehavior::getBehaviorType, BEHAVIOR_ORDER)
                .last("LIMIT 1");
        UserBehavior existing = userBehaviorMapper.selectOne(wrapper);
        if (existing == null) {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setHouseId(houseId);
            behavior.setBehaviorType(BEHAVIOR_ORDER);
            behavior.setScore(BEHAVIOR_ORDER_SCORE);
            behavior.setCreateTime(LocalDateTime.now());
            userBehaviorMapper.insert(behavior);
            return;
        }
        // 若已有 ORDER 行为，刷新时间和分值即可，避免产生重复行为记录。
        existing.setScore(BEHAVIOR_ORDER_SCORE);
        existing.setCreateTime(LocalDateTime.now());
        userBehaviorMapper.updateById(existing);
    }

    //redis限流一天
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
