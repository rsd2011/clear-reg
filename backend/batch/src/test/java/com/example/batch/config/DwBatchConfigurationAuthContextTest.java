package com.example.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.batch.ingestion.DwIngestionService;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionOutboxService;

class DwBatchConfigurationAuthContextTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("배치 Tasklet도 시스템 AuthContext로 실행된다")
    void taskletRunsWithSystemContext() throws Exception {
        DwIngestionService ingestionService = mock(DwIngestionService.class);
        DwIngestionOutboxService outboxService = mock(DwIngestionOutboxService.class);
        AtomicReference<AuthContext> observed = new AtomicReference<>();
        doAnswer(invocation -> {
            observed.set(AuthContextHolder.current().orElse(null));
            return null;
        }).when(outboxService).enqueue(ArgumentMatchers.any());

        DwBatchConfiguration configuration = new DwBatchConfiguration(ingestionService, outboxService);
        Tasklet tasklet = configuration.dwIngestionTasklet();

        StepExecution stepExecution = new StepExecution("dwIngestionStep", new JobExecution(1L));
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        tasklet.execute(contribution, chunkContext);

        assertThat(observed).hasValueSatisfying(ctx ->
                assertThat(ctx.username()).isEqualTo("dw-batch"));
        assertThat(AuthContextHolder.current()).isEmpty();
    }
}
