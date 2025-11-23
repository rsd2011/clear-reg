package com.example.audit.infra;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import com.example.audit.infra.masking.MaskingProperties;
import com.example.audit.infra.persistence.AuditLogEntity;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.policy.AuditPolicyResolver;
import com.example.common.masking.Maskable;
import com.example.common.masking.MaskingService;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class AuditRecordService implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(AuditRecordService.class);

    private final AuditLogRepository repository;
    private final AuditPolicyResolver policyResolver;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final KafkaTemplate<String, String> dlqTemplate;
    private final String dlqTopic;
    private final com.example.audit.infra.siem.SiemForwarder siemForwarder;
    private final boolean hmacEnabled;
    private final String hmacSecret;
    private final String hmacKeyId;
    private final MaskingProperties maskingProperties;
    private final MaskingService maskingService;

    public AuditRecordService(AuditLogRepository repository,
                              AuditPolicyResolver policyResolver,
                              ObjectMapper objectMapper,
                              @Nullable KafkaTemplate<String, String> kafkaTemplate,
                              @Value("${audit.kafka.topic:audit.events.v1}") String topic,
                              @Nullable @Value("${audit.kafka.dlq-topic:}") String dlqTopic,
                              @Nullable KafkaTemplate<String, String> dlqTemplate,
                              @Value("${audit.hash-chain.hmac-enabled:false}") boolean hmacEnabled,
                              @Value("${audit.hash-chain.secret:}") String hmacSecret,
                              @Value("${audit.hash-chain.key-id:default}") String hmacKeyId,
                              MaskingProperties maskingProperties,
                              @Nullable MaskingService maskingService,
                              @Nullable com.example.audit.infra.siem.SiemForwarder siemForwarder) {
        this.repository = repository;
        this.policyResolver = policyResolver;
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.dlqTopic = dlqTopic;
        this.dlqTemplate = dlqTemplate != null ? dlqTemplate : kafkaTemplate;
        this.hmacEnabled = hmacEnabled;
        this.hmacSecret = hmacSecret;
        this.hmacKeyId = hmacKeyId;
        this.maskingProperties = maskingProperties;
        this.maskingService = maskingService;
        this.siemForwarder = siemForwarder;
    }

    @Override
    @Transactional
    public void record(AuditEvent event, AuditMode mode) {
        record(event, mode, null);
    }

    public void record(AuditEvent event, AuditMode mode, @Nullable MaskingTarget maskingTarget) {
        AuditPolicySnapshot policy = resolve(event.getAction(), event.getEventType())
                .orElse(AuditPolicySnapshot.secureDefault());
        if (!policy.isEnabled()) {
            return;
        }
        persist(event, mode, policy, maskingTarget);
        publish(event, mode);
        forwardSiem(event);
    }

    @Override
    public Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType) {
        return policyResolver.resolve(endpoint, eventType);
    }

    private void persist(AuditEvent event, AuditMode mode, AuditPolicySnapshot policy, @Nullable MaskingTarget maskingTarget) {
        AuditLogEntity entity = toEntity(event, policy.isMaskingEnabled(), maskingTarget);
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
                // STRICT 모드에서는 DLQ로도 남겨두고 예외를 전파
                sendDlq(event);
                throw new IllegalStateException("Failed to publish audit event", ex);
            }
            log.warn("Audit publish failed in async fallback: {}", ex.getMessage());
            sendDlq(event);
        }
    }

    private void sendDlq(AuditEvent event) {
        if (dlqTemplate == null || dlqTopic == null || dlqTopic.isBlank()) {
            return;
        }
        try {
            dlqTemplate.send(dlqTopic, event.getEventId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Audit DLQ publish failed: {}", e.getMessage());
        }
    }

    private void forwardSiem(AuditEvent event) {
        if (siemForwarder == null) return;
        try {
            siemForwarder.forward(event);
        } catch (Exception e) {
            log.warn("SIEM forward failed: {}", e.getMessage());
        }
    }

    private AuditLogEntity toEntity(AuditEvent event, boolean maskingEnabled, @Nullable MaskingTarget maskingTarget) {
        String extraJson = null;
        try {
            if (event.getExtra() != null && !event.getExtra().isEmpty()) {
                String raw = objectMapper.writeValueAsString(event.getExtra());
                extraJson = applyMask(raw, maskingEnabled, maskingTarget, "extra");
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
                applyMask(event.getReasonText(), maskingEnabled, maskingTarget, "reasonText"),
                event.getLegalBasisCode(),
                event.getRiskLevel() != null ? event.getRiskLevel().name() : null,
                applyMask(event.getBeforeSummary(), maskingEnabled, maskingTarget, "beforeSummary"),
                applyMask(event.getAfterSummary(), maskingEnabled, maskingTarget, "afterSummary"),
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
                payload += "|keyId=" + hmacKeyId;
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

    private String sanitize(String text) {
        if (text == null) return null;
        String masked = maskingProperties.applyAll(text);
        if (masked.length() > 1024) {
            return masked.substring(0, 1024);
        }
        return masked;
    }

    private String applyMask(String raw, boolean maskingEnabled, @Nullable MaskingTarget target, String fieldName) {
        if (raw == null) return null;
        if (!maskingEnabled) return raw;
        String masked = sanitize(raw);
        if (target != null && target.getMaskRule() != null) {
            masked = com.example.common.masking.MaskRuleProcessor.apply(target.getMaskRule(), masked, target.getMaskParams());
        }
        if (maskingService == null) {
            return masked;
        }
        final String maskedFinal = masked;
        Maskable inline = new Maskable() {
            @Override public String raw() { return raw; }
            @Override public String masked() { return maskedFinal; }
        };
        return maskingService.render(inline, target, fieldName);
    }
}
