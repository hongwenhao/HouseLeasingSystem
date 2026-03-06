package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.service.MessageService;
import com.houseleasing.entity.Message;
import com.houseleasing.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public void sendMessage(Long userId, String title, String content, String type) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setIsRead(false);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    @Override
    public PageResult<Message> listMessages(Long userId, int page, int size) {
        Page<Message> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);
        wrapper.orderByDesc(Message::getCreateTime);
        Page<Message> result = messageMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BusinessException(404, "Message not found");
        }
        message.setIsRead(true);
        messageMapper.updateById(message);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false);
        Message update = new Message();
        update.setIsRead(true);
        messageMapper.update(update, wrapper);
    }

    @Override
    public long countUnread(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false);
        return messageMapper.selectCount(wrapper);
    }
}
