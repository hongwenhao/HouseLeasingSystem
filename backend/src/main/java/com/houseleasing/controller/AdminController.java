package com.houseleasing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.House;
import com.houseleasing.entity.Order;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.mq.MessageProducer;
import com.houseleasing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台管理控制器
 *
 * @author hongwenhao
 * @description 提供系统管理员专用的后台管理 REST API，包括用户管理、订单查看、合同查看和系统统计，所有接口仅限 ADMIN 角色访问
 */
@Tag(name = "Admin", description = "Admin management endpoints")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final int AREA_STATS_LIMIT = 12; // 区域统计最多返回 12 个城市，避免图表过长
    private static final int PRICE_TRENDS_LIMIT = 6; // 租金趋势最多取最近 6 个月
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM"); // 月份字符串解析格式
    private static final String HOUSE_ACTION_ONLINE = "上架"; // 房源上架动作文案
    private static final String HOUSE_ACTION_OFFLINE = "下架"; // 房源下架动作文案
    private static final String OWNER_HOUSE_ONLINE_MESSAGE = "管理员已将您的房源上架，可正常展示给租客。"; // 发给房东的上架通知正文
    private static final String OWNER_HOUSE_OFFLINE_MESSAGE = "管理员已将您的房源下架，暂不可展示给租客。"; // 发给房东的下架通知正文
    private static final String TENANT_HOUSE_OFFLINE_MESSAGE_TEMPLATE = "管理员已将您关注/下单的房源《%s》下架。"; // 发给租客的下架通知模板

    private static final String CREDIT_RANGE_EXCELLENT = "90-200(优秀)"; // 信用分优秀区间标签
    private static final String CREDIT_RANGE_GOOD = "70-89(良好)"; // 信用分良好区间标签
    private static final String CREDIT_RANGE_NORMAL = "60-69(一般)"; // 信用分一般区间标签
    private static final String CREDIT_RANGE_LOW = "60以下(较低)"; // 信用分较低区间标签
    /**
     * 后台订单状态白名单（统一使用大写枚举值）：
     * 当 keyword 命中该集合时，按状态精确过滤；否则按订单号模糊过滤。
     */
    private static final Set<String> ORDER_STATUS_KEYWORDS = Set.of( // 订单状态合法值集合（用于筛选入参校验）
            "PENDING", "APPROVED", "SIGNED", "REJECTED", "CANCELLED", "COMPLETED"
    );
    /**
     * 后台合同状态白名单（统一使用大写枚举值），用于状态下拉框后端兜底校验。
     */
    private static final Set<String> CONTRACT_STATUS_KEYWORDS = Set.of( // 合同状态合法值集合（防止非法状态入参）
            "DRAFT", "PENDING_SIGN", "TENANT_SIGNED", "LANDLORD_SIGNED", "FULLY_SIGNED", "CANCELLED"
    );
    /**
     * 后台房源状态白名单：
     * 仅允许按 ONLINE/OFFLINE 进行筛选，管理端统一为“已上架/已下架”两种状态。
     */
    private static final Set<String> HOUSE_STATUS_KEYWORDS = Set.of("ONLINE", "OFFLINE"); // 管理端房源筛选只允许上架/下架

    private static final String CREDIT_RANGE_CASE_SQL = // 统计信用分分布时复用的 SQL CASE 表达式
            "CASE " +
                    "WHEN credit_score >= 90 THEN '" + CREDIT_RANGE_EXCELLENT + "' " +
                    "WHEN credit_score >= 70 THEN '" + CREDIT_RANGE_GOOD + "' " +
                    "WHEN credit_score >= 60 THEN '" + CREDIT_RANGE_NORMAL + "' " +
                    "ELSE '" + CREDIT_RANGE_LOW + "' END";

    private final UserService userService; // 用户业务服务（封装用户相关核心逻辑）
    private final UserMapper userMapper; // 用户表数据访问对象
    private final HouseMapper houseMapper; // 房源表数据访问对象
    private final OrderMapper orderMapper; // 订单表数据访问对象
    private final ContractMapper contractMapper; // 合同表数据访问对象
    private final MessageProducer messageProducer; // 消息发送器（用于站内通知/消息队列）

    /**
     * 查询系统所有用户列表（支持关键词搜索）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词（可选）
     * @return 分页用户列表
     */
    @Operation(summary = "List all users")
    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, keyword)); // 调用服务层查询用户分页数据并统一包装成功响应
    }

    /**
     * 封禁指定用户账号
     *
     * @param id 要封禁的用户 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Ban user")
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        userService.banUser(id); // 执行封禁逻辑（更新用户状态）
        return Result.success(); // 返回统一成功结果
    }

    /**
     * 解封指定用户账号
     *
     * @param id 要解封的用户 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Unban user")
    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id); // 执行解封逻辑（恢复用户状态）
        return Result.success(); // 返回统一成功结果
    }

    /**
     * 查询系统所有订单列表（分页）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词（可选，支持订单号与订单状态）
     * @return 所有订单的分页列表
     */
    @Operation(summary = "List all orders")
    @GetMapping("/orders")
    public Result<PageResult<Order>> listAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 组装分页对象（当前页 + 每页条数）
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建订单查询条件
        boolean hasExplicitStatusFilter = StringUtils.hasText(status); // 是否传了 status 参数（优先使用显式状态筛选）
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim(); // 去掉关键词首尾空格，避免误匹配
            String normalizedStatusKeyword = trimmedKeyword.toUpperCase(Locale.ROOT); // 转大写，统一与状态白名单比较
            if (!hasExplicitStatusFilter && ORDER_STATUS_KEYWORDS.contains(normalizedStatusKeyword)) {
                wrapper.eq(Order::getStatus, normalizedStatusKeyword); // 关键词命中状态枚举时，按状态精确筛选
            } else {
                wrapper.like(Order::getOrderNo, trimmedKeyword); // 否则按订单号模糊查询
            }
        }
        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT); // 标准化状态入参（去空格+转大写）
            if (ORDER_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(Order::getStatus, normalizedStatus); // 仅白名单内状态才生效，防止非法入参污染查询
            }
        }
        wrapper.orderByDesc(Order::getCreateTime); // 按创建时间倒序，最新订单优先展示
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> result = orderMapper.selectPage(pageObj, wrapper); // 执行分页查询
        List<Order> records = result.getRecords(); // 取出当前页订单列表

        Set<Long> houseIds = records.stream()
                .map(Order::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()); // 收集本页订单关联的房源 ID（去重）
        Map<Long, House> houseMap = houseIds.isEmpty()
                ? Map.of()
                : mapById(houseMapper.selectBatchIds(houseIds), House::getId); // 批量查房源并转成 id->house 映射
        Set<Long> userIds = new HashSet<>();
        records.forEach(order -> {
            if (order.getTenantId() != null) userIds.add(order.getTenantId()); // 收集租客 ID
            if (order.getLandlordId() != null) userIds.add(order.getLandlordId()); // 收集房东 ID
        });
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : mapById(userMapper.selectBatchIds(userIds), User::getId); // 批量查用户并转成 id->user 映射
        records.forEach(order -> {
            order.setHouse(houseMap.get(order.getHouseId())); // 给订单填充房源详情
            order.setTenant(sanitizeUser(userMap.get(order.getTenantId()))); // 给订单填充并脱敏租客信息
            order.setLandlord(sanitizeUser(userMap.get(order.getLandlordId()))); // 给订单填充并脱敏房东信息
        });
        return Result.success(PageResult.of(result.getTotal(), records, page, size)); // 返回统一分页结构
    }

    /**
     * 查询系统所有合同列表（分页）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词（可选，匹配合同编号/关联订单号）
     * @param status  状态筛选（可选，命中白名单时生效）
     * @return 所有合同的分页列表
     */
    @Operation(summary = "List all contracts")
    @GetMapping("/contracts")
    public Result<PageResult<Contract>> listAllContracts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 组装合同分页对象
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建合同查询条件

        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim(); // 清理关键词首尾空格
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> orderNoWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建“订单号匹配”辅助查询条件
            orderNoWrapper.like(Order::getOrderNo, trimmedKeyword).select(Order::getId); // 只查匹配关键词的订单 ID
            List<Order> matchedOrders = orderMapper.selectList(orderNoWrapper); // 查询命中的订单
            Set<Long> matchedOrderIds = matchedOrders.stream()
                    .map(Order::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet()); // 提取命中订单 ID 集合
            wrapper.and(w -> {
                w.like(Contract::getContractNo, trimmedKeyword); // 条件1：合同编号模糊匹配关键词
                if (!matchedOrderIds.isEmpty()) {
                    w.or().in(Contract::getOrderId, matchedOrderIds); // 条件2：关联订单号命中关键词的合同
                }
            });
        }
        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT); // 规范化状态筛选参数
            if (CONTRACT_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(Contract::getStatus, normalizedStatus); // 命中白名单才按状态精确过滤
            }
        }
        wrapper.orderByDesc(Contract::getCreateTime); // 按创建时间倒序显示合同
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> result = contractMapper.selectPage(pageObj, wrapper); // 执行合同分页查询
        List<Contract> records = result.getRecords(); // 提取当前页合同记录

        Set<Long> houseIds = records.stream()
                .map(Contract::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()); // 收集本页合同关联的房源 ID
        Map<Long, House> houseMap = houseIds.isEmpty()
                ? Map.of()
                : mapById(houseMapper.selectBatchIds(houseIds), House::getId); // 批量查询房源并转映射
        Set<Long> userIds = new HashSet<>();
        records.forEach(contract -> {
            if (contract.getTenantId() != null) userIds.add(contract.getTenantId()); // 收集租客 ID
            if (contract.getLandlordId() != null) userIds.add(contract.getLandlordId()); // 收集房东 ID
        });
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : mapById(userMapper.selectBatchIds(userIds), User::getId); // 批量查用户并转映射
        Set<Long> orderIds = records.stream()
                .map(Contract::getOrderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()); // 收集本页合同关联订单 ID
        Map<Long, Order> orderMap = orderIds.isEmpty()
                ? Map.of()
                : mapById(orderMapper.selectBatchIds(orderIds), Order::getId); // 批量查订单并转映射
        records.forEach(contract -> {
            contract.setHouse(houseMap.get(contract.getHouseId())); // 回填合同房源信息
            contract.setTenant(sanitizeUser(userMap.get(contract.getTenantId()))); // 回填并脱敏租客信息
            contract.setLandlord(sanitizeUser(userMap.get(contract.getLandlordId()))); // 回填并脱敏房东信息
            Order order = orderMap.get(contract.getOrderId()); // 查当前合同关联订单
            if (order != null) contract.setOrderNo(order.getOrderNo()); // 回填订单号便于前端展示
        });
        return Result.success(PageResult.of(result.getTotal(), records, page, size)); // 返回合同分页结果
    }

    /**
     * 管理员取消订单（管理兜底能力）：
     * - 仅允许将“未取消”订单更新为 CANCELLED，避免重复写入；
     * - 管理端取消不依赖租客/房东身份校验，适用于人工客服介入场景；
     * - 保持幂等：订单已是 CANCELLED 时直接返回成功。
     */
    @Operation(summary = "Cancel order by admin")
    @PutMapping("/orders/{id}/cancel")
    @Transactional
    public Result<Void> cancelOrderByAdmin(@PathVariable Long id) {
        Order order = orderMapper.selectById(id); // 根据订单 ID 查询订单
        if (order == null) {
            throw new BusinessException(404, "订单不存在"); // 订单不存在直接报错
        }
        if ("CANCELLED".equals(order.getStatus())) {
            return Result.success(); // 已取消时直接成功返回（幂等）
        }

        Contract latestContract = findCancellableLatestContractForOrderByAdmin(order); // 检查并获取可联动取消的最新合同
        order.setStatus("CANCELLED"); // 订单状态更新为已取消
        order.setUpdateTime(LocalDateTime.now()); // 记录订单更新时间
        orderMapper.updateById(order); // 持久化订单状态变更
        if (latestContract != null) {
            latestContract.setStatus("CANCELLED"); // 联动取消最新合同
            latestContract.setUpdateTime(LocalDateTime.now()); // 记录合同更新时间
            contractMapper.updateById(latestContract); // 持久化合同状态变更
            notifyContractCancelledByAdmin(latestContract); // 通知合同双方“管理员已取消”
        }
        notifyOrderCancelledByAdmin(order); // 通知订单双方“管理员已取消”
        return Result.success(); // 返回取消成功
    }

    /**
     * 管理员取消合同（管理兜底能力）：
     * - 仅允许取消未“双方已签(FULLY_SIGNED)”的合同；
     * - 合同取消后将房源状态恢复为 ONLINE，保持与业务取消链路一致；
     * - 已取消合同重复操作时直接返回成功（幂等）。
     */
    @Operation(summary = "Cancel contract by admin")
    @PutMapping("/contracts/{id}/cancel")
    @Transactional
    public Result<Void> cancelContractByAdmin(@PathVariable Long id) {
        Contract contract = contractMapper.selectById(id); // 根据合同 ID 查询合同
        if (contract == null) {
            throw new BusinessException(404, "合同不存在"); // 合同不存在直接报错
        }
        if ("CANCELLED".equals(contract.getStatus())) {
            return Result.success(); // 已取消时直接成功返回（幂等）
        }
        if ("FULLY_SIGNED".equals(contract.getStatus())) {
            throw new BusinessException("已签署的合同不可取消"); // 已完成签署的合同按规则不允许取消
        }
        contract.setStatus("CANCELLED"); // 合同状态改为已取消
        contract.setUpdateTime(LocalDateTime.now()); // 记录合同更新时间
        contractMapper.updateById(contract); // 落库保存合同状态
        cancelRelatedOrderForContractByAdmin(contract); // 联动取消其关联订单（若可取消）

        notifyContractCancelledByAdmin(contract); // 通知合同双方“管理员取消合同”
        House house = houseMapper.selectById(contract.getHouseId()); // 查询合同对应房源
        if (house != null) {
            house.setStatus("ONLINE"); // 合同取消后恢复房源为上架状态
            house.setUpdateTime(LocalDateTime.now()); // 记录房源更新时间
            houseMapper.updateById(house); // 保存房源状态变更
        }
        return Result.success(); // 返回取消成功
    }

    /**
     * 管理员取消订单前，校验该订单关联的最新合同是否允许被联动取消。
     * 设计要点：
     * 1) 只处理“最新合同”，与订单详情页展示口径保持一致；
     * 2) 若最新合同已取消，直接忽略，保证幂等；
     * 3) 若最新合同已双方签署（FULLY_SIGNED），按业务规则禁止取消，
     * 抛出异常终止本次管理员取消订单操作，避免出现“订单已取消、合同未取消”的不一致状态。
     *
     * @return 可被联动取消的最新合同；若无需处理返回 null
     */
    private Contract findCancellableLatestContractForOrderByAdmin(Order order) {
        Contract latestContract = findLatestContractByOrderId(order.getId()); // 查询该订单关联的最新合同
        if (latestContract == null || "CANCELLED".equals(latestContract.getStatus())) {
            return null; // 没合同或已取消则无需联动处理
        }
        if ("FULLY_SIGNED".equals(latestContract.getStatus())) {
            String contractIdentifier = StringUtils.hasText(latestContract.getContractNo())
                    ? latestContract.getContractNo()
                    : safeIdentifier(latestContract.getId()); // 编号为空时用主键兜底显示
            throw new BusinessException(String.format("合同[%s]已签署，不可取消", contractIdentifier)); // 已签署合同禁止取消，阻断本次操作
        }
        return latestContract; // 返回可被联动取消的最新合同
    }

    /**
     * 管理员取消合同时，联动取消其对应订单（若存在且未取消）。
     * 设计要点：
     * 1) 订单不存在时直接忽略，兼容历史脏数据；
     * 2) 订单已取消时直接返回，保证幂等；
     * 3) 与主取消动作运行在同一事务中，确保“合同/订单”状态要么同时成功，要么同时回滚。
     */
    private void cancelRelatedOrderForContractByAdmin(Contract contract) {
        if (contract.getOrderId() == null) {
            return; // 合同未绑定订单则无需处理
        }
        Order relatedOrder = orderMapper.selectById(contract.getOrderId()); // 查询关联订单
        if (relatedOrder == null || "CANCELLED".equals(relatedOrder.getStatus())) {
            return; // 订单不存在或已取消时直接返回
        }
        relatedOrder.setStatus("CANCELLED"); // 联动将订单改为已取消
        relatedOrder.setUpdateTime(LocalDateTime.now()); // 写入订单更新时间
        orderMapper.updateById(relatedOrder); // 保存订单状态更新
        notifyOrderCancelledByAdmin(relatedOrder); // 通知订单双方
    }

    /**
     * 查询订单关联的最新合同（按创建时间倒序）。
     * 若无合同记录，返回 null。
     */
    private Contract findLatestContractByOrderId(Long orderId) {
        if (orderId == null) {
            return null; // 没有订单 ID 时无法查询合同
        }
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>(); // 构建合同查询条件
        wrapper.eq(Contract::getOrderId, orderId)
                .orderByDesc(Contract::getCreateTime)
                .last("LIMIT 1"); // 只取最新一条合同记录
        return contractMapper.selectOne(wrapper); // 执行查询并返回
    }

    /**
     * 获取系统统计数据（用户总数、房源总数、订单总数、合同总数）
     *
     * @return 包含各项统计数据的 Map
     */
    @Operation(summary = "Get system statistics")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>(); // 创建统计结果容器
        stats.put("userCount", userMapper.selectCount(null)); // 统计用户总数
        stats.put("houseCount", houseMapper.selectCount(null)); // 统计房源总数
        stats.put("orderCount", orderMapper.selectCount(null)); // 统计订单总数

        stats.put("contractCount", contractMapper.selectCount(null)); // 统计合同总数
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> pendingWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建“待签署合同”查询条件
        pendingWrapper.eq(Contract::getStatus, "PENDING_SIGN"); // 只统计待签署状态
        stats.put("pendingContracts", contractMapper.selectCount(pendingWrapper)); // 写入待签署合同数量
        return Result.success(stats); // 返回统计结果
    }

    /**
     * 获取系统统计数据（与 /statistics 保持同口径，供前端 /admin/stats 直连使用）。
     *
     * @return 包含用户、房源、合同等统计字段
     */
    @Operation(summary = "Get dashboard stats")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return getStatistics(); // 复用同一套统计逻辑，避免重复代码
    }

    /**
     * 获取各城市房源数量统计（用于管理后台柱状图）。
     *
     * @return [{"city":"北京","count":12}, ...]
     */
    @Operation(summary = "Get area stats")
    @GetMapping("/stats/area")
    public Result<List<Map<String, Object>>> getAreaStats() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(); // 构建按城市聚合的查询
        wrapper.select("city AS city", "COUNT(*) AS count")
                .isNotNull("city")
                .ne("city", "")
                .groupBy("city")
                .orderByDesc("count")
                .last("LIMIT " + AREA_STATS_LIMIT); // 限制返回城市数，提升性能和展示效果
        return Result.success(houseMapper.selectMaps(wrapper)); // 返回每个城市的房源数量
    }

    /**
     * 获取近 6 个月租金均价趋势（用于管理后台折线图）。
     *
     * @return [{"month":"2026-01","avgPrice":3500.00}, ...]
     */
    @Operation(summary = "Get price trend stats")
    @GetMapping("/stats/price-trends")
    public Result<List<Map<String, Object>>> getPriceTrends() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(); // 构建按月份统计均价的查询
        wrapper.select("DATE_FORMAT(create_time, '%Y-%m') AS month", "ROUND(AVG(price), 2) AS avgPrice")
                .groupBy("DATE_FORMAT(create_time, '%Y-%m')")
                .orderByDesc("month")
                .last("LIMIT " + PRICE_TRENDS_LIMIT); // 只取最近 N 个月
        List<Map<String, Object>> raw = houseMapper.selectMaps(wrapper); // 读取数据库原始统计结果
        List<Map<String, Object>> formatted = new ArrayList<>(); // 组装前端更稳定的输出结构
        raw.stream()
                .sorted(Comparator.comparing(map -> YearMonth.parse(String.valueOf(map.get("month")), MONTH_FORMATTER)))
                .forEach(item -> {
                    Map<String, Object> row = new LinkedHashMap<>(); // 每个月生成一行结果
                    row.put("month", item.get("month")); // 月份字段
                    row.put("avgPrice", parseDecimal(item.get("avgPrice"))); // 均价字段（安全转数字）
                    formatted.add(row); // 加入最终列表
                });
        return Result.success(formatted); // 返回按时间正序的趋势数据
    }

    /**
     * 获取信用分分布（用于管理后台饼图）。
     *
     * @return [{"range":"90-200(优秀)","count":5}, ...]
     */
    @Operation(summary = "Get credit distribution stats")
    @GetMapping("/stats/credit")
    public Result<List<Map<String, Object>>> getCreditDistribution() {
        Map<String, Long> buckets = new LinkedHashMap<>(); // 先按固定区间初始化结果，保证前端总能拿到完整分类
        buckets.put(CREDIT_RANGE_EXCELLENT, 0L); // 优秀区间默认 0
        buckets.put(CREDIT_RANGE_GOOD, 0L); // 良好区间默认 0
        buckets.put(CREDIT_RANGE_NORMAL, 0L); // 一般区间默认 0
        buckets.put(CREDIT_RANGE_LOW, 0L); // 较低区间默认 0

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(); // 构建信用分分桶统计查询
        wrapper.select(CREDIT_RANGE_CASE_SQL + " AS score_range", "COUNT(*) AS count")
                .groupBy(CREDIT_RANGE_CASE_SQL); // 依据 CASE 语句按区间分组
        List<Map<String, Object>> aggregated = userMapper.selectMaps(wrapper); // 查询数据库聚合结果
        for (Map<String, Object> item : aggregated) {
            String range = String.valueOf(item.get("score_range")); // 读取区间名
            long count = toLong(item.get("count")); // 读取人数并安全转 long
            if (buckets.containsKey(range)) {
                buckets.put(range, count); // 覆盖对应区间统计值
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(); // 转换为前端约定结构
        buckets.forEach((range, count) -> {
            Map<String, Object> row = new LinkedHashMap<>(); // 每个区间生成一条记录
            row.put("range", range); // 区间名称
            row.put("count", count); // 区间人数
            result.add(row); // 放入返回列表
        });
        return Result.success(result); // 返回信用分分布
    }

    /**
     * 查询未上架房源（当前实现口径：OFFLINE未上架）。
     *
     * @param page    当前页码
     * @param size    每页数量
     * @param keyword 关键词（匹配标题/地址/城市）
     * @return 待审核房源分页数据
     */
    @Operation(summary = "List pending houses")
    @GetMapping("/houses/pending")
    public Result<PageResult<House>> listPendingHouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 创建分页参数对象
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建房源筛选条件
        wrapper.eq(House::getStatus, "OFFLINE"); // 仅查询当前为下架状态的房源
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getCity, keyword)
                    .or().like(House::getAddress, keyword)); // 关键词匹配标题/城市/地址
        }
        wrapper.orderByDesc(House::getCreateTime); // 按发布时间倒序
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result = houseMapper.selectPage(pageObj, wrapper); // 执行分页查询
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size)); // 返回统一分页结果
    }

    /**
     * 管理员房源管理：分页查询全部房源（支持关键词），用于“房源管理”页。
     *
     * @param page    当前页码
     * @param size    每页条数
     * @param keyword 关键词（可匹配标题、城市、地址）
     * @return 全量房源分页列表
     */
    @Operation(summary = "List all houses for admin management")
    @GetMapping("/houses")
    public Result<PageResult<House>> listAllHouses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 创建房源分页对象
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建房源查询条件
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getCity, keyword)
                    .or().like(House::getAddress, keyword)); // 按标题/城市/地址做模糊匹配
        }

        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT); // 标准化状态筛选参数
            if (HOUSE_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(House::getStatus, normalizedStatus); // 白名单内状态才用于过滤
            }
        }
        wrapper.orderByDesc(House::getCreateTime); // 按创建时间倒序显示
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result = houseMapper.selectPage(pageObj, wrapper); // 执行分页查询
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size)); // 返回全量房源分页数据
    }

    /**
     * 管理员房源管理：查询单个房源详情。
     *
     * @param id 房源 ID
     * @return 房源详情
     */
    @Operation(summary = "Get house detail for admin management")
    @GetMapping("/houses/{id}")
    public Result<House> getHouseDetailForAdmin(@PathVariable Long id) {
        House house = houseMapper.selectById(id); // 按主键查询房源详情
        if (house == null) {
            throw new BusinessException(404, "房源不存在"); // 未找到则返回 404 业务异常
        }
        return Result.success(house); // 返回房源详情
    }

    /**
     * 管理员审核房源上线状态：
     * status=APPROVED/ONLINE 视为通过并上线；
     * status=REJECTED/OFFLINE 统一视为下线（落库为 OFFLINE），
     * 与管理端“仅上架/下架两种状态”的展示口径保持一致。
     *
     * @param id   房源 ID
     * @param body 审核请求体，示例：{"status":"APPROVED","reason":"..."}
     * @return 操作成功
     */
    @Operation(summary = "Audit house")
    @PutMapping("/houses/{id}/audit")
    public Result<Void> auditHouse(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, Object> body) {
        House house = houseMapper.selectById(id); // 查询要审核的房源
        if (house == null) {
            throw new BusinessException(404, "房源不存在"); // 房源不存在直接报错
        }
        String status = body == null ? null : String.valueOf(body.getOrDefault("status", "")); // 从请求体读取审核状态
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(400, "审核状态不能为空"); // 状态为空不允许提交
        }
        boolean approved = "APPROVED".equalsIgnoreCase(status) || "ONLINE".equalsIgnoreCase(status); // 通过状态统一识别

        house.setStatus(approved ? "ONLINE" : "OFFLINE"); // 审核通过=上架，不通过=下架
        house.setUpdateTime(LocalDateTime.now()); // 更新修改时间
        houseMapper.updateById(house); // 保存审核结果

        sendHouseManagementNotificationToOwner(house, approved ? HOUSE_ACTION_ONLINE : HOUSE_ACTION_OFFLINE,
                approved ? OWNER_HOUSE_ONLINE_MESSAGE : OWNER_HOUSE_OFFLINE_MESSAGE); // 向房东发送审核结果通知
        return Result.success(); // 返回审核成功
    }

    /**
     * 管理员房源管理：上架房源（将状态置为 ONLINE），并推送通知给房东与相关租客。
     *
     * @param id 房源 ID
     * @return 操作成功
     */
    @Operation(summary = "Put house online by admin")
    @PutMapping("/houses/{id}/online")
    public Result<Void> putHouseOnline(@PathVariable Long id) {
        House house = houseMapper.selectById(id); // 查询目标房源
        if (house == null) {
            throw new BusinessException(404, "房源不存在"); // 房源不存在则报错
        }
        house.setStatus("ONLINE"); // 强制上架房源
        house.setUpdateTime(LocalDateTime.now()); // 更新时间戳
        houseMapper.updateById(house); // 落库房源状态
        sendHouseManagementNotificationToOwner(house, HOUSE_ACTION_ONLINE, OWNER_HOUSE_ONLINE_MESSAGE); // 通知房东已上架
        return Result.success(); // 返回成功
    }

    /**
     * 管理员房源管理：下架房源（将状态置为 OFFLINE），并推送通知给房东与已关联租客。
     *
     * @param id 房源 ID
     * @return 操作成功
     */
    @Operation(summary = "Put house offline by admin")
    @PutMapping("/houses/{id}/offline")
    public Result<Void> putHouseOffline(@PathVariable Long id) {
        House house = houseMapper.selectById(id); // 查询目标房源
        if (house == null) {
            throw new BusinessException(404, "房源不存在"); // 房源不存在则报错
        }
        house.setStatus("OFFLINE"); // 强制下架房源
        house.setUpdateTime(LocalDateTime.now()); // 更新时间戳
        houseMapper.updateById(house); // 落库房源状态
        sendHouseManagementNotificationToOwner(house, HOUSE_ACTION_OFFLINE, OWNER_HOUSE_OFFLINE_MESSAGE); // 通知房东已下架

        notifyTenantsOfHouseOffline(house, String.format(TENANT_HOUSE_OFFLINE_MESSAGE_TEMPLATE, safeHouseTitle(house))); // 通知相关租客房源下架
        return Result.success(); // 返回成功
    }

    /**
     * 返回脱敏后的用户信息，避免在后台管理列表中透出密码等敏感字段。
     */
    private User sanitizeUser(User source) {
        if (source == null) {
            return null; // 输入为空直接返回空，避免空指针
        }
        User user = new User(); // 新建一个干净对象，只复制允许暴露的字段
        user.setId(source.getId()); // 复制用户 ID
        user.setUsername(source.getUsername()); // 复制用户名
        user.setPhone(source.getPhone()); // 复制手机号
        user.setEmail(source.getEmail()); // 复制邮箱
        user.setRole(source.getRole()); // 复制角色
        user.setRealName(source.getRealName()); // 复制实名信息
        user.setCreditScore(source.getCreditScore()); // 复制信用分
        user.setIsRealNameAuth(source.getIsRealNameAuth()); // 复制实名认证状态
        user.setStatus(source.getStatus()); // 复制账号状态
        user.setAvatar(source.getAvatar()); // 复制头像
        return user; // 返回脱敏后的用户对象
    }

    /**
     * 将统计查询返回值安全地转换为 BigDecimal。
     */
    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO; // 空值统一按 0 处理
        }
        try {
            return new BigDecimal(String.valueOf(value)); // 尝试把统计值转成高精度数字
        } catch (Exception ignored) {
            return BigDecimal.ZERO; // 转换失败时兜底返回 0，防止接口报错
        }
    }

    /**
     * 将对象转换为 long（兼容 Number 与字符串形式）。
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0L; // 空值按 0 处理
        }
        if (value instanceof Number number) {
            return number.longValue(); // 数字类型直接取 long 值
        }
        try {
            return Long.parseLong(String.valueOf(value)); // 字符串类型尝试解析为 long
        } catch (Exception ignored) {
            return 0L; // 解析失败兜底 0
        }
    }

    /**
     * 将对象列表按主键映射为 Map，便于 O(1) 关联访问。
     */
    private <T> Map<Long, T> mapById(List<T> list, Function<T, Long> idGetter) {
        if (list == null || list.isEmpty()) {
            return Map.of(); // 空列表直接返回空映射
        }
        return list.stream()
                .filter(item -> item != null && idGetter.apply(item) != null)
                .collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a)); // 转成 id->对象，重复 key 保留第一条
    }

    /**
     * 给房源所属房东发送“管理员房源管理”通知。
     */
    private void sendHouseManagementNotificationToOwner(House house, String actionLabel, String actionMessage) {
        if (house == null || house.getOwnerId() == null) {
            return; // 没有房源或房东 ID 时不发送
        }
        String content = String.format("您的房源《%s》%s", safeHouseTitle(house), actionMessage); // 组装通知文案
        messageProducer.sendAdminHouseManagementNotification(house.getOwnerId(), actionLabel, content, house.getId()); // 发送给房东
    }

    /**
     * 根据房源关联订单，通知历史租客房源状态变化。
     * 这里只对存在订单关系的租客推送，避免给全站租客广播造成干扰。
     */
    private void notifyTenantsOfHouseOffline(House house, String content) {
        if (house == null || house.getId() == null) {
            return; // 房源信息不完整则无法通知
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 构建按房源查历史订单的条件
        wrapper.eq(Order::getHouseId, house.getId())
                .select(Order::getTenantId); // 只查租客 ID，减少无关字段开销
        List<Order> orders = orderMapper.selectList(wrapper); // 查询与该房源有关的订单
        Set<Long> tenantIds = orders.stream()
                .map(Order::getTenantId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()); // 提取并去重租客 ID
        tenantIds.forEach(tenantId ->
                messageProducer.sendAdminHouseManagementNotification(tenantId, HOUSE_ACTION_OFFLINE, "房源状态变更提醒：" + content, house.getId())); // 逐个租客推送下架通知
    }

    /**
     * 安全返回房源标题，避免空值拼接时出现“null”字样。
     */
    private String safeHouseTitle(House house) {
        if (house == null || !StringUtils.hasText(house.getTitle())) {
            return "未命名房源"; // 标题为空时给默认名，避免文案出现 null
        }
        return house.getTitle(); // 返回真实房源标题
    }

    /**
     * 管理员取消订单后，异步通知订单双方。
     * 文案明确“管理员已取消”，避免用户误判为交易对方主动取消。
     */
    private void notifyOrderCancelledByAdmin(Order order) {
        if (order == null) {
            return; // 订单为空时无需通知
        }
        String orderNo = StringUtils.hasText(order.getOrderNo()) ? order.getOrderNo() : safeIdentifier(order.getId()); // 优先用订单号，否则用主键兜底
        String message = String.format("管理员已取消订单（订单号：%s），如有疑问请联系平台客服。", orderNo); // 组装通知文案
        if (order.getTenantId() != null) {
            messageProducer.sendOrderStatusChange(order.getTenantId(), message, order.getId()); // 通知租客
        }
        if (order.getLandlordId() != null) {
            messageProducer.sendOrderStatusChange(order.getLandlordId(), message, order.getId()); // 通知房东
        }
    }

    /**
     * 管理员取消合同后，异步通知合同双方。
     * 与订单通知一致采用统一 MQ 通道，确保消息落库逻辑一致可观测。
     */
    private void notifyContractCancelledByAdmin(Contract contract) {
        if (contract == null) {
            return; // 合同为空时无需通知
        }
        String contractNo = StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : safeIdentifier(contract.getId()); // 优先合同编号，缺失时主键兜底
        String message = String.format("管理员已取消合同（合同编号：%s），如有疑问请联系平台客服。", contractNo); // 组装通知文案
        if (contract.getTenantId() != null) {
            messageProducer.sendContractStatusChange(contract.getTenantId(), message, contract.getId()); // 通知租客
        }
        if (contract.getLandlordId() != null) {
            messageProducer.sendContractStatusChange(contract.getLandlordId(), message, contract.getId()); // 通知房东
        }
    }

    /**
     * 统一的标识符兜底格式化：
     * 当编号为空时返回 N/A，避免消息文案出现“null”造成歧义。
     */
    private String safeIdentifier(Object id) {
        return id == null ? "N/A" : String.valueOf(id); // 统一空标识显示，避免消息出现 null
    }
}
