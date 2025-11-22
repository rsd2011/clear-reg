package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.permission.audit.PermissionAuditLogger;
import com.example.auth.permission.context.AuthContextHolder;

@ExtendWith(MockitoExtension.class)
class RequirePermissionAspectNoAnnotationTest {

    @Mock ProceedingJoinPoint joinPoint;
    @Mock MethodSignature methodSignature;
    @Mock PermissionEvaluator evaluator;
    @Mock PermissionAuditLogger auditLogger;

    @AfterEach
    void cleanup() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("@RequirePermission이 없으면 평가/감사 없이 그냥 진행한다")
    void proceedsWhenNoAnnotation() throws Throwable {
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getMethod()).willReturn(NoSecuredComponent.class.getDeclaredMethod("plain"));
        given(joinPoint.getTarget()).willReturn(new NoSecuredComponent());
        given(joinPoint.proceed()).willReturn("ok");

        RequirePermissionAspect aspect = new RequirePermissionAspect(evaluator, auditLogger);
        Object result = aspect.enforce(joinPoint);

        assertThat(result).isEqualTo("ok");
        verify(auditLogger, never()).onAccessGranted(org.mockito.ArgumentMatchers.any());
        verify(auditLogger, never()).onAccessDenied(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(AuthContextHolder.current()).isEmpty();
    }

    static class NoSecuredComponent {
        void plain() {}
    }
}
