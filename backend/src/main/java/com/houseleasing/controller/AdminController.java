package com.houseleasing.controller;

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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台管理控制器
 *
 * @author HouseLeasingSystem开发团队
 * @description 提供系统管理员专用的后台管理 REST API，包括用户管理、房源审核、
 * 订单查看、合同查看和系统统计，所有接口仅限 ADMIN 角色访问
 */
@Tag(name = "Admin", description = "Admin management endpoints")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')") // 仅允许管理员角色访问此控制器的所有接口
public class AdminController {
    private static final int AREA_STATS_LIMIT = 12;
    private static final int PRICE_TRENDS_LIMIT = 6;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String HOUSE_ACTION_ONLINE = "上架";
    private static final String HOUSE_ACTION_OFFLINE = "下架";
    private static final String OWNER_HOUSE_ONLINE_MESSAGE = "管理员已将您的房源上架，可正常展示给租客。";
    private static final String OWNER_HOUSE_OFFLINE_MESSAGE = "管理员已将您的房源下架，暂不可展示给租客。";
    private static final String TENANT_HOUSE_OFFLINE_MESSAGE_TEMPLATE = "管理员已将您关注/下单的房源《%s》下架。";

    private static final String CREDIT_RANGE_EXCELLENT = "90-100(优秀)";
    private static final String CREDIT_RANGE_GOOD = "70-89(良好)";
    private static final String CREDIT_RANGE_NORMAL = "60-69(一般)";
    private static final String CREDIT_RANGE_LOW = "60以下(较低)";

    private static final String CREDIT_RANGE_CASE_SQL =
            "CASE " +
                    "WHEN credit_score >= 90 THEN '" + CREDIT_RANGE_EXCELLENT + "' " +
                    "WHEN credit_score >= 70 THEN '" + CREDIT_RANGE_GOOD + "' " +
                    "WHEN credit_score >= 60 THEN '" + CREDIT_RANGE_NORMAL + "' " +
                    "ELSE '" + CREDIT_RANGE_LOW + "' END";

