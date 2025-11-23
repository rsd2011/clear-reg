package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.DataPolicyMatch;

class DataPolicyServiceBranchTest {

    private final DataPolicyRepository repository = mock(DataPolicyRepository.class);
    private final DataPolicyService service = new DataPolicyService(repository);

    @Test
    @DisplayName("featureCode 미스매치 시 빈 결과를 반환한다")
    void returnsEmptyWhenFeatureDoesNotMatch() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_READ")
                .rowScope("OWN")
                .defaultMaskRule("NONE")
                .priority(1)
                .active(true)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("OTHER", null, null, null, null, null, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("그룹 코드 매칭 시 orgGroup 필터가 통과한다")
    void matchesOrgGroupWhenPresent() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_READ")
                .orgGroupCode("BRANCH_A")
                .rowScope("ORG")
                .defaultMaskRule("PARTIAL")
                .priority(1)
                .active(true)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("CUSTOMER_READ", null, null, null,
                List.of("branch_a"), null, Instant.now());

        assertThat(result).isPresent();
        assertThat(result.get().getRowScope()).isEqualTo("ORG");
    }

    @Test
    @DisplayName("actionCode가 없으면 대상 actionCode가 있는 정책은 필터링된다")
    void filtersWhenActionCodeMissing() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_EXPORT")
                .actionCode("DOWNLOAD")
                .rowScope("ALL")
                .defaultMaskRule("HASH")
                .priority(1)
                .active(true)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("CUSTOMER_EXPORT", null, null, null,
                List.of("BRANCH_A"), null, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유효기간 밖이면 정책이 무시된다")
    void ignoresWhenOutOfEffectiveWindow() {
        Instant future = Instant.now().plusSeconds(3600);
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_EXPORT")
                .rowScope("ALL")
                .defaultMaskRule("HASH")
                .priority(1)
                .active(true)
                .effectiveFrom(future)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("CUSTOMER_EXPORT", null, null, null,
                List.of("BRANCH_A"), null, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("permGroupCode가 다르면 매칭되지 않는다")
    void permGroupMismatchReturnsEmpty() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_EXPORT")
                .permGroupCode("PG_ADMIN")
                .rowScope("ALL")
                .defaultMaskRule("HASH")
                .priority(1)
                .active(true)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("CUSTOMER_EXPORT", null, "PG_USER", null,
                List.of("BRANCH_A"), null, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("orgPolicyId가 다르면 매칭되지 않는다")
    void orgPolicyIdMismatchReturnsEmpty() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_EXPORT")
                .orgPolicyId(10L)
                .rowScope("ALL")
                .defaultMaskRule("HASH")
                .priority(1)
                .active(true)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("CUSTOMER_EXPORT", null, null, 9L,
                List.of("BRANCH_A"), null, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("businessType이 다르면 매칭되지 않는다")
    void businessTypeMismatchReturnsEmpty() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("CUSTOMER_EXPORT")
                .businessType("LOAN")
                .rowScope("ALL")
                .defaultMaskRule("HASH")
                .priority(1)
                .active(true)
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> result = service.evaluate("CUSTOMER_EXPORT", null, null, null,
                List.of("BRANCH_A"), "CARD", Instant.now());

        assertThat(result).isEmpty();
    }
}
