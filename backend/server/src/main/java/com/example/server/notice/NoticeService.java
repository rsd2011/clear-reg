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
        Notice notice = Notice.createDraft(
                numberGenerator.nextDisplayNumber(now),
                request.title(),
                request.content(),
                request.severity(),
                request.audience(),
                request.publishAt(),
                request.expireAt(),
                Boolean.TRUE.equals(request.pinned()),
                actor,
                now
        );
        return NoticeAdminResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeAdminResponse updateNotice(UUID id, NoticeUpdateRequest request, String actor) {
        Notice notice = noticeRepository.findLockedById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        notice.updateContent(
                request.title(),
                request.content(),
                request.severity(),
                request.audience(),
                request.publishAt(),
                request.expireAt(),
                Boolean.TRUE.equals(request.pinned()),
                actor,
                now()
        );
        return NoticeAdminResponse.from(notice);
    }

    @Transactional
    public NoticeAdminResponse publishNotice(UUID id, NoticePublishRequest request, String actor) {
        Notice notice = noticeRepository.findLockedById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        notice.publish(request.publishAt(), request.expireAt(), Boolean.TRUE.equals(request.pinned()), actor, now());
        return NoticeAdminResponse.from(notice);
    }

    @Transactional
    public NoticeAdminResponse archiveNotice(UUID id, NoticeArchiveRequest request, String actor) {
        Notice notice = noticeRepository.findLockedById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
        notice.archive(request.expireAt(), actor, now());
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

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
