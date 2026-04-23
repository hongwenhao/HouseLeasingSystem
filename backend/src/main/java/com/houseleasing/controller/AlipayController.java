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
@RequestMapping("/api/alipay") // 统一支付宝接口前缀
@RequiredArgsConstructor // 自动生成构造函数并注入依赖
public class AlipayController { // 负责支付宝支付创建与回调验签

    private final AlipayService alipayService; // 支付宝业务处理服务
    private final UserMapper userMapper; // 用户查询，用于拿到当前登录用户

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
                                                       @AuthenticationPrincipal UserDetails userDetails) { // 生成支付宝支付表单
        if (request == null || request.getOrderId() == null) { // 防止前端没传订单ID
            throw new BusinessException(400, "订单ID不能为空"); // 参数不完整，直接返回业务错误
        }
        User user = resolveUser(userDetails.getUsername()); // 根据登录用户名查出系统用户
        // 前端会传当前站点生成的 returnUrl，确保支付后回到“与登录态一致的同源地址”。
        String formHtml = alipayService.createPayForm(request.getOrderId(), user.getId(), request.getReturnUrl()); // 生成支付宝自动提交的 HTML 表单
        return Result.success(new AlipayCreateResponse(formHtml)); // 把表单包装后返回给前端
    }

    /**
     * 同步回调验签与结果落库（仅同步，不使用异步通知）
     *
     * 前端在支付宝 return_url 页面收集 query 参数后，直接调用本接口。
     * 后端完成验签、金额校验和订单状态更新，然后返回明确业务结果。
     *
     * @param params 同步回调参数
     * @return 验签+业务处理结果
     */
    @Operation(summary = "Verify alipay sync return params")
    @PostMapping("/pay/sync/verify")
    public Result<AlipaySyncVerifyResponse> verifySyncReturn(@RequestBody Map<String, String> params) { // 验签并处理支付宝同步回调
        return Result.success(alipayService.verifyAndHandleSyncReturn(params)); // 验证签名成功后更新订单支付状态并返回结果
    }

    /**
     * 根据用户名解析用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    private User resolveUser(String username) { // 通用方法：把用户名转换为用户实体
        User user = userMapper.selectByUsername(username); // 按用户名查询数据库
        if (user == null) { // 查不到说明登录态对应用户异常
            throw new BusinessException(404, "用户不存在"); // 明确提示“用户不存在”
        }
        return user; // 返回查询到的用户对象
    }
}
