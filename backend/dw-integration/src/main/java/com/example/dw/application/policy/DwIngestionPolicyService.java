package com.example.dw.application.policy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dw.config.DwIngestionProperties;
import com.example.dw.config.DwIngestionProperties.JobScheduleProperties;
import com.example.dw.domain.HrIngestionPolicyEntity;
import com.example.dw.infrastructure.persistence.HrIngestionPolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class DwIngestionPolicyService implements DwIngestionPolicyProvider {

    private static final String DOCUMENT_CODE = "dw.ingestion.policy";

    private final HrIngestionPolicyRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper yamlMapper;
    private final AtomicReference<HrIngestionPolicyState> cache;

    public DwIngestionPolicyService(HrIngestionPolicyRepository repository,
                                    DwIngestionProperties defaults,
                                    ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.registerModule(new JavaTimeModule());
        HrIngestionPolicyState initial = HrIngestionPolicyState.from(defaults);
        this.cache = new AtomicReference<>(initial);
        repository.findByCode(DOCUMENT_CODE)
                .ifPresent(entity -> this.cache.set(readState(entity.getYaml())));
    }

    public DwIngestionPolicyView view() {
        HrIngestionPolicyState state = cache.get();
        return new DwIngestionPolicyView(state.batchCron(), state.timezone(), state.retention(),
                toView(state.jobSchedules()));
    }

    @Transactional
    public DwIngestionPolicyView update(DwIngestionPolicyUpdateRequest request) {
        HrIngestionPolicyState updated = cache.updateAndGet(current -> current.merge(request));
        persist(updated);
        eventPublisher.publishEvent(new DwIngestionPolicyChangedEvent(updated.timezone(), updated.jobSchedules()));
        return new DwIngestionPolicyView(updated.batchCron(), updated.timezone(), updated.retention(),
                toView(updated.jobSchedules()));
    }

    private void persist(HrIngestionPolicyState state) {
        String yaml = writeState(state);
        HrIngestionPolicyEntity entity = repository.findByCode(DOCUMENT_CODE)
                .orElseGet(() -> new HrIngestionPolicyEntity(DOCUMENT_CODE, yaml));
        entity.updateYaml(yaml);
        repository.save(entity);
    }

    private HrIngestionPolicyState readState(String yaml) {
        try {
            HrIngestionPolicyDto dto = yamlMapper.readValue(yaml, HrIngestionPolicyDto.class);
            return HrIngestionPolicyState.from(dto);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid DW ingestion policy YAML", exception);
        }
    }

    private String writeState(HrIngestionPolicyState state) {
        try {
            HrIngestionPolicyDto dto = new HrIngestionPolicyDto(state.batchCron(), state.timezone(), state.retention(),
                    toDto(state.jobSchedules()));
            return yamlMapper.writeValueAsString(dto);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize DW ingestion policy", exception);
        }
    }

    @Override
    public String batchCron() {
        return cache.get().batchCron();
    }

    @Override
    public String timezone() {
        return cache.get().timezone();
    }

    @Override
    public Duration retention() {
        return cache.get().retention();
    }

    @Override
    public List<DwBatchJobSchedule> jobSchedules() {
        return cache.get().jobSchedules();
    }

    private List<DwBatchJobScheduleView> toView(List<DwBatchJobSchedule> schedules) {
        return schedules.stream()
                .map(schedule -> new DwBatchJobScheduleView(schedule.jobKey(), schedule.enabled(),
                        schedule.cronExpression(), schedule.timezone()))
                .toList();
    }

    private List<JobScheduleDto> toDto(List<DwBatchJobSchedule> schedules) {
        return schedules.stream()
                .map(schedule -> new JobScheduleDto(schedule.jobKey(), schedule.enabled(),
                        schedule.cronExpression(), schedule.timezone()))
                .toList();
    }

    private record HrIngestionPolicyDto(String batchCron,
                                        String timezone,
                                        Duration retention,
                                        List<JobScheduleDto> jobSchedules) {
    }

    private record JobScheduleDto(String jobKey, boolean enabled, String cronExpression, String timezone) {
    }

    private record HrIngestionPolicyState(String batchCron,
                                          String timezone,
                                          Duration retention,
                                          List<DwBatchJobSchedule> jobSchedules) {

        static HrIngestionPolicyState from(DwIngestionProperties properties) {
            List<DwBatchJobSchedule> schedules = properties.getJobSchedules().stream()
                    .map(schedule -> fromProperties(schedule, properties.getTimezone()))
                    .toList();
            return new HrIngestionPolicyState(properties.getBatchCron(), properties.getTimezone(),
                    properties.getRetention(), schedules);
        }

        private static DwBatchJobSchedule fromProperties(JobScheduleProperties properties, String defaultTimezone) {
            String timezone = properties.getTimezone() != null ? properties.getTimezone() : defaultTimezone;
            return new DwBatchJobSchedule(properties.getJobKey(), properties.isEnabled(),
                    properties.getCronExpression(), timezone);
        }

        static HrIngestionPolicyState from(HrIngestionPolicyDto dto) {
            List<DwBatchJobSchedule> schedules = dto.jobSchedules() == null
                    ? List.of()
                    : dto.jobSchedules().stream()
                            .map(schedule -> new DwBatchJobSchedule(schedule.jobKey(), schedule.enabled(),
                                    schedule.cronExpression(),
                                    schedule.timezone()))
                            .toList();
            if (schedules.isEmpty()) {
                schedules = List.of(new DwBatchJobSchedule("DW_INGESTION", true, dto.batchCron(), dto.timezone()));
            }
            return new HrIngestionPolicyState(dto.batchCron(), dto.timezone(), dto.retention(), schedules);
        }

        HrIngestionPolicyState merge(DwIngestionPolicyUpdateRequest request) {
            String newBatchCron = request.batchCron() != null ? request.batchCron() : batchCron;
            String newTimezone = request.timezone() != null ? request.timezone() : timezone;
            Duration newRetention = request.retention() != null ? request.retention() : retention;
            List<DwBatchJobSchedule> newSchedules = request.jobSchedules() != null
                    ? mapRequests(request.jobSchedules(), newTimezone, jobSchedules)
                    : jobSchedules;
            return new HrIngestionPolicyState(newBatchCron, newTimezone, newRetention,
                    new ArrayList<>(newSchedules));
        }

        private static List<DwBatchJobSchedule> mapRequests(List<DwBatchJobScheduleRequest> requests,
                                                            String defaultTimezone,
                                                            List<DwBatchJobSchedule> fallback) {
            if (requests.isEmpty()) {
                return fallback;
            }
            java.util.LinkedHashMap<String, DwBatchJobSchedule> merged = new java.util.LinkedHashMap<>();
            for (DwBatchJobSchedule schedule : fallback) {
                merged.put(schedule.jobKey(), schedule);
            }
            for (DwBatchJobScheduleRequest request : requests) {
                String jobKey = request.jobKey();
                if (jobKey == null) {
                    throw new IllegalArgumentException("jobKey is required");
                }
                DwBatchJobSchedule existing = merged.get(jobKey);
                boolean enabled = request.enabled() != null
                        ? request.enabled()
                        : existing != null && existing.enabled();
                String cron = request.cronExpression() != null
                        ? request.cronExpression()
                        : existing != null ? existing.cronExpression() : null;
                String timezone = request.timezone() != null
                        ? request.timezone()
                        : existing != null ? existing.timezone() : defaultTimezone;
                if (cron == null) {
                    throw new IllegalArgumentException("cronExpression is required for job " + jobKey);
                }
                merged.put(jobKey, new DwBatchJobSchedule(jobKey, enabled, cron, timezone));
            }
            return new ArrayList<>(merged.values());
        }
    }
}
