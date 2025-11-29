package com.example.admin.rowaccesspolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;

@DisplayName("RowAccessPolicy 엔티티")
class RowAccessPolicyTest {

    @Nested
    @DisplayName("isEffectiveAt 메서드는")
    class IsEffectiveAt {

        @Test
        @DisplayName("active가 false면 false를 반환한다")
        void returnsFalseWhenInactive() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .active(false)
                    .build();

            assertThat(policy.isEffectiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("effectiveFrom 이전이면 false를 반환한다")
        void returnsFalseBeforeEffectiveFrom() {
            Instant now = Instant.now();
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .effectiveFrom(now.plusSeconds(100))
                    .build();

            assertThat(policy.isEffectiveAt(now)).isFalse();
        }

        @Test
        @DisplayName("effectiveTo 이후면 false를 반환한다")
        void returnsFalseAfterEffectiveTo() {
            Instant now = Instant.now();
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .effectiveTo(now.minusSeconds(100))
                    .build();

            assertThat(policy.isEffectiveAt(now)).isFalse();
        }

        @Test
        @DisplayName("유효 기간 내면 true를 반환한다")
        void returnsTrueWhenWithinRange() {
            Instant now = Instant.now();
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .effectiveFrom(now.minusSeconds(100))
                    .effectiveTo(now.plusSeconds(100))
                    .build();

            assertThat(policy.isEffectiveAt(now)).isTrue();
        }

        @Test
        @DisplayName("effectiveFrom/To가 null이면 항상 유효하다")
        void returnsTrueWhenNoDateBounds() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.isEffectiveAt(Instant.now())).isTrue();
        }
    }

    @Nested
    @DisplayName("matches 메서드는")
    class Matches {

        @Test
        @DisplayName("featureCode가 다르면 false를 반환한다")
        void returnsFalseWhenFeatureMismatch() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.CUSTOMER, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasActionButParamNull() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 actionCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoActionButParamHas() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(null)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 있고 매개변수가 null이면 false를 반환한다")
        void returnsFalseWhenPolicyHasPermGroupButParamNull() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode("ADMIN")
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("정책에 permGroupCode가 null인데 매개변수에 있으면 false를 반환한다")
        void returnsFalseWhenPolicyNoPermGroupButParamHas() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode(null)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, "ADMIN", Instant.now())).isFalse();
        }

        @Test
        @DisplayName("모든 조건이 매칭되면 true를 반환한다")
        void returnsTrueWhenAllMatch() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .permGroupCode("ADMIN")
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.READ, "ADMIN", Instant.now())).isTrue();
        }

        @Test
        @DisplayName("actionCode가 다르면 false를 반환한다")
        void returnsFalseWhenActionMismatch() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .actionCode(ActionCode.READ)
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, ActionCode.UPDATE, null, Instant.now())).isFalse();
        }

        @Test
        @DisplayName("permGroupCode가 다르면 false를 반환한다")
        void returnsFalseWhenPermGroupMismatch() {
            RowAccessPolicy policy = RowAccessPolicy.builder()
                    .featureCode(FeatureCode.ORGANIZATION)
                    .permGroupCode("ADMIN")
                    .rowScope(RowScope.ALL)
                    .active(true)
                    .build();

            assertThat(policy.matches(FeatureCode.ORGANIZATION, null, "USER", Instant.now())).isFalse();
        }
    }

    @Test
    @DisplayName("빌더 기본값이 올바르게 설정된다")
    void builderDefaultValues() {
        RowAccessPolicy policy = RowAccessPolicy.builder()
                .featureCode(FeatureCode.ORGANIZATION)
                .rowScope(RowScope.OWN)
                .build();

        assertThat(policy.getPriority()).isEqualTo(100);
        assertThat(policy.getActive()).isTrue();
    }
}
