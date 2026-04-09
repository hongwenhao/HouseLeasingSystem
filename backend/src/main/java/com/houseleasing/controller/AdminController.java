package com.houseleasing.controller; // 声明当前控制器所属的 Java 包，方便项目按层级组织代码

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
@Tag(name = "Admin", description = "Admin management endpoints") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
@RestController // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
@RequestMapping("/api/admin") // 管理后台接口统一前缀
@RequiredArgsConstructor // 自动注入依赖组件
@SecurityRequirement(name = "Bearer Authentication") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
@PreAuthorize("hasRole('ADMIN')") // 仅允许管理员角色访问此控制器的所有接口
public class AdminController { // 管理员专用控制器：用户、房源、订单、合同、统计等后台能力
    private static final int AREA_STATS_LIMIT = 12; // 城市统计最多返回 12 个城市
    private static final int PRICE_TRENDS_LIMIT = 6; // 租金趋势最多返回最近 6 个月
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM"); // 月份字符串格式
    private static final String HOUSE_ACTION_ONLINE = "上架"; // 房源上架动作名
    private static final String HOUSE_ACTION_OFFLINE = "下架"; // 房源下架动作名
    private static final String OWNER_HOUSE_ONLINE_MESSAGE = "管理员已将您的房源上架，可正常展示给租客。"; // 给房东的上架通知文案
    private static final String OWNER_HOUSE_OFFLINE_MESSAGE = "管理员已将您的房源下架，暂不可展示给租客。"; // 给房东的下架通知文案
    private static final String TENANT_HOUSE_OFFLINE_MESSAGE_TEMPLATE = "管理员已将您关注/下单的房源《%s》下架。"; // 给租客的下架通知模板

    private static final String CREDIT_RANGE_EXCELLENT = "90-200(优秀)"; // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
    private static final String CREDIT_RANGE_GOOD = "70-89(良好)"; // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
    private static final String CREDIT_RANGE_NORMAL = "60-69(一般)"; // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
    private static final String CREDIT_RANGE_LOW = "60以下(较低)"; // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
    /**
     * 后台订单状态白名单（统一使用大写枚举值）：
     * 当 keyword 命中该集合时，按状态精确过滤；否则按订单号模糊过滤。
     */
    private static final Set<String> ORDER_STATUS_KEYWORDS = Set.of( // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
            "PENDING", "APPROVED", "SIGNED", "REJECTED", "CANCELLED", "COMPLETED" // 继续列举后台允许的状态关键字，后续可据此做白名单校验
    ); // 结束当前代码块，表示这一层逻辑到此完成
    /**
     * 后台合同状态白名单（统一使用大写枚举值），用于状态下拉框后端兜底校验。
     */
    private static final Set<String> CONTRACT_STATUS_KEYWORDS = Set.of( // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
            "DRAFT", "PENDING_SIGN", "TENANT_SIGNED", "LANDLORD_SIGNED", "FULLY_SIGNED", "CANCELLED" // 继续列举后台允许的状态关键字，后续可据此做白名单校验
    ); // 结束当前代码块，表示这一层逻辑到此完成
    /**
     * 后台房源状态白名单：
     * 仅允许按 ONLINE/OFFLINE 进行筛选，管理端统一为“已上架/已下架”两种状态。
     */
    private static final Set<String> HOUSE_STATUS_KEYWORDS = Set.of("ONLINE", "OFFLINE"); // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落

    private static final String CREDIT_RANGE_CASE_SQL = // 定义不可变常量，统一管理业务规则和提示文案，避免魔法值散落
            "CASE " + // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    "WHEN credit_score >= 90 THEN '" + CREDIT_RANGE_EXCELLENT + "' " + // 继续拼接信用分段的 SQL 条件分支，用于统计时按区间分组
                    "WHEN credit_score >= 70 THEN '" + CREDIT_RANGE_GOOD + "' " + // 继续拼接信用分段的 SQL 条件分支，用于统计时按区间分组
                    "WHEN credit_score >= 60 THEN '" + CREDIT_RANGE_NORMAL + "' " + // 继续拼接信用分段的 SQL 条件分支，用于统计时按区间分组
                    "ELSE '" + CREDIT_RANGE_LOW + "' END"; // 拼接兜底分支：当以上条件都不命中时归入最低信用区间

    private final UserService userService; // 用户管理业务服务
    private final UserMapper userMapper; // 用户数据访问组件
    private final HouseMapper houseMapper; // 房源数据访问组件
    private final OrderMapper orderMapper; // 订单数据访问组件
    private final ContractMapper contractMapper; // 合同数据访问组件
    private final MessageProducer messageProducer; // MQ 消息发送器（用于异步通知）

