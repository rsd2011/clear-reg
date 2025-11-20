package com.example.auth.permission.declarative;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.example.common.security.RowScope;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionGroupDefinition(String code,
                                 String name,
                                 String description,
                                 RowScope defaultRowScope,
                                 List<PermissionAssignmentDefinition> assignments,
                                 List<FieldMaskRuleDefinition> maskRules) {

    List<PermissionAssignmentDefinition> assignmentsOrEmpty() {
        return assignments == null ? List.of() : Collections.unmodifiableList(assignments);
    }

    List<FieldMaskRuleDefinition> maskRulesOrEmpty() {
        return maskRules == null ? List.of() : Collections.unmodifiableList(maskRules);
    }
}
