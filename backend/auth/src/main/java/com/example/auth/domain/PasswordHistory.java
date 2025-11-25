package com.example.auth.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "JPA entity exposes related aggregate; managed by JPA session")
@Entity
@Table(name = "password_history")
public class PasswordHistory extends PrimaryKeyEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt = Instant.now();

  protected PasswordHistory() {}

  public PasswordHistory(UserAccount user, String passwordHash) {
    this.user = user;
    this.passwordHash = passwordHash;
  }

  public UserAccount getUser() {
    return user;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Instant getChangedAt() {
    return changedAt;
  }
}
