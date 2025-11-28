package com.example.admin.permission.context;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * 인증/인가 컨텍스트.
 *
 * <p>DataPolicy 기반 마스킹을 위해 orgPolicyId, orgGroupCodes, businessType 필드 포함.
 * 마스킹 규칙은 DataPolicy에서 조회하므로 fieldMaskRules 제거됨.
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
    Long orgPolicyId,
    List<String> orgGroupCodes,
    String businessType) {

  public AuthContext {
    orgGroupCodes = orgGroupCodes == null ? List.of() : List.copyOf(orgGroupCodes);
  }

  /**
   * 역호환성을 위한 팩토리 메서드 - DataPolicy 관련 필드 없이 생성.
   */
  public static AuthContext of(String username, String organizationCode, String permissionGroupCode,
                               FeatureCode feature, ActionCode action, RowScope rowScope) {
    return new AuthContext(username, organizationCode, permissionGroupCode,
                           feature, action, rowScope, null, List.of(), null);
  }
}
