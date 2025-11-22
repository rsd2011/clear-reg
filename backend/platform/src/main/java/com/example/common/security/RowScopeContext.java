package com.example.common.security;

import java.util.Collection;

public record RowScopeContext(String organizationCode,
                              Collection<String> organizationHierarchy) {
}
