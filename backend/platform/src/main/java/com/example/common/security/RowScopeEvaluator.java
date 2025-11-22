package com.example.common.security;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.DataPolicyMatch;

public final class RowScopeEvaluator {

    private RowScopeEvaluator() {}

    public static <T> Specification<T> toSpecification(DataPolicyMatch match,
                                                       RowScopeContext ctx,
                                                       Specification<T> customSpec) {
        RowScope rowScope = parse(match.getRowScope());
        return RowScopeSpecifications.organizationScoped("organizationCode",
                rowScope,
                ctx != null ? ctx.organizationCode() : null,
                ctx != null ? ctx.organizationHierarchy() : null,
                customSpec);
    }

    private static RowScope parse(String value) {
        if (value == null) return RowScope.ALL;
        return RowScope.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
