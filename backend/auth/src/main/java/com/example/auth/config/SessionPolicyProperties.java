package com.example.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.session")
public class SessionPolicyProperties {

  private int maxActiveSessions = 2;

  public int getMaxActiveSessions() {
    return maxActiveSessions;
  }

  public void setMaxActiveSessions(int maxActiveSessions) {
    this.maxActiveSessions = maxActiveSessions;
  }
}
