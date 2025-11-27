package com.example.admin.permission.context;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.admin.permission.FieldMaskRule;
import com.example.common.masking.MaskRuleDefinition;
import com.example.common.security.CurrentUser;
import com.example.common.security.CurrentUserProvider;
import com.example.common.security.RowScope;

@Component
public class AuthCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> current() {
        return AuthContextHolder.current().map(this::toCurrentUser);
    }

    private CurrentUser toCurrentUser(AuthContext context) {
        Map<String, MaskRuleDefinition> rules = context.fieldMaskRules().entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> toDefinition(entry.getValue())));
        return new CurrentUser(context.username(),
                context.organizationCode(),
                context.permissionGroupCode(),
                context.feature() != null ? context.feature().name() : null,
                context.action() != null ? context.action().name() : null,
                context.rowScope() != null ? context.rowScope() : RowScope.ALL,
                rules);
    }

    private MaskRuleDefinition toDefinition(FieldMaskRule rule) {
        return new MaskRuleDefinition(rule.getTag(), rule.getMaskWith(),
                rule.getRequiredAction() != null ? rule.getRequiredAction().name() : null,
                rule.isAudit());
    }
}
