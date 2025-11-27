package com.example.admin.organization;

import com.example.common.cache.CacheNames;
import java.util.List;
import java.util.Set;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@CacheConfig(cacheNames = CacheNames.ORGANIZATION_POLICIES)
class OrganizationPolicyCache {

  private final OrganizationPolicyRepository repository;

  OrganizationPolicyCache(OrganizationPolicyRepository repository) {
    this.repository = repository;
  }

  @Cacheable(key = "#root.args[0]", sync = true)
  OrganizationPolicySnapshot fetch(String organizationCode) {
    return repository
        .findByOrganizationCode(organizationCode)
        .map(OrganizationPolicySnapshot::fromEntity)
        .orElseGet(() -> OrganizationPolicySnapshot.missing(organizationCode));
  }

  @CacheEvict(key = "#root.args[0]")
  void evict(String organizationCode) {
    // no-op; annotation clears cache
  }

  record OrganizationPolicySnapshot(
      String organizationCode,
      String defaultPermissionGroupCode,
      Set<String> additionalPermissionGroups,
      List<String> approvalFlow) {

    static OrganizationPolicySnapshot fromEntity(OrganizationPolicy policy) {
      return new OrganizationPolicySnapshot(
          policy.getOrganizationCode(),
          policy.getDefaultPermissionGroupCode(),
          policy.getAdditionalPermissionGroups(),
          policy.getApprovalFlow());
    }

    static OrganizationPolicySnapshot missing(String organizationCode) {
      return new OrganizationPolicySnapshot(organizationCode, "DEFAULT", Set.of(), List.of());
    }
  }
}
