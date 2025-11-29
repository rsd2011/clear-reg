package com.example.admin.permission.context;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * 인증/인가 컨텍스트.
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
    RowScope rowScope,
    List<String> orgGroupCodes) {

  public AuthContext {
    orgGroupCodes = orgGroupCodes == null ? List.of() : List.copyOf(orgGroupCodes);
  }

  /**
   * 역호환성을 위한 팩토리 메서드 - orgGroupCodes 없이 생성.
   */
  public static AuthContext of(String username, String organizationCode, String permissionGroupCode,
                               FeatureCode feature, ActionCode action, RowScope rowScope) {
    return new AuthContext(username, organizationCode, permissionGroupCode,
                           feature, action, rowScope, List.of());
  }
}
