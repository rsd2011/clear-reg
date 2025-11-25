package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.audit.PermissionAuditLogger;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.auth.permission.context.PermissionDecision;
import com.example.common.security.RowScope;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequirePermissionAspectClassLevelTest {

  @Mock ProceedingJoinPoint joinPoint;
  @Mock MethodSignature signature;
  @Mock PermissionEvaluator evaluator;
  @Mock PermissionAuditLogger auditLogger;

  @AfterEach
  void cleanup() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("클래스에 @RequirePermission이 있으면 메서드에 없어도 평가/감사를 수행한다")
  void classLevelAnnotationEnforced() throws Throwable {
    given(joinPoint.getSignature()).willReturn(signature);
    given(joinPoint.getTarget()).willReturn(new ClassSecuredComponent());
    given(signature.getMethod()).willReturn(ClassSecuredComponent.class.getDeclaredMethod("plain"));
    given(joinPoint.proceed()).willReturn("ok");

    PermissionDecision decision =
        new PermissionDecision(
            UserAccount.builder()
                .username("u")
                .password("p")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .roles(Set.of("R"))
                .build(),
            new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN),
            new PermissionGroup("PG", "name"));
    given(evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ)).willReturn(decision);

    RequirePermissionAspect aspect = new RequirePermissionAspect(evaluator, auditLogger);
    Object result = aspect.enforce(joinPoint);

    assertThat(result).isEqualTo("ok");
    verify(auditLogger).onAccessGranted(decision.toContext());
  }

  @RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)
  static class ClassSecuredComponent {
    void plain() {}
  }
}
