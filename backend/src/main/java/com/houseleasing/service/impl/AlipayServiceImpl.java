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
 *
 * <p>设计说明：
 * 1) 发起支付时只生成支付宝官方收银台表单，不直接改订单状态；
 * 2) 用户完成支付并回跳前端后，由前端把同步参数交给本服务验签；
 * 3) 验签通过且支付成功后，再调用订单支付逻辑完成“PAID + COMPLETED”落库，保证状态变更口径统一。</p>
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

    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_FINISHED = "TRADE_FINISHED";

    /**
     * 生成支付宝收银台表单（同步回调方案）
     *
     * @param orderId  订单 ID
     * @param tenantId 当前租客 ID
     * @return 可直接提交到支付宝网关的 HTML 表单
     */
    @Override
    public String createPayForm(Long orderId, Long tenantId) { // 生成支付宝收银台表单
        validateAlipayConfig();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Objects.equals(order.getTenantId(), tenantId)) {
            throw new BusinessException(403, "没有操作权限");
        }
        validateOrderCanPay(order);
        BigDecimal payableAmount = calculatePayableAmount(order);

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(alipayProperties.getReturnUrl());
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(order.getOrderNo());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTotalAmount(payableAmount.toPlainString());
        model.setSubject("房屋租赁订单支付-" + order.getOrderNo());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("tenantId", order.getTenantId());
        model.setBody(JSONUtil.toJsonStr(payload));
        model.setTimeoutExpress(alipayProperties.getTimeoutExpress());
        request.setBizModel(model);

        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付表单失败，订单ID={}", orderId, e);
            throw new BusinessException(500, "发起支付宝支付失败，请稍后重试");
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
        validateAlipayConfig();
        if (params == null || params.isEmpty()) {
            throw new BusinessException(400, "回调参数为空");
        }
        String outTradeNo = params.get("out_trade_no");
        String appId = params.get("app_id");
        String totalAmountStr = params.get("total_amount");

        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException(400, "缺少订单号");
        }
        if (!StringUtils.hasText(appId) || !Objects.equals(appId, alipayProperties.getAppId())) {
            throw new BusinessException(400, "回调应用标识不匹配");
        }

        boolean signVerified = verifySign(params);
        if (!signVerified) {
            throw new BusinessException(400, "支付宝验签失败");
        }

        Order order = findByOrderNo(outTradeNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 幂等处理：若该订单已支付，直接返回成功，避免用户重复刷新回跳页导致重复落库。
        if ("PAID".equals(order.getPaymentStatus())) {
            return new AlipaySyncVerifyResponse(true, "订单已支付", order.getId(), order.getPaymentStatus());
        }
        if ("REFUNDED".equals(order.getPaymentStatus())) {
            throw new BusinessException(400, "订单已退款，支付结果不可重复确认");
        }

        // 兼容说明：
        // 支付宝页面同步回跳（return_url）在部分场景下不会携带 trade_status，
        // 例如用户浏览器重定向参数被裁剪、网关只返回最小参数集合等。
        // 此时若直接按空状态判失败，会出现“明明已支付却提示未知状态”的误判。
        // 因此这里先尝试使用回跳参数中的 trade_status；若缺失则主动调“交易查询”接口兜底确认。
        String tradeStatus = resolveTradeStatus(params, outTradeNo);
        // 若回跳缺参且主动查询也失败，此时无法可靠确认支付结果，需明确告知用户稍后重试。
        if (!StringUtils.hasText(tradeStatus)) {
            throw new BusinessException(400, "暂时无法确认支付状态, 请在1-3分钟后到订单列表刷新查看. 若仍未更新请联系管理员处理");
        }
        if (!TRADE_SUCCESS.equals(tradeStatus) && !TRADE_FINISHED.equals(tradeStatus)) {
            throw new BusinessException(400, "支付未成功（当前状态：" + mapTradeStatusLabel(tradeStatus) + "），请完成支付后重试");
        }

        BigDecimal expectedAmount = calculatePayableAmount(order);
        BigDecimal callbackAmount = parseMoney(totalAmountStr);
        if (callbackAmount == null || callbackAmount.compareTo(expectedAmount) != 0) {
            throw new BusinessException(400, "支付金额校验失败（期望：" + expectedAmount + "，实际：" + callbackAmount + "）");
        }

        // 复用既有支付落库逻辑：统一执行状态校验、状态变更与消息通知。
        orderService.payOrder(order.getId(), order.getTenantId());
        Order latest = orderMapper.selectById(order.getId());
        return new AlipaySyncVerifyResponse(true, "支付成功", latest.getId(), latest.getPaymentStatus());
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
    private String resolveTradeStatus(Map<String, String> params, String outTradeNo) {
        String callbackTradeStatus = params.get("trade_status");
        if (StringUtils.hasText(callbackTradeStatus)) {
            return callbackTradeStatus;
        }
        log.warn("支付宝同步回跳缺少 trade_status，开始主动查询交易状态，商户订单号={}", outTradeNo);
        return queryTradeStatusByOutTradeNo(outTradeNo);
    }

    /**
     * 调用支付宝交易查询接口获取最新交易状态
     *
     * <p>该方法仅作为同步回跳缺少 trade_status 时的兜底，不替代验签逻辑。</p>
     *
     * @param outTradeNo 商户订单号
     * @return 支付宝交易状态码；查询失败时返回 null
     */
    private String queryTradeStatusByOutTradeNo(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            // 防御式编程：SDK 正常应返回对象，但若出现底层网络/反序列化边界异常导致空响应，这里避免空指针并给出可观测日志。
            if (response == null) {
                log.warn("支付宝交易查询返回空响应，商户订单号={}", outTradeNo);
                return null;
            }
            if (!response.isSuccess()) {
                log.warn(
                        "支付宝交易查询失败，outTradeNo={}, code={}, subCode={}, msg={}, subMsg={}",
                        outTradeNo,
                        response.getCode(),
                        response.getSubCode(),
                        response.getMsg(),
                        response.getSubMsg()
                );
                return null;
            }
            return response.getTradeStatus();
        } catch (AlipayApiException e) {
            log.error("支付宝交易查询异常，将返回空状态并触发支付确认失败，商户订单号={}", outTradeNo, e);
            return null;
        }
    }

    /**
     * 按业务规则校验订单是否满足发起支付条件
     *
     * @param order 订单
     */
    private void validateOrderCanPay(Order order) {
        // 与 OrderServiceImpl.payOrder 保持一致：
        // 签约后订单会被标记为 SIGNED，因此支付前置状态需同时兼容 APPROVED/SIGNED。
        if (!OrderStatusUtil.isPayableStatus(order.getStatus())) {
            throw new BusinessException(400, "仅已确认或已签约订单可支付");
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
    }

    /**
     * 计算订单应付金额
     *
     * <p>优先使用 orders.total_amount；若为空或非正数，则回退为 月租金 + 押金，结果统一保留两位小数。</p>
     *
     * @param order 订单
     * @return 应付金额（两位小数）
     */
    private BigDecimal calculatePayableAmount(Order order) {
        BigDecimal total = order.getTotalAmount();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal monthlyRent = order.getMonthlyRent() == null ? BigDecimal.ZERO : order.getMonthlyRent();
            BigDecimal deposit = order.getDeposit() == null ? BigDecimal.ZERO : order.getDeposit();
            total = monthlyRent.add(deposit);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "订单金额异常，无法支付");
        }
        return total;
    }

    /**
     * 验签支付宝同步回调参数
     *
     * @param params 回调参数
     * @return true 表示验签通过
     */
    private boolean verifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签异常", e);
            return false;
        }
    }

    /**
     * 校验支付宝关键配置是否齐全
     *
     * <p>由于本项目允许在未配置支付宝时继续使用其他功能，因此这里在“发起支付/验签”时做懒校验。</p>
     */
    private void validateAlipayConfig() {
        requireConfig(alipayProperties.getAppId(), "alipay.app-id");
        requireConfig(alipayProperties.getPrivateKey(), "alipay.private-key");
        requireConfig(alipayProperties.getPublicKey(), "alipay.public-key");
        requireConfig(alipayProperties.getGatewayUrl(), "alipay.gateway-url");
        requireConfig(alipayProperties.getReturnUrl(), "alipay.return-url");
    }

    /**
     * 配置非空断言
     *
     * @param val  配置值
     * @param name 配置名
     */
    private void requireConfig(String val, String name) {
        if (!StringUtils.hasText(val)) {
            throw new BusinessException(500, "缺少支付宝配置：" + name);
        }
    }

    /**
     * 按订单号查询订单
     *
     * @param orderNo 订单编号
     * @return 订单实体
     */
    private Order findByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        java.util.List<Order> orders = orderMapper.selectList(wrapper);
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        return orders.get(0);
    }

    /**
     * 查询订单对应的最新合同（按创建时间倒序）
     *
     * @param orderId 订单 ID
     * @return 最新合同
     */
    private Contract findLatestContractByOrderId(Long orderId) {
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>();
        qw.eq(Contract::getOrderId, orderId).orderByDesc(Contract::getCreateTime);
        java.util.List<Contract> contracts = contractMapper.selectList(qw);
        if (contracts == null || contracts.isEmpty()) {
            return null;
        }
        return contracts.get(0);
    }

    /**
     * 字符串金额安全解析
     *
     * @param val 金额字符串
     * @return BigDecimal 或 null
     */
    private BigDecimal parseMoney(String val) {
        if (!StringUtils.hasText(val)) {
            return null;
        }
        try {
            return new BigDecimal(val).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 将支付宝交易状态映射为更友好的中文文案
     *
     * @param tradeStatus 支付宝交易状态码
     * @return 中文状态描述
     */
    private String mapTradeStatusLabel(String tradeStatus) {
        if (!StringUtils.hasText(tradeStatus)) {
            return "未知状态";
        }
        return switch (tradeStatus) {
            case "WAIT_BUYER_PAY" -> "等待支付";
            case "TRADE_CLOSED" -> "交易已关闭";
            case "TRADE_SUCCESS" -> "支付成功";
            case "TRADE_FINISHED" -> "交易完成";
            default -> "状态码：" + tradeStatus;
        };
    }
}
