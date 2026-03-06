package com.houseleasing.service;

import com.houseleasing.common.PageResult;
import com.houseleasing.entity.Message;

public interface MessageService {
    void sendMessage(Long userId, String title, String content, String type);
    PageResult<Message> listMessages(Long userId, int page, int size);
    void markAsRead(Long messageId, Long userId);
    void markAllAsRead(Long userId);
    long countUnread(Long userId);
}
