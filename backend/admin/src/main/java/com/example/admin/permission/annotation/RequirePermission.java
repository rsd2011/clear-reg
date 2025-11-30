package com.example.admin.permission.annotation;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequirePermission {

  FeatureCode feature();

  ActionCode action();

  boolean audit() default true;
}
