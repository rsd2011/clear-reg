package com.example.admin.permission.context;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * 인증/인가 컨텍스트.
 * <p>
 * RowScope는 RowAccessPolicy로 이관되어 별도 정책에서 관리됩니다.
 * </p>
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Record stores immutable list defensively; domain refs kept for context sharing")
public record AuthContext(
    String username,
    String organizationCode,
    String permissionGroupCode,
    FeatureCode feature,
    ActionCode action,
    List<String> orgGroupCodes) {

  public AuthContext {
    orgGroupCodes = orgGroupCodes == null ? List.of() : List.copyOf(orgGroupCodes);
  }

  /**
   * 팩토리 메서드 - orgGroupCodes 없이 생성.
   */
  public static AuthContext of(String username, String organizationCode, String permissionGroupCode,
                               FeatureCode feature, ActionCode action) {
    return new AuthContext(username, organizationCode, permissionGroupCode,
                           feature, action, List.of());
  }

  /**
   * 팩토리 메서드 - orgGroupCodes 포함하여 생성.
   */
  public static AuthContext of(String username, String organizationCode, String permissionGroupCode,
                               FeatureCode feature, ActionCode action, List<String> orgGroupCodes) {
    return new AuthContext(username, organizationCode, permissionGroupCode,
                           feature, action, orgGroupCodes);
  }
}
