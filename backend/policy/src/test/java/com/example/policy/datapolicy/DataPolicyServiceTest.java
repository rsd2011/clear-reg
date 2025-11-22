package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
@EntityScan("com.example.policy.datapolicy")
@Import(DataPolicyService.class)
class DataPolicyServiceTest {

    @Autowired
    DataPolicyRepository repository;

    @Autowired
    DataPolicyService service;

    @org.springframework.context.annotation.Configuration
    @EnableJpaRepositories(basePackageClasses = DataPolicyRepository.class)
    @EntityScan(basePackageClasses = DataPolicy.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("우선순위와 조건 매칭에 따라 가장 구체적인 정책을 선택한다")
    void evaluate_picksHighestPriorityMatch() {
        repository.save(DataPolicy.builder()
                .featureCode("ACCOUNT")
                .actionCode(null)
                .permGroupCode(null)
                .orgPolicyId(null)
                .businessType(null)
                .rowScope("ORG")
                .defaultMaskRule("PARTIAL")
                .priority(100)
                .active(true)
                .build());

        repository.save(DataPolicy.builder()
                .featureCode("ACCOUNT")
                .actionCode("VIEW")
                .permGroupCode("ADMIN")
                .rowScope("OWN")
                .defaultMaskRule("NONE")
                .priority(10)
                .active(true)
                .build());

        var match = service.evaluate("ACCOUNT", "VIEW", "ADMIN", null, null, null, Instant.now())
                .orElseThrow();

        assertThat(match.getRowScope()).isEqualTo("OWN");
        assertThat(match.getMaskRule()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("비활성 또는 기간 외 정책은 제외된다")
    void evaluate_ignoresInactiveOrExpired() {
        repository.save(DataPolicy.builder()
                .featureCode("ACCOUNT")
                .rowScope("ALL")
                .defaultMaskRule("FULL")
                .priority(1)
                .active(false)
                .build());

        var match = service.evaluate("ACCOUNT", null, null, null, null, null, Instant.now());

        assertThat(match).isEmpty();
    }
}
