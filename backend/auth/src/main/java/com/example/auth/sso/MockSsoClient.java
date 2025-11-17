package com.example.auth.sso;

import org.springframework.stereotype.Component;

import com.example.auth.InvalidCredentialsException;

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
