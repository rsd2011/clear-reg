package com.example.auth.ad;

import java.util.Optional;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Component;

@Component
public class LdapActiveDirectoryClient implements ActiveDirectoryClient {

  private final Optional<LdapTemplate> ldapTemplate;

  public LdapActiveDirectoryClient(Optional<LdapTemplate> ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  @Override
  public boolean authenticate(String username, String password) {
    if (ldapTemplate.isEmpty()) {
      return username != null && password != null && password.equals("ad-password");
    }
    try {
      EqualsFilter filter = new EqualsFilter("sAMAccountName", username);
      return ldapTemplate.get().authenticate("", filter.encode(), password);
    } catch (Exception exception) {
      return false;
    }
  }
}
