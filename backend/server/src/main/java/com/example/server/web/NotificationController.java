package com.example.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.server.notification.NotificationService;
import com.example.server.notification.dto.NotificationResponse;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.ALERT, action = ActionCode.READ)
    public List<NotificationResponse> myNotifications() {
        String username = currentUsername();
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return notificationService.notificationsFor(username).stream()
                .map(n -> NotificationResponse.from(n, masker))
                .toList();
    }

    @PostMapping("/{id}/read")
    @RequirePermission(feature = FeatureCode.ALERT, action = ActionCode.READ)
    public void markRead(@PathVariable UUID id) {
        notificationService.markAsRead(id, currentUsername());
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(ctx -> ctx.username())
                .orElseThrow(() -> new IllegalStateException("인증 정보가 없습니다."));
    }
}
