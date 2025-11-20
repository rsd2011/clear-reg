package com.example.server.notice;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.server.notice.NoticeAdminResponse;
import com.example.server.notice.NoticeResponse;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeNumberGenerator numberGenerator;
    private final Clock clock;

    public NoticeService(NoticeRepository noticeRepository,
                         NoticeNumberGenerator numberGenerator,
                         Clock clock) {
        this.noticeRepository = noticeRepository;
        this.numberGenerator = numberGenerator;
        this.clock = clock;
    }

    @Transactional
    public NoticeAdminResponse createNotice(NoticeCreateRequest request, String actor) {
        OffsetDateTime now = now();
        Notice notice = new Notice();
        notice.setDisplayNumber(numberGenerator.nextDisplayNumber(now));
        applyDetails(notice, request.title(), request.content(), request.severity(), request.audience(),
                request.publishAt(), request.expireAt(), request.pinned());
        notice.setStatus(NoticeStatus.DRAFT);
        notice.markCreated(actor, now);
        return NoticeAdminResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeAdminResponse updateNotice(UUID id, NoticeUpdateRequest request, String actor) {
        Notice notice = noticeRepository.findLockedById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        if (notice.isArchived()) {
            throw new NoticeStateException("이미 보관된 공지사항은 수정할 수 없습니다.");
        }
        applyDetails(notice, request.title(), request.content(), request.severity(), request.audience(),
                request.publishAt(), request.expireAt(), request.pinned());
        notice.markUpdated(actor, now());
        return NoticeAdminResponse.from(notice);
    }

    @Transactional
    public NoticeAdminResponse publishNotice(UUID id, NoticePublishRequest request, String actor) {
        Notice notice = noticeRepository.findLockedById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        if (notice.isArchived()) {
            throw new NoticeStateException("보관된 공지사항은 게시할 수 없습니다.");
        }
        OffsetDateTime publishAt = request.publishAt() != null ? request.publishAt() : now();
        notice.setPublishAt(publishAt);
        if (request.expireAt() != null) {
            notice.setExpireAt(request.expireAt());
        }
        notice.setPinned(Boolean.TRUE.equals(request.pinned()));
        notice.setStatus(NoticeStatus.PUBLISHED);
        notice.markUpdated(actor, now());
        return NoticeAdminResponse.from(notice);
    }

    @Transactional
    public NoticeAdminResponse archiveNotice(UUID id, NoticeArchiveRequest request, String actor) {
        Notice notice = noticeRepository.findLockedById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        if (notice.isArchived()) {
            return NoticeAdminResponse.from(notice);
        }
        notice.setStatus(NoticeStatus.ARCHIVED);
        notice.setPinned(false);
        notice.setExpireAt(request.expireAt() != null ? request.expireAt() : now());
        notice.markUpdated(actor, now());
        return NoticeAdminResponse.from(notice);
    }

    @Transactional(readOnly = true)
    public List<NoticeAdminResponse> listNotices() {
        return noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "publishAt")).stream()
                .map(NoticeAdminResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> listActiveNotices(NoticeAudience audience) {
        NoticeAudience target = audience == null ? NoticeAudience.GLOBAL : audience;
        return noticeRepository.findActiveNotices(target, now()).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    private void applyDetails(Notice notice,
                              String title,
                              String content,
                              NoticeSeverity severity,
                              NoticeAudience audience,
                              OffsetDateTime publishAt,
                              OffsetDateTime expireAt,
                              Boolean pinned) {
        notice.setTitle(title);
        notice.setContent(content);
        notice.setSeverity(severity);
        notice.setAudience(audience);
        if (publishAt != null) {
            notice.setPublishAt(publishAt);
        }
        if (expireAt != null) {
            notice.setExpireAt(expireAt);
        }
        if (pinned != null) {
            notice.setPinned(pinned);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
