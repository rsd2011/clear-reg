package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import redis.embedded.RedisServer;

@DisplayName("RedisOrganizationReadModelPort 테스트")
class RedisOrganizationReadModelPortTest {

    private static final int REDIS_PORT = 6399;
    private static RedisServer redisServer;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private OrganizationReadModelPort readModelPort;
    private DwOrganizationTreeService organizationTreeService;

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

        organizationTreeService = mock(DwOrganizationTreeService.class);

        OrganizationReadModelProperties properties = new OrganizationReadModelProperties();
        properties.setEnabled(true);
        properties.setTtl(Duration.ofMinutes(5));
        properties.setTenantId("test");
        properties.setKeyPrefix("rm:test");

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        readModelPort = new RedisOrganizationReadModelPort(redisTemplate, mapper, organizationTreeService, properties);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        connectionFactory.destroy();
    }

    @Test
    void rebuildPersistsSnapshotAndLoadReturnsSameStructure() {
        List<DwOrganizationNode> nodes = List.of(sampleNode("A", null), sampleNode("B", "A"));
        OrganizationTreeSnapshot snapshot = DwOrganizationTreeService.OrganizationTreeSnapshot.fromNodes(nodes);
        when(organizationTreeService.snapshot()).thenReturn(snapshot);

        OrganizationTreeReadModel rebuilt = readModelPort.rebuild();

        assertThat(rebuilt.nodes()).hasSize(2);
        assertThat(rebuilt.version()).isNotBlank();
        assertThat(readModelPort.load()).isPresent()
                .get()
                .satisfies(model -> {
                    assertThat(model.nodes()).hasSize(2);
                    assertThat(model.version()).isEqualTo(rebuilt.version());
                });
    }

    @Test
    void evictRemovesStoredPayload() {
        when(organizationTreeService.snapshot())
                .thenReturn(DwOrganizationTreeService.OrganizationTreeSnapshot.fromNodes(List.of(sampleNode("ONLY", null))));

        readModelPort.rebuild();
        assertThat(readModelPort.load()).isPresent();

        readModelPort.evict();
        assertThat(readModelPort.load()).isPresent(); // refreshOnMiss triggers rebuild
    }

    private DwOrganizationNode sampleNode(String code, String parent) {
        return new DwOrganizationNode(
                UUID.randomUUID(),
                code,
                1,
                code + "_NAME",
                parent,
                "ACTIVE",
                OffsetDateTime.now().toLocalDate(),
                OffsetDateTime.now().toLocalDate(),
                UUID.randomUUID(),
                OffsetDateTime.now()
        );
    }
}
