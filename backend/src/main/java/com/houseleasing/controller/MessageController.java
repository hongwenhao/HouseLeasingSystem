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
 * @author HouseLeasingSystem开发团队
 * @description 提供站内消息相关的 REST API，包括查询消息列表、未读消息计数和标记已读，
 *              所有接口均需要 JWT 认证
 */
@Tag(name = "Message", description = "Message management")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class MessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;

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
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(messageService.listMessages(user.getId(), page, size));
    }

    /**
     * 统计当前用户的未读消息数量
     *
     * @param userDetails 当前登录用户信息
     * @return 未读消息数量
     */
    @Operation(summary = "Count unread messages")
    @GetMapping("/unread/count")
    public Result<Long> countUnread(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(messageService.countUnread(user.getId()));
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
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        messageService.markAsRead(id, user.getId());
        return Result.success();
    }

    /**
     * 将当前用户所有未读消息标记为已读
     *
     * @param userDetails 当前登录用户信息
     * @return 操作成功的响应
     */
    @Operation(summary = "Mark all messages as read")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        messageService.markAllAsRead(user.getId());
        return Result.success();
    }

    /**
     * 根据用户名解析用户信息
     *
     * @param username 用户名
     * @return 对应的用户实体
     */
    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
