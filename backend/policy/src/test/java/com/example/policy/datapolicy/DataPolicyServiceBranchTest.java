package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.DataPolicyMatch;

@DisplayName("DataPolicyService 브랜치 추가 커버")
class DataPolicyServiceBranchTest {

    DataPolicyRepository repository = Mockito.mock(DataPolicyRepository.class);
    DataPolicyService service = new DataPolicyService(repository);

    @Test
    void matchGroupAndNullableFalseBranches() {
        Instant now = Instant.now();
        DataPolicy policy = DataPolicy.builder()
                .featureCode("F")
                .actionCode("A")
                .permGroupCode("PG")
                .orgGroupCode("GRP1")
                .rowScope("ALL")
                .defaultMaskRule("NONE")
                .priority(5)
                .active(true)
                .build();
        Mockito.when(repository.findByActiveTrueOrderByPriorityAsc()).thenReturn(List.of(policy));

        // orgGroupCodes 불일치 -> empty
        Optional<DataPolicyMatch> noMatch = service.evaluate("F", "A", "PG", null, List.of("OTHER"), null, now);
        assertThat(noMatch).isEmpty();

        // action 불일치 -> empty
        Optional<DataPolicyMatch> noMatchAction = service.evaluate("F", "DIFF", "PG", null, List.of("GRP1"), null, now);
        assertThat(noMatchAction).isEmpty();
    }

    @Test
    void isEffectiveFalseWhenAfterToOrBeforeFrom() {
        Instant now = Instant.now();
        DataPolicy future = DataPolicy.builder()
                .featureCode("F").rowScope("ALL").defaultMaskRule("NONE").priority(1).active(true)
                .effectiveFrom(now.plusSeconds(60)).build();
        DataPolicy expired = DataPolicy.builder()
                .featureCode("F").rowScope("ALL").defaultMaskRule("NONE").priority(2).active(true)
                .effectiveTo(now.minusSeconds(60)).build();
        Mockito.when(repository.findByActiveTrueOrderByPriorityAsc()).thenReturn(List.of(future, expired));

        Optional<DataPolicyMatch> match = service.evaluate("F", null, null, null, null, null, now);
        assertThat(match).isEmpty();
    }
}

