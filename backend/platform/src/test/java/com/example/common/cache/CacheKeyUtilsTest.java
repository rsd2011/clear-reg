package com.example.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CacheKeyUtilsTest {

    @Test
    void givenNullInputs_whenBuildingKey_thenUsesSafeDefaults() {
        String key = CacheKeyUtils.organizationScopeKey(null, null);

        assertThat(key).isEqualTo("UNKNOWN|0|0|UNSORTED");
    }

    @Test
    void givenPagination_whenBuildingKey_thenEncodesPagingDetails() {
        Sort sort = Sort.by(Sort.Order.desc("lastName"));
        PageRequest pageable = PageRequest.of(2, 25, sort);

        String key = CacheKeyUtils.organizationScopeKey("ACME", pageable);

        assertThat(key).isEqualTo("ACME|2|25|" + sort.toString());
    }
}
