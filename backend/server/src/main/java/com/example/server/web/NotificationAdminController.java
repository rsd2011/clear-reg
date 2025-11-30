package com.example.server.web;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.server.notification.dto.NotificationSendCommand;
import com.example.server.notification.NotificationService;
import com.example.server.notification.dto.NotificationSendRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/notifications")
@Validated
@Tag(name = "Notification Admin", description = "관리자 알림 발송 API")
public class NotificationAdminController {

    private final NotificationService notificationService;

    public NotificationAdminController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @RequirePermission(feature = FeatureCode.ALERT, action = ActionCode.UPDATE)
    public void send(@Valid @RequestBody NotificationSendRequest request) {
        NotificationSendCommand command = new NotificationSendCommand(
                request.recipients(),
                request.title(),
                request.message(),
                request.severity(),
                request.channel(),
                request.link(),
                request.metadata());
        notificationService.send(command, currentUsername());
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(ctx -> ctx.username())
                .orElseThrow(() -> new IllegalStateException("인증 정보가 없습니다."));
    }
}
