package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.dw.application.DwOrganizationTreeService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisOrganizationReadModelPortLineCoverTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock DwOrganizationTreeService treeService;
    @Mock OrganizationReadModelProperties properties;
    @Mock ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("refreshOnMiss=false에서 캐시 미스면 empty를 반환하고 rebuild를 호출하지 않는다")
    void loadMissWithoutRefresh() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(any())).thenReturn(null);
        when(properties.isRefreshOnMiss()).thenReturn(false);
        when(properties.getKeyPrefix()).thenReturn("org");
        when(properties.getTenantId()).thenReturn("t1");

        RedisOrganizationReadModelPort port = new RedisOrganizationReadModelPort(
                redisTemplate, objectMapper, treeService, properties);

        assertThat(port.load()).isEmpty();
        verify(treeService, never()).snapshot();
    }
}
