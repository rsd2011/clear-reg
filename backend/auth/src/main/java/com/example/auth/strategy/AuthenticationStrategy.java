package com.example.auth.strategy;

import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.dto.LoginRequest;

public interface AuthenticationStrategy {

    LoginType supportedType();

    UserAccount authenticate(LoginRequest request);
}
