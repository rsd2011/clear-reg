package com.example.admin.permission.declarative;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionAssignmentDefinition(
    FeatureCode feature, ActionCode action, RowScope rowScope, String condition) {}
