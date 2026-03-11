package com.houseleasing.common;

import lombok.Data;

/**
 * 统一响应结果封装类
 *
 * @author HouseLeasingSystem开发团队
 * @description 用于统一封装 API 接口的响应结果，包含状态码、消息和数据
 * @param <T> 响应数据的类型
 */
@Data
public class Result<T> {
    /** HTTP 状态码 */
    private int code;
    /** 响应消息 */
    private String message;
    /** 响应数据 */
    private T data;

    /**
     * 私有构造方法，通过静态工厂方法创建实例
     *
     * @param code    状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 返回无数据的成功结果（HTTP 200）
     *
     * @param <T> 数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 返回携带数据的成功结果（HTTP 200）
     *
     * @param <T>  数据类型
     * @param data 响应数据
     * @return 成功的响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 返回携带自定义消息和数据的成功结果（HTTP 200）
     *
     * @param <T>     数据类型
     * @param message 自定义消息
     * @param data    响应数据
     * @return 成功的响应结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 返回默认错误码（500）的失败结果
     *
     * @param <T>     数据类型
     * @param message 错误消息
     * @return 失败的响应结果
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 返回自定义错误码的失败结果
     *
     * @param <T>     数据类型
     * @param code    自定义错误码
     * @param message 错误消息
     * @return 失败的响应结果
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
