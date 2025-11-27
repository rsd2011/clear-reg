package com.example.auth.domain;

import com.example.admin.permission.spi.UserInfo;
import com.example.admin.permission.spi.UserInfoProvider;
import org.springframework.stereotype.Component;

/**
 * UserAccountService를 UserInfoProvider로 변환하는 어댑터.
 */
@Component
public class UserInfoProviderAdapter implements UserInfoProvider {

  private final UserAccountService userAccountService;

  public UserInfoProviderAdapter(UserAccountService userAccountService) {
    this.userAccountService = userAccountService;
  }

  @Override
  public UserInfo getByUsernameOrThrow(String username) {
    return userAccountService.getByUsernameOrThrow(username);
  }
}
