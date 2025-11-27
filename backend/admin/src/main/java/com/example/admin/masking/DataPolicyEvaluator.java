package com.example.admin.masking;

import com.example.admin.permission.ActionCode;
import com.example.common.masking.MaskRuleDefinition;
import com.example.common.security.CurrentUser;
import com.example.common.security.CurrentUserProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Uses immutable snapshot of CurrentUser; no internal mutable state exposed")
@Component
@Slf4j
public class DataPolicyEvaluator {

  private final CurrentUserProvider currentUserProvider;

  public DataPolicyEvaluator(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  public Object mask(String tag, Object rawValue) {
    Optional<CurrentUser> maybeUser = currentUserProvider.current();
    if (maybeUser.isEmpty()) {
      log.warn(
          "MASKING_WITHOUT_CONTEXT tag={} valueType={}",
          tag,
          rawValue != null ? rawValue.getClass().getName() : "null");
      return maskValue(rawValue, "***");
    }
    CurrentUser user = maybeUser.get();
    MaskRuleDefinition rule = user.maskRule(tag).orElse(null);
    if (rule == null) {
      return maskValue(rawValue, "***");
    }
    ActionCode required = parseAction(rule.requiredActionCode(), ActionCode.UNMASK);
    ActionCode currentAction = parseAction(user.actionCode(), ActionCode.READ);
    if (currentAction.satisfies(required)) {
      if (rule.audit()) {
        log.info(
            "UNMASK_GRANTED user={} tag={} feature={} action={}",
            user.username(),
            tag,
            user.featureCode(),
            user.actionCode());
      }
      return rawValue;
    }
    if (rule.audit()) {
      log.warn(
          "UNMASK_BLOCKED user={} tag={} feature={} action={}",
          user.username(),
          tag,
          user.featureCode(),
          user.actionCode());
    }
    return maskValue(rawValue, rule.maskWith());
  }

  private ActionCode parseAction(String code, ActionCode fallback) {
    if (code == null || code.isBlank()) {
      return fallback;
    }
    try {
      return ActionCode.valueOf(code.trim());
    } catch (IllegalArgumentException ex) {
      return fallback;
    }
  }

  private Object maskValue(Object rawValue, String maskWith) {
    if (rawValue == null) {
      return null;
    }
    if (rawValue instanceof Number) {
      return 0;
    }
    if (rawValue instanceof Boolean) {
      return false;
    }
    return maskWith;
  }
}
