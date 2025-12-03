package com.example.auth.security;

import com.example.common.cache.CacheNames;
import com.example.common.user.spi.UserAccountProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountDetailsService implements UserDetailsService {

  private final UserAccountProvider userAccountProvider;

  public UserAccountDetailsService(UserAccountProvider userAccountProvider) {
    this.userAccountProvider = userAccountProvider;
  }

  @Override
  @Cacheable(cacheNames = CacheNames.USER_DETAILS, key = "#username")
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userAccountProvider
        .findByUsername(username)
        .map(
            account ->
                User.withUsername(account.getUsername())
                    .password(account.getPassword())
                    .authorities(account.getRoles().toArray(String[]::new))
                    .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}
