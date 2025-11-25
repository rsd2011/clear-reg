package com.example.auth.security;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Validator holds configuration; no mutable state leaked")
@Component
public class PasswordPolicyValidator {

  private final AuthPolicyProperties properties;
  private final PolicyToggleProvider policyToggleProvider;

  private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

  public PasswordPolicyValidator(
      AuthPolicyProperties properties, PolicyToggleProvider policyToggleProvider) {
    this.properties = properties;
    this.policyToggleProvider = policyToggleProvider;
  }

  public void validate(String password) {
    if (!policyToggleProvider.isPasswordPolicyEnabled()) {
      return;
    }
    if (password == null) {
      throw new InvalidCredentialsException();
    }
    if (password.length() < properties.getPasswordMinLength()) {
      throw new InvalidCredentialsException();
    }
    if (properties.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
      throw new InvalidCredentialsException();
    }
    if (properties.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
      throw new InvalidCredentialsException();
    }
    if (properties.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
      throw new InvalidCredentialsException();
    }
    if (properties.isRequireSpecial() && !SPECIAL.matcher(password).find()) {
      throw new InvalidCredentialsException();
    }
  }
}
