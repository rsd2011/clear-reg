package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RowScopeTest {

    @Test
    void givenOrgScope_whenCheckingHierarchy_thenTrue() {
        assertThat(RowScope.ORG.includesHierarchy()).isTrue();
        assertThat(RowScope.OWN.includesHierarchy()).isFalse();
    }

    @Test
    void givenAllScope_whenCheckingVisibility_thenAllVisible() {
        assertThat(RowScope.ALL.isAllVisible()).isTrue();
        assertThat(RowScope.CUSTOM.isAllVisible()).isFalse();
    }
}
