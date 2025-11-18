package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.audit.PermissionAuditLogger;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.auth.permission.context.PermissionDecision;
import com.example.common.security.RowScope;

@ExtendWith(MockitoExtension.class)
class RequirePermissionAspectTest {

    @Mock
    private PermissionEvaluator evaluator;
    @Mock
    private PermissionAuditLogger auditLogger;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;

    private RequirePermissionAspect aspect;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        aspect = new RequirePermissionAspect(evaluator, auditLogger);
    }

    @AfterEach
    void cleanup() {
        AuthContextHolder.clear();
    }

    @Test
    void givenAnnotatedMethod_whenProceeding_thenContextBoundAndCleared() throws Throwable {
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(SecuredComponent.class.getDeclaredMethod("annotated"));
        given(joinPoint.proceed()).willReturn("ok");

        PermissionDecision decision = buildDecision();
        given(evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ)).willReturn(decision);

        Object result = aspect.enforce(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(auditLogger).onAccessGranted(decision.toContext());
        assertThat(AuthContextHolder.current()).isEmpty();
    }

    @Test
    void givenProceedingThrows_whenEnforce_thenAuditDeniedAndPropagate() throws Throwable {
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(SecuredComponent.class.getDeclaredMethod("annotated"));
        RuntimeException failure = new RuntimeException("boom");
        given(joinPoint.proceed()).willThrow(failure);
        PermissionDecision decision = buildDecision();
        given(evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ)).willReturn(decision);

        assertThatThrownBy(() -> aspect.enforce(joinPoint)).isEqualTo(failure);
        verify(auditLogger).onAccessDenied(decision.toContext(), failure);
    }

    private static PermissionDecision buildDecision() {
        UserAccount account = UserAccount.builder()
                .username("auditor")
                .password("encoded")
                .roles(Set.of("ROLE_AUDITOR"))
                .organizationCode("ORG1")
                .permissionGroupCode("AUDIT")
                .build();
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
        PermissionGroup group = new PermissionGroup("AUDIT", "Auditor");
        return new PermissionDecision(account, assignment, group);
    }

    private static class SecuredComponent {

        @RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)
        void annotated() {
        }
    }
}
