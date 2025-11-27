package com.example.admin.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(OrganizationPolicyServiceCachingTest.Config.class)
@DisplayName("OrganizationPolicyService 캐싱 테스트")
class OrganizationPolicyServiceCachingTest {

  @Autowired private OrganizationPolicyRepository repository;

  @Autowired private OrganizationPolicyService service;

  @Test
  @DisplayName("Given 반복 조회 When 캐시가 활성화되어 있으면 Then 저장소 호출이 한 번으로 제한된다")
  void givenRepeatedAccess_whenPolicyCached_thenRepositoryHitOnce() {
    OrganizationPolicy policy = new OrganizationPolicy("ORG", "AUDIT");
    given(repository.findByOrganizationCode("ORG")).willReturn(Optional.of(policy));

    String first = service.defaultPermissionGroup("ORG");
    String second = service.defaultPermissionGroup("ORG");

    assertThat(first).isEqualTo("AUDIT");
    assertThat(second).isEqualTo("AUDIT");
    verify(repository, times(1)).findByOrganizationCode("ORG");
  }

  @Test
  @DisplayName("Given 캐시 항목을 비우고 재조회할 때 Then 저장소에서 다시 로드한다")
  void givenCacheEvicted_whenAccessingAgain_thenReloadFromRepository() {
    OrganizationPolicy initial = new OrganizationPolicy("BRANCH", "DEFAULT");
    OrganizationPolicy updated = new OrganizationPolicy("BRANCH", "FINANCE");
    given(repository.findByOrganizationCode("BRANCH"))
        .willReturn(Optional.of(initial))
        .willReturn(Optional.of(updated));

    assertThat(service.defaultPermissionGroup("BRANCH")).isEqualTo("DEFAULT");
    service.evictPolicy("BRANCH");
    assertThat(service.defaultPermissionGroup("BRANCH")).isEqualTo("FINANCE");
    verify(repository, times(2)).findByOrganizationCode("BRANCH");
  }

  @Configuration
  @EnableCaching
  static class Config {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("organizationPolicies");
    }

    @Bean
    OrganizationPolicyRepository organizationPolicyRepository() {
      return mock(OrganizationPolicyRepository.class);
    }

    @Bean
    OrganizationPolicyCache organizationPolicyCache(OrganizationPolicyRepository repository) {
      return new OrganizationPolicyCache(repository);
    }

    @Bean
    OrganizationPolicyService organizationPolicyService(OrganizationPolicyCache cache) {
      return new OrganizationPolicyService(cache);
    }
  }
}
