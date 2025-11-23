package com.example.batch.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

class QuartzConfigTest {

    private final QuartzConfig config = new QuartzConfig();

    @Test
    @DisplayName("SchedulerFactoryBean에 JobFactory와 종료 대기 설정을 적용한다")
    void schedulerFactoryBeanConfigured() {
        // Given
        AutowiringSpringBeanJobFactory jobFactory = config.jobFactory();

        // When
        SchedulerFactoryBean factoryBean = config.schedulerFactoryBean(jobFactory);

        // Then
        assertThat(ReflectionTestUtils.getField(factoryBean, "jobFactory")).isSameAs(jobFactory);
        assertThat(ReflectionTestUtils.getField(factoryBean, "waitForJobsToCompleteOnShutdown")).isEqualTo(true);
    }

    @Test
    @DisplayName("quartzScheduler는 SchedulerFactoryBean에서 Scheduler를 반환한다")
    void quartzSchedulerReturnsFromFactory() {
        // Given
        SchedulerFactoryBean factoryBean = mock(SchedulerFactoryBean.class);
        Scheduler scheduler = mock(Scheduler.class);
        when(factoryBean.getScheduler()).thenReturn(scheduler);

        // When
        Scheduler result = config.quartzScheduler(factoryBean);

        // Then
        assertThat(result).isSameAs(scheduler);
    }
}
