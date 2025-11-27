package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
class RedisOrganizationReadModelPortTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DwOrganizationTreeService treeService = mock(DwOrganizationTreeService.class);
    private final OrganizationReadModelProperties properties = new OrganizationReadModelProperties();

    @Test
    @DisplayName("캐시에 없고 refresh가 꺼져 있으면 Optional.empty를 반환한다")
    void loadReturnsEmptyWhenCacheMissAndNoRefresh() {
        properties.setRefreshOnMiss(false);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(ops);
        given(ops.get(any())).willReturn(null);

        RedisOrganizationReadModelPort port = new RedisOrganizationReadModelPort(redisTemplate, objectMapper, treeService, properties);

        assertThat(port.load()).isEmpty();
    }

    @Test
    @DisplayName("캐시 페이로드 역직렬화 실패 시 empty를 반환한다")
    void loadReturnsEmptyOnDeserializeError() {
        properties.setRefreshOnMiss(false);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(ops);
        given(ops.get(any())).willReturn("not-json");

        RedisOrganizationReadModelPort port = new RedisOrganizationReadModelPort(redisTemplate, objectMapper, treeService, properties);

        assertThat(port.load()).isEmpty();
    }

    @Test
    @DisplayName("rebuild는 snapshot을 사용해 저장하고 반환한다")
    void rebuildPersistsSnapshot() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(ops);
        properties.setRefreshOnMiss(true);

        DwOrganizationNode node = new DwOrganizationNode(UUID.randomUUID(), "ORG", 1, "Org", null, "ACTIVE",
                null, null, null, OffsetDateTime.now(ZoneOffset.UTC));
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.fromNodes(List.of(node));
        given(treeService.snapshot()).willReturn(snapshot);

        RedisOrganizationReadModelPort port = new RedisOrganizationReadModelPort(redisTemplate, objectMapper, treeService, properties);

        OrganizationTreeReadModel model = port.rebuild();

        assertThat(model.nodes()).hasSize(1);
    }
}
