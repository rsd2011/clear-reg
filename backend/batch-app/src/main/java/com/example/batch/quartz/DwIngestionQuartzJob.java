package com.example.batch.quartz;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.example.batch.security.DwBatchAuthContext;
import com.example.auth.permission.context.AuthContextPropagator;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionOutboxService;

@DisallowConcurrentExecution
public class DwIngestionQuartzJob extends QuartzJobBean {

    @Autowired
    private DwIngestionOutboxService outboxService;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
        AuthContextPropagator.runWithContext(DwBatchAuthContext.systemContext(),
                () -> outboxService.enqueue(DwIngestionJob.fetchNext()));
    }
}
