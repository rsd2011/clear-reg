package com.example.auth.security;

import java.time.Instant;

/**
 * Service for managing JWT token blacklist.
 *
 * <p>When a user logs out or a token needs to be invalidated before its expiration,
 * the token's JTI (JWT ID) is added to the blacklist. Tokens in the blacklist are
 * automatically removed after their original expiration time.
 */
public interface TokenBlacklistService {

  /**
   * Adds a token's JTI to the blacklist.
   *
   * @param jti the JWT ID to blacklist
   * @param expiresAt the original expiration time of the token
   */
  void blacklist(String jti, Instant expiresAt);

  /**
   * Checks if a token's JTI is in the blacklist.
   *
   * @param jti the JWT ID to check
   * @return true if the token is blacklisted, false otherwise
   */
  boolean isBlacklisted(String jti);

  /**
   * Removes expired entries from the blacklist.
   * This is typically called periodically by a scheduled task.
   */
  void cleanupExpired();
}
