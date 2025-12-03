package com.example.auth.strategy;

import com.example.auth.LoginType;
import com.example.auth.dto.LoginRequest;
import com.example.common.user.spi.UserAccountInfo;

public interface AuthenticationStrategy {

  LoginType supportedType();

  UserAccountInfo authenticate(LoginRequest request);
}
