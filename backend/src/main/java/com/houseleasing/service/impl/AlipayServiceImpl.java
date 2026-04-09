package com.houseleasing.service.impl;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.common.utils.OrderStatusUtil;
import com.houseleasing.config.AlipayProperties;
import com.houseleasing.dto.AlipaySyncVerifyResponse;
import com.houseleasing.entity.Contract;
import com.houseleasing.entity.Order;
import com.houseleasing.mapper.ContractMapper;
import com.houseleasing.mapper.OrderMapper;
import com.houseleasing.service.AlipayService;
import com.houseleasing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付宝沙箱支付服务实现（仅同步回调）
 *设计说明：
 * 1) 发起支付时只生成支付宝官方收银台表单，不直接改订单状态；
 * 2) 用户完成支付并回跳前端后，由前端把同步参数交给本服务验签；
 * 3) 验签通过且支付成功后，再调用订单支付逻辑完成“PAID + COMPLETED”落库，保证状态变更口径统一。
 */
@Slf4j
@Service // 声明为支付宝业务服务
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService { // 支付宝支付与回调验签的实现类

    private final AlipayClient alipayClient; // 支付宝 SDK 客户端
    private final AlipayProperties alipayProperties; // 支付宝配置（appId/密钥/回调地址等）
    private final OrderMapper orderMapper; // 订单数据访问组件
    private final ContractMapper contractMapper; // 合同数据访问组件（支付前合同校验会用到）
    private final OrderService orderService; // 复用统一订单支付落库逻辑

    private static final String TRADE_SUCCESS = "TRADE_SUCCESS"; // 支付宝明确表示“交易支付成功”的状态码
    private static final String TRADE_FINISHED = "TRADE_FINISHED"; // 支付宝表示“交易结束且不可退款”的状态码

    /**
     * 生成支付宝收银台表单（同步回调方案）
     *
     * @param orderId  订单 ID
     * @param tenantId 当前租客 ID
     * @return 可直接提交到支付宝网关的 HTML 表单
     */
    @Override
    public String createPayForm(Long orderId, Long tenantId) { // 生成支付宝收银台表单
        validateAlipayConfig(); // 启动前先验证支付宝配置完整，避免运行时参数缺失
        Order order = orderMapper.selectById(orderId); // 读取订单，作为支付发起的业务依据
        if (order == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "订单不存在"); // 以异常形式提示调用方当前问题
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(403, "没有操作权限"); // 抛出业务异常并中断当前流程
        }
        validateOrderCanPay(order); // 校验订单状态、合同状态和支付状态是否允许发起支付
        BigDecimal payableAmount = calculatePayableAmount(order); // 计算最终应付金额并统一保留两位小数

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest(); // 创建支付宝“网页支付”请求对象
        request.setReturnUrl(alipayProperties.getReturnUrl()); // 设置同步回跳地址，支付完成后回到前端页面
        AlipayTradePagePayModel model = new AlipayTradePagePayModel(); // 构建支付宝交易模型（订单号/金额/标题等）
        model.setOutTradeNo(order.getOrderNo()); // 使用系统订单号作为商户订单号，便于后续对账
        model.setProductCode("FAST_INSTANT_TRADE_PAY"); // 固定网页即时到账产品码
        model.setTotalAmount(payableAmount.toPlainString()); // 写入应付金额（字符串格式，避免科学计数法）
        model.setSubject("房屋租赁订单支付-" + order.getOrderNo()); // 设置支付标题，便于用户识别支付对象
        Map<String, Object> payload = new LinkedHashMap<>(); // 组装扩展业务上下文，方便回调后核对
        payload.put("orderId", order.getId()); // 回传订单ID用于精准定位业务单据
        payload.put("tenantId", order.getTenantId()); // 回传租客ID用于权限核验
        model.setBody(JSONUtil.toJsonStr(payload)); // 扩展上下文转为 JSON 放入 body 字段
        model.setTimeoutExpress(alipayProperties.getTimeoutExpress()); // 设置支付超时时间，防止订单长期挂起
        request.setBizModel(model); // 把交易模型挂载到请求对象中

        try { // 在当前步骤完成必要业务动作
            return alipayClient.pageExecute(request).getBody(); // 返回当前阶段的处理结果
        } catch (AlipayApiException e) { // 在当前步骤完成必要业务动作
            log.error("生成支付宝支付表单失败，订单ID={}", orderId, e); // 借助已有方法完成该业务动作
            throw new BusinessException(500, "发起支付宝支付失败，请稍后重试"); // 立即返回错误避免继续执行
        }
    }

    /**
     * 校验支付宝同步回跳参数并处理支付结果
     *
     * @param params 回跳参数（query string）
     * @return 验签与处理结果
     */
    @Override
    @Transactional
    public AlipaySyncVerifyResponse verifyAndHandleSyncReturn(Map<String, String> params) { // 验签回调并确认支付结果
        validateAlipayConfig(); // 借助已有方法完成该业务动作
        if (params == null || params.isEmpty()) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "回调参数为空"); // 立即返回错误避免继续执行
        }
        String outTradeNo = params.get("out_trade_no"); // 调用组件能力完成当前步骤
        String appId = params.get("app_id"); // 执行对应服务/DAO方法推进流程
        String totalAmountStr = params.get("total_amount"); // 执行对应服务/DAO方法推进流程

        if (!StringUtils.hasText(outTradeNo)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(400, "缺少订单号"); // 立即返回错误避免继续执行
        }
        if (!StringUtils.hasText(appId) || !Objects.equals(appId, alipayProperties.getAppId())) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "回调应用标识不匹配"); // 以异常形式提示调用方当前问题
        }

        boolean signVerified = verifySign(params); // 执行对应服务/DAO方法推进流程
        if (!signVerified) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "支付宝验签失败"); // 以异常形式提示调用方当前问题
        }

        Order order = findByOrderNo(outTradeNo); // 读取当前业务所需数据
        if (order == null) { // 依据当前状态决定后续处理路径
            throw new BusinessException(404, "订单不存在"); // 以异常形式提示调用方当前问题
        }

        // 幂等处理：若该订单已支付，直接返回成功，避免用户重复刷新回跳页导致重复落库。
        if ("PAID".equals(order.getPaymentStatus())) { // 按该条件分支处理不同业务场景
            return new AlipaySyncVerifyResponse(true, "订单已支付", order.getId(), order.getPaymentStatus()); // 把结果交还给上层调用方
        }
        if ("REFUNDED".equals(order.getPaymentStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "订单已退款，支付结果不可重复确认"); // 抛出业务异常并中断当前流程
        }

        // 兼容说明：
        // 支付宝页面同步回跳（return_url）在部分场景下不会携带 trade_status，
        // 例如用户浏览器重定向参数被裁剪、网关只返回最小参数集合等。
        // 此时若直接按空状态判失败，会出现“明明已支付却提示未知状态”的误判。
        // 因此这里先尝试使用回跳参数中的 trade_status；若缺失则主动调“交易查询”接口兜底确认。
        String tradeStatus = resolveTradeStatus(params, outTradeNo); // 执行对应服务/DAO方法推进流程
        // 若回跳缺参且主动查询也失败，此时无法可靠确认支付结果，需明确告知用户稍后重试。
        if (!StringUtils.hasText(tradeStatus)) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "暂时无法确认支付状态, 请在1-3分钟后到订单列表刷新查看. 若仍未更新请联系管理员处理"); // 以异常形式提示调用方当前问题
        }
        if (!TRADE_SUCCESS.equals(tradeStatus) && !TRADE_FINISHED.equals(tradeStatus)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(400, "支付未成功（当前状态：" + mapTradeStatusLabel(tradeStatus) + "），请完成支付后重试"); // 抛出业务异常并中断当前流程
        }

        BigDecimal expectedAmount = calculatePayableAmount(order); // 调用组件能力完成当前步骤
        BigDecimal callbackAmount = parseMoney(totalAmountStr); // 借助已有方法完成该业务动作
        if (callbackAmount == null || callbackAmount.compareTo(expectedAmount) != 0) { // 依据当前状态决定后续处理路径
            throw new BusinessException(400, "支付金额校验失败（期望：" + expectedAmount + "，实际：" + callbackAmount + "）"); // 以异常形式提示调用方当前问题
        }

        // 复用既有支付落库逻辑：统一执行状态校验、状态变更与消息通知。
        orderService.payOrder(order.getId(), order.getTenantId()); // 调用组件能力完成当前步骤
        Order latest = orderMapper.selectById(order.getId()); // 从数据库加载后续处理对象
        return new AlipaySyncVerifyResponse(true, "支付成功", latest.getId(), latest.getPaymentStatus()); // 输出本方法最终结果
    }

    /**
     * 解析可用于业务判定的交易状态
     *
     * <p>优先使用同步回跳参数中的 trade_status；若缺失则主动查询支付宝交易状态。</p>
     *
     * @param params     回跳参数
     * @param outTradeNo 商户订单号
     * @return 交易状态码（可能为 null）
     */
    private String resolveTradeStatus(Map<String, String> params, String outTradeNo) { // 借助已有方法完成该业务动作
        String callbackTradeStatus = params.get("trade_status"); // 调用组件能力完成当前步骤
        if (StringUtils.hasText(callbackTradeStatus)) { // 依据当前状态决定后续处理路径
            return callbackTradeStatus; // 把结果交还给上层调用方
        }
        log.warn("支付宝同步回跳缺少 trade_status，开始主动查询交易状态，商户订单号={}", outTradeNo); // 借助已有方法完成该业务动作
        return queryTradeStatusByOutTradeNo(outTradeNo); // 返回当前阶段的处理结果
    }

    /**
     * 调用支付宝交易查询接口获取最新交易状态
     *
     * <p>该方法仅作为同步回跳缺少 trade_status 时的兜底，不替代验签逻辑。</p>
     *
     * @param outTradeNo 商户订单号
     * @return 支付宝交易状态码；查询失败时返回 null
     */
    private String queryTradeStatusByOutTradeNo(String outTradeNo) { // 从数据库加载后续处理对象
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest(); // 读取当前业务所需数据
        AlipayTradeQueryModel model = new AlipayTradeQueryModel(); // 从数据库加载后续处理对象
        model.setOutTradeNo(outTradeNo); // 补齐对象属性供后续流程使用
        request.setBizModel(model); // 给对象写入当前步骤需要的字段值
        try { // 在当前步骤完成必要业务动作
            AlipayTradeQueryResponse response = alipayClient.execute(request); // 先查出目标记录再做业务判断
            // 防御式编程：SDK 正常应返回对象，但若出现底层网络/反序列化边界异常导致空响应，这里避免空指针并给出可观测日志。
            if (response == null) { // 在该判断成立时执行对应逻辑
                log.warn("支付宝交易查询返回空响应，商户订单号={}", outTradeNo); // 调用组件能力完成当前步骤
                return null; // 把结果交还给上层调用方
            }
            if (!response.isSuccess()) { // 按该条件分支处理不同业务场景
                log.warn(
                        "支付宝交易查询失败，outTradeNo={}, code={}, subCode={}, msg={}, subMsg={}",
                        outTradeNo,
                        response.getCode(),
                        response.getSubCode(),
                        response.getMsg(),
                        response.getSubMsg()
                ); // 在当前步骤完成必要业务动作
                return null; // 把结果交还给上层调用方
            }
            return response.getTradeStatus(); // 返回当前阶段的处理结果
        } catch (AlipayApiException e) { // 在当前步骤完成必要业务动作
            log.error("支付宝交易查询异常，将返回空状态并触发支付确认失败，商户订单号={}", outTradeNo, e); // 借助已有方法完成该业务动作
            return null; // 把结果交还给上层调用方
        }
    }

    /**
     * 按业务规则校验订单是否满足发起支付条件
     *
     * @param order 订单
     */
    private void validateOrderCanPay(Order order) { // 借助已有方法完成该业务动作
        // 与 OrderServiceImpl.payOrder 保持一致：
        // 签约后订单会被标记为 SIGNED，因此支付前置状态需同时兼容 APPROVED/SIGNED。
        if (!OrderStatusUtil.isPayableStatus(order.getStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "仅已确认或已签约订单可支付"); // 抛出业务异常并中断当前流程
        }
        if ("PAID".equals(order.getPaymentStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "订单已支付，请勿重复支付"); // 以异常形式提示调用方当前问题
        }
        if ("REFUNDED".equals(order.getPaymentStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "订单已退款，无法再次支付"); // 立即返回错误避免继续执行
        }
        Contract contract = findLatestContractByOrderId(order.getId()); // 读取当前业务所需数据
        if (contract == null || !"FULLY_SIGNED".equals(contract.getStatus())) { // 按该条件分支处理不同业务场景
            throw new BusinessException(400, "合同双方签署完成后方可支付"); // 抛出业务异常并中断当前流程
        }
    }

    /**
     * 计算订单应付金额
     *
     * <p>优先使用 orders.total_amount；若为空或非正数，则回退为 月租金 + 押金，结果统一保留两位小数。</p>
     *
     * @param order 订单
     * @return 应付金额（两位小数）
     */
    private BigDecimal calculatePayableAmount(Order order) { // 调用组件能力完成当前步骤
        BigDecimal total = order.getTotalAmount(); // 执行对应服务/DAO方法推进流程
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) { // 按该条件分支处理不同业务场景
            BigDecimal monthlyRent = order.getMonthlyRent() == null ? BigDecimal.ZERO : order.getMonthlyRent(); // 借助已有方法完成该业务动作
            BigDecimal deposit = order.getDeposit() == null ? BigDecimal.ZERO : order.getDeposit(); // 调用组件能力完成当前步骤
            total = monthlyRent.add(deposit); // 调用组件能力完成当前步骤
        }
        total = total.setScale(2, RoundingMode.HALF_UP); // 补齐对象属性供后续流程使用
        if (total.compareTo(BigDecimal.ZERO) <= 0) { // 在该判断成立时执行对应逻辑
            throw new BusinessException(400, "订单金额异常，无法支付"); // 立即返回错误避免继续执行
        }
        return total; // 把结果交还给上层调用方
    }

    /**
     * 验签支付宝同步回调参数
     *
     * @param params 回调参数
     * @return true 表示验签通过
     */
    private boolean verifySign(Map<String, String> params) { // 执行对应服务/DAO方法推进流程
        try { // 在当前步骤完成必要业务动作
            return AlipaySignature.rsaCheckV1( // 输出本方法最终结果
                    params,
                    alipayProperties.getPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            ); // 在当前步骤完成必要业务动作
        } catch (AlipayApiException e) { // 在当前步骤完成必要业务动作
            log.error("支付宝回调验签异常", e); // 借助已有方法完成该业务动作
            return false; // 把结果交还给上层调用方
        }
    }

    /**
     * 校验支付宝关键配置是否齐全
     *
     * <p>由于本项目允许在未配置支付宝时继续使用其他功能，因此这里在“发起支付/验签”时做懒校验。</p>
     */
    private void validateAlipayConfig() { // 借助已有方法完成该业务动作
        requireConfig(alipayProperties.getAppId(), "alipay.app-id"); // 执行对应服务/DAO方法推进流程
        requireConfig(alipayProperties.getPrivateKey(), "alipay.private-key"); // 调用组件能力完成当前步骤
        requireConfig(alipayProperties.getPublicKey(), "alipay.public-key"); // 借助已有方法完成该业务动作
        requireConfig(alipayProperties.getGatewayUrl(), "alipay.gateway-url"); // 执行对应服务/DAO方法推进流程
        requireConfig(alipayProperties.getReturnUrl(), "alipay.return-url"); // 执行对应服务/DAO方法推进流程
    }

    /**
     * 配置非空断言
     *
     * @param val  配置值
     * @param name 配置名
     */
    private void requireConfig(String val, String name) { // 借助已有方法完成该业务动作
        if (!StringUtils.hasText(val)) { // 依据当前状态决定后续处理路径
            throw new BusinessException(500, "缺少支付宝配置：" + name); // 以异常形式提示调用方当前问题
        }
    }

    /**
     * 按订单号查询订单
     *
     * @param orderNo 订单编号
     * @return 订单实体
     */
    private Order findByOrderNo(String orderNo) { // 读取当前业务所需数据
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        wrapper.eq(Order::getOrderNo, orderNo); // 借助已有方法完成该业务动作
        java.util.List<Order> orders = orderMapper.selectList(wrapper); // 先查出目标记录再做业务判断
        if (orders == null || orders.isEmpty()) { // 按该条件分支处理不同业务场景
            return null; // 把结果交还给上层调用方
        }
        return orders.get(0); // 输出本方法最终结果
    }

    /**
     * 查询订单对应的最新合同（按创建时间倒序）
     *
     * @param orderId 订单 ID
     * @return 最新合同
     */
    private Contract findLatestContractByOrderId(Long orderId) { // 读取当前业务所需数据
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>(); // 先查出目标记录再做业务判断
        qw.eq(Contract::getOrderId, orderId).orderByDesc(Contract::getCreateTime); // 执行对应服务/DAO方法推进流程
        java.util.List<Contract> contracts = contractMapper.selectList(qw); // 先查出目标记录再做业务判断
        if (contracts == null || contracts.isEmpty()) { // 按该条件分支处理不同业务场景
            return null; // 把结果交还给上层调用方
        }
        return contracts.get(0); // 返回当前阶段的处理结果
    }

    /**
     * 字符串金额安全解析
     *
     * @param val 金额字符串
     * @return BigDecimal 或 null
     */
    private BigDecimal parseMoney(String val) { // 借助已有方法完成该业务动作
        if (!StringUtils.hasText(val)) { // 依据当前状态决定后续处理路径
            return null; // 把结果交还给上层调用方
        }
        try { // 在当前步骤完成必要业务动作
            return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP); // 返回当前阶段的处理结果
        } catch (NumberFormatException ex) { // 这里执行当前语句的核心处理
            return null; // 把结果交还给上层调用方
        }
    }

    /**
     * 将支付宝交易状态映射为更友好的中文文案
     *
     * @param tradeStatus 支付宝交易状态码
     * @return 中文状态描述
     */
    private String mapTradeStatusLabel(String tradeStatus) { // 调用组件能力完成当前步骤
        if (!StringUtils.hasText(tradeStatus)) { // 按该条件分支处理不同业务场景
            return "未知状态"; // 输出本方法最终结果
        }
        return switch (tradeStatus) { // 输出本方法最终结果
            case "WAIT_BUYER_PAY" -> "等待支付"; // 这里执行当前语句的核心处理
            case "TRADE_CLOSED" -> "交易已关闭"; // 按既定流程继续处理后续逻辑
            case "TRADE_SUCCESS" -> "支付成功"; // 按既定流程继续处理后续逻辑
            case "TRADE_FINISHED" -> "交易完成"; // 这里执行当前语句的核心处理
            default -> "状态码：" + tradeStatus; // 这里执行当前语句的核心处理
        };
    }
}
