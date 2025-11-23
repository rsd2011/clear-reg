package com.example.common.security;

import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.DataPolicyMatch;

/**
 * 데이터 정책 매칭 결과를 기반으로 RowScope Specification을 생성한다.
 */
public record RowScopeFilter(RowScope rowScope,
                             Specification<?> specification) {

    @SuppressWarnings("unchecked")
    public <T> Specification<T> cast() {
        return (Specification<T>) specification;
    }

    public static <T> RowScopeFilter from(DataPolicyMatch match, RowScopeContext ctx, Specification<T> custom) {
        RowScope row = parse(match.getRowScope());
        Specification<T> spec = RowScopeSpecifications.organizationScoped("organizationCode",
                row,
                ctx != null ? ctx.organizationCode() : null,
                ctx != null ? ctx.organizationHierarchy() : null,
                custom);
        return new RowScopeFilter(row, spec);
    }

    private static RowScope parse(String value) {
        if (value == null) return RowScope.ALL;
        return RowScope.valueOf(value.toUpperCase());
    }
}
