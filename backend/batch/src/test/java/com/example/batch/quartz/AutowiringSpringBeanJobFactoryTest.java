package com.example.batch.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.quartz.impl.triggers.SimpleTriggerImpl;

@ExtendWith(MockitoExtension.class)
class AutowiringSpringBeanJobFactoryTest {

    @Mock
    ApplicationContext applicationContext;
    @Mock
    AutowireCapableBeanFactory beanFactory;

    public static class DummyJob implements org.quartz.Job {
        @Override
        public void execute(org.quartz.JobExecutionContext context) { }
    }

    @Test
    @DisplayName("Quartz Job 인스턴스를 만들 때 스프링 BeanFactory로 자동 주입을 수행한다")
    void autowiresCreatedJobInstance() throws Exception {
        // Given: ApplicationContext에서 BeanFactory를 획득할 수 있고
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
        AutowiringSpringBeanJobFactory factory = new AutowiringSpringBeanJobFactory();
        factory.setApplicationContext(applicationContext);

        JobDetail jobDetail = JobBuilder.newJob(DummyJob.class).withIdentity("dummy").build();
        SimpleTriggerImpl trigger = new SimpleTriggerImpl();
        trigger.setJobKey(jobDetail.getKey());
        TriggerFiredBundle bundle = new TriggerFiredBundle(jobDetail, trigger, null, false, null, null, null, null);

        // When: 잡 인스턴스를 생성하면
        Object job = factory.createJobInstance(bundle);

        // Then: BeanFactory의 autowireBean 이 호출되고 생성된 잡을 반환한다
        verify(beanFactory).autowireBean(job);
        assertThat(job).isInstanceOf(DummyJob.class);
    }
}
