package com.example.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;

@SpringBootTest(classes = AsyncConfig.class)
class AsyncConfigTest {

    @Autowired
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
        applicationTaskExecutor.shutdown();
    }

    @Test
    @DisplayName("Async TaskExecutor가 AuthContext를 전파한다")
    void asyncExecutorPropagatesAuthContext() throws Exception {
        AuthContext context = new AuthContext("tester", "ORG", "DEFAULT",
                FeatureCode.NOTICE, ActionCode.READ, RowScope.OWN, java.util.Map.of());
        AuthContextHolder.set(context);

        Future<String> future = applicationTaskExecutor.submit(() ->
                AuthContextHolder.current().map(AuthContext::username).orElse("missing"));

        assertThat(future.get()).isEqualTo("tester");
        assertThat(AuthContextHolder.current()).contains(context);
    }
}
