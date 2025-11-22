package com.example.audit.infra;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPolicySnapshot;
import com.example.audit.AuditPort;
import com.example.audit.infra.persistence.AuditLogEntity;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.policy.AuditPolicyResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class AuditRecordService implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditRecordService.class);

    private final AuditLogRepository repository;
    private final AuditPolicyResolver policyResolver;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final boolean hmacEnabled;
    private final String hmacSecret;

    public AuditRecordService(AuditLogRepository repository,
                              AuditPolicyResolver policyResolver,
                              ObjectMapper objectMapper,
                              @Nullable KafkaTemplate<String, String> kafkaTemplate,
                              @Value("${audit.kafka.topic:audit.events.v1}") String topic,
                              @Value("${audit.hash-chain.hmac-enabled:false}") boolean hmacEnabled,
                              @Value("${audit.hash-chain.secret:}") String hmacSecret) {
        this.repository = repository;
        this.policyResolver = policyResolver;
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.hmacEnabled = hmacEnabled;
        this.hmacSecret = hmacSecret;
    }

    @Override
    @Transactional
    public void record(AuditEvent event, AuditMode mode) {
        AuditPolicySnapshot policy = resolve(event.getAction(), event.getEventType())
                .orElse(AuditPolicySnapshot.secureDefault());
        if (!policy.isEnabled()) {
            return;
        }
        persist(event, mode);
        publish(event, mode);
    }

    @Override
    public Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType) {
        return policyResolver.resolve(endpoint, eventType);
    }

    private void persist(AuditEvent event, AuditMode mode) {
        AuditLogEntity entity = toEntity(event);
        try {
            repository.save(entity);
        } catch (RuntimeException ex) {
            if (mode == AuditMode.STRICT) {
                throw ex;
            }
            log.warn("Audit persist failed in async fallback: {}", ex.getMessage());
        }
    }

    private void publish(AuditEvent event, AuditMode mode) {
        if (kafkaTemplate == null) {
            return;
        }
        try {
            kafkaTemplate.send(topic, event.getEventId().toString(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException | RuntimeException ex) {
            if (mode == AuditMode.STRICT) {
                throw new IllegalStateException("Failed to publish audit event", ex);
            }
            log.warn("Audit publish failed in async fallback: {}", ex.getMessage());
        }
    }

    private AuditLogEntity toEntity(AuditEvent event) {
        String extraJson = null;
        try {
            if (event.getExtra() != null && !event.getExtra().isEmpty()) {
                extraJson = objectMapper.writeValueAsString(event.getExtra());
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit extra payload: {}", ex.getMessage());
        }

        String prevHash = repository.findTopByOrderByEventTimeDesc()
                .map(AuditLogEntity::getHashChain)
                .orElse("");
        String hashChain = computeHash(prevHash, event);

        return new AuditLogEntity(event.getEventId(),
                event.getEventTime(),
                event.getEventType(),
                event.getModuleName(),
                event.getAction(),
                event.getActor() != null ? event.getActor().getId() : null,
                event.getActor() != null && event.getActor().getType() != null ? event.getActor().getType().name() : null,
                event.getActor() != null ? event.getActor().getRole() : null,
                event.getActor() != null ? event.getActor().getDept() : null,
                event.getSubject() != null ? event.getSubject().getType() : null,
                event.getSubject() != null ? event.getSubject().getKey() : null,
                event.getChannel(),
                event.getClientIp(),
                event.getUserAgent(),
                event.getDeviceId(),
                event.isSuccess(),
                event.getResultCode(),
                event.getReasonCode(),
                event.getReasonText(),
                event.getLegalBasisCode(),
                event.getRiskLevel() != null ? event.getRiskLevel().name() : null,
                event.getBeforeSummary(),
                event.getAfterSummary(),
                extraJson,
                hashChain);
    }

    private String computeHash(String prevHash, AuditEvent event) {
        try {
            String payload = prevHash + "|" + event.getEventId() + "|" + event.getEventTime()
                    + "|" + event.getEventType() + "|" + event.getAction()
                    + "|" + (event.getActor() != null ? event.getActor().getId() : "")
                    + "|" + (event.getSubject() != null ? event.getSubject().getKey() : "");
            byte[] hash;
            if (hmacEnabled && hmacSecret != null && !hmacSecret.isBlank()) {
                var mac = javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(new javax.crypto.spec.SecretKeySpec(hmacSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
                hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } else {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                hash = digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, skipping hash chain");
            return prevHash;
        } catch (Exception e) {
            log.warn("Hash chain calculation failed: {}", e.getMessage());
            return prevHash;
        }
    }
}
