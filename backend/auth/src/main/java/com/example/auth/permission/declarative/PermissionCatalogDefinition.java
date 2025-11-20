package com.example.auth.permission.declarative;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionCatalogDefinition(List<PermissionGroupDefinition> permissionGroups) {

    List<PermissionGroupDefinition> groupsOrEmpty() {
        return permissionGroups == null ? List.of() : Collections.unmodifiableList(permissionGroups);
    }
}
