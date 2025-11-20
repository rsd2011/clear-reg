package com.example.auth.permission.declarative;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FieldMaskRule;

@JsonIgnoreProperties(ignoreUnknown = true)
record FieldMaskRuleDefinition(String tag,
                               String maskWith,
                               ActionCode requiredAction,
                               Boolean audit) {

    FieldMaskRule toMaskRule() {
        return new FieldMaskRule(tag, maskWith, requiredAction, audit != null && audit);
    }
}
