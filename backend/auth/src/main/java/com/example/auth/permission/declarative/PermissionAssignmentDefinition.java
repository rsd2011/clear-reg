package com.example.auth.permission.declarative;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.common.security.RowScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionAssignmentDefinition(
    FeatureCode feature, ActionCode action, RowScope rowScope, String condition) {}
