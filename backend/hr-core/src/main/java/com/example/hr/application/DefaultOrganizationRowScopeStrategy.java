package com.example.hr.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.hr.domain.HrOrganizationEntity;
import com.example.hr.infrastructure.persistence.HrOrganizationRepository;

@Component
public class DefaultOrganizationRowScopeStrategy implements OrganizationRowScopeStrategy {

    private final HrOrganizationRepository repository;

    public DefaultOrganizationRowScopeStrategy(HrOrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<HrOrganizationEntity> apply(Pageable pageable, String organizationCode) {
        return repository.findByOrganizationCode(organizationCode, pageable);
    }
}
