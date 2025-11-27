package com.example.admin.permission.declarative;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FieldMaskRule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record FieldMaskRuleDefinition(
    String tag, String maskWith, ActionCode requiredAction, Boolean audit) {

  FieldMaskRule toMaskRule() {
    return new FieldMaskRule(tag, maskWith, requiredAction, audit != null && audit);
  }
}
