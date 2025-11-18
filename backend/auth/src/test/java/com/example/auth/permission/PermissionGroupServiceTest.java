package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class PermissionGroupServiceTest {

    @Mock
    private PermissionGroupRepository repository;

    private PermissionGroupService service;

    @BeforeEach
    void setUp() {
        service = new PermissionGroupService(repository);
    }

    @Test
    void givenGroupInRepository_whenFetchingMultipleTimes_thenUsesCache() {
        PermissionGroup group = new PermissionGroup("AUDITOR", "Auditor");
        given(repository.findByCode("AUDITOR")).willReturn(Optional.of(group));

        Scenario.given("권한 그룹 서비스", () -> service)
                .when("첫 조회", svc -> svc.getByCodeOrThrow("AUDITOR"))
                .then("그룹 반환", loaded -> assertThat(loaded).isSameAs(group))
                .and("두 번째 조회 시 캐시 사용", loaded -> {
                    PermissionGroup second = service.getByCodeOrThrow("AUDITOR");
                    assertThat(second).isSameAs(group);
                    verify(repository).findByCode("AUDITOR");
                });
    }

    @Test
    void givenUnknownGroup_whenFetching_thenThrow() {
        given(repository.findByCode("MISSING")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCodeOrThrow("MISSING"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenCache_whenEvicted_thenReloadFromRepository() {
        PermissionGroup group = new PermissionGroup("ANALYST", "Analyst");
        given(repository.findByCode("ANALYST")).willReturn(Optional.of(group));
        service.getByCodeOrThrow("ANALYST");
        service.evict("ANALYST");
        service.getByCodeOrThrow("ANALYST");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).findByCode(captor.capture());
        assertThat(captor.getAllValues()).containsOnly("ANALYST");
    }
}
