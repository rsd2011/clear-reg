package com.example.batch.audit;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * HOT→COLD 이동 이후 Object Lock/Glacier 전송을 외부 스크립트/배치로 호출하기 위한 훅.
 * 현재는 로그만 남기며, 향후 Shell/BatchLauncher 연동 시 이 클래스를 확장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditArchiveJob {

    private final Clock clock;
    private final MeterRegistry meterRegistry;

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
    private Counter successCounter;
    private Counter failureCounter;
    private Timer latencyTimer;

    @PostConstruct
    void initMeters() {
        successCounter = meterRegistry.counter("audit_archive_success_total");
        failureCounter = meterRegistry.counter("audit_archive_failure_total");
        latencyTimer = Timer.builder("audit_archive_elapsed_ms")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

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
                    long elapsed = System.currentTimeMillis() - start;
                    latencyTimer.record(elapsed, TimeUnit.MILLISECONDS);
                    successCounter.increment();
                    maybeAlertDelay(target, elapsed);
                    return;
                }
                log.warn("[audit-archive] command exited {} on attempt {}/{}", exit, i, attempts);
            } catch (Exception e) {
                log.warn("[audit-archive] command failed on attempt {}/{}: {}", i, attempts, e.getMessage());
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        latencyTimer.record(elapsed, TimeUnit.MILLISECONDS);
        failureCounter.increment();
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
        String payload = """
                {
                  "text":"[audit-archive] :x: FAILED target=%s exitCode=%d (host=%s) %s",
                  "channel":"%s"
                }
                """.formatted(target, exitCode, host(), mention(), alertChannel)
                .replaceAll("\\s+", " ");
        sendSlackWithRetry(payload, "failure");
    }

    private void maybeAlertDelay(LocalDate target, long elapsed) {
        if (!alertEnabled || !StringUtils.hasText(slackWebhook)) return;
        if (elapsed < delayThresholdMs) return;
        String payload = """
                {
                  "text":"[audit-archive] :warning: SLOW archive target=%s elapsedMs=%d (host=%s) %s",
                  "channel":"%s"
                }
                """.formatted(target, elapsed, host(), mention(), alertChannel)
                .replaceAll("\\s+", " ");
        sendSlackWithRetry(payload, "delay");
    }

    private String mention() {
        return StringUtils.hasText(alertMention) ? alertMention : "";
    }

    private String host() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void sendSlackWithRetry(String payload, String type) {
        int attempts = Math.max(1, retry);
        for (int i = 1; i <= attempts; i++) {
            try {
                restTemplate.postForEntity(slackWebhook, payload, String.class);
                log.info("[audit-archive] slack {} alert sent (attempt {})", type, i);
                return;
            } catch (Exception e) {
                log.warn("[audit-archive] slack {} alert failed attempt {}/{}: {}", type, i, attempts, e.getMessage());
                try {
                    Thread.sleep((long) Math.min(120_000, Math.pow(2, i) * 1000L)); // exponential backoff up to 2m
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @FunctionalInterface
    interface CommandInvoker {
        int run(String command, String targetMonth) throws Exception;
    }
}
