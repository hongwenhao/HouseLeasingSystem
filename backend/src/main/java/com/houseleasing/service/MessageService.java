package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.entity.Message;

/**
 * 消息服务接口
 *
 * @author HouseLeasingSystem开发团队
 * @description 定义站内消息相关的业务操作，包括发送消息、查询、标记已读等
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

    /**
     * 将指定消息标记为已读
     *
     * @param messageId 消息 ID
     * @param userId    操作人用户 ID（需与消息接收人一致）
     */
    void markAsRead(Long messageId, Long userId);

    /**
     * 将指定用户的所有未读消息标记为已读
     *
     * @param userId 用户 ID
     */
    void markAllAsRead(Long userId);

    /**
     * 统计指定用户的未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息总数
     */
    long countUnread(Long userId);
}
