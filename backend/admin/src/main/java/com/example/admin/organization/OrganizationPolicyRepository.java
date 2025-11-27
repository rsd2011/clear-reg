package com.example.admin.organization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationPolicyRepository extends JpaRepository<OrganizationPolicy, UUID> {

  Optional<OrganizationPolicy> findByOrganizationCode(String organizationCode);
}
