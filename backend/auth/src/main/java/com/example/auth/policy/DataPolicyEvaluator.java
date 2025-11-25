package com.example.auth.policy;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Uses AuthContext snapshot; no internal mutable state exposed")
@Component
@Slf4j
public class DataPolicyEvaluator {

  public Object mask(String tag, Object rawValue) {
    Optional<AuthContext> maybeContext = AuthContextHolder.current();
    if (maybeContext.isEmpty()) {
      log.warn(
          "MASKING_WITHOUT_CONTEXT tag={} valueType={}",
          tag,
          rawValue != null ? rawValue.getClass().getName() : "null");
      return maskValue(tag, rawValue, "***");
    }
    AuthContext context = maybeContext.get();
    FieldMaskRule rule = context.ruleFor(tag).orElse(null);
    if (rule == null) {
      return maskValue(tag, rawValue, "***");
    }
    ActionCode required = rule.getRequiredAction();
    if (context.action().satisfies(required)) {
      if (rule.isAudit()) {
        log.info(
            "UNMASK_GRANTED user={} tag={} feature={} action={}",
            context.username(),
            tag,
            context.feature(),
            context.action());
      }
      return rawValue;
    }
    if (rule.isAudit()) {
      log.warn(
          "UNMASK_BLOCKED user={} tag={} feature={} action={}",
          context.username(),
          tag,
          context.feature(),
          context.action());
    }
    return maskValue(tag, rawValue, rule.getMaskWith());
  }

  private Object maskValue(String tag, Object rawValue, String maskWith) {
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
