package com.example.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.admin.permission.audit.PermissionAuditLogger;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.PermissionDecision;
import com.example.admin.permission.spi.UserInfo;
import com.example.common.security.RowScope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Set;

@DisplayName("RequirePermissionAspect 테스트")
class RequirePermissionAspectTest {

    @Mock
    private PermissionEvaluator permissionEvaluator;

    @Mock
    private PermissionAuditLogger auditLogger;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private RequirePermissionAspect aspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new RequirePermissionAspect(permissionEvaluator, auditLogger);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Nested
    @DisplayName("enforce 메서드")
    class Enforce {

        @Test
        @DisplayName("Given 어노테이션 없음 When 호출하면 Then 그냥 proceed")
        void givenNoAnnotation_proceedsWithoutCheck() throws Throwable {
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(NoAnnotationClass.class.getMethod("noAnnotationMethod"));
            given(joinPoint.getTarget()).willReturn(new NoAnnotationClass());
            given(joinPoint.proceed()).willReturn("result");

            Object result = aspect.enforce(joinPoint);

            assertThat(result).isEqualTo("result");
            verify(permissionEvaluator, never()).evaluate(any(), any());
        }

        @Test
        @DisplayName("Given 메서드 어노테이션 When 권한 있으면 Then 실행 및 감사 로깅")
        void givenMethodAnnotation_whenPermitted_executesAndAudits() throws Throwable {
            Method method = AnnotatedClass.class.getMethod("annotatedMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new AnnotatedClass());
            given(joinPoint.proceed()).willReturn("success");

            UserInfo userInfo = createUserInfo();
            PermissionAssignment assignment = createAssignment();
            PermissionGroup group = new PermissionGroup("TEST_GROUP", "테스트그룹");
            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);

            given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.READ)).willReturn(decision);

            Object result = aspect.enforce(joinPoint);

            assertThat(result).isEqualTo("success");
            verify(auditLogger).onAccessGranted(any(AuthContext.class));
            assertThat(AuthContextHolder.current()).isEmpty(); // finally에서 clear됨
        }

        @Test
        @DisplayName("Given audit=false When 권한 있으면 Then 감사 로깅 안함")
        void givenAuditFalse_skipsAuditLogging() throws Throwable {
            Method method = NoAuditClass.class.getMethod("noAuditMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new NoAuditClass());
            given(joinPoint.proceed()).willReturn("result");

            UserInfo userInfo = createUserInfo();
            PermissionAssignment assignment = createAssignment();
            PermissionGroup group = new PermissionGroup("GROUP", "그룹");
            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);

            given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.CREATE)).willReturn(decision);

            aspect.enforce(joinPoint);

            verify(auditLogger, never()).onAccessGranted(any());
        }

        @Test
        @DisplayName("Given 클래스 어노테이션 When 호출하면 Then 클래스 어노테이션 사용")
        void givenClassAnnotation_usesClassAnnotation() throws Throwable {
            Method method = ClassAnnotatedClass.class.getMethod("someMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new ClassAnnotatedClass());
            given(joinPoint.proceed()).willReturn("result");

            UserInfo userInfo = createUserInfo();
            PermissionAssignment assignment = createAssignmentForOrg();
            PermissionGroup group = new PermissionGroup("GROUP", "그룹");
            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);

            given(permissionEvaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ)).willReturn(decision);

            aspect.enforce(joinPoint);

            verify(permissionEvaluator).evaluate(FeatureCode.ORGANIZATION, ActionCode.READ);
        }

        @Test
        @DisplayName("Given PermissionDenied When evaluate Then 감사 로그 후 예외")
        void givenPermissionDenied_auditsAndThrows() throws Throwable {
            Method method = AnnotatedClass.class.getMethod("annotatedMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new AnnotatedClass());

            PermissionDeniedException exception = new PermissionDeniedException("권한 없음");
            given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.READ)).willThrow(exception);

            assertThatThrownBy(() -> aspect.enforce(joinPoint))
                    .isInstanceOf(PermissionDeniedException.class);

            verify(auditLogger).onAccessDenied(null, exception);
        }

        @Test
        @DisplayName("Given audit=false When PermissionDenied Then 감사 로그 없이 예외")
        void givenNoAuditAndPermissionDenied_throwsWithoutAudit() throws Throwable {
            Method method = NoAuditClass.class.getMethod("noAuditMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new NoAuditClass());

            PermissionDeniedException exception = new PermissionDeniedException("권한 없음");
            given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.CREATE)).willThrow(exception);

            assertThatThrownBy(() -> aspect.enforce(joinPoint))
                    .isInstanceOf(PermissionDeniedException.class);

            verify(auditLogger, never()).onAccessDenied(any(), any());
        }

        @Test
        @DisplayName("Given proceed에서 예외 When 호출하면 Then 감사 로그 후 예외 전파")
        void givenExceptionDuringProceed_auditsAndRethrows() throws Throwable {
            Method method = AnnotatedClass.class.getMethod("annotatedMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new AnnotatedClass());

            UserInfo userInfo = createUserInfo();
            PermissionAssignment assignment = createAssignment();
            PermissionGroup group = new PermissionGroup("GROUP", "그룹");
            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);

            given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.READ)).willReturn(decision);
            RuntimeException proceedException = new RuntimeException("실행 중 오류");
            given(joinPoint.proceed()).willThrow(proceedException);

            assertThatThrownBy(() -> aspect.enforce(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("실행 중 오류");

            verify(auditLogger).onAccessGranted(any(AuthContext.class));
            verify(auditLogger).onAccessDenied(any(AuthContext.class), any(RuntimeException.class));
        }

        @Test
        @DisplayName("Given audit=false + proceed 예외 When 호출하면 Then 감사 없이 예외")
        void givenNoAuditAndProceedException_throwsWithoutAudit() throws Throwable {
            Method method = NoAuditClass.class.getMethod("noAuditMethod");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getMethod()).willReturn(method);
            given(joinPoint.getTarget()).willReturn(new NoAuditClass());

            UserInfo userInfo = createUserInfo();
            PermissionAssignment assignment = createAssignment();
            PermissionGroup group = new PermissionGroup("GROUP", "그룹");
            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);

            given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.CREATE)).willReturn(decision);
            given(joinPoint.proceed()).willThrow(new RuntimeException("오류"));

            assertThatThrownBy(() -> aspect.enforce(joinPoint))
                    .isInstanceOf(RuntimeException.class);

            verify(auditLogger, never()).onAccessDenied(any(), any());
        }
    }

    private UserInfo createUserInfo() {
        return new UserInfo() {
            @Override
            public String getUsername() {
                return "testuser";
            }

            @Override
            public String getOrganizationCode() {
                return "ORG001";
            }

            @Override
            public String getPermissionGroupCode() {
                return "PERM_GROUP";
            }

            @Override
            public Set<String> getRoles() {
                return Set.of("ROLE_USER");
            }
        };
    }

    private PermissionAssignment createAssignment() {
        return new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, RowScope.ORG);
    }

    private PermissionAssignment createAssignmentForOrg() {
        return new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
    }

    // 테스트용 클래스들
    static class NoAnnotationClass {
        public String noAnnotationMethod() {
            return "no annotation";
        }
    }

    static class AnnotatedClass {
        @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.READ)
        public String annotatedMethod() {
            return "annotated";
        }
    }

    static class NoAuditClass {
        @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.CREATE, audit = false)
        public String noAuditMethod() {
            return "no audit";
        }
    }

    @RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)
    static class ClassAnnotatedClass {
        public String someMethod() {
            return "class annotated";
        }
    }
}
