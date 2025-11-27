package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.DataPolicyMatch;

@DisplayName("DataPolicyService 분기 커버리지")
class DataPolicyServiceTest {

    DataPolicyRepository repository = Mockito.mock(DataPolicyRepository.class);
    DataPolicyService service = new DataPolicyService(repository);

    @Test
    void evaluateReturnsMatchWhenEffective() {
        Instant now = Instant.now();
        DataPolicy policy = DataPolicy.builder()
                .featureCode("F")
                .actionCode("A")
                .permGroupCode("PG")
                .rowScope("OWN")
                .defaultMaskRule("FULL")
                .priority(1)
                .active(true)
                .effectiveFrom(now.minusSeconds(10))
                .effectiveTo(now.plusSeconds(10))
                .build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of(policy));

        Optional<DataPolicyMatch> match = service.evaluate("F", "A", "PG", null, List.of(), "BT", null, now);
        assertThat(match).isPresent();
        assertThat(match.get().getMaskRule()).isEqualTo("FULL");
    }

    @Test
    void evaluateSkipsWhenInactiveOrOutOfRange() {
        Instant now = Instant.now();
        DataPolicy inactive = DataPolicy.builder()
                .featureCode("F").rowScope("ALL").defaultMaskRule("NONE").priority(1).active(false).build();
        DataPolicy expired = DataPolicy.builder()
                .featureCode("F").rowScope("ALL").defaultMaskRule("NONE").priority(2).active(true)
                .effectiveFrom(now.minusSeconds(20)).effectiveTo(now.minusSeconds(1)).build();
        given(repository.findByActiveTrueOrderByPriorityAsc()).willReturn(List.of());

        Optional<DataPolicyMatch> match = service.evaluate("F", null, null, null, null, null, null, now);
        assertThat(match).isEmpty();
    }
}
