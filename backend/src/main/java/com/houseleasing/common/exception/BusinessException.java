package com.houseleasing.common.exception;

import lombok.Getter;

/**
 * 业务逻辑异常类
 *
 * @author hongwenhao
 * @description 用于表示业务逻辑异常，携带自定义的 HTTP 状态码和错误消息，
 *              由全局异常处理器捕获并返回给客户端
 */
@Getter
public class BusinessException extends RuntimeException {
    /** HTTP 错误状态码 */
    private final int code;

    /**
     * 创建默认状态码（400）的业务异常
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    /**
     * 创建自定义状态码的业务异常
     *
     * @param code    HTTP 错误状态码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
