package com.example.dw.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.example.common.cache.CacheNames;

@Component
public class DefaultOrganizationRowScopeStrategy implements OrganizationRowScopeStrategy {

    private final HrOrganizationRepository repository;

    public DefaultOrganizationRowScopeStrategy(HrOrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ORGANIZATION_ROW_SCOPE,
            key = "T(com.example.common.cache.CacheKeyUtils).organizationScopeKey(#organizationCode, #pageable)",
            sync = true)
    public Page<DwOrganizationNode> apply(Pageable pageable, String organizationCode) {
        return repository.findByOrganizationCode(organizationCode, pageable)
                .map(DwOrganizationNode::fromEntity);
    }
}
