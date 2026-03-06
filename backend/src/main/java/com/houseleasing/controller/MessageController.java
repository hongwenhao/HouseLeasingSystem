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

@Tag(name = "Message", description = "Message management")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class MessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;

    @Operation(summary = "List messages")
    @GetMapping
    public Result<PageResult<Message>> listMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(messageService.listMessages(user.getId(), page, size));
    }

    @Operation(summary = "Count unread messages")
    @GetMapping("/unread/count")
    public Result<Long> countUnread(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        return Result.success(messageService.countUnread(user.getId()));
    }

    @Operation(summary = "Mark message as read")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        messageService.markAsRead(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "Mark all messages as read")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails.getUsername());
        messageService.markAllAsRead(user.getId());
        return Result.success();
    }

    private User resolveUser(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }
}
