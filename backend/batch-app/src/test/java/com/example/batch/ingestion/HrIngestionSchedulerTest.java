package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.example.hr.application.policy.HrIngestionPolicyProvider;
import com.example.hr.domain.HrImportBatchEntity;

@ExtendWith(MockitoExtension.class)
class HrIngestionSchedulerTest {

    @Mock
    private HrIngestionService ingestionService;
    @Mock
    private HrIngestionPolicyProvider policyProvider;

    private HrIngestionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new HrIngestionScheduler(ingestionService, policyProvider);
        given(policyProvider.batchCron()).willReturn("0 0 * * * *");
        given(policyProvider.timezone()).willReturn("UTC");
    }

    @Test
    void givenScheduler_whenConfigured_thenRegistersTriggeredTask() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        scheduler.configureTasks(registrar);
        assertThat(registrar.getTriggerTaskList()).hasSize(1);

        given(ingestionService.ingestNextFile()).willReturn(Optional.of(new HrImportBatchEntity()));
        var triggerTask = registrar.getTriggerTaskList().getFirst();
        triggerTask.getRunnable().run();
        var context = new org.springframework.scheduling.support.SimpleTriggerContext();
        triggerTask.getTrigger().nextExecutionTime(context);
        verify(ingestionService).ingestNextFile();
    }
}
