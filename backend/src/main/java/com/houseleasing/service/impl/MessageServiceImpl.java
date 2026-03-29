package com.houseleasing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.houseleasing.common.PageResult;
import com.houseleasing.service.MessageService;
import com.houseleasing.entity.Message;
import com.houseleasing.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 消息服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 实现站内消息的发送和分页查询业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    /**
     * 发送站内消息，创建消息记录并保存到数据库
     *
     * @param userId  接收者用户 ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     */
    @Override
    @Transactional
    public void sendMessage(Long userId, String title, String content, String type) {
        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    /**
     * 分页查询用户的消息列表，按创建时间降序排列
     *
     * @param userId 用户 ID
     * @param page   当前页码
     * @param size   每页大小
     * @return 分页消息列表
     */
    @Override
    public PageResult<Message> listMessages(Long userId, int page, int size) {
        Page<Message> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId);
        wrapper.orderByDesc(Message::getCreateTime);
        Page<Message> result = messageMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), page, size);
    }
}
