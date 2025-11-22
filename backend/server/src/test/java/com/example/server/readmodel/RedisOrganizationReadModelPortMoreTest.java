package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisOrganizationReadModelPortMoreTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    DwOrganizationTreeService organizationTreeService;

    @Mock
    OrganizationReadModelProperties properties;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    RedisOrganizationReadModelPort port;

    @Test
    @DisplayName("캐시 역직렬화 실패 시 빈 Optional을 반환한다")
    void returnsEmptyWhenDeserializeFails() throws Exception {
        given(properties.getKeyPrefix()).willReturn("org-cache");
        given(properties.getTenantId()).willReturn("t1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("org-cache:t1")).thenReturn("broken-payload");
        lenient().when(objectMapper.readValue("broken-payload", OrganizationTreeReadModel.class))
                .thenThrow(new JsonProcessingException("boom") {});

        Optional<OrganizationTreeReadModel> result = port.load();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evict 중 Redis 예외가 발생해도 삼킨다")
    void evictSwallowsDataAccessException() {
        given(properties.getKeyPrefix()).willReturn("org-cache");
        given(properties.getTenantId()).willReturn("t1");
        given(redisTemplate.delete("org-cache:t1")).willThrow(new DataAccessException("fail") {});

        port.evict();
    }

    @Test
    @DisplayName("캐시 미스이고 refreshOnMiss=true이면 rebuild를 수행한다")
    void rebuildOnCacheMissWhenRefreshEnabled() throws Exception {
        given(properties.getKeyPrefix()).willReturn("org-cache");
        given(properties.getTenantId()).willReturn("t1");
        given(properties.isRefreshOnMiss()).willReturn(true);
        given(properties.getTtl()).willReturn(java.time.Duration.ofMinutes(5));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("org-cache:t1")).thenReturn(null);

        var tree = mock(com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot.class);
        given(organizationTreeService.snapshot()).willReturn(tree);
        given(tree.flatten()).willReturn(java.util.List.of());
        when(objectMapper.writeValueAsString(any(OrganizationTreeReadModel.class))).thenReturn("{}");

        Optional<OrganizationTreeReadModel> result = port.load();

        assertThat(result).isPresent();
        verify(valueOperations).set(eq("org-cache:t1"), eq("{}"), eq(java.time.Duration.ofMinutes(5)));
    }
}
