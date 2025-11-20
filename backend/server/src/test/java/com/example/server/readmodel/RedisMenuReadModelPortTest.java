package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.dw.application.readmodel.MenuItem;
import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import redis.embedded.RedisServer;

@DisplayName("RedisMenuReadModelPort 테스트")
class RedisMenuReadModelPortTest {

    private static final int REDIS_PORT = 6398;
    private static RedisServer redisServer;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private MenuReadModelPort readModelPort;
    private MenuReadModelSource menuReadModelSource;

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

        menuReadModelSource = org.mockito.Mockito.mock(MenuReadModelSource.class);

        MenuReadModelProperties properties = new MenuReadModelProperties();
        properties.setEnabled(true);
        properties.setKeyPrefix("rm:menu");
        properties.setTenantId("test");
        properties.setRefreshOnMiss(true);
        properties.setTtl(Duration.ofMinutes(5));

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        readModelPort = new RedisMenuReadModelPort(redisTemplate, mapper, menuReadModelSource, properties);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        connectionFactory.destroy();
    }

    @Test
    void rebuildPersistsSnapshotAndLoadReturnsSameStructure() {
        MenuReadModel snapshot = new MenuReadModel(
                "v1",
                OffsetDateTime.now(),
                List.of(new MenuItem("MENU1", "메뉴1", "FEATURE1", "READ", "/menu1"))
        );
        when(menuReadModelSource.snapshot()).thenReturn(snapshot);

        MenuReadModel rebuilt = readModelPort.rebuild();

        assertThat(rebuilt.items()).hasSize(1);
        assertThat(rebuilt.version()).isEqualTo("v1");
        assertThat(readModelPort.load()).isPresent()
                .get()
                .satisfies(model -> {
                    assertThat(model.items()).hasSize(1);
                    assertThat(model.version()).isEqualTo("v1");
                });
    }

    @Test
    void evictRemovesStoredPayload() {
        when(menuReadModelSource.snapshot()).thenReturn(new MenuReadModel("v1", OffsetDateTime.now(), List.of()));

        readModelPort.rebuild();
        assertThat(readModelPort.load()).isPresent();

        readModelPort.evict();
        assertThat(readModelPort.load()).isPresent();
    }
}
