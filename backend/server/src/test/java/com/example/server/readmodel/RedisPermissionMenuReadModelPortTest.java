package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

        PermissionMenuReadModelProperties properties = new PermissionMenuReadModelProperties();
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
                List.of(new PermissionMenuItem("MENU1", "메뉴1", "FEATURE1", "READ", "/menu1", Set.of("MASK1")))
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
}
