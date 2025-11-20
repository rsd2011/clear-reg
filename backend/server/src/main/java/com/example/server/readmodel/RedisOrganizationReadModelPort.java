package com.example.server.readmodel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "readmodel.organization", name = "enabled", havingValue = "true")
public class RedisOrganizationReadModelPort implements OrganizationReadModelPort {

    private static final Logger log = LoggerFactory.getLogger(RedisOrganizationReadModelPort.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DwOrganizationTreeService organizationTreeService;
    private final OrganizationReadModelProperties properties;

    public RedisOrganizationReadModelPort(StringRedisTemplate redisTemplate,
                                          ObjectMapper objectMapper,
                                          DwOrganizationTreeService organizationTreeService,
                                          OrganizationReadModelProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.organizationTreeService = organizationTreeService;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<OrganizationTreeReadModel> load() {
        String payload = redisTemplate.opsForValue().get(key());
        if (payload == null) {
            if (properties.isRefreshOnMiss()) {
                return Optional.of(rebuild());
            }
            return Optional.empty();
        }
        try {
            OrganizationTreeReadModel model = objectMapper.readValue(payload, OrganizationTreeReadModel.class);
            return Optional.of(model);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize organization read model payload", ex);
            return Optional.empty();
        }
    }

    @Override
    public OrganizationTreeReadModel rebuild() {
        List<DwOrganizationNode> nodes = organizationTreeService.snapshot().flatten();
        OrganizationTreeReadModel model = new OrganizationTreeReadModel(
                computeVersion(nodes),
                OffsetDateTime.now(ZoneOffset.UTC),
                nodes
        );
        persist(model);
        return model;
    }

    @Override
    public void evict() {
        try {
            redisTemplate.delete(key());
        } catch (DataAccessException ex) {
            log.warn("Failed to delete organization read model cache", ex);
        }
    }

    private void persist(OrganizationTreeReadModel model) {
        try {
            String serialized = objectMapper.writeValueAsString(model);
            redisTemplate.opsForValue().set(key(), serialized, properties.getTtl());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize organization read model", ex);
        }
    }

    private String computeVersion(List<DwOrganizationNode> nodes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            nodes.stream()
                    .map(node -> node.organizationCode() + ":" + node.version())
                    .sorted()
                    .map(value -> value.getBytes(StandardCharsets.UTF_8))
                    .forEach(digest::update);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm missing", ex);
        }
    }

    private String key() {
        return properties.getKeyPrefix() + ":" + properties.getTenantId();
    }
}
