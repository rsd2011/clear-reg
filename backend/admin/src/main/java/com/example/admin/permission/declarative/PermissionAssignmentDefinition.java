package com.example.admin.permission.declarative;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * YAML 기반 권한 할당 정의.
 * <p>
 * RowScope와 condition은 RowAccessPolicy로 이관되어 제거되었습니다.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionAssignmentDefinition(FeatureCode feature, ActionCode action) {}
