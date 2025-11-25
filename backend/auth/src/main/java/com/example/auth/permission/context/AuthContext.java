package com.example.auth.permission.context;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.common.security.RowScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Record stores immutable map defensively; domain refs kept for context sharing")
public record AuthContext(
    String username,
    String organizationCode,
    String permissionGroupCode,
    FeatureCode feature,
    ActionCode action,
    RowScope rowScope,
    Map<String, FieldMaskRule> fieldMaskRules) {

  public AuthContext {
    fieldMaskRules =
        fieldMaskRules == null ? Map.of() : Collections.unmodifiableMap(fieldMaskRules);
  }

  public Optional<FieldMaskRule> ruleFor(String tag) {
    return Optional.ofNullable(fieldMaskRules.get(tag));
  }
}
