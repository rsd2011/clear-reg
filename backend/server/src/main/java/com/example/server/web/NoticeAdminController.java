package com.example.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.server.notice.NoticeAdminResponse;
import com.example.server.notice.NoticeArchiveRequest;
import com.example.server.notice.NoticeCreateRequest;
import com.example.server.notice.NoticePublishRequest;
import com.example.server.notice.NoticeService;
import com.example.server.notice.NoticeUpdateRequest;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/admin/notices")
public class NoticeAdminController {

    private final NoticeService noticeService;

    public NoticeAdminController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        return noticeService.createNotice(request, currentUsername());
    }

    @PutMapping("/{id}")
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse updateNotice(@PathVariable UUID id,
                                            @Valid @RequestBody NoticeUpdateRequest request) {
        return noticeService.updateNotice(id, request, currentUsername());
    }

    @PostMapping("/{id}/publish")
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse publishNotice(@PathVariable UUID id,
                                             @Valid @RequestBody NoticePublishRequest request) {
        return noticeService.publishNotice(id, request, currentUsername());
    }

    @PostMapping("/{id}/archive")
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse archiveNotice(@PathVariable UUID id,
                                             @RequestBody(required = false) NoticeArchiveRequest request) {
        NoticeArchiveRequest payload = request == null ? new NoticeArchiveRequest(null) : request;
        return noticeService.archiveNotice(id, payload, currentUsername());
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public List<NoticeAdminResponse> listNotices() {
        return noticeService.listNotices();
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(context -> context.username())
                .orElse("system");
    }
}
