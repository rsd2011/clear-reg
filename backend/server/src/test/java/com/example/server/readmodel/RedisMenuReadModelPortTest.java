package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
    private MenuReadModelProperties properties;

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

        properties = new MenuReadModelProperties();
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
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
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

    @Test
    @DisplayName("캐시 미스인데 refreshOnMiss=true이면 스냅샷을 재빌드하고 저장한다")
    void loadRebuildsOnMissWhenRefreshEnabled() {
        MenuReadModel snapshot = new MenuReadModel(
                "v-miss",
                OffsetDateTime.now(),
                List.of(new MenuItem("MENU-MISS", "메뉴", "FEATURE", "READ", "/"))
        );
        org.mockito.Mockito.when(menuReadModelSource.snapshot()).thenReturn(snapshot);

        Optional<MenuReadModel> loaded = readModelPort.load();

        assertThat(loaded).isPresent();
        assertThat(loaded.get().version()).isEqualTo("v-miss");
        // 두 번째 호출은 캐시 히트이며 snapshot()을 추가로 호출하지 않는다
        Optional<MenuReadModel> cached = readModelPort.load();
        assertThat(cached).isPresent();
        assertThat(cached.get().version()).isEqualTo("v-miss");
        org.mockito.Mockito.verify(menuReadModelSource, org.mockito.Mockito.times(1)).snapshot();
    }

    @Test
    @DisplayName("refreshOnMiss=false이면 캐시 미스 시 빈 Optional을 반환한다")
    void loadReturnsEmptyWhenRefreshDisabled() {
        properties.setRefreshOnMiss(false);
        MenuReadModelPort port = new RedisMenuReadModelPort(redisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()), menuReadModelSource, properties);

        Optional<MenuReadModel> result = port.load();

        assertThat(result).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(menuReadModelSource);
    }

    @Test
    @DisplayName("저장된 JSON을 역직렬화할 수 없으면 재빌드하고 스냅샷을 저장한다")
    void loadRebuildsOnDeserializeFailure() {
        redisTemplate.opsForValue().set("rm:menu:test", "{not-json}");
        MenuReadModel snapshot = new MenuReadModel(
                "v-bad",
                OffsetDateTime.now(),
                List.of(new MenuItem("MENU-BAD", "메뉴", "FEATURE", "READ", "/bad"))
        );
        org.mockito.Mockito.when(menuReadModelSource.snapshot()).thenReturn(snapshot);

        Optional<MenuReadModel> loaded = readModelPort.load();

        assertThat(loaded).isPresent();
        assertThat(loaded.get().version()).isEqualTo("v-bad");
        org.mockito.Mockito.verify(menuReadModelSource).snapshot();
    }

    @Test
    @DisplayName("readmodel이 비활성화되면 load는 항상 빈 Optional을 반환하고 snapshot을 호출하지 않는다")
    void loadReturnsEmptyWhenDisabled() {
        properties.setEnabled(false);
        MenuReadModelPort disabledPort = new RedisMenuReadModelPort(redisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()), menuReadModelSource, properties);

        Optional<MenuReadModel> result = disabledPort.load();

        assertThat(result).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(menuReadModelSource);
    }

    @Test
    @DisplayName("스냅샷 버전이 비어 있으면 stableVersion이 생성되고 저장된다")
    void rebuildGeneratesStableVersionWhenMissing() {
        MenuReadModel snapshot = new MenuReadModel(
                " ",
                OffsetDateTime.now(),
                List.of(new MenuItem("MENU2", "메뉴2", "FEATURE", "READ", "/menu2"))
        );
        org.mockito.Mockito.when(menuReadModelSource.snapshot()).thenReturn(snapshot);

        MenuReadModel rebuilt = readModelPort.rebuild();

        assertThat(rebuilt.version()).isNotBlank();
        assertThat(readModelPort.load()).isPresent();
        assertThat(readModelPort.load().get().version()).isEqualTo(rebuilt.version());
        org.mockito.Mockito.verify(menuReadModelSource, org.mockito.Mockito.times(1)).snapshot();
    }

}
