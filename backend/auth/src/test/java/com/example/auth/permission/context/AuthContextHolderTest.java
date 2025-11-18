package com.example.auth.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.common.security.RowScope;

class AuthContextHolderTest {

    @AfterEach
    void cleanup() {
        AuthContextHolder.clear();
    }

    @Test
    void givenContext_whenSet_thenAvailableUntilCleared() {
        AuthContext context = new AuthContext("tester", "ORG", "AUDIT",
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN, Map.of());
        AuthContextHolder.set(context);

        assertThat(AuthContextHolder.current()).contains(context);
        AuthContextHolder.clear();
        assertThat(AuthContextHolder.current()).isEmpty();
    }
}
