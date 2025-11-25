package com.example.auth.permission.context;

import java.util.concurrent.Callable;

/** Utility methods to capture and propagate {@link AuthContext} across threads. */
public final class AuthContextPropagator {

  private AuthContextPropagator() {}

  public static Runnable wrapCurrentContext(Runnable delegate) {
    AuthContext captured = AuthContextHolder.current().orElse(null);
    return wrap(delegate, captured);
  }

  public static Runnable wrap(Runnable delegate, AuthContext context) {
    return () -> runWithContext(context, delegate);
  }

  public static <V> Callable<V> wrapCurrentContext(Callable<V> delegate) {
    AuthContext captured = AuthContextHolder.current().orElse(null);
    return wrap(delegate, captured);
  }

  public static <V> Callable<V> wrap(Callable<V> delegate, AuthContext context) {
    return () -> callWithContext(context, delegate);
  }

  public static void runWithContext(AuthContext context, Runnable delegate) {
    AuthContext previous = AuthContextHolder.current().orElse(null);
    try {
      if (context != null) {
        AuthContextHolder.set(context);
      }
      delegate.run();
    } finally {
      restore(previous);
    }
  }

  public static void runWithCurrentContext(Runnable delegate) {
    runWithContext(AuthContextHolder.current().orElse(null), delegate);
  }

  public static <V> V callWithContext(AuthContext context, Callable<V> delegate) throws Exception {
    AuthContext previous = AuthContextHolder.current().orElse(null);
    try {
      if (context != null) {
        AuthContextHolder.set(context);
      }
      return delegate.call();
    } finally {
      restore(previous);
    }
  }

  private static void restore(AuthContext context) {
    if (context == null) {
      AuthContextHolder.clear();
    } else {
      AuthContextHolder.set(context);
    }
  }
}
