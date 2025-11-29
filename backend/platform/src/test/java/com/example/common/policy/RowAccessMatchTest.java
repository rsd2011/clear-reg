package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.security.RowScope;

@DisplayName("RowAccessMatch")
class RowAccessMatchTest {

    @Test
    @DisplayName("빌더로 모든 필드를 설정할 수 있다")
    void builderCoversFields() {
        UUID id = UUID.randomUUID();
        RowAccessMatch match = RowAccessMatch.builder()
                .policyId(id)
                .rowScope(RowScope.ORG)
                .priority(10)
                .build();

        assertThat(match.getPolicyId()).isEqualTo(id);
        assertThat(match.getRowScope()).isEqualTo(RowScope.ORG);
        assertThat(match.getPriority()).isEqualTo(10);
    }

    @Test
    @DisplayName("equals와 hashCode가 올바르게 동작한다")
    void equalsAndHashCode() {
        UUID id = UUID.randomUUID();
        RowAccessMatch match1 = RowAccessMatch.builder()
                .policyId(id)
                .rowScope(RowScope.ALL)
                .priority(1)
                .build();
        RowAccessMatch match2 = RowAccessMatch.builder()
                .policyId(id)
                .rowScope(RowScope.ALL)
                .priority(1)
                .build();

        assertThat(match1).isEqualTo(match2);
        assertThat(match1.hashCode()).isEqualTo(match2.hashCode());
    }

    @Test
    @DisplayName("toString이 올바르게 동작한다")
    void toStringWorks() {
        RowAccessMatch match = RowAccessMatch.builder()
                .policyId(UUID.randomUUID())
                .rowScope(RowScope.OWN)
                .priority(5)
                .build();

        String str = match.toString();
        assertThat(str).contains("RowAccessMatch");
        assertThat(str).contains("OWN");
    }
}
