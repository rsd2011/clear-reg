package com.example.common.security;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks repositories that must enforce RowScope-aware filtering.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface RequiresRowScope {
}
