package com.example.auth.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.testing.bdd.Scenario;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationPolicyService 테스트")
class OrganizationPolicyServiceTest {

  @Mock private OrganizationPolicyRepository repository;

  @Test
  @DisplayName("Given 조직 정책이 존재할 때 When 기본값 조회 Then 저장된 코드를 반환한다")
  void givenPolicy_whenQueryingDefaults_thenReturnStoredValues() {
    OrganizationPolicy policy = new OrganizationPolicy("HQ", "AUDIT");
    OrganizationPolicyService service =
        new OrganizationPolicyService(new OrganizationPolicyCache(repository));
    given(repository.findByOrganizationCode("HQ")).willReturn(Optional.of(policy));

    Scenario.given("조직 정책 서비스", () -> service)
        .when("기본 그룹 조회", svc -> svc.defaultPermissionGroup("HQ"))
        .then("저장된 코드 반환", result -> assertThat(result).isEqualTo("AUDIT"));
  }

  @Test
  @DisplayName("Given 정책이 없을 때 When 기본 그룹 조회 Then DEFAULT를 반환한다")
  void givenMissingPolicy_whenQuerying_thenUseFallback() {
    OrganizationPolicyService service =
        new OrganizationPolicyService(new OrganizationPolicyCache(repository));
    Scenario.given("정책 미정", () -> service.defaultPermissionGroup("UNKNOWN"))
        .then("DEFAULT 반환", result -> assertThat(result).isEqualTo("DEFAULT"));
  }

  @Test
  @DisplayName("Given 정책에 추가 그룹이 있을 때 When availableGroups 호출 Then 코드 목록을 돌려준다")
  void givenPolicy_whenAvailableGroups_thenReturnAdditionalCodes() throws Exception {
    OrganizationPolicy policy = new OrganizationPolicy("ORG", "DEFAULT");
    var field = OrganizationPolicy.class.getDeclaredField("additionalPermissionGroups");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Set<String> groups = (java.util.Set<String>) field.get(policy);
    groups.add("AUDIT");
    OrganizationPolicyService service =
        new OrganizationPolicyService(new OrganizationPolicyCache(repository));
    given(repository.findByOrganizationCode("ORG")).willReturn(Optional.of(policy));

    assertThat(service.availableGroups("ORG")).contains("AUDIT");
  }
}
