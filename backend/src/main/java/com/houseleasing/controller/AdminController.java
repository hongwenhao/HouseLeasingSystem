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
import com.houseleasing.service.HouseService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final UserService userService;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;
    private final OrderMapper orderMapper;
    private final ContractMapper contractMapper;

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
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        int finalSize = size != null ? size : (pageSize != null ? pageSize : 10);
        return Result.success(userService.listUsers(page, finalSize, keyword));
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
        // 追加关联信息，便于管理后台直接展示房源标题、租客/房东用户名等
        records.forEach(order -> {
            House house = houseMapper.selectById(order.getHouseId());
            User tenant = userMapper.selectById(order.getTenantId());
            User landlord = userMapper.selectById(order.getLandlordId());
            order.setHouse(house);
            order.setTenant(sanitizeUser(tenant));
            order.setLandlord(sanitizeUser(landlord));
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
        // 追加关联信息，便于后台合同管理页直观展示业务上下文
        records.forEach(contract -> {
            House house = houseMapper.selectById(contract.getHouseId());
            User tenant = userMapper.selectById(contract.getTenantId());
            User landlord = userMapper.selectById(contract.getLandlordId());
            contract.setHouse(house);
            contract.setTenant(sanitizeUser(tenant));
            contract.setLandlord(sanitizeUser(landlord));
            Order order = orderMapper.selectById(contract.getOrderId());
            if (order != null) {
                contract.setOrderNo(order.getOrderNo());
            }
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
                .last("LIMIT 12");
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
                .last("LIMIT 6");
        List<Map<String, Object>> raw = houseMapper.selectMaps(wrapper);
        List<Map<String, Object>> formatted = new ArrayList<>();
        raw.stream()
                .sorted(Comparator.comparing(map -> YearMonth.parse(String.valueOf(map.get("month")), DateTimeFormatter.ofPattern("yyyy-MM"))))
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
        buckets.put("90-100(优秀)", 0L);
        buckets.put("70-89(良好)", 0L);
        buckets.put("60-69(一般)", 0L);
        buckets.put("60以下(较低)", 0L);
        List<User> users = userMapper.selectList(null);
        for (User user : users) {
            int score = user.getCreditScore() == null ? 0 : user.getCreditScore();
            if (score >= 90) {
                buckets.put("90-100(优秀)", buckets.get("90-100(优秀)") + 1);
            } else if (score >= 70) {
                buckets.put("70-89(良好)", buckets.get("70-89(良好)") + 1);
            } else if (score >= 60) {
                buckets.put("60-69(一般)", buckets.get("60-69(一般)") + 1);
            } else {
                buckets.put("60以下(较低)", buckets.get("60以下(较低)") + 1);
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
}
