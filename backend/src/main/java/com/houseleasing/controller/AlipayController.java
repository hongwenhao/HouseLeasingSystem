package com.houseleasing.controller;

import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.dto.AlipayCreateRequest;
import com.houseleasing.dto.AlipayCreateResponse;
import com.houseleasing.dto.AlipaySyncVerifyResponse;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.AlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付宝支付控制器（沙箱，同步回调方案）
 *
 * <p>接口职责：
 * 1) 租客发起支付：返回支付宝自动提交表单；
 * 2) 前端回跳页提交同步参数：后端验签并更新订单支付状态。</p>
 */
@Tag(name = "Alipay", description = "Alipay sandbox payment APIs")
@RestController
@RequestMapping("/api/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;
    private final UserMapper userMapper;

    /**
     * 发起支付宝支付（租客）
     *
     * @param request     请求体，仅需 orderId
     * @param userDetails 当前登录用户
     * @return 支付宝自动提交表单 HTML
     */
    @Operation(summary = "Create alipay page pay form")
    @PostMapping("/pay/create")
    public Result<AlipayCreateResponse> createPayForm(@RequestBody AlipayCreateRequest request,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException(400, "订单ID不能为空");
        }
        User user = resolveUser(userDetails.getUsername());
        String formHtml = alipayService.createPayForm(request.getOrderId(), user.getId());
        return Result.success(new AlipayCreateResponse(formHtml));
    }

    /**
     * 同步回调验签与结果落库（仅同步，不使用异步通知）
     *
     * <p>前端在支付宝 return_url 页面收集 query 参数后，直接调用本接口。
     * 后端完成验签、金额校验和订单状态更新，然后返回明确业务结果。</p>
     *
     * @param params 同步回调参数
     * @return 验签+业务处理结果
     */
    @Operation(summary = "Verify alipay sync return params")
    @PostMapping("/pay/sync/verify")
    public Result<AlipaySyncVerifyResponse> verifySyncReturn(@RequestBody Map<String, String> params) {
        return Result.success(alipayService.verifyAndHandleSyncReturn(params));
    }

    /**
     * 根据用户名解析用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}

