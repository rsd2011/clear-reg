package com.example.server.config;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.example.audit.AuditPort;
import com.example.audit.NoopAuditPort;

@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(AuditKafkaProperties.class)
public class AuditKafkaConfig {

    @Bean
    @ConditionalOnMissingBean(AuditPort.class)
    public AuditPort auditPortFallback() {
        return new NoopAuditPort();
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.kafka", name = "bootstrap-servers")
    @ConditionalOnMissingBean(name = "auditProducerFactory")
    public ProducerFactory<String, String> auditProducerFactory(AuditKafkaProperties properties) {
        Map<String, Object> configs = properties.producerConfigs();
        // defaults if not set
        configs.putIfAbsent(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.putIfAbsent(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit.kafka", name = "bootstrap-servers")
    @ConditionalOnMissingBean(name = "auditKafkaTemplate")
    public KafkaTemplate<String, String> auditKafkaTemplate(ProducerFactory<String, String> auditProducerFactory) {
        return new KafkaTemplate<>(auditProducerFactory);
    }
}
