package com.example.auth.organization;

import com.example.admin.permission.spi.OrganizationPolicyProvider;
import org.springframework.stereotype.Component;

/**
 * OrganizationPolicyService를 OrganizationPolicyProvider로 변환하는 어댑터.
 */
@Component
public class OrganizationPolicyProviderAdapter implements OrganizationPolicyProvider {

  private final OrganizationPolicyService organizationPolicyService;

  public OrganizationPolicyProviderAdapter(OrganizationPolicyService organizationPolicyService) {
    this.organizationPolicyService = organizationPolicyService;
  }

  @Override
  public String defaultPermissionGroup(String organizationCode) {
    return organizationPolicyService.defaultPermissionGroup(organizationCode);
  }
}
