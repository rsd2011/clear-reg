package com.example.hr.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.hr.domain.HrOrganizationEntity;

@FunctionalInterface
public interface OrganizationRowScopeStrategy {

    Page<HrOrganizationEntity> apply(Pageable pageable, String organizationCode);
}
