package com.example.batch.audit;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HOT→COLD 이동 이후 Object Lock/Glacier 전송을 외부 스크립트/배치로 호출하기 위한 훅.
 * 현재는 로그만 남기며, 향후 Shell/BatchLauncher 연동 시 이 클래스를 확장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditArchiveJob {

    private final Clock clock;

    @Value("${audit.archive.enabled:false}")
    private boolean enabled;

    @Value("${audit.archive.cron:0 30 3 2 * *}")
    private String cron;

    @Value("${audit.archive.command:}")
    private String archiveCommand;

    @Value("${audit.archive.retry:3}")
    private int retry;

    @Value("${audit.archive.slack-webhook:}")
    private String slackWebhook;

    @Value("${audit.archive.alert.enabled:true}")
    private boolean alertEnabled;

    @Value("${audit.archive.alert.mention:}")
    private String alertMention;

    @Value("${audit.archive.alert.channel:#audit-ops}")
    private String alertChannel;

    @Value("${audit.archive.alert.delay-threshold-ms:60000}")
    private long delayThresholdMs;

    private RestTemplate restTemplate = new RestTemplate();
    /** 테스트 용도 주입 가능한 커맨드 실행자 */
    private CommandInvoker invoker = this::runCommand;

    @Scheduled(cron = "${audit.archive.cron:0 30 3 2 * *}")
    public void archiveColdPartitions() {
        if (!enabled) {
            return;
        }
        LocalDate target = LocalDate.now(clock).minusMonths(7).withDayOfMonth(1);
        log.info("[audit-archive] trigger archive for {} (cron={}, cmd={}, retry={})", target, cron, archiveCommand, retry);
        if (!StringUtils.hasText(archiveCommand)) {
            return; // noop if command not provided
        }
        int attempts = Math.max(retry, 1);
        AtomicReference<Integer> lastExit = new AtomicReference<>(0);
        long start = System.currentTimeMillis();
        for (int i = 1; i <= attempts; i++) {
            try {
                int exit = invoker.run(archiveCommand, target.toString());
                lastExit.set(exit);
                if (exit == 0) {
                    log.info("[audit-archive] archive command succeeded on attempt {}", i);
                    maybeAlertDelay(target, start);
                    return;
                }
                log.warn("[audit-archive] command exited {} on attempt {}/{}", exit, i, attempts);
            } catch (Exception e) {
                log.warn("[audit-archive] command failed on attempt {}/{}: {}", i, attempts, e.getMessage());
            }
        }
        log.error("[audit-archive] archive command failed after {} attempts, lastExit={}", attempts, lastExit.get());
        notifyFailure(target, lastExit.get());
    }

    /** 테스트 용도 */
    void setInvoker(CommandInvoker invoker) {
        this.invoker = invoker;
    }

    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private int runCommand(String command, String targetMonth) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command, targetMonth);
        // 기본 환경 변수 전달 (Object Lock 스크립트에서 사용)
        pb.environment().putAll(System.getenv());
        Process proc = pb.start();
        int exit = proc.waitFor();
        return exit;
    }

    private void notifyFailure(LocalDate target, int exitCode) {
        if (!alertEnabled || !StringUtils.hasText(slackWebhook)) {
            return;
        }
        try {
            String payload = """
                    {
                      "text":"[audit-archive] :x: FAILED target=%s exitCode=%d (host=%s) %s",
                      "channel":"%s"
                    }
                    """.formatted(target, exitCode, java.net.InetAddress.getLocalHost().getHostName(),
                    mention(), alertChannel);
            restTemplate.postForEntity(slackWebhook, payload.replaceAll("\\s+", " "), String.class);
        } catch (Exception e) {
            log.warn("[audit-archive] failed to send slack alert: {}", e.getMessage());
        }
    }

    private void maybeAlertDelay(LocalDate target, long startMs) {
        long elapsed = System.currentTimeMillis() - startMs;
        if (!alertEnabled || !StringUtils.hasText(slackWebhook)) return;
        if (elapsed < delayThresholdMs) return;
        try {
            String payload = """
                    {
                      "text":"[audit-archive] :warning: SLOW archive target=%s elapsedMs=%d (host=%s) %s",
                      "channel":"%s"
                    }
                    """.formatted(target, elapsed, java.net.InetAddress.getLocalHost().getHostName(),
                    mention(), alertChannel);
            restTemplate.postForEntity(slackWebhook, payload.replaceAll("\\s+", " "), String.class);
        } catch (Exception e) {
            log.warn("[audit-archive] failed to send slack delay alert: {}", e.getMessage());
        }
    }

    private String mention() {
        return StringUtils.hasText(alertMention) ? alertMention : "";
    }

    @FunctionalInterface
    interface CommandInvoker {
        int run(String command, String targetMonth) throws Exception;
    }
}
