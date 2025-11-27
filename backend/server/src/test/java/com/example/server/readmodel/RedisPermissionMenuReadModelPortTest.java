package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.dw.application.readmodel.PermissionMenuItem;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import redis.embedded.RedisServer;

@DisplayName("RedisPermissionMenuReadModelPort 테스트")
class RedisPermissionMenuReadModelPortTest {

    private static final int REDIS_PORT = 6397;
    private static RedisServer redisServer;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private PermissionMenuReadModelPort readModelPort;
    private PermissionMenuReadModelSource source;
    private PermissionMenuReadModelProperties properties;

    @BeforeAll
    static void startRedis() {
        redisServer = new RedisServer(REDIS_PORT);
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("localhost", REDIS_PORT);
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        source = org.mockito.Mockito.mock(PermissionMenuReadModelSource.class);

        properties = new PermissionMenuReadModelProperties();
        properties.setEnabled(true);
        properties.setKeyPrefix("rm:perm");
        properties.setTenantId("test");
        properties.setRefreshOnMiss(true);
        properties.setTtl(Duration.ofMinutes(5));

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        readModelPort = new RedisPermissionMenuReadModelPort(redisTemplate, mapper, source, properties);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        connectionFactory.destroy();
    }

    @Test
    void rebuildAndLoadByPrincipal() {
        PermissionMenuReadModel snapshot = new PermissionMenuReadModel(
                "v-perm",
                OffsetDateTime.now(),
                List.of(new PermissionMenuItem("MENU1", "메뉴1", "FEATURE1", "READ", "/menu1"))
        );
        when(source.snapshot("user1")).thenReturn(snapshot);

        PermissionMenuReadModel rebuilt = readModelPort.rebuild("user1");

        assertThat(rebuilt.items()).hasSize(1);
        assertThat(rebuilt.version()).isEqualTo("v-perm");
        assertThat(readModelPort.load("user1")).isPresent()
                .get()
                .satisfies(model -> assertThat(model.version()).isEqualTo("v-perm"));
    }

    @Test
    void evictRemovesPrincipalKey() {
        when(source.snapshot("user2")).thenReturn(new PermissionMenuReadModel("v2", OffsetDateTime.now(), List.of()));

        readModelPort.rebuild("user2");
        assertThat(readModelPort.load("user2")).isPresent();

        readModelPort.evict("user2");
        assertThat(readModelPort.load("user2")).isPresent();
    }

    @Test
    @DisplayName("비활성화 상태면 아무 것도 로드하지 않는다")
    void disabledReturnsEmpty() {
        properties.setEnabled(false);
        PermissionMenuReadModelPort disabled = new RedisPermissionMenuReadModelPort(redisTemplate, new ObjectMapper(), source, properties);

        assertThat(disabled.load("userX")).isEmpty();
        verifyNoInteractions(source);
    }

    @Test
    @DisplayName("refreshOnMiss=false면 캐시 미스 시 빈 Optional을 반환한다")
    void cacheMissRefreshOffReturnsEmpty() {
        properties.setRefreshOnMiss(false);
        PermissionMenuReadModelPort port = new RedisPermissionMenuReadModelPort(redisTemplate, new ObjectMapper(), source, properties);

        assertThat(port.load("userY")).isEmpty();
        verifyNoInteractions(source);
    }

    @Test
    @DisplayName("역직렬화 실패 시 스냅샷을 다시 저장한다")
    void deserializeFailureTriggersRebuild() {
        PermissionMenuReadModel snapshot = new PermissionMenuReadModel("recovered", OffsetDateTime.now(), List.of());
        when(source.snapshot("userZ")).thenReturn(snapshot);
        redisTemplate.opsForValue().set("rm:perm:test:userZ", "{not-json}");

        PermissionMenuReadModel model = readModelPort.load("userZ").orElseThrow();

        assertThat(model.version()).isEqualTo("recovered");
    }
}
