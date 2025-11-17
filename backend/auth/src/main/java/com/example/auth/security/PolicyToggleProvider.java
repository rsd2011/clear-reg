package com.example.auth.security;

import java.util.List;

import com.example.auth.LoginType;

public interface PolicyToggleProvider {

    boolean isPasswordPolicyEnabled();

    boolean isPasswordHistoryEnabled();

    boolean isAccountLockEnabled();

    List<LoginType> enabledLoginTypes();
}
