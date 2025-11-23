package com.example.dw.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@FunctionalInterface
public interface OrganizationRowScopeStrategy {

    Page<DwOrganizationNode> apply(Pageable pageable, String organizationCode);
}
