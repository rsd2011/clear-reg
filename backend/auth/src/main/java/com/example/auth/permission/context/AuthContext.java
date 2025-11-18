package com.example.auth.permission.context;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.common.security.RowScope;

public record AuthContext(String username,
                          String organizationCode,
                          String permissionGroupCode,
                          FeatureCode feature,
                          ActionCode action,
                          RowScope rowScope,
                          Map<String, FieldMaskRule> fieldMaskRules) {

    public AuthContext {
        fieldMaskRules = fieldMaskRules == null ? Map.of() : Collections.unmodifiableMap(fieldMaskRules);
    }

    public Optional<FieldMaskRule> ruleFor(String tag) {
        return Optional.ofNullable(fieldMaskRules.get(tag));
    }
}
