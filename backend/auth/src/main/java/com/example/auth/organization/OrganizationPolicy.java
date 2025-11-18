package com.example.auth.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_policies")
public class OrganizationPolicy extends PrimaryKeyEntity {

    @Column(nullable = false, unique = true)
    private String organizationCode;

    @Column(nullable = false)
    private String defaultPermissionGroupCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_policy_permission_groups", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "permission_group_code")
    private Set<String> additionalPermissionGroups = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_policy_approval_flow", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "approval_group_code")
    @OrderColumn(name = "step_index")
    private List<String> approvalFlow = new ArrayList<>();

    protected OrganizationPolicy() {
    }

    public OrganizationPolicy(String organizationCode, String defaultPermissionGroupCode) {
        this.organizationCode = organizationCode;
        this.defaultPermissionGroupCode = defaultPermissionGroupCode;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public String getDefaultPermissionGroupCode() {
        return defaultPermissionGroupCode;
    }

    public Set<String> getAdditionalPermissionGroups() {
        return Collections.unmodifiableSet(additionalPermissionGroups);
    }

    public List<String> getApprovalFlow() {
        return Collections.unmodifiableList(approvalFlow);
    }
}
