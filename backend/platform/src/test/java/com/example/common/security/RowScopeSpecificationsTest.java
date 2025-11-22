package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class RowScopeSpecificationsTest {

    @SuppressWarnings("unchecked")
    private final Root<Object> root = Mockito.mock(Root.class);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<Object> query = Mockito.mock(CriteriaQuery.class);
    @SuppressWarnings("unchecked")
    private final CriteriaBuilder builder = Mockito.mock(CriteriaBuilder.class);
    @SuppressWarnings("unchecked")
    private final Path<Object> path = Mockito.mock(Path.class);
    @SuppressWarnings("unchecked")
    private final Predicate predicate = Mockito.mock(Predicate.class);

    @Test
    @DisplayName("CUSTOM 범위에서 customSpecification이 없으면 예외를 던진다")
    void customScopeRequiresSpecification() {
        assertThrows(IllegalArgumentException.class, () ->
                RowScopeSpecifications.organizationScoped("orgCode", RowScope.CUSTOM, null, null)
                        .toPredicate(root, query, builder));
    }

    @Test
    @DisplayName("OWN 범위는 organizationCode가 필요하다")
    void ownScopeRequiresOrganizationCode() {
        assertThrows(IllegalArgumentException.class, () ->
                RowScopeSpecifications.organizationScoped("orgCode", RowScope.OWN, null, null)
                        .toPredicate(root, query, builder));
    }

    @Test
    @DisplayName("ORG 범위는 조직 계층 목록이 필요하다")
    void orgScopeRequiresHierarchy() {
        assertThrows(IllegalArgumentException.class, () ->
                RowScopeSpecifications.organizationScoped("orgCode", RowScope.ORG, "A", List.of())
                        .toPredicate(root, query, builder));
    }

    @Test
    @DisplayName("OWN 범위는 organizationCode와 일치하는 equals predicate를 생성한다")
    void ownScopeBuildsEqualPredicate() {
        Mockito.when(root.get("orgCode")).thenReturn(path);
        Mockito.when(builder.equal(path, "ORG1")).thenReturn(predicate);

        Specification<Object> spec = RowScopeSpecifications.organizationScoped("orgCode", RowScope.OWN, "ORG1", null);

        Predicate result = spec.toPredicate(root, query, builder);
        assertThat(result).isSameAs(predicate);
        Mockito.verify(builder).equal(path, "ORG1");
    }

    @Test
    @DisplayName("ORG 범위는 organizationHierarchy in predicate를 생성한다")
    void orgScopeBuildsInPredicate() {
        Mockito.when(root.get("orgCode")).thenReturn(path);
        Mockito.when(path.in(List.of("ORG1", "CHILD"))).thenReturn(predicate);

        Specification<Object> spec = RowScopeSpecifications.organizationScoped("orgCode", RowScope.ORG, "ORG1", List.of("ORG1", "CHILD"));

        Predicate result = spec.toPredicate(root, query, builder);
        assertThat(result).isSameAs(predicate);
        Mockito.verify(path).in(List.of("ORG1", "CHILD"));
    }

    @Test
    @DisplayName("ALL 범위는 conjunction을 반환한다")
    void allScopeReturnsConjunction() {
        Mockito.when(builder.conjunction()).thenReturn(predicate);

        Predicate result = RowScopeSpecifications.organizationScoped("orgCode", RowScope.ALL, null, null)
                .toPredicate(root, query, builder);

        assertThat(result).isSameAs(predicate);
        Mockito.verify(builder).conjunction();
    }
}
