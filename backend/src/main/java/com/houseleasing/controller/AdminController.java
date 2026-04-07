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
    /**
     * 后台订单状态白名单（统一使用大写枚举值）：
     * 当 keyword 命中该集合时，按状态精确过滤；否则按订单号模糊过滤。
     */
    private static final Set<String> ORDER_STATUS_KEYWORDS = Set.of(
            "PENDING", "APPROVED", "SIGNED", "REJECTED", "CANCELLED", "COMPLETED"
    );
    /**
     * 后台合同状态白名单（统一使用大写枚举值），用于状态下拉框后端兜底校验。
     */
    private static final Set<String> CONTRACT_STATUS_KEYWORDS = Set.of(
            "DRAFT", "PENDING_SIGN", "TENANT_SIGNED", "LANDLORD_SIGNED", "FULLY_SIGNED", "CANCELLED"
    );
    /**
     * 后台房源状态白名单：
     * 仅允许按 ONLINE/OFFLINE 进行筛选，管理端统一为“已上架/已下架”两种状态。
     */
    private static final Set<String> HOUSE_STATUS_KEYWORDS = Set.of("ONLINE", "OFFLINE");

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
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        // 管理员订单检索增强：
        // 1) 若 keyword 命中状态枚举，则按状态精确匹配（大小写不敏感）；
        // 2) 否则按订单号模糊匹配，覆盖客服/工单按编号定位场景；
        // 3) 关键词为空时不加筛选条件，保持历史行为不变。
        boolean hasExplicitStatusFilter = StringUtils.hasText(status);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            String normalizedStatusKeyword = trimmedKeyword.toUpperCase(Locale.ROOT);
            // 兼容历史行为：仅当“未显式传入 status 下拉参数”时，才允许 keyword 触发状态过滤。
            // 若显式 status 已存在，则 keyword 一律按订单号处理，避免出现 status=APPROVED 且 keyword=CANCELLED 这种冲突条件。
            if (!hasExplicitStatusFilter && ORDER_STATUS_KEYWORDS.contains(normalizedStatusKeyword)) {
                wrapper.eq(Order::getStatus, normalizedStatusKeyword);
            } else {
                wrapper.like(Order::getOrderNo, trimmedKeyword);
            }
        }
        // 管理员新增状态下拉筛选：
        // status 非空且命中白名单时，额外按状态精确过滤。
        // 这样可与 keyword 同时生效（例如“订单号模糊 + 指定状态”）。
        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (ORDER_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(Order::getStatus, normalizedStatus);
            }
        }
        wrapper.orderByDesc(Order::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> result = orderMapper.selectPage(pageObj, wrapper);
        List<Order> records = result.getRecords();
        // 批量查询关联信息，避免 N+1 查询
        Set<Long> houseIds = records.stream()
                .map(Order::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, House> houseMap = houseIds.isEmpty()
                ? Map.of()
                : mapById(houseMapper.selectBatchIds(houseIds), House::getId);
        Set<Long> userIds = new HashSet<>();
        records.forEach(order -> {
            if (order.getTenantId() != null) userIds.add(order.getTenantId());
            if (order.getLandlordId() != null) userIds.add(order.getLandlordId());
        });
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : mapById(userMapper.selectBatchIds(userIds), User::getId);
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
     * @param keyword 搜索关键词（可选，匹配合同编号/关联订单号）
     * @param status 状态筛选（可选，命中白名单时生效）
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
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        // 合同搜索增强：
        // 1) keyword 支持匹配合同编号 contractNo；
        // 2) keyword 同时支持匹配关联订单号 orderNo（先查 orders，再转为 orderId 集合过滤）；
        // 3) status 下拉命中白名单时按状态精确过滤。
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> orderNoWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            orderNoWrapper.like(Order::getOrderNo, trimmedKeyword).select(Order::getId);
            List<Order> matchedOrders = orderMapper.selectList(orderNoWrapper);
            Set<Long> matchedOrderIds = matchedOrders.stream()
                    .map(Order::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            wrapper.and(w -> {
                w.like(Contract::getContractNo, trimmedKeyword);
                if (!matchedOrderIds.isEmpty()) {
                    w.or().in(Contract::getOrderId, matchedOrderIds);
                }
            });
        }
        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (CONTRACT_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(Contract::getStatus, normalizedStatus);
            }
        }
        wrapper.orderByDesc(Contract::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> result = contractMapper.selectPage(pageObj, wrapper);
        List<Contract> records = result.getRecords();
        // 批量查询关联信息，避免 N+1 查询
        Set<Long> houseIds = records.stream()
                .map(Contract::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, House> houseMap = houseIds.isEmpty()
                ? Map.of()
                : mapById(houseMapper.selectBatchIds(houseIds), House::getId);
        Set<Long> userIds = new HashSet<>();
        records.forEach(contract -> {
            if (contract.getTenantId() != null) userIds.add(contract.getTenantId());
            if (contract.getLandlordId() != null) userIds.add(contract.getLandlordId());
        });
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : mapById(userMapper.selectBatchIds(userIds), User::getId);
        Set<Long> orderIds = records.stream()
                .map(Contract::getOrderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Order> orderMap = orderIds.isEmpty()
                ? Map.of()
                : mapById(orderMapper.selectBatchIds(orderIds), Order::getId);
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
     * 管理员取消订单（管理兜底能力）：
     * - 仅允许将“未取消”订单更新为 CANCELLED，避免重复写入；
     * - 管理端取消不依赖租客/房东身份校验，适用于人工客服介入场景；
     * - 保持幂等：订单已是 CANCELLED 时直接返回成功。
     */
    @Operation(summary = "Cancel order by admin")
    @PutMapping("/orders/{id}/cancel")
    @Transactional
    public Result<Void> cancelOrderByAdmin(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            return Result.success();
        }
        // 双向联动一致性说明：
        // 管理员取消订单时，必须同步取消该订单关联的“最新合同”。
        // 这里先做“可取消性预校验”（遇到 FULLY_SIGNED 直接失败），
        // 预校验阶段若抛出异常，本方法不会写入订单状态，事务整体回滚保持原子性。
        // 再执行“订单取消 -> 合同取消”，保证顺序统一且失败可回滚。
        Contract latestContract = findCancellableLatestContractForOrderByAdmin(order);
        order.setStatus("CANCELLED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        if (latestContract != null) {
            latestContract.setStatus("CANCELLED");
            latestContract.setUpdateTime(LocalDateTime.now());
            contractMapper.updateById(latestContract);
            notifyContractCancelledByAdmin(latestContract);
        }
        // 管理员取消订单属于关键业务动作：
        // 通过 MQ 异步推送给订单双方（租客+房东），确保双方及时在消息中心看到状态变化。
        notifyOrderCancelledByAdmin(order);
        return Result.success();
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
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        if ("CANCELLED".equals(contract.getStatus())) {
            return Result.success();
        }
        if ("FULLY_SIGNED".equals(contract.getStatus())) {
            throw new BusinessException("已签署的合同不可取消");
        }
        contract.setStatus("CANCELLED");
        contract.setUpdateTime(LocalDateTime.now());
        contractMapper.updateById(contract);
        // 双向联动一致性说明：
        // 管理员取消合同时，必须同步取消其对应订单。
        // 这样可避免出现“合同已取消但订单仍可继续流转”的不一致状态。
        cancelRelatedOrderForContractByAdmin(contract);
        // 合同取消后异步通知合同双方（租客+房东），告知取消来源为管理员兜底处理，避免双方误解为对方主动取消。
        notifyContractCancelledByAdmin(contract);
        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null) {
            house.setStatus("ONLINE");
            house.setUpdateTime(LocalDateTime.now());
            houseMapper.updateById(house);
        }
        return Result.success();
    }

    /**
     * 管理员取消订单前，校验该订单关联的最新合同是否允许被联动取消。
     *
     * 设计要点：
     * 1) 只处理“最新合同”，与订单详情页展示口径保持一致；
     * 2) 若最新合同已取消，直接忽略，保证幂等；
     * 3) 若最新合同已双方签署（FULLY_SIGNED），按业务规则禁止取消，
     *    抛出异常终止本次管理员取消订单操作，避免出现“订单已取消、合同未取消”的不一致状态。
     *
     * @return 可被联动取消的最新合同；若无需处理返回 null
     */
    private Contract findCancellableLatestContractForOrderByAdmin(Order order) {
        Contract latestContract = findLatestContractByOrderId(order.getId());
        if (latestContract == null || "CANCELLED".equals(latestContract.getStatus())) {
            return null;
        }
        if ("FULLY_SIGNED".equals(latestContract.getStatus())) {
            String contractIdentifier = StringUtils.hasText(latestContract.getContractNo())
                    ? latestContract.getContractNo()
                    : safeIdentifier(latestContract.getId());
            throw new BusinessException(String.format("合同[%s]已签署，不可取消", contractIdentifier));
        }
        return latestContract;
    }

    /**
     * 管理员取消合同时，联动取消其对应订单（若存在且未取消）。
     *
     * 设计要点：
     * 1) 订单不存在时直接忽略，兼容历史脏数据；
     * 2) 订单已取消时直接返回，保证幂等；
     * 3) 与主取消动作运行在同一事务中，确保“合同/订单”状态要么同时成功，要么同时回滚。
     */
    private void cancelRelatedOrderForContractByAdmin(Contract contract) {
        if (contract.getOrderId() == null) {
            return;
        }
        Order relatedOrder = orderMapper.selectById(contract.getOrderId());
        if (relatedOrder == null || "CANCELLED".equals(relatedOrder.getStatus())) {
            return;
        }
        relatedOrder.setStatus("CANCELLED");
        relatedOrder.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(relatedOrder);
        notifyOrderCancelledByAdmin(relatedOrder);
    }

    /**
     * 查询订单关联的最新合同（按创建时间倒序）。
     * 若无合同记录，返回 null。
     */
    private Contract findLatestContractByOrderId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getOrderId, orderId)
                .orderByDesc(Contract::getCreateTime)
                .last("LIMIT 1");
        return contractMapper.selectOne(wrapper);
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getCity, keyword)
                    .or().like(House::getAddress, keyword));
        }
        // 管理员房源状态下拉筛选：仅命中白名单时生效，保证接口健壮性与向后兼容。
        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (HOUSE_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(House::getStatus, normalizedStatus);
            }
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
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BusinessException(404, "房源不存在");
        }
        String status = body == null ? null : String.valueOf(body.getOrDefault("status", ""));
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(400, "审核状态不能为空");
        }
        boolean approved = "APPROVED".equalsIgnoreCase(status) || "ONLINE".equalsIgnoreCase(status);
        // 管理端房源管理口径仅保留 ONLINE/OFFLINE，两类下线语义统一写入 OFFLINE。
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

    /**
     * 管理员取消订单后，异步通知订单双方。
     * 文案明确“管理员已取消”，避免用户误判为交易对方主动取消。
     */
    private void notifyOrderCancelledByAdmin(Order order) {
        if (order == null) {
            return;
        }
        String orderNo = StringUtils.hasText(order.getOrderNo()) ? order.getOrderNo() : safeIdentifier(order.getId());
        String message = String.format("管理员已取消订单（订单号：%s），如有疑问请联系平台客服。", orderNo);
        if (order.getTenantId() != null) {
            messageProducer.sendOrderStatusChange(order.getTenantId(), message);
        }
        if (order.getLandlordId() != null) {
            messageProducer.sendOrderStatusChange(order.getLandlordId(), message);
        }
    }

    /**
     * 管理员取消合同后，异步通知合同双方。
     * 与订单通知一致采用统一 MQ 通道，确保消息落库逻辑一致可观测。
     */
    private void notifyContractCancelledByAdmin(Contract contract) {
        if (contract == null) {
            return;
        }
        String contractNo = StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : safeIdentifier(contract.getId());
        String message = String.format("管理员已取消合同（合同编号：%s），如有疑问请联系平台客服。", contractNo);
        if (contract.getTenantId() != null) {
            messageProducer.sendContractStatusChange(contract.getTenantId(), message);
        }
        if (contract.getLandlordId() != null) {
            messageProducer.sendContractStatusChange(contract.getLandlordId(), message);
        }
    }

    /**
     * 统一的标识符兜底格式化：
     * 当编号为空时返回 N/A，避免消息文案出现“null”造成歧义。
     */
    private String safeIdentifier(Object id) {
        return id == null ? "N/A" : String.valueOf(id);
    }
}
