package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.example.dw.application.readmodel.MenuItem;
import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelSource;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.example.dw.application.readmodel.PermissionMenuItem;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;
import com.example.approval.api.ApprovalFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import redis.embedded.RedisServer;

@SpringBootTest(properties = {
        "readmodel.worker.enabled=true",
        "readmodel.organization.enabled=false",
        "readmodel.menu.enabled=false",
        "readmodel.permission-menu.enabled=false"
})
@Import({ReadModelWorkerE2ETest.ReadModelTestConfig.class, com.example.server.TestDraftAuditConfig.class})
@DisplayName("ReadModelWorker E2E (embedded Redis)")
class ReadModelWorkerE2ETest {

    private static final int REDIS_PORT = 6396;
    private static RedisServer redisServer;

    @org.springframework.beans.factory.annotation.Autowired
    private ReadModelWorker worker;
    @org.springframework.beans.factory.annotation.Autowired
    private OrganizationReadModelPort organizationPort;
    @org.springframework.beans.factory.annotation.Autowired
    private MenuReadModelPort menuPort;
    @org.springframework.beans.factory.annotation.Autowired
    private PermissionMenuReadModelPort permissionMenuPort;
    @org.springframework.beans.factory.annotation.Autowired
    private LettuceConnectionFactory connectionFactory;

    @MockBean
    private ApprovalFacade approvalFacade;

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

    @AfterEach
    void cleanupRedis() {
        connectionFactory.getConnection().serverCommands().flushAll();
    }

    @Test
    void rebuildsAndLoadsAllReadModels() {
        worker.rebuildOrganization();
        worker.rebuildMenu();
        worker.rebuildPermissionMenu("user1");

        assertThat(organizationPort.load()).isPresent();
        assertThat(menuPort.load()).isPresent();
        assertThat(permissionMenuPort.load("user1")).isPresent();
    }

    @TestConfiguration
    static class ReadModelTestConfig {

        @Bean
        LettuceConnectionFactory lettuceConnectionFactory() {
            RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("localhost", REDIS_PORT);
            LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
            factory.afterPropertiesSet();
            return factory;
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }

        @Bean
        @org.springframework.context.annotation.Primary
        OrganizationReadModelPort organizationReadModelPort() {
            return new OrganizationReadModelPort() {
                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public Optional<OrganizationTreeReadModel> load() {
                    return Optional.of(new OrganizationTreeReadModel("org-v1", OffsetDateTime.now(), List.of()));
                }

                @Override
                public OrganizationTreeReadModel rebuild() {
                    return new OrganizationTreeReadModel("org-v1", OffsetDateTime.now(), List.of());
                }

                @Override
                public void evict() {
                }
            };
        }

        @Bean
        MenuReadModelPort menuReadModelPort(StringRedisTemplate template,
                                            ObjectMapper mapper,
                                            MenuReadModelProperties props) {
            props.setEnabled(true);
            MenuReadModelSource source = () -> new MenuReadModel(
                    "menu-v1",
                    OffsetDateTime.now(),
                    List.of(new MenuItem("MENU1", "Menu1", "FILE", "READ", "/file")));
            return new RedisMenuReadModelPort(template, mapper, source, props);
        }

        @Bean
        PermissionMenuReadModelPort permissionMenuReadModelPort(StringRedisTemplate template,
                                                                ObjectMapper mapper,
                                                                PermissionMenuReadModelProperties props) {
            props.setEnabled(true);
            PermissionMenuReadModelSource source = principal -> new PermissionMenuReadModel(
                    "perm-v1",
                    OffsetDateTime.now(),
                    List.of(new PermissionMenuItem("PM1", "PM", "FILE", "READ", "/file"))
            );
            return new RedisPermissionMenuReadModelPort(template, mapper, source, props);
        }
    }
}
