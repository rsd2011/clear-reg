package com.example.server.readmodel;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "readmodel.permission-menu", name = "enabled", havingValue = "true")
public class RedisPermissionMenuReadModelPort implements PermissionMenuReadModelPort {

    private static final Logger log = LoggerFactory.getLogger(RedisPermissionMenuReadModelPort.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PermissionMenuReadModelSource source;
    private final PermissionMenuReadModelProperties properties;

    public RedisPermissionMenuReadModelPort(StringRedisTemplate redisTemplate,
                                            ObjectMapper objectMapper,
                                            PermissionMenuReadModelSource source,
                                            PermissionMenuReadModelProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.source = source;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public Optional<PermissionMenuReadModel> load(String principalId) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String payload = redisTemplate.opsForValue().get(key(principalId));
        if (payload == null) {
            if (properties.isRefreshOnMiss()) {
                return Optional.of(rebuild(principalId));
            }
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, PermissionMenuReadModel.class));
        }
        catch (Exception ex) {
            log.warn("Failed to deserialize permission menu read model, rebuilding... principal={}", principalId, ex);
            return Optional.of(rebuild(principalId));
        }
    }

    @Override
    public PermissionMenuReadModel rebuild(String principalId) {
        PermissionMenuReadModel snapshot = source.snapshot(principalId);
        PermissionMenuReadModel enriched = new PermissionMenuReadModel(
                stableVersion(snapshot),
                snapshot.generatedAt() != null ? snapshot.generatedAt() : OffsetDateTime.now(),
                snapshot.items()
        );
        persist(principalId, enriched);
        return enriched;
    }

    @Override
    public void evict(String principalId) {
        redisTemplate.delete(key(principalId));
    }

    private void persist(String principalId, PermissionMenuReadModel model) {
        try {
            String payload = objectMapper.writeValueAsString(model);
            redisTemplate.opsForValue().set(key(principalId), payload, properties.getTtl());
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to persist permission menu read model for principal=" + principalId, ex);
        }
    }

    private String key(String principalId) {
        return properties.getKeyPrefix() + ":" + properties.getTenantId() + ":" + principalId;
    }

    private String stableVersion(PermissionMenuReadModel model) {
        if (model.version() != null && !model.version().isBlank()) {
            return model.version();
        }
        return UUID.randomUUID().toString();
    }
}
