package com.example.common.security;

import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.RowAccessMatch;

import java.util.Objects;

/**
 * 데이터 정책 매칭 결과를 기반으로 RowScope Specification을 생성한다.
 *
 * <p>도메인 로직({@link #cast()})을 포함하므로 Class로 구현됨.
 */
public final class RowScopeFilter {

    private final RowScope rowScope;
    private final Specification<?> specification;

    public RowScopeFilter(RowScope rowScope, Specification<?> specification) {
        this.rowScope = rowScope;
        this.specification = specification;
    }

    public RowScope rowScope() {
        return rowScope;
    }

    public Specification<?> specification() {
        return specification;
    }

    /**
     * Specification을 지정된 엔티티 타입으로 캐스팅한다.
     *
     * @param <T> 대상 엔티티 타입
     * @return 캐스팅된 Specification
     */
    @SuppressWarnings("unchecked")
    public <T> Specification<T> cast() {
        return (Specification<T>) specification;
    }

    /**
     * RowAccessMatch와 컨텍스트를 기반으로 RowScopeFilter를 생성한다.
     *
     * @param match RowAccessPolicy 매칭 결과
     * @param ctx   RowScope 컨텍스트
     * @param custom 커스텀 Specification (null 가능)
     * @param <T>   엔티티 타입
     * @return 생성된 RowScopeFilter
     */
    public static <T> RowScopeFilter from(RowAccessMatch match, RowScopeContext ctx, Specification<T> custom) {
        RowScope row = match.getRowScope();
        Specification<T> spec = RowScopeSpecifications.organizationScoped("organizationCode",
                row,
                ctx != null ? ctx.organizationCode() : null,
                ctx != null ? ctx.organizationHierarchy() : null,
                custom);
        return new RowScopeFilter(row, spec);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RowScopeFilter that)) return false;
        return rowScope == that.rowScope && Objects.equals(specification, that.specification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowScope, specification);
    }

    @Override
    public String toString() {
        return "RowScopeFilter[" +
                "rowScope=" + rowScope +
                ", specification=" + specification +
                ']';
    }
}