    private final UserService userService;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;
    private final OrderMapper orderMapper;
    private final ContractMapper contractMapper;
    private final MessageProducer messageProducer;

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
        return Result.success(userService.listUsers(page, size, keyword));
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
        userService.banUser(id);
        return Result.success();
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
        userService.unbanUser(id);
        return Result.success();
    }

    /**
     * 查询系统所有订单列表（分页）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @return 所有订单的分页列表
     */
    @Operation(summary = "List all orders")
    @GetMapping("/orders")
    public Result<PageResult<Order>> listAllOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> result = orderMapper.selectPage(pageObj, wrapper);
        List<Order> records = result.getRecords();
        // 批量查询关联信息，避免 N+1 查询
        Map<Long, House> houseMap = mapById(houseMapper.selectBatchIds(records.stream()
                .map(Order::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())), House::getId);
        Set<Long> userIds = new HashSet<>();
        records.forEach(order -> {
            if (order.getTenantId() != null) userIds.add(order.getTenantId());
            if (order.getLandlordId() != null) userIds.add(order.getLandlordId());
        });
        Map<Long, User> userMap = mapById(userMapper.selectBatchIds(userIds), User::getId);
        records.forEach(order -> {
            order.setHouse(houseMap.get(order.getHouseId()));
            order.setTenant(sanitizeUser(userMap.get(order.getTenantId())));
            order.setLandlord(sanitizeUser(userMap.get(order.getLandlordId())));
        });
        return Result.success(PageResult.of(result.getTotal(), records, page, size));
    }

    /**
     * 查询系统所有合同列表（分页）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @return 所有合同的分页列表
     */
    @Operation(summary = "List all contracts")
    @GetMapping("/contracts")
    public Result<PageResult<Contract>> listAllContracts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(Contract::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> result = contractMapper.selectPage(pageObj, wrapper);
        List<Contract> records = result.getRecords();
        // 批量查询关联信息，避免 N+1 查询
        Map<Long, House> houseMap = mapById(houseMapper.selectBatchIds(records.stream()
                .map(Contract::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())), House::getId);
        Set<Long> userIds = new HashSet<>();
        records.forEach(contract -> {
            if (contract.getTenantId() != null) userIds.add(contract.getTenantId());
            if (contract.getLandlordId() != null) userIds.add(contract.getLandlordId());
        });
        Map<Long, User> userMap = mapById(userMapper.selectBatchIds(userIds), User::getId);
        Map<Long, Order> orderMap = mapById(orderMapper.selectBatchIds(records.stream()
                .map(Contract::getOrderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet())), Order::getId);
        records.forEach(contract -> {
            contract.setHouse(houseMap.get(contract.getHouseId()));
            contract.setTenant(sanitizeUser(userMap.get(contract.getTenantId())));
            contract.setLandlord(sanitizeUser(userMap.get(contract.getLandlordId())));
            Order order = orderMap.get(contract.getOrderId());
            if (order != null) contract.setOrderNo(order.getOrderNo());
        });
        return Result.success(PageResult.of(result.getTotal(), records, page, size));
    }

    /**
     * 获取系统统计数据（用户总数、房源总数、订单总数、合同总数）
     *
     * @return 包含各项统计数据的 Map
     */
    @Operation(summary = "Get system statistics")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));     // 用户总数
        stats.put("houseCount", houseMapper.selectCount(null));   // 房源总数
        stats.put("orderCount", orderMapper.selectCount(null));   // 订单总数
        // 合同统计：总数及待签署/审核数量
        stats.put("contractCount", contractMapper.selectCount(null)); // 合同总数
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> pendingWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        pendingWrapper.eq(Contract::getStatus, "PENDING_SIGN");
        stats.put("pendingContracts", contractMapper.selectCount(pendingWrapper)); // 待审核合同数
        return Result.success(stats);
    }

    /**
     * 获取系统统计数据（与 /statistics 保持同口径，供前端 /admin/stats 直连使用）。
     *
     * @return 包含用户、房源、合同等统计字段
     */
    @Operation(summary = "Get dashboard stats")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return getStatistics();
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
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("city AS city", "COUNT(*) AS count")
                .isNotNull("city")
                .ne("city", "")
                .groupBy("city")
                .orderByDesc("count")
                .last("LIMIT " + AREA_STATS_LIMIT);
        return Result.success(houseMapper.selectMaps(wrapper));
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
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("DATE_FORMAT(create_time, '%Y-%m') AS month", "ROUND(AVG(price), 2) AS avgPrice")
                .groupBy("DATE_FORMAT(create_time, '%Y-%m')")
                .orderByDesc("month")
                .last("LIMIT " + PRICE_TRENDS_LIMIT);
        List<Map<String, Object>> raw = houseMapper.selectMaps(wrapper);
        List<Map<String, Object>> formatted = new ArrayList<>();
        raw.stream()
                .sorted(Comparator.comparing(map -> YearMonth.parse(String.valueOf(map.get("month")), MONTH_FORMATTER)))
                .forEach(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", item.get("month"));
                    row.put("avgPrice", parseDecimal(item.get("avgPrice")));
                    formatted.add(row);
                });
        return Result.success(formatted);
    }

    /**
     * 获取信用分分布（用于管理后台饼图）。
     *
     * @return [{"range":"90-100(优秀)","count":5}, ...]
     */
    @Operation(summary = "Get credit distribution stats")
    @GetMapping("/stats/credit")
    public Result<List<Map<String, Object>>> getCreditDistribution() {
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put(CREDIT_RANGE_EXCELLENT, 0L);
        buckets.put(CREDIT_RANGE_GOOD, 0L);
        buckets.put(CREDIT_RANGE_NORMAL, 0L);
        buckets.put(CREDIT_RANGE_LOW, 0L);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select(CREDIT_RANGE_CASE_SQL + " AS score_range", "COUNT(*) AS count")
                .groupBy(CREDIT_RANGE_CASE_SQL);
        List<Map<String, Object>> aggregated = userMapper.selectMaps(wrapper);
        for (Map<String, Object> item : aggregated) {
            String range = String.valueOf(item.get("score_range"));
            long count = toLong(item.get("count"));
            if (buckets.containsKey(range)) {
                buckets.put(range, count);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        buckets.forEach((range, count) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("range", range);
            row.put("count", count);
            result.add(row);
        });
        return Result.success(result);
    }

    /**
     * 查询待审核房源（当前实现口径：OFFLINE 视作待审核/未上线）。
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
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(House::getStatus, "OFFLINE");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getCity, keyword)
                    .or().like(House::getAddress, keyword));
        }
        wrapper.orderByDesc(House::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result = houseMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size));
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
            @RequestParam(required = false) String keyword) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getCity, keyword)
                    .or().like(House::getAddress, keyword));
        }
        wrapper.orderByDesc(House::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result = houseMapper.selectPage(pageObj, wrapper);
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size));
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
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        return Result.success(house);
    }

    /**
     * 管理员审核房源上线状态：
     * status=APPROVED/ONLINE 视为通过并上线；status=REJECTED/OFFLINE 视为驳回并保持下线。
     *
     * @param id   房源 ID
     * @param body 审核请求体，示例：{"status":"APPROVED","reason":"..."}
     * @return 操作成功
     */
    @Operation(summary = "Audit house")
    @PutMapping("/houses/{id}/audit")
    public Result<Void> auditHouse(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        String status = body == null ? null : String.valueOf(body.getOrDefault("status", ""));
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(400, "审核状态不能为空");
        }
        boolean approved = "APPROVED".equalsIgnoreCase(status) || "ONLINE".equalsIgnoreCase(status);
        house.setStatus(approved ? "ONLINE" : "OFFLINE");
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        // 审核动作后通知房东，便于房东及时感知房源状态变化
        sendHouseManagementNotificationToOwner(house, approved ? HOUSE_ACTION_ONLINE : HOUSE_ACTION_OFFLINE,
                approved ? OWNER_HOUSE_ONLINE_MESSAGE : OWNER_HOUSE_OFFLINE_MESSAGE);
        return Result.success();
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
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        house.setStatus("ONLINE");
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        sendHouseManagementNotificationToOwner(house, HOUSE_ACTION_ONLINE, OWNER_HOUSE_ONLINE_MESSAGE);
        return Result.success();
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
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        house.setStatus("OFFLINE");
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        sendHouseManagementNotificationToOwner(house, HOUSE_ACTION_OFFLINE, OWNER_HOUSE_OFFLINE_MESSAGE);
        // 对该房源产生过订单的租客推送提醒，避免租客继续对不可用房源发起意向
        notifyTenantsOfHouseOffline(house, String.format(TENANT_HOUSE_OFFLINE_MESSAGE_TEMPLATE, safeHouseTitle(house)));
        return Result.success();
    }

    /**
     * 返回脱敏后的用户信息，避免在后台管理列表中透出密码等敏感字段。
     */
    private User sanitizeUser(User source) {
        if (source == null) {
            return null;
        }
        User user = new User();
        user.setId(source.getId());
        user.setUsername(source.getUsername());
        user.setPhone(source.getPhone());
        user.setEmail(source.getEmail());
        user.setRole(source.getRole());
        user.setRealName(source.getRealName());
        user.setCreditScore(source.getCreditScore());
        user.setIsRealNameAuth(source.getIsRealNameAuth());
        user.setStatus(source.getStatus());
        user.setAvatar(source.getAvatar());
        return user;
    }

    /**
     * 将统计查询返回值安全地转换为 BigDecimal。
     */
    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 将对象转换为 long（兼容 Number 与字符串形式）。
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return 0L;
        }
    }

    /**
     * 将对象列表按主键映射为 Map，便于 O(1) 关联访问。
     */
    private <T> Map<Long, T> mapById(List<T> list, Function<T, Long> idGetter) {
        if (list == null || list.isEmpty()) {
            return Map.of();
        }
        return list.stream()
                .filter(item -> item != null && idGetter.apply(item) != null)
                .collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
    }

    /**
     * 给房源所属房东发送“管理员房源管理”通知。
     */
    private void sendHouseManagementNotificationToOwner(House house, String actionLabel, String actionMessage) {
        if (house == null || house.getOwnerId() == null) {
            return;
        }
        String content = String.format("您的房源《%s》%s", safeHouseTitle(house), actionMessage);
        messageProducer.sendAdminHouseManagementNotification(house.getOwnerId(), actionLabel, content);
    }

    /**
     * 根据房源关联订单，通知历史租客房源状态变化。
     * 这里只对存在订单关系的租客推送，避免给全站租客广播造成干扰。
     */
    private void notifyTenantsOfHouseOffline(House house, String content) {
        if (house == null || house.getId() == null) {
            return;
        }
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Order::getHouseId, house.getId())
                .select(Order::getTenantId);
        List<Order> orders = orderMapper.selectList(wrapper);
        Set<Long> tenantIds = orders.stream()
                .map(Order::getTenantId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        tenantIds.forEach(tenantId ->
                messageProducer.sendAdminHouseManagementNotification(tenantId, HOUSE_ACTION_OFFLINE, "房源状态变更提醒：" + content));
    }

    /**
     * 安全返回房源标题，避免空值拼接时出现“null”字样。
     */
    private String safeHouseTitle(House house) {
        if (house == null || !StringUtils.hasText(house.getTitle())) {
            return "未命名房源";
        }
        return house.getTitle();
    }
}
