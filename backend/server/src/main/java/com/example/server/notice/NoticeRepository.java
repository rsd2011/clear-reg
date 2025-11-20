package com.example.server.notice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notice n where n.id = :id")
    Optional<Notice> findLockedById(@Param("id") UUID id);

    @Query("""
            select n from Notice n
            where n.status = com.example.server.notice.NoticeStatus.PUBLISHED
              and n.publishAt <= :now
              and (n.expireAt is null or n.expireAt > :now)
              and (n.audience = :audience or n.audience = com.example.server.notice.NoticeAudience.GLOBAL)
            order by n.pinned desc, n.publishAt desc
            """)
    List<Notice> findActiveNotices(@Param("audience") NoticeAudience audience,
                                   @Param("now") OffsetDateTime now);
}
