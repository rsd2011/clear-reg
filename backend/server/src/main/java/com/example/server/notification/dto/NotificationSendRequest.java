package com.example.server.notification.dto;

import java.util.List;
import java.util.Map;

import com.example.server.notification.NotificationChannel;
import com.example.server.notification.NotificationSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record NotificationSendRequest(@NotEmpty List<@NotBlank String> recipients,
                                      @NotBlank String title,
                                      @NotBlank String message,
                                      NotificationSeverity severity,
                                      NotificationChannel channel,
                                      String link,
                                      Map<String, Object> metadata) {
}
