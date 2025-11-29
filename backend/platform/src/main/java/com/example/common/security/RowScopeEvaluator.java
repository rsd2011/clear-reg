package com.example.common.security;

import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.RowAccessMatch;

public final class RowScopeEvaluator {

    private RowScopeEvaluator() {}

    /**
     * RowAccessMatch를 사용하여 Specification을 생성합니다.
     */
    public static <T> Specification<T> toSpecification(RowAccessMatch match,
                                                       RowScopeContext ctx,
                                                       Specification<T> customSpec) {
        RowScopeContext effective = ctx != null ? ctx : RowScopeContextHolder.get();
        RowScope rowScope = match.getRowScope();
        return RowScopeSpecifications.organizationScoped("organizationCode",
                rowScope,
                effective != null ? effective.organizationCode() : null,
                effective != null ? effective.organizationHierarchy() : null,
                customSpec);
    }

    /**
     * @deprecated DataPolicyMatch 대신 RowAccessMatch를 사용하세요.
     */
    @Deprecated(forRemoval = true)
    public static <T> Specification<T> toSpecification(DataPolicyMatch match,
                                                       RowScopeContext ctx,
                                                       Specification<T> customSpec) {
        RowScopeContext effective = ctx != null ? ctx : RowScopeContextHolder.get();
        RowScope rowScope = match.getRowScope();
        return RowScopeSpecifications.organizationScoped("organizationCode",
                rowScope,
                effective != null ? effective.organizationCode() : null,
                effective != null ? effective.organizationHierarchy() : null,
                customSpec);
    }
}
