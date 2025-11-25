package com.example.auth.sso;

public interface SsoClient {

  String resolveUsername(String token);
}
