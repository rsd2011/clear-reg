package com.example.server.notification;

import java.util.List;
import java.util.Map;

public record NotificationSendCommand(List<String> recipients,
                                      String title,
                                      String message,
                                      NotificationSeverity severity,
                                      NotificationChannel channel,
                                      String link,
                                      Map<String, Object> metadata) {
}
