package com.example.auth.permission.context;

import org.springframework.core.task.TaskDecorator;

/**
 * Spring {@link TaskDecorator} that propagates the current {@link AuthContext}
 * to asynchronous executors.
 */
public final class AuthContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return AuthContextPropagator.wrapCurrentContext(runnable);
    }
}
