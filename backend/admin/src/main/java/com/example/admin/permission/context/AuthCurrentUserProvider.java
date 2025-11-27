package com.example.admin.permission.context;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.common.security.CurrentUser;
import com.example.common.security.CurrentUserProvider;
import com.example.common.security.RowScope;

/**
 * AuthContext를 CurrentUser로 변환하는 Provider.
 *
 * <p>DataPolicy 기반 마스킹으로 마이그레이션 완료.
 * 마스킹 규칙은 DataPolicyEvaluator가 DataPolicyProvider를 통해 직접 조회합니다.
 */
@Component
public class AuthCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> current() {
        return AuthContextHolder.current().map(this::toCurrentUser);
    }

    private CurrentUser toCurrentUser(AuthContext context) {
        return new CurrentUser(
                context.username(),
                context.organizationCode(),
                context.permissionGroupCode(),
                context.feature() != null ? context.feature().name() : null,
                context.action() != null ? context.action().name() : null,
                context.rowScope() != null ? context.rowScope() : RowScope.ALL,
                context.orgPolicyId(),
                context.orgGroupCodes(),
                context.businessType()
        );
    }
}
