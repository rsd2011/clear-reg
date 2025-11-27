package com.example.admin.organization;

import com.example.admin.organization.OrganizationPolicyCache.OrganizationPolicySnapshot;
import com.example.admin.permission.spi.OrganizationPolicyProvider;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrganizationPolicyService implements OrganizationPolicyProvider {

  private final OrganizationPolicyCache cache;

  public OrganizationPolicyService(OrganizationPolicyCache cache) {
    this.cache = cache;
  }

  @Override
  public String defaultPermissionGroup(String organizationCode) {
    return policy(organizationCode).defaultPermissionGroupCode();
  }

  @Override
  public List<String> approvalFlow(String organizationCode) {
    return policy(organizationCode).approvalFlow();
  }

  @Override
  public Set<String> availableGroups(String organizationCode) {
    return policy(organizationCode).additionalPermissionGroups();
  }

  public void evictPolicy(String organizationCode) {
    cache.evict(organizationCode);
  }

  private OrganizationPolicySnapshot policy(String organizationCode) {
    return cache.fetch(organizationCode);
  }
}
