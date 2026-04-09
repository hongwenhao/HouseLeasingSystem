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
 * @author hongwenhao
 * @description 实现站内消息的发送、分页查询、已读标记等业务逻辑
 */
@Slf4j
@Service // 声明为消息业务服务实现
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService { // 站内消息发送与已读管理的具体实现

    private final MessageMapper messageMapper; // 消息表数据访问组件

    /**
     * 发送站内消息，创建消息记录并保存到数据库（不关联具体业务对象）
     *
     * @param userId  接收者用户 ID
     * @param title   消息标题
     * @param content 消息内容
     * @param type    消息类型
     */
    @Override
    @Transactional
    public void sendMessage(Long userId, String title, String content, String type) { // 发送不带业务关联ID的消息
        // 无关联业务对象时，relatedId 传 null
        sendMessage(userId, title, content, type, null); // 复用完整重载方法，避免重复造消息对象
    }

    /**
     * 发送站内消息，创建消息记录并保存到数据库（关联具体业务对象）
     * relatedId 存储关联的订单/合同/房源 ID，前端可据此直接跳转到对应详情页
     *
     * @param userId    接收者用户 ID
     * @param title     消息标题
     * @param content   消息内容
     * @param type      消息类型
     * @param relatedId 关联业务对象 ID（可为 null）
     */
    @Override
    @Transactional
    public void sendMessage(Long userId, String title, String content, String type, Long relatedId) { // 发送可关联订单/合同/房源的消息
        Message message = new Message(); // 创建消息实体，准备写入数据库
        message.setUserId(userId); // 记录消息接收者，后续按用户维度查询
        message.setTitle(title); // 设置消息标题，列表页用于快速识别主题
        message.setContent(content); // 设置消息正文，详情页展示完整内容
        message.setType(type); // 设置消息类型，前端可按类型展示不同图标/样式
        message.setIsRead(false); // 新消息默认为未读
        message.setRelatedId(relatedId); // 记录关联业务对象 ID，便于前端跳转
        message.setCreateTime(LocalDateTime.now()); // 记录发送时间，支持按时间倒序展示
        messageMapper.insert(message); // 持久化消息记录
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
    public PageResult<Message> listMessages(Long userId, int page, int size) { // 分页查询某个用户的消息列表
        Page<Message> pageObj = new Page<>(page, size); // 封装分页参数（当前页/每页条数）
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>(); // 构建查询条件
        wrapper.eq(Message::getUserId, userId); // 只查当前用户的消息，避免越权读取
        wrapper.orderByDesc(Message::getCreateTime); // 按创建时间倒序，最新消息优先显示
        Page<Message> result = messageMapper.selectPage(pageObj, wrapper); // 执行分页查询
        return PageResult.of(result.getTotal(), result.getRecords(), page, size); // 统一返回前端分页结构
    }

    /**
     * 将指定消息标记为已读，验证消息所有权后更新状态
     *
     * @param messageId 消息 ID
     * @param userId    操作人用户 ID
     */
    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) { // 把指定消息标记为已读（含归属校验）
        Message message = messageMapper.selectById(messageId); // 先查消息，后续校验归属并更新状态
        // 消息不存在或不属于该用户时抛出异常
        if (message == null || !message.getUserId().equals(userId)) { // 按该条件分支处理不同业务场景
            throw new BusinessException(404, "消息不存在或无权访问"); // 抛出业务异常并中断当前流程
        }
        message.setIsRead(true); // 把已读标记更新为 true
        messageMapper.updateById(message); // 回写数据库，避免重复计入未读数
    }

    /**
     * 将用户所有未读消息批量标记为已读
     *
     * @param userId 用户 ID
     */
    @Override
    @Transactional
    public void markAllAsRead(Long userId) { // 批量把用户所有未读消息设为已读
        // 构建批量更新条件：属于该用户且未读的消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>(); // 构建批量更新筛选器
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false); // 精确锁定“该用户的未读消息”
        Message update = new Message(); // 构建“要更新成什么值”的实体
        update.setIsRead(true); // 批量把未读改为已读
        messageMapper.update(update, wrapper); // 执行批量更新，减少逐条更新开销
    }

    /**
     * 统计用户的未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    @Override
    public long countUnread(Long userId) { // 统计用户未读消息数量
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>(); // 构建未读统计条件
        wrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false); // 仅统计当前用户且未读的消息
        return messageMapper.selectCount(wrapper); // 返回未读条数给前端角标使用
    }
}
