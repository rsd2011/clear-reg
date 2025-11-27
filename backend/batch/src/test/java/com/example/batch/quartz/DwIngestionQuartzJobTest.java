package com.example.batch.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.admin.permission.context.AuthContextHolder;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionOutboxService;

class DwIngestionQuartzJobTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Quartz Job 실행 시 시스템 AuthContext가 설정된다")
    void executeInternalSetsSystemContext() throws Exception {
        DwIngestionQuartzJob job = new DwIngestionQuartzJob();
        DwIngestionOutboxService outboxService = Mockito.mock(DwIngestionOutboxService.class);
        ReflectionTestUtils.setField(job, "outboxService", outboxService);

        JobExecutionContext executionContext = mock(JobExecutionContext.class);
        job.executeInternal(executionContext);

        assertThat(AuthContextHolder.current()).isEmpty();
        then(outboxService).should().enqueue(DwIngestionJob.fetchNext());
    }
}
