package com.example.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.testing.bdd.Scenario;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionGroupService 테스트")
class PermissionGroupServiceTest {

  @Mock private PermissionGroupRepository repository;

  private PermissionGroupService service;

  @BeforeEach
  void setUp() {
    service = new PermissionGroupService(repository);
  }

  @Test
  @DisplayName("Given 그룹 정보가 저장소에 있을 때 When 반복 조회하면 Then 캐시가 활용된다")
  void givenGroupInRepository_whenFetchingMultipleTimes_thenUsesCache() {
    PermissionGroup group = new PermissionGroup("AUDITOR", "Auditor");
    given(repository.findByCode("AUDITOR")).willReturn(Optional.of(group));

    Scenario.given("권한 그룹 서비스", () -> service)
        .when("첫 조회", svc -> svc.getByCodeOrThrow("AUDITOR"))
        .then("그룹 반환", loaded -> assertThat(loaded).isSameAs(group))
        .and(
            "두 번째 조회 시 캐시 사용",
            loaded -> {
              PermissionGroup second = service.getByCodeOrThrow("AUDITOR");
              assertThat(second).isSameAs(group);
              verify(repository).findByCode("AUDITOR");
            });
  }

  @Test
  @DisplayName("Given 존재하지 않는 그룹 When 조회하면 Then 예외를 던진다")
  void givenUnknownGroup_whenFetching_thenThrow() {
    given(repository.findByCode("MISSING")).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.getByCodeOrThrow("MISSING"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Given 캐시된 그룹 When evict 호출 후 재조회하면 Then 저장소를 다시 조회한다")
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
