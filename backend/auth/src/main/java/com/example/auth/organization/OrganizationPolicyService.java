package com.example.auth.organization;

import com.example.auth.organization.OrganizationPolicyCache.OrganizationPolicySnapshot;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrganizationPolicyService {

  private final OrganizationPolicyCache cache;

  public OrganizationPolicyService(OrganizationPolicyCache cache) {
    this.cache = cache;
  }

  public String defaultPermissionGroup(String organizationCode) {
    return policy(organizationCode).defaultPermissionGroupCode();
  }

  public List<String> approvalFlow(String organizationCode) {
    return policy(organizationCode).approvalFlow();
  }

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
