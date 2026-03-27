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

/**
 * 消息服务实现类
 *
 * @author HouseLeasingSystem开发团队
 * @description 实现站内消息的发送、分页查询、已读标记等业务逻辑
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
        message.setIsRead(false); // 新消息默认为未读
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

    /**
     * 将指定消息标记为已读，验证消息所有权后更新状态
     *
     * @param messageId 消息 ID
     * @param userId    操作人用户 ID
     */
    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        // 消息不存在或不属于该用户时抛出异常
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BusinessException(404, "消息不存在或无权访问");
        }
        message.setIsRead(true);
        messageMapper.updateById(message);
    }

    /**
     * 将用户所有未读消息批量标记为已读
     *
     * @param userId 用户 ID
     */
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        // 构建批量更新条件：属于该用户且未读的消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false);
        Message update = new Message();
        update.setIsRead(true);
        messageMapper.update(update, wrapper);
    }

    /**
     * 统计用户的未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    @Override
    public long countUnread(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false);
        return messageMapper.selectCount(wrapper);
    }
}
