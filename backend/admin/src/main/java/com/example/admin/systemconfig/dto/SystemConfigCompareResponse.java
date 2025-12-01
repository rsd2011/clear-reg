package com.example.admin.systemconfig.dto;

import com.example.admin.systemconfig.domain.SystemConfigRevision;

/**
 * 두 버전 비교 응답 DTO.
 */
public record SystemConfigCompareResponse(
    SystemConfigRevisionResponse version1,
    SystemConfigRevisionResponse version2,
    String yamlDiff
) {
  public static SystemConfigCompareResponse from(SystemConfigRevision v1, SystemConfigRevision v2) {
    return new SystemConfigCompareResponse(
        SystemConfigRevisionResponse.from(v1),
        SystemConfigRevisionResponse.from(v2),
        computeDiff(v1.getYamlContent(), v2.getYamlContent())
    );
  }

  private static String computeDiff(String yaml1, String yaml2) {
    // 간단한 diff 표현 - 실제 구현에서는 diff 라이브러리 사용 권장
    if (yaml1 == null && yaml2 == null) {
      return "No changes";
    }
    if (yaml1 == null) {
      return "Added: " + yaml2;
    }
    if (yaml2 == null) {
      return "Removed: " + yaml1;
    }
    if (yaml1.equals(yaml2)) {
      return "No changes";
    }
    return "Content changed (use external diff tool for detailed comparison)";
  }
}
