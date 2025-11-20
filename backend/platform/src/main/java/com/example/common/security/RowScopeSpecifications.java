package com.example.common.security;

import java.util.Collection;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;

/**
 * Provides reusable Specifications enforcing organization-based RowScope filtering.
 */
public final class RowScopeSpecifications {

    private RowScopeSpecifications() {
    }

    public static <T> Specification<T> organizationScoped(String organizationProperty,
                                                          RowScope rowScope,
                                                          String organizationCode,
                                                          Collection<String> organizationHierarchy) {
        return organizationScoped(organizationProperty, rowScope, organizationCode, organizationHierarchy, null);
    }

    public static <T> Specification<T> organizationScoped(String organizationProperty,
                                                          RowScope rowScope,
                                                          String organizationCode,
                                                          Collection<String> organizationHierarchy,
                                                          Specification<T> customSpecification) {
        Objects.requireNonNull(organizationProperty, "organizationProperty must not be null");
        Objects.requireNonNull(rowScope, "rowScope must not be null");

        if (rowScope == RowScope.CUSTOM) {
            Assert.notNull(customSpecification, "customSpecification is required for CUSTOM row scope");
        }
        if (rowScope == RowScope.OWN) {
            Assert.hasText(organizationCode, "organizationCode is required for OWN row scope");
        }
        if (rowScope == RowScope.ORG) {
            Assert.notEmpty(organizationHierarchy, "organizationHierarchy is required for ORG row scope");
        }

        return (root, query, builder) -> switch (rowScope) {
            case ALL -> builder.conjunction();
            case OWN -> builder.equal(root.get(organizationProperty), organizationCode);
            case ORG -> root.get(organizationProperty).in(organizationHierarchy);
            case CUSTOM -> customSpecification.toPredicate(root, query, builder);
        };
    }
}
