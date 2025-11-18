package com.example.hr.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.hr.domain.HrOrganizationEntity;
import com.example.hr.infrastructure.persistence.HrOrganizationRepository;

@Service
@RequiredArgsConstructor
public class HrOrganizationQueryService {

    private final HrOrganizationRepository organizationRepository;

    public Page<HrOrganizationEntity> getOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);
    }
}
