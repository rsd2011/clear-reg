package com.example.admin.permission.declarative;

import com.example.common.security.RowScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionGroupDefinition(
    String code,
    String name,
    String description,
    RowScope defaultRowScope,
    List<PermissionAssignmentDefinition> assignments) {

  List<PermissionAssignmentDefinition> assignmentsOrEmpty() {
    return assignments == null ? List.of() : Collections.unmodifiableList(assignments);
  }
}
