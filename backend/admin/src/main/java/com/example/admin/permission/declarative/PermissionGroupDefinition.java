package com.example.admin.permission.declarative;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

/**
 * YAML 기반 권한 그룹 정의.
 * <p>
 * RowScope는 RowAccessPolicy로 이관되어 제거되었습니다.
 * approvalGroupCodes가 추가되었습니다.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PermissionGroupDefinition(
    String code,
    String name,
    String description,
    List<String> approvalGroupCodes,
    List<PermissionAssignmentDefinition> assignments) {

  List<PermissionAssignmentDefinition> assignmentsOrEmpty() {
    return assignments == null ? List.of() : Collections.unmodifiableList(assignments);
  }

  List<String> approvalGroupCodesOrEmpty() {
    return approvalGroupCodes == null ? List.of() : Collections.unmodifiableList(approvalGroupCodes);
  }
}
