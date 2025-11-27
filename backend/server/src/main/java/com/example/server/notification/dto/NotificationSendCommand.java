package com.example.server.notification.dto;

import java.util.List;
import java.util.Map;

import com.example.server.notification.NotificationChannel;
import com.example.server.notification.NotificationSeverity;

public record NotificationSendCommand(List<String> recipients,
                                      String title,
                                      String message,
                                      NotificationSeverity severity,
                                      NotificationChannel channel,
                                      String link,
                                      Map<String, Object> metadata) {
}
