package com.example.hr.application.policy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hr.config.HrIngestionProperties;
import com.example.hr.domain.HrIngestionPolicyEntity;
import com.example.hr.infrastructure.persistence.HrIngestionPolicyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Service
public class HrIngestionPolicyService implements HrIngestionPolicyProvider {

    private static final String DOCUMENT_CODE = "hr.ingestion.policy";

    private final HrIngestionPolicyRepository repository;
    private final ObjectMapper yamlMapper;
    private final AtomicReference<HrIngestionPolicyState> cache;

    public HrIngestionPolicyService(HrIngestionPolicyRepository repository,
                                    HrIngestionProperties defaults) {
        this.repository = repository;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        HrIngestionPolicyState initial = HrIngestionPolicyState.from(defaults);
        this.cache = new AtomicReference<>(initial);
        repository.findByCode(DOCUMENT_CODE)
                .ifPresent(entity -> this.cache.set(readState(entity.getYaml())));
    }

    public HrIngestionPolicyView view() {
        HrIngestionPolicyState state = cache.get();
        return new HrIngestionPolicyView(state.batchCron(), state.timezone(), state.retention());
    }

    @Transactional
    public HrIngestionPolicyView update(HrIngestionPolicyUpdateRequest request) {
        HrIngestionPolicyState updated = cache.updateAndGet(current -> current.merge(request));
        persist(updated);
        return new HrIngestionPolicyView(updated.batchCron(), updated.timezone(), updated.retention());
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
            throw new IllegalArgumentException("Invalid HR ingestion policy YAML", exception);
        }
    }

    private String writeState(HrIngestionPolicyState state) {
        try {
            HrIngestionPolicyDto dto = new HrIngestionPolicyDto(state.batchCron(), state.timezone(), state.retention());
            return yamlMapper.writeValueAsString(dto);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize HR ingestion policy", exception);
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

    private record HrIngestionPolicyDto(String batchCron, String timezone, Duration retention) {
    }

    private record HrIngestionPolicyState(String batchCron, String timezone, Duration retention) {

        static HrIngestionPolicyState from(HrIngestionProperties properties) {
            return new HrIngestionPolicyState(properties.getBatchCron(), properties.getTimezone(), properties.getRetention());
        }

        static HrIngestionPolicyState from(HrIngestionPolicyDto dto) {
            return new HrIngestionPolicyState(dto.batchCron(), dto.timezone(), dto.retention());
        }

        HrIngestionPolicyState merge(HrIngestionPolicyUpdateRequest request) {
            String newBatchCron = request.batchCron() != null ? request.batchCron() : batchCron;
            String newTimezone = request.timezone() != null ? request.timezone() : timezone;
            Duration newRetention = request.retention() != null ? request.retention() : retention;
            return new HrIngestionPolicyState(newBatchCron, newTimezone, newRetention);
        }
    }
}
