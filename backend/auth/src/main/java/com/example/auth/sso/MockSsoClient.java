package com.example.auth.sso;

import com.example.auth.InvalidCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class MockSsoClient implements SsoClient {

  @Override
  public String resolveUsername(String token) {
    if (token == null || !token.startsWith("SSO-")) {
      throw new InvalidCredentialsException();
    }
    return token.substring(4);
  }
}
