package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.RowAccessMatch;

class RowScopeFilterTest {

    @Test
    @DisplayName("Given DataPolicyMatch ORG When RowScopeFilter.from Then organizationScoped spec이 생성된다")
    void createFilterFromMatch() {
        DataPolicyMatch match = DataPolicyMatch.builder()
                .rowScope(RowScope.ORG)
                .build();
        RowScopeContext ctx = new RowScopeContext("ORG1", java.util.List.of("ORG1", "ORG1-CHILD"));
        Specification<Object> custom = (root, query, cb) -> cb.conjunction();

        RowScopeFilter filter = RowScopeFilter.from(match, ctx, custom);

        assertThat(filter.rowScope()).isEqualTo(RowScope.ORG);
        assertThat(filter.cast()).isNotNull();
    }

    @Test
    @DisplayName("Given RowAccessMatch ORG When RowScopeFilter.from Then organizationScoped spec이 생성된다")
    void createFilterFromRowAccessMatch() {
        RowAccessMatch match = RowAccessMatch.builder()
                .rowScope(RowScope.ORG)
                .build();
        RowScopeContext ctx = new RowScopeContext("ORG1", java.util.List.of("ORG1", "ORG1-CHILD"));
        Specification<Object> custom = (root, query, cb) -> cb.conjunction();

        RowScopeFilter filter = RowScopeFilter.from(match, ctx, custom);

        assertThat(filter.rowScope()).isEqualTo(RowScope.ORG);
        assertThat(filter.cast()).isNotNull();
    }
}