    /**
     * 查询系统所有用户列表（支持关键词搜索）
     *
     * @param page    当前页码
     * @param size    每页大小
     * @param keyword 搜索关键词（可选）
     * @return 分页用户列表
     */
    @Operation(summary = "List all users") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/users") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<PageResult<User>> listUsers( // 声明成员字段或工具方法，为后续业务逻辑提供数据与能力
            @RequestParam(defaultValue = "1") int page, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(defaultValue = "10") Integer size, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String keyword) { // 管理员分页查询用户，可按关键词检索
        return Result.success(userService.listUsers(page, size, keyword)); // 返回用户分页结果
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 封禁指定用户账号
     *
     * @param id 要封禁的用户 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Ban user") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/users/{id}/ban") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> banUser(@PathVariable Long id) { // 封禁指定用户
        userService.banUser(id); // 更新用户状态为封禁
        return Result.success(); // 返回成功
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 解封指定用户账号
     *
     * @param id 要解封的用户 ID
     * @return 操作成功的响应
     */
    @Operation(summary = "Unban user") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/users/{id}/unban") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> unbanUser(@PathVariable Long id) { // 解封指定用户
        userService.unbanUser(id); // 更新用户状态为正常
        return Result.success(); // 返回成功
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 查询系统所有订单列表（分页）
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param keyword 搜索关键词（可选，支持订单号与订单状态）
     * @return 所有订单的分页列表
     */
    @Operation(summary = "List all orders") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/orders") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<PageResult<Order>> listAllOrders( // 声明成员字段或工具方法，为后续业务逻辑提供数据与能力
            @RequestParam(defaultValue = "1") int page, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(defaultValue = "10") int size, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String keyword, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String status) { // 后台分页查询订单（支持关键词+状态）
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> pageObj = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        // 管理员订单检索增强：
        // 1) 若 keyword 命中状态枚举，则按状态精确匹配（大小写不敏感）；
        // 2) 否则按订单号模糊匹配
        // 3) 关键词为空时不加筛选条件，保持历史行为不变。
        boolean hasExplicitStatusFilter = StringUtils.hasText(status); // 定义基础类型变量，保存流程中的关键判断值或计数结果
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            String normalizedStatusKeyword = trimmedKeyword.toUpperCase(Locale.ROOT);
            // 兼容历史行为：仅当“未显式传入 status 下拉参数”时，才允许 keyword 触发状态过滤。
            // 若显式 status 已存在，则 keyword 一律按订单号处理，避免出现 status=APPROVED 且 keyword=CANCELLED 这种冲突条件。
            if (!hasExplicitStatusFilter && ORDER_STATUS_KEYWORDS.contains(normalizedStatusKeyword)) {
                wrapper.eq(Order::getStatus, normalizedStatusKeyword);
            } else { // 进入另一条分支逻辑，处理前置条件不满足时的情况
                wrapper.like(Order::getOrderNo, trimmedKeyword);
            }
        } // 结束当前代码块，表示这一层逻辑到此完成
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
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> result = orderMapper.selectPage(pageObj, wrapper); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        List<Order> records = result.getRecords(); // 创建集合变量，用于承载后续查询与组装的数据
        // 批量查询关联信息，避免 N+1 查询
        Set<Long> houseIds = records.stream() // 创建集合变量，用于承载后续查询与组装的数据
                .map(Order::getHouseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, House> houseMap = houseIds.isEmpty() // 创建集合变量，用于承载后续查询与组装的数据
                ? Map.of()
                : mapById(houseMapper.selectBatchIds(houseIds), House::getId);
        Set<Long> userIds = new HashSet<>(); // 创建集合变量，用于承载后续查询与组装的数据
        records.forEach(order -> {
            if (order.getTenantId() != null) userIds.add(order.getTenantId());
            if (order.getLandlordId() != null) userIds.add(order.getLandlordId());
        });
        Map<Long, User> userMap = userIds.isEmpty() // 创建集合变量，用于承载后续查询与组装的数据
                ? Map.of()
                : mapById(userMapper.selectBatchIds(userIds), User::getId);
        records.forEach(order -> {
            order.setHouse(houseMap.get(order.getHouseId()));
            order.setTenant(sanitizeUser(userMap.get(order.getTenantId())));
            order.setLandlord(sanitizeUser(userMap.get(order.getLandlordId())));
        }); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(PageResult.of(result.getTotal(), records, page, size)); // 返回处理好的订单分页结果
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
    @Operation(summary = "List all contracts") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/contracts") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<PageResult<Contract>> listAllContracts( // 声明成员字段或工具方法，为后续业务逻辑提供数据与能力
            @RequestParam(defaultValue = "1") int page, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(defaultValue = "10") int size, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String keyword, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String status) { // 后台分页查询合同（支持编号、订单号、状态）
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
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> orderNoWrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            orderNoWrapper.like(Order::getOrderNo, trimmedKeyword).select(Order::getId); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            List<Order> matchedOrders = orderMapper.selectList(orderNoWrapper); // 创建集合变量，用于承载后续查询与组装的数据
            Set<Long> matchedOrderIds = matchedOrders.stream() // 创建集合变量，用于承载后续查询与组装的数据
                    .map(Order::getId) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    .filter(id -> id != null) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    .collect(Collectors.toSet()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            wrapper.and(w -> { // 当前行是多行表达式的一部分，需要与前后行一起阅读
                w.like(Contract::getContractNo, trimmedKeyword); // 执行当前语句以推进业务流程，并为后续步骤准备数据
                if (!matchedOrderIds.isEmpty()) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
                    w.or().in(Contract::getOrderId, matchedOrderIds); // 执行当前语句以推进业务流程，并为后续步骤准备数据
                }
            }); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        }
        if (StringUtils.hasText(status)) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if (CONTRACT_STATUS_KEYWORDS.contains(normalizedStatus)) {
                wrapper.eq(Contract::getStatus, normalizedStatus);
            }
        }
        wrapper.orderByDesc(Contract::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Contract> result = contractMapper.selectPage(pageObj, wrapper);
        List<Contract> records = result.getRecords(); // 创建集合变量，用于承载后续查询与组装的数据
        // 批量查询关联信息，避免 N+1 查询
        Set<Long> houseIds = records.stream() // 创建集合变量，用于承载后续查询与组装的数据
                .map(Contract::getHouseId) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .filter(id -> id != null) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .collect(Collectors.toSet()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        Map<Long, House> houseMap = houseIds.isEmpty() // 创建集合变量，用于承载后续查询与组装的数据
                ? Map.of() // 当前行是多行表达式的一部分，需要与前后行一起阅读
                : mapById(houseMapper.selectBatchIds(houseIds), House::getId); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        Set<Long> userIds = new HashSet<>(); // 创建集合变量，用于承载后续查询与组装的数据
        records.forEach(contract -> { // 当前行是多行表达式的一部分，需要与前后行一起阅读
            if (contract.getTenantId() != null) userIds.add(contract.getTenantId()); // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            if (contract.getLandlordId() != null) userIds.add(contract.getLandlordId()); // 进行条件判断，仅在满足业务前提时才执行后续逻辑
        }); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        Map<Long, User> userMap = userIds.isEmpty() // 创建集合变量，用于承载后续查询与组装的数据
                ? Map.of() // 当前行是多行表达式的一部分，需要与前后行一起阅读
                : mapById(userMapper.selectBatchIds(userIds), User::getId); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        Set<Long> orderIds = records.stream() // 创建集合变量，用于承载后续查询与组装的数据
                .map(Contract::getOrderId) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .filter(id -> id != null) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .collect(Collectors.toSet()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        Map<Long, Order> orderMap = orderIds.isEmpty() // 创建集合变量，用于承载后续查询与组装的数据
                ? Map.of() // 当前行是多行表达式的一部分，需要与前后行一起阅读
                : mapById(orderMapper.selectBatchIds(orderIds), Order::getId); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        records.forEach(contract -> { // 当前行是多行表达式的一部分，需要与前后行一起阅读
            contract.setHouse(houseMap.get(contract.getHouseId())); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            contract.setTenant(sanitizeUser(userMap.get(contract.getTenantId()))); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            contract.setLandlord(sanitizeUser(userMap.get(contract.getLandlordId()))); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            Order order = orderMap.get(contract.getOrderId()); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
            if (order != null) contract.setOrderNo(order.getOrderNo()); // 进行条件判断，仅在满足业务前提时才执行后续逻辑
        }); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(PageResult.of(result.getTotal(), records, page, size)); // 返回处理好的合同分页数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员取消订单（管理兜底能力）：
     * - 仅允许将“未取消”订单更新为 CANCELLED，避免重复写入；
     * - 管理端取消不依赖租客/房东身份校验，适用于人工客服介入场景；
     * - 保持幂等：订单已是 CANCELLED 时直接返回成功。
     */
    @Operation(summary = "Cancel order by admin") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/orders/{id}/cancel") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @Transactional // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> cancelOrderByAdmin(@PathVariable Long id) { // 管理员兜底取消订单
        Order order = orderMapper.selectById(id); // 先查订单是否存在
        if (order == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(404, "订单不存在"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        if ("CANCELLED".equals(order.getStatus())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return Result.success(); // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        // 双向联动一致性说明：
        // 管理员取消订单时，必须同步取消该订单关联的“最新合同”。
        // 这里先做“可取消性预校验”（遇到 FULLY_SIGNED 直接失败），
        // 预校验阶段若抛出异常，本方法不会写入订单状态，事务整体回滚保持原子性。
        // 再执行“订单取消 -> 合同取消”，保证顺序统一且失败可回滚。
        Contract latestContract = findCancellableLatestContractForOrderByAdmin(order); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        order.setStatus("CANCELLED"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        order.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        orderMapper.updateById(order); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        if (latestContract != null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            latestContract.setStatus("CANCELLED"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            latestContract.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            contractMapper.updateById(latestContract); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            notifyContractCancelledByAdmin(latestContract); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        // 管理员取消订单属于关键业务动作：
        // 通过 MQ 异步推送给订单双方（租客+房东），确保双方及时在消息中心看到状态变化。
        notifyOrderCancelledByAdmin(order); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(); // 取消成功（含联动合同与通知）
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员取消合同（管理兜底能力）：
     * - 仅允许取消未“双方已签(FULLY_SIGNED)”的合同；
     * - 合同取消后将房源状态恢复为 ONLINE，保持与业务取消链路一致；
     * - 已取消合同重复操作时直接返回成功（幂等）。
     */
    @Operation(summary = "Cancel contract by admin") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/contracts/{id}/cancel") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @Transactional // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> cancelContractByAdmin(@PathVariable Long id) { // 管理员兜底取消合同
        Contract contract = contractMapper.selectById(id); // 先查合同是否存在
        if (contract == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(404, "合同不存在"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        if ("CANCELLED".equals(contract.getStatus())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return Result.success(); // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        if ("FULLY_SIGNED".equals(contract.getStatus())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException("已签署的合同不可取消"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        contract.setStatus("CANCELLED"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        contract.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        contractMapper.updateById(contract); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        // 双向联动一致性说明：
        // 管理员取消合同时，必须同步取消其对应订单。
        // 这样可避免出现“合同已取消但订单仍可继续流转”的不一致状态。
        cancelRelatedOrderForContractByAdmin(contract); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        // 合同取消后异步通知合同双方（租客+房东），告知取消来源为管理员兜底处理，避免双方误解为对方主动取消。
        notifyContractCancelledByAdmin(contract); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        House house = houseMapper.selectById(contract.getHouseId()); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (house != null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            house.setStatus("ONLINE"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            house.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            houseMapper.updateById(house); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        return Result.success(); // 取消成功（含联动订单、房源与通知）
    } // 结束当前代码块，表示这一层逻辑到此完成

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
    private Contract findCancellableLatestContractForOrderByAdmin(Order order) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        Contract latestContract = findLatestContractByOrderId(order.getId()); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (latestContract == null || "CANCELLED".equals(latestContract.getStatus())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return null; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        if ("FULLY_SIGNED".equals(latestContract.getStatus())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            String contractIdentifier = StringUtils.hasText(latestContract.getContractNo()) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    ? latestContract.getContractNo() // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    : safeIdentifier(latestContract.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            throw new BusinessException(String.format("合同[%s]已签署，不可取消", contractIdentifier)); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        return latestContract; // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员取消合同时，联动取消其对应订单（若存在且未取消）。
     *
     * 设计要点：
     * 1) 订单不存在时直接忽略，兼容历史脏数据；
     * 2) 订单已取消时直接返回，保证幂等；
     * 3) 与主取消动作运行在同一事务中，确保“合同/订单”状态要么同时成功，要么同时回滚。
     */
    private void cancelRelatedOrderForContractByAdmin(Contract contract) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (contract.getOrderId() == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return; // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        Order relatedOrder = orderMapper.selectById(contract.getOrderId()); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (relatedOrder == null || "CANCELLED".equals(relatedOrder.getStatus())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return; // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        relatedOrder.setStatus("CANCELLED"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        relatedOrder.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        orderMapper.updateById(relatedOrder); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        notifyOrderCancelledByAdmin(relatedOrder); // 执行当前语句以推进业务流程，并为后续步骤准备数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 查询订单关联的最新合同（按创建时间倒序）。
     * 若无合同记录，返回 null。
     */
    private Contract findLatestContractByOrderId(Long orderId) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (orderId == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return null; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>(); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        wrapper.eq(Contract::getOrderId, orderId) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .orderByDesc(Contract::getCreateTime) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .last("LIMIT 1"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return contractMapper.selectOne(wrapper); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 获取系统统计数据（用户总数、房源总数、订单总数、合同总数）
     *
     * @return 包含各项统计数据的 Map
     */
    @Operation(summary = "Get system statistics") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/statistics") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Map<String, Object>> getStatistics() { // 获取后台首页统计
        Map<String, Object> stats = new HashMap<>(); // 统计结果容器
        stats.put("userCount", userMapper.selectCount(null));     // 用户总数
        stats.put("houseCount", houseMapper.selectCount(null));   // 房源总数
        stats.put("orderCount", orderMapper.selectCount(null));   // 订单总数
        // 合同统计：总数及待签署/审核数量
        stats.put("contractCount", contractMapper.selectCount(null)); // 合同总数
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Contract> pendingWrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        pendingWrapper.eq(Contract::getStatus, "PENDING_SIGN"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        stats.put("pendingContracts", contractMapper.selectCount(pendingWrapper)); // 待审核合同数
        return Result.success(stats); // 返回统计数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 获取系统统计数据（与 /statistics 保持同口径，供前端 /admin/stats 直连使用）。
     *
     * @return 包含用户、房源、合同等统计字段
     */
    @Operation(summary = "Get dashboard stats") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/stats") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Map<String, Object>> getStats() { // 与 /statistics 同口径，给前端另一路径调用
        return getStatistics(); // 直接复用统计逻辑
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 获取各城市房源数量统计（用于管理后台柱状图）。
     *
     * @return [{"city":"北京","count":12}, ...]
     */
    @Operation(summary = "Get area stats") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/stats/area") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<List<Map<String, Object>>> getAreaStats() { // 统计不同城市的房源数量
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        wrapper.select("city AS city", "COUNT(*) AS count") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .isNotNull("city") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .ne("city", "") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .groupBy("city") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .orderByDesc("count") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .last("LIMIT " + AREA_STATS_LIMIT); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(houseMapper.selectMaps(wrapper)); // 返回城市与数量列表
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 获取近 6 个月租金均价趋势（用于管理后台折线图）。
     *
     * @return [{"month":"2026-01","avgPrice":3500.00}, ...]
     */
    @Operation(summary = "Get price trend stats") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/stats/price-trends") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<List<Map<String, Object>>> getPriceTrends() { // 统计近几个月平均租金趋势
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<House> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        wrapper.select("DATE_FORMAT(create_time, '%Y-%m') AS month", "ROUND(AVG(price), 2) AS avgPrice") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .groupBy("DATE_FORMAT(create_time, '%Y-%m')") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .orderByDesc("month") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .last("LIMIT " + PRICE_TRENDS_LIMIT); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        List<Map<String, Object>> raw = houseMapper.selectMaps(wrapper); // 创建集合变量，用于承载后续查询与组装的数据
        List<Map<String, Object>> formatted = new ArrayList<>(); // 创建集合变量，用于承载后续查询与组装的数据
        raw.stream() // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .sorted(Comparator.comparing(map -> YearMonth.parse(String.valueOf(map.get("month")), MONTH_FORMATTER))) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .forEach(item -> { // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    Map<String, Object> row = new LinkedHashMap<>(); // 创建集合变量，用于承载后续查询与组装的数据
                    row.put("month", item.get("month")); // 执行当前语句以推进业务流程，并为后续步骤准备数据
                    row.put("avgPrice", parseDecimal(item.get("avgPrice"))); // 执行当前语句以推进业务流程，并为后续步骤准备数据
                    formatted.add(row); // 执行当前语句以推进业务流程，并为后续步骤准备数据
                }); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(formatted); // 返回按时间升序整理后的趋势数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 获取信用分分布（用于管理后台饼图）。
     *
     * @return [{"range":"90-100(优秀)","count":5}, ...]
     */
    @Operation(summary = "Get credit distribution stats") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/stats/credit") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<List<Map<String, Object>>> getCreditDistribution() { // 统计用户信用分区间分布
        Map<String, Long> buckets = new LinkedHashMap<>(); // 创建集合变量，用于承载后续查询与组装的数据
        buckets.put(CREDIT_RANGE_EXCELLENT, 0L); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        buckets.put(CREDIT_RANGE_GOOD, 0L); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        buckets.put(CREDIT_RANGE_NORMAL, 0L); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        buckets.put(CREDIT_RANGE_LOW, 0L); // 执行当前语句以推进业务流程，并为后续步骤准备数据

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        wrapper.select(CREDIT_RANGE_CASE_SQL + " AS score_range", "COUNT(*) AS count") // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .groupBy(CREDIT_RANGE_CASE_SQL); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        List<Map<String, Object>> aggregated = userMapper.selectMaps(wrapper); // 创建集合变量，用于承载后续查询与组装的数据
        for (Map<String, Object> item : aggregated) { // 遍历集合中的每一项，逐个完成数据处理
            String range = String.valueOf(item.get("score_range")); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
            long count = toLong(item.get("count")); // 定义基础类型变量，保存流程中的关键判断值或计数结果
            if (buckets.containsKey(range)) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
                buckets.put(range, count); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            } // 结束当前代码块，表示这一层逻辑到此完成
        } // 结束当前代码块，表示这一层逻辑到此完成

        List<Map<String, Object>> result = new ArrayList<>(); // 创建集合变量，用于承载后续查询与组装的数据
        buckets.forEach((range, count) -> { // 当前行是多行表达式的一部分，需要与前后行一起阅读
            Map<String, Object> row = new LinkedHashMap<>(); // 创建集合变量，用于承载后续查询与组装的数据
            row.put("range", range); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            row.put("count", count); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            result.add(row); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        }); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(result); // 返回饼图需要的区间统计结果
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 查询待审核房源（当前实现口径：OFFLINE 视作待审核/未上线）。
     *
     * @param page    当前页码
     * @param size    每页数量
     * @param keyword 关键词（匹配标题/地址/城市）
     * @return 待审核房源分页数据
     */
    @Operation(summary = "List pending houses") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/houses/pending") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<PageResult<House>> listPendingHouses( // 声明成员字段或工具方法，为后续业务逻辑提供数据与能力
            @RequestParam(defaultValue = "1") int page, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(defaultValue = "10") int size, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String keyword) { // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        wrapper.eq(House::getStatus, "OFFLINE"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        if (StringUtils.hasText(keyword)) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            wrapper.and(w -> w.like(House::getTitle, keyword) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    .or().like(House::getCity, keyword) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    .or().like(House::getAddress, keyword)); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        wrapper.orderByDesc(House::getCreateTime); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result = houseMapper.selectPage(pageObj, wrapper); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size)); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员房源管理：分页查询全部房源（支持关键词），用于“房源管理”页。
     *
     * @param page    当前页码
     * @param size    每页条数
     * @param keyword 关键词（可匹配标题、城市、地址）
     * @return 全量房源分页列表
     */
    @Operation(summary = "List all houses for admin management") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/houses") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<PageResult<House>> listAllHouses( // 声明成员字段或工具方法，为后续业务逻辑提供数据与能力
            @RequestParam(defaultValue = "1") int page, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(defaultValue = "10") int size, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String keyword, // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
            @RequestParam(required = false) String status) { // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> pageObj = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        if (StringUtils.hasText(keyword)) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            wrapper.and(w -> w.like(House::getTitle, keyword) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    .or().like(House::getCity, keyword) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                    .or().like(House::getAddress, keyword)); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        // 管理员房源状态下拉筛选：仅命中白名单时生效，保证接口健壮性与向后兼容。
        if (StringUtils.hasText(status)) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
            if (HOUSE_STATUS_KEYWORDS.contains(normalizedStatus)) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
                wrapper.eq(House::getStatus, normalizedStatus); // 执行当前语句以推进业务流程，并为后续步骤准备数据
            } // 结束当前代码块，表示这一层逻辑到此完成
        } // 结束当前代码块，表示这一层逻辑到此完成
        wrapper.orderByDesc(House::getCreateTime); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<House> result = houseMapper.selectPage(pageObj, wrapper); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        return Result.success(PageResult.of(result.getTotal(), result.getRecords(), page, size)); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员房源管理：查询单个房源详情。
     *
     * @param id 房源 ID
     * @return 房源详情
     */
    @Operation(summary = "Get house detail for admin management") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @GetMapping("/houses/{id}") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<House> getHouseDetailForAdmin(@PathVariable Long id) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        House house = houseMapper.selectById(id); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (house == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(404, "房源不存在"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        return Result.success(house); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

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
    @Operation(summary = "Audit house") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/houses/{id}/audit") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> auditHouse(@PathVariable Long id, // 声明成员字段或工具方法，为后续业务逻辑提供数据与能力
                                   @RequestBody(required = false) Map<String, Object> body) { // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
        House house = houseMapper.selectById(id); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (house == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(404, "房源不存在"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        String status = body == null ? null : String.valueOf(body.getOrDefault("status", "")); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (!StringUtils.hasText(status)) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(400, "审核状态不能为空"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        boolean approved = "APPROVED".equalsIgnoreCase(status) || "ONLINE".equalsIgnoreCase(status); // 定义基础类型变量，保存流程中的关键判断值或计数结果
        // 管理端房源管理口径仅保留 ONLINE/OFFLINE，两类下线语义统一写入 OFFLINE。
        house.setStatus(approved ? "ONLINE" : "OFFLINE"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        house.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        houseMapper.updateById(house); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        // 审核动作后通知房东，便于房东及时感知房源状态变化
        sendHouseManagementNotificationToOwner(house, approved ? HOUSE_ACTION_ONLINE : HOUSE_ACTION_OFFLINE, // 当前行是多行表达式的一部分，需要与前后行一起阅读
                approved ? OWNER_HOUSE_ONLINE_MESSAGE : OWNER_HOUSE_OFFLINE_MESSAGE); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员房源管理：上架房源（将状态置为 ONLINE），并推送通知给房东与相关租客。
     *
     * @param id 房源 ID
     * @return 操作成功
     */
    @Operation(summary = "Put house online by admin") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/houses/{id}/online") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> putHouseOnline(@PathVariable Long id) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        House house = houseMapper.selectById(id); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (house == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(404, "房源不存在"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        house.setStatus("ONLINE"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        house.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        houseMapper.updateById(house); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        sendHouseManagementNotificationToOwner(house, HOUSE_ACTION_ONLINE, OWNER_HOUSE_ONLINE_MESSAGE); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员房源管理：下架房源（将状态置为 OFFLINE），并推送通知给房东与已关联租客。
     *
     * @param id 房源 ID
     * @return 操作成功
     */
    @Operation(summary = "Put house offline by admin") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    @PutMapping("/houses/{id}/offline") // 通过注解声明接口路由/权限/文档等元信息，让框架自动生效
    public Result<Void> putHouseOffline(@PathVariable Long id) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        House house = houseMapper.selectById(id); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (house == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            throw new BusinessException(404, "房源不存在"); // 主动抛出业务异常，阻断非法流程并交给全局异常处理
        } // 结束当前代码块，表示这一层逻辑到此完成
        house.setStatus("OFFLINE"); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        house.setUpdateTime(LocalDateTime.now()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        houseMapper.updateById(house); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        sendHouseManagementNotificationToOwner(house, HOUSE_ACTION_OFFLINE, OWNER_HOUSE_OFFLINE_MESSAGE); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        // 对该房源产生过订单的租客推送提醒，避免租客继续对不可用房源发起意向
        notifyTenantsOfHouseOffline(house, String.format(TENANT_HOUSE_OFFLINE_MESSAGE_TEMPLATE, safeHouseTitle(house))); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return Result.success(); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 返回脱敏后的用户信息，避免在后台管理列表中透出密码等敏感字段。
     */
    private User sanitizeUser(User source) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (source == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return null; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        User user = new User(); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        user.setId(source.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setUsername(source.getUsername()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setPhone(source.getPhone()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setEmail(source.getEmail()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setRole(source.getRole()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setRealName(source.getRealName()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setCreditScore(source.getCreditScore()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setIsRealNameAuth(source.getIsRealNameAuth()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setStatus(source.getStatus()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        user.setAvatar(source.getAvatar()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        return user; // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 将统计查询返回值安全地转换为 BigDecimal。
     */
    private BigDecimal parseDecimal(Object value) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (value == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return BigDecimal.ZERO; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        try { // 开始受控异常处理块，保护关键业务代码的执行安全
            return new BigDecimal(String.valueOf(value)); // 返回本次方法的处理结果给调用方，结束当前流程
        } catch (Exception ignored) { // 捕获该段逻辑可能出现的异常，防止单条脏数据影响整体流程
            return BigDecimal.ZERO; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 将对象转换为 long（兼容 Number 与字符串形式）。
     */
    private long toLong(Object value) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (value == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return 0L; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        if (value instanceof Number number) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return number.longValue(); // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        try { // 开始受控异常处理块，保护关键业务代码的执行安全
            return Long.parseLong(String.valueOf(value)); // 返回本次方法的处理结果给调用方，结束当前流程
        } catch (Exception ignored) { // 捕获该段逻辑可能出现的异常，防止单条脏数据影响整体流程
            return 0L; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 将对象列表按主键映射为 Map，便于 O(1) 关联访问。
     */
    private <T> Map<Long, T> mapById(List<T> list, Function<T, Long> idGetter) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (list == null || list.isEmpty()) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return Map.of(); // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        return list.stream() // 返回本次方法的处理结果给调用方，结束当前流程
                .filter(item -> item != null && idGetter.apply(item) != null) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a)); // 执行当前语句以推进业务流程，并为后续步骤准备数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 给房源所属房东发送“管理员房源管理”通知。
     */
    private void sendHouseManagementNotificationToOwner(House house, String actionLabel, String actionMessage) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (house == null || house.getOwnerId() == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return; // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        String content = String.format("您的房源《%s》%s", safeHouseTitle(house), actionMessage); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        messageProducer.sendAdminHouseManagementNotification(house.getOwnerId(), actionLabel, content, house.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 根据房源关联订单，通知历史租客房源状态变化。
     * 这里只对存在订单关系的租客推送，避免给全站租客广播造成干扰。
     */
    private void notifyTenantsOfHouseOffline(House house, String content) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (house == null || house.getId() == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return; // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> wrapper = // 当前行是多行表达式的一部分，需要与前后行一起阅读
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>(); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        wrapper.eq(Order::getHouseId, house.getId()) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .select(Order::getTenantId); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        List<Order> orders = orderMapper.selectList(wrapper); // 创建集合变量，用于承载后续查询与组装的数据
        Set<Long> tenantIds = orders.stream() // 创建集合变量，用于承载后续查询与组装的数据
                .map(Order::getTenantId) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .filter(id -> id != null) // 当前行是多行表达式的一部分，需要与前后行一起阅读
                .collect(Collectors.toSet()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        tenantIds.forEach(tenantId -> // 当前行是多行表达式的一部分，需要与前后行一起阅读
                messageProducer.sendAdminHouseManagementNotification(tenantId, HOUSE_ACTION_OFFLINE, "房源状态变更提醒：" + content, house.getId())); // 执行当前语句以推进业务流程，并为后续步骤准备数据
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 安全返回房源标题，避免空值拼接时出现“null”字样。
     */
    private String safeHouseTitle(House house) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (house == null || !StringUtils.hasText(house.getTitle())) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return "未命名房源"; // 返回本次方法的处理结果给调用方，结束当前流程
        } // 结束当前代码块，表示这一层逻辑到此完成
        return house.getTitle(); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员取消订单后，异步通知订单双方。
     * 文案明确“管理员已取消”，避免用户误判为交易对方主动取消。
     */
    private void notifyOrderCancelledByAdmin(Order order) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (order == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return; // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        String orderNo = StringUtils.hasText(order.getOrderNo()) ? order.getOrderNo() : safeIdentifier(order.getId()); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        String message = String.format("管理员已取消订单（订单号：%s），如有疑问请联系平台客服。", orderNo); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (order.getTenantId() != null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            messageProducer.sendOrderStatusChange(order.getTenantId(), message, order.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        if (order.getLandlordId() != null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            messageProducer.sendOrderStatusChange(order.getLandlordId(), message, order.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 管理员取消合同后，异步通知合同双方。
     * 与订单通知一致采用统一 MQ 通道，确保消息落库逻辑一致可观测。
     */
    private void notifyContractCancelledByAdmin(Contract contract) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        if (contract == null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            return; // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        String contractNo = StringUtils.hasText(contract.getContractNo()) ? contract.getContractNo() : safeIdentifier(contract.getId()); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        String message = String.format("管理员已取消合同（合同编号：%s），如有疑问请联系平台客服。", contractNo); // 执行赋值操作，将计算结果保存到变量中供后续步骤使用
        if (contract.getTenantId() != null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            messageProducer.sendContractStatusChange(contract.getTenantId(), message, contract.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
        if (contract.getLandlordId() != null) { // 进行条件判断，仅在满足业务前提时才执行后续逻辑
            messageProducer.sendContractStatusChange(contract.getLandlordId(), message, contract.getId()); // 执行当前语句以推进业务流程，并为后续步骤准备数据
        } // 结束当前代码块，表示这一层逻辑到此完成
    } // 结束当前代码块，表示这一层逻辑到此完成

    /**
     * 统一的标识符兜底格式化：
     * 当编号为空时返回 N/A，避免消息文案出现“null”造成歧义。
     */
    private String safeIdentifier(Object id) { // 声明一个方法入口，约定参数和返回值以完成对应业务动作
        return id == null ? "N/A" : String.valueOf(id); // 返回本次方法的处理结果给调用方，结束当前流程
    } // 结束当前代码块，表示这一层逻辑到此完成
} // 结束当前代码块，表示这一层逻辑到此完成
