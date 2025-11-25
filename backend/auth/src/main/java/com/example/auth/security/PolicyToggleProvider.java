package com.example.auth.security;

import com.example.auth.LoginType;
import java.util.List;

public interface PolicyToggleProvider {

  boolean isPasswordPolicyEnabled();

  boolean isPasswordHistoryEnabled();

  boolean isAccountLockEnabled();

  List<LoginType> enabledLoginTypes();
}
