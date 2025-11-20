package com.example.auth.permission.declarative;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.common.security.RowScope;

@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionAssignmentDefinition(FeatureCode feature,
                                      ActionCode action,
                                      RowScope rowScope,
                                      String condition) {
}
