package com.example.auth.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class OrganizationPolicyServiceTest {

    @Mock
    private OrganizationPolicyRepository repository;

    @Test
    void givenPolicy_whenQueryingDefaults_thenReturnStoredValues() {
        OrganizationPolicy policy = new OrganizationPolicy("HQ", "AUDIT");
        OrganizationPolicyService service = new OrganizationPolicyService(repository);
        given(repository.findByOrganizationCode("HQ")).willReturn(Optional.of(policy));

        Scenario.given("조직 정책 서비스", () -> service)
                .when("기본 그룹 조회", svc -> svc.defaultPermissionGroup("HQ"))
                .then("저장된 코드 반환", result -> assertThat(result).isEqualTo("AUDIT"));
    }

    @Test
    void givenMissingPolicy_whenQuerying_thenUseFallback() {
        OrganizationPolicyService service = new OrganizationPolicyService(repository);
        Scenario.given("정책 미정", () -> service.defaultPermissionGroup("UNKNOWN"))
                .then("DEFAULT 반환", result -> assertThat(result).isEqualTo("DEFAULT"));
    }

    @Test
    void givenPolicy_whenAvailableGroups_thenReturnAdditionalCodes() throws Exception {
        OrganizationPolicy policy = new OrganizationPolicy("ORG", "DEFAULT");
        var field = OrganizationPolicy.class.getDeclaredField("additionalPermissionGroups");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<String> groups = (java.util.Set<String>) field.get(policy);
        groups.add("AUDIT");
        OrganizationPolicyService service = new OrganizationPolicyService(repository);
        given(repository.findByOrganizationCode("ORG")).willReturn(Optional.of(policy));

        assertThat(service.availableGroups("ORG")).contains("AUDIT");
    }
}
