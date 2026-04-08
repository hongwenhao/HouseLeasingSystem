package com.houseleasing.controller;

import com.houseleasing.common.PageResult;
import com.houseleasing.common.Result;
import com.houseleasing.common.exception.BusinessException;
import com.houseleasing.entity.Message;
import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import com.houseleasing.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 消息管理控制器
 *
 * @author hongwenhao
 * @description 提供站内消息相关的 REST API，包括查询消息列表、未读消息计数和标记已读，
 *              所有接口均需要 JWT 认证
 */
@Tag(name = "Message", description = "Message management")
@RestController
@RequestMapping("/api/messages") // 消息接口统一前缀
@RequiredArgsConstructor // 自动生成构造函数注入依赖
@SecurityRequirement(name = "Bearer Authentication")
public class MessageController { // 处理消息列表、已读状态等请求

    private final MessageService messageService; // 消息业务服务
    private final UserMapper userMapper; // 用户查询组件

    /**
     * 查询当前用户的消息列表（分页，按时间降序）
     *
     * @param userDetails 当前登录用户信息
     * @param page        当前页码
     * @param size        每页大小
     * @return 分页消息列表
     */
    @Operation(summary = "List messages")
    @GetMapping
    public Result<PageResult<Message>> listMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) { // 分页参数：默认第1页，每页10条
        User user = resolveUser(userDetails.getUsername()); // 解析当前登录用户
        return Result.success(messageService.listMessages(user.getId(), page, size)); // 查询并返回该用户的消息分页列表
    }

    /**
     * 统计当前用户的未读消息数量
     *
     * @param userDetails 当前登录用户信息
     * @return 未读消息数量
     */
    @Operation(summary = "Count unread messages")
    @GetMapping("/unread/count")
    public Result<Long> countUnread(@AuthenticationPrincipal UserDetails userDetails) { // 获取当前用户未读消息数量
        User user = resolveUser(userDetails.getUsername()); // 解析登录用户
        return Result.success(messageService.countUnread(user.getId())); // 返回未读条数
    }

    /**
     * 将指定消息标记为已读
     *
     * @param id          消息 ID
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Mark message as read")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails) { // 把指定消息标记为已读
        User user = resolveUser(userDetails.getUsername()); // 获取当前用户ID，防止越权操作
        messageService.markAsRead(id, user.getId()); // 执行“已读”更新
        return Result.success(); // 返回操作成功
    }

    /**
     * 将当前用户所有未读消息标记为已读
     *
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Mark all messages as read")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) { // 把当前用户全部未读消息设为已读
        User user = resolveUser(userDetails.getUsername()); // 解析当前用户
        messageService.markAllAsRead(user.getId()); // 执行批量已读
        return Result.success(); // 返回操作成功
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) { // 通用方法：通过用户名查用户
        User user = userMapper.selectByUsername(username); // 访问数据库查询用户
        if (user == null) { // 查不到说明用户无效
            throw new BusinessException(404, "用户不存在"); // 抛出可读的业务异常
        }
        return user; // 返回用户对象
    }
}
