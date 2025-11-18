package com.example.auth.organization;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class OrganizationPolicyService {

    private final OrganizationPolicyRepository repository;

    public OrganizationPolicyService(OrganizationPolicyRepository repository) {
        this.repository = repository;
    }

    public String defaultPermissionGroup(String organizationCode) {
        return repository.findByOrganizationCode(organizationCode)
                .map(OrganizationPolicy::getDefaultPermissionGroupCode)
                .orElse("DEFAULT");
    }

    public List<String> approvalFlow(String organizationCode) {
        return repository.findByOrganizationCode(organizationCode)
                .map(OrganizationPolicy::getApprovalFlow)
                .orElse(List.of());
    }

    public Set<String> availableGroups(String organizationCode) {
        return repository.findByOrganizationCode(organizationCode)
                .map(OrganizationPolicy::getAdditionalPermissionGroups)
                .orElse(Set.of());
    }
}
