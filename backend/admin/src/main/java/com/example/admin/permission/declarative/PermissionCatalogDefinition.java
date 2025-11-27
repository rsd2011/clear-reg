package com.example.admin.permission.declarative;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionCatalogDefinition(List<PermissionGroupDefinition> permissionGroups) {

  List<PermissionGroupDefinition> groupsOrEmpty() {
    return permissionGroups == null ? List.of() : Collections.unmodifiableList(permissionGroups);
  }
}
