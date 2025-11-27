package com.example.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * In-memory implementation of {@link TokenBlacklistService}.
 *
 * <p>This implementation stores blacklisted JTIs in a {@link ConcurrentHashMap}.
 * Suitable for single-instance deployments. For distributed deployments,
 * consider using a Redis-based implementation.
 *
 * <p>Expired entries are automatically cleaned up every 10 minutes.
 */
@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

  private static final Logger log = LoggerFactory.getLogger(InMemoryTokenBlacklistService.class);

  private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

  @Override
  public void blacklist(String jti, Instant expiresAt) {
    if (jti == null || expiresAt == null) {
      return;
    }
    blacklist.put(jti, expiresAt);
    log.debug("Token blacklisted: jti={}, expiresAt={}", jti, expiresAt);
  }

  @Override
  public boolean isBlacklisted(String jti) {
    if (jti == null) {
      return false;
    }
    Instant expiresAt = blacklist.get(jti);
    if (expiresAt == null) {
      return false;
    }
    // 만료된 토큰은 블랙리스트에서 제거하고 false 반환
    if (Instant.now().isAfter(expiresAt)) {
      blacklist.remove(jti);
      return false;
    }
    return true;
  }

  @Override
  @Scheduled(fixedRate = 600_000) // 10분마다 실행
  public void cleanupExpired() {
    Instant now = Instant.now();
    int beforeSize = blacklist.size();
    blacklist.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    int removed = beforeSize - blacklist.size();
    if (removed > 0) {
      log.info("Cleaned up {} expired tokens from blacklist, remaining: {}", removed, blacklist.size());
    }
  }

  /** Returns the current size of the blacklist (for monitoring). */
  public int size() {
    return blacklist.size();
  }
}
