package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.OrderCreateRequest;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.MessageService;
import com.houseleasing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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
    private final MessageProducer messageProducer;
    private final MessageService messageService;

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

        // 仅当“租客本人”取消时触发信用分惩罚逻辑：
        // 1) 先在“更新订单状态前”统计当日该租客对该房源已取消次数（不含本次）
        // 2) 本次取消提交后，若累计次数将超过 10 次（即之前已 >=10 次），则扣减 10 分
        // 这样与“同一天取消同个房源预约次数超过10次”规则严格一致。
        boolean tenantCancelling = Objects.equals(userId, order.getTenantId());
        boolean shouldDeductCredit = false;
        if (tenantCancelling) {
            LocalDate today = LocalDate.now();
            LocalDateTime dayStart = today.atStartOfDay();
            LocalDateTime nextDayStart = today.plusDays(1).atStartOfDay();
            Integer cancelledCountBefore = orderMapper.countTenantHouseCancelledInRange(
                    order.getTenantId(), order.getHouseId(), dayStart, nextDayStart);
            int safeCountBefore = cancelledCountBefore == null ? 0 : cancelledCountBefore;
            shouldDeductCredit = safeCountBefore >= 10;
        }

        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

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
            if (order.getTenantId() != null) {
                User tenant = userMapper.selectById(order.getTenantId());
                if (tenant != null) {
                    tenant.setPassword(null);
                    tenant.setIdCard(null);
                    order.setTenant(tenant);
                }
            }
            if (order.getHouseId() != null) {
                order.setHouse(houseMapper.selectById(order.getHouseId()));
            }
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
}
