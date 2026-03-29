package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.entity.Message;

/**
 * 消息服务接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 定义站内消息相关的业务操作，包括发送消息和查询
 */
public interface MessageService {

    /**
     * 向指定用户发送站内消息
     *
     * @param userId  接收消息的用户 ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型（APPOINTMENT/CONTRACT/ORDER）
     */
    void sendMessage(Long userId, String title, String content, String type);

    /**
     * 分页查询用户的消息列表（按创建时间降序）
     *
     * @param userId 用户 ID
     * @param page   当前页码
     * @param size   每页大小
     * @return 该用户的分页消息列表
     */
    PageResult<Message> listMessages(Long userId, int page, int size);
}
