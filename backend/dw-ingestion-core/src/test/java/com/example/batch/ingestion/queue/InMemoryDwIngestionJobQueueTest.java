package com.example.batch.ingestion.queue;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.batch.ingestion.DwIngestionService;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.application.job.DwIngestionOutboxService;

class InMemoryDwIngestionJobQueueTest {

    DwIngestionService ingestionService = Mockito.mock(DwIngestionService.class);
    DwIngestionOutboxService outboxService = Mockito.mock(DwIngestionOutboxService.class);
    Executor direct = Runnable::run; // 동기로 실행해 테스트 단순화

    @Test
    @DisplayName("FETCH_NEXT 성공 시 outbox 없는 경우에도 완료 시도 없이 통과한다")
    void fetchNext_noOutbox_completesWithoutMarking() {
        DwIngestionJob job = DwIngestionJob.fetchNext();
        DwIngestionJobQueue queue = new InMemoryDwIngestionJobQueue(ingestionService, outboxService,
                direct, 2, 100, 2.0, 1000);

        queue.enqueue(job);

        verify(ingestionService).ingestNextFile();
        verify(outboxService, never()).markCompleted(Mockito.any());
    }

    @Test
    @DisplayName("최대 재시도 초과 시 dead-letter로 이동한다")
    void failure_deadLetterWhenMaxAttemptsExceeded() {
        UUID outboxId = UUID.randomUUID();
        DwIngestionJob job = DwIngestionJob.fromOutbox(outboxId, DwIngestionJobType.FETCH_NEXT);
        DwIngestionJobQueue queue = new InMemoryDwIngestionJobQueue(ingestionService, outboxService,
                direct, 1, 10, 2.0, 100);
        Mockito.doThrow(new RuntimeException("fail")).when(ingestionService).ingestNextFile();

        queue.enqueue(job);

        verify(outboxService).markDeadLetter(outboxId, "fail");
        verify(outboxService, never()).markCompleted(Mockito.any());
    }

    @Test
    @DisplayName("outbox 없는 FETCH_NEXT 실패 시 outbox 마킹 없이 종료된다")
    void failureWithoutOutbox_noMarks() {
        DwIngestionJob job = DwIngestionJob.fetchNext();
        DwIngestionJobQueue queue = new InMemoryDwIngestionJobQueue(ingestionService, outboxService,
                direct, 1, 10, 2.0, 100);
        Mockito.doThrow(new RuntimeException("fail")).when(ingestionService).ingestNextFile();

        queue.enqueue(job);

        verify(outboxService, never()).markCompleted(Mockito.any());
        verify(outboxService, never()).markDeadLetter(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("outbox 참조가 있고 재시도 가능하면 scheduleRetry를 호출한다")
    void failureWithOutbox_schedulesRetry() {
        UUID outboxId = UUID.randomUUID();
        DwIngestionJob job = DwIngestionJob.fromOutbox(outboxId, DwIngestionJobType.FETCH_NEXT);
        DwIngestionJobQueue queue = new InMemoryDwIngestionJobQueue(ingestionService, outboxService,
                direct, 3, 10, 2.0, 1000);
        Mockito.doThrow(new RuntimeException("fail")).when(ingestionService).ingestNextFile();

        queue.enqueue(job);

        verify(outboxService).scheduleRetry(Mockito.eq(outboxId), Mockito.any(), Mockito.eq("fail"));
        verify(outboxService, never()).markDeadLetter(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("outbox 없이 실패하면 백오프 후 재시도한다")
    void retryWithoutOutbox_thenSucceeds() {
        DwIngestionJob job = DwIngestionJob.fetchNext();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        Mockito.doAnswer(invocation -> {
            if (counter.getAndIncrement() == 0) {
                throw new RuntimeException("first");
            }
            return null;
        }).when(ingestionService).ingestNextFile();

        DwIngestionJobQueue queue = new InMemoryDwIngestionJobQueue(ingestionService, outboxService,
                direct, 2, 50, 1.5, 50);

        queue.enqueue(job);

        verify(outboxService, never()).markDeadLetter(Mockito.any(), Mockito.any());
        verify(outboxService, never()).markCompleted(Mockito.any());
        assertThat(counter.get()).isEqualTo(2);
    }
}
