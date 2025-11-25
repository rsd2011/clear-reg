package com.example.auth.permission;

import com.example.auth.permission.audit.PermissionAuditLogger;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.auth.permission.context.PermissionDecision;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequirePermissionAspect {

  private final PermissionEvaluator permissionEvaluator;
  private final PermissionAuditLogger auditLogger;

  public RequirePermissionAspect(
      PermissionEvaluator permissionEvaluator, PermissionAuditLogger auditLogger) {
    this.permissionEvaluator = permissionEvaluator;
    this.auditLogger = auditLogger;
  }

  @Around(
      "@annotation(com.example.auth.permission.RequirePermission) ||"
          + " @within(com.example.auth.permission.RequirePermission)")
  public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
    RequirePermission annotation = resolveAnnotation(joinPoint);
    if (annotation == null) {
      return joinPoint.proceed();
    }
    PermissionDecision decision = evaluateWithAudit(annotation);
    AuthContext context = decision.toContext();
    AuthContextHolder.set(context);
    if (annotation.audit()) {
      auditLogger.onAccessGranted(context);
    }
    try {
      return joinPoint.proceed();
    } catch (Throwable throwable) {
      if (annotation.audit()) {
        auditLogger.onAccessDenied(context, throwable);
      }
      throw throwable;
    } finally {
      AuthContextHolder.clear();
    }
  }

  private PermissionDecision evaluateWithAudit(RequirePermission annotation) {
    try {
      return permissionEvaluator.evaluate(annotation.feature(), annotation.action());
    } catch (PermissionDeniedException exception) {
      if (annotation.audit()) {
        auditLogger.onAccessDenied(null, exception);
      }
      throw exception;
    }
  }

  private RequirePermission resolveAnnotation(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    RequirePermission annotation =
        AnnotationUtils.findAnnotation(signature.getMethod(), RequirePermission.class);
    if (annotation != null) {
      return annotation;
    }
    Class<?> targetClass = joinPoint.getTarget().getClass();
    return AnnotationUtils.findAnnotation(targetClass, RequirePermission.class);
  }
}
