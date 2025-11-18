package com.example.hr.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.common.security.RowScope;
import com.example.hr.domain.HrOrganizationEntity;
import com.example.hr.infrastructure.persistence.HrOrganizationRepository;

@Service
public class HrOrganizationQueryService {

    private final HrOrganizationRepository organizationRepository;
    private final OrganizationRowScopeStrategy customScopeStrategy;

    public HrOrganizationQueryService(HrOrganizationRepository organizationRepository,
                                      OrganizationRowScopeStrategy customScopeStrategy) {
        this.organizationRepository = organizationRepository;
        this.customScopeStrategy = customScopeStrategy;
    }

    public Page<HrOrganizationEntity> getOrganizations(Pageable pageable,
                                                       RowScope rowScope,
                                                       String organizationCode) {
        if (rowScope == null) {
            throw new IllegalArgumentException("Row scope must be provided");
        }
        if (organizationCode == null && rowScope != RowScope.ALL) {
            throw new IllegalArgumentException("Organization code is required for row scope " + rowScope);
        }
        return switch (rowScope) {
            case OWN -> organizationRepository.findByOrganizationCode(organizationCode, pageable);
            case ORG -> organizationRepository.findByOrganizationCodeOrParentOrganizationCode(organizationCode,
                    organizationCode, pageable);
            case ALL -> organizationRepository.findAll(pageable);
            case CUSTOM -> customScopeStrategy.apply(pageable, organizationCode);
        };
    }
}
