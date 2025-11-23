package com.example.dw.application.export;

/**
 * Export 실패 시 알림/후처리를 위한 인터페이스.
 * 구현체는 Slack/Webhook/SIEM 등 외부 연동을 담당하며, 기본 구현은 No-op.
 */
public interface ExportFailureNotifier {
    void notify(ExportFailureEvent event);

    ExportFailureNotifier NOOP = event -> {};
}
