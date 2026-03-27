package com.houseleasing.common.exception;

import com.houseleasing.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author HouseLeasingSystem开发团队
 * @description 统一处理控制器层抛出的各类异常，将异常信息转换为标准响应格式返回给客户端，
 *              涵盖业务异常、参数校验异常、认证异常、权限异常及未知异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务逻辑异常
     *
     * @param e 业务异常对象
     * @return 包含业务错误码和消息的响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求参数校验失败异常
     * 将所有字段校验错误信息拼接后返回
     *
     * @param e 参数校验异常对象
     * @return 包含所有校验错误信息的响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 收集所有字段校验错误消息并用逗号分隔
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理认证失败异常（未登录或 Token 无效）
     *
     * @param e 认证异常对象
     * @return 401 未授权响应结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证异常: {}", e.getMessage());
        return Result.error(401, "认证失败，请重新登录或重新获取令牌");
    }

    /**
     * 处理权限不足异常（已登录但无操作权限）
     *
     * @param e 访问拒绝异常对象
     * @return 403 禁止访问响应结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(403, "无权访问");
    }

    /**
     * 处理所有未捕获的未知异常
     *
     * @param e 异常对象
     * @return 500 服务器内部错误响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("未预期的系统异常", e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
