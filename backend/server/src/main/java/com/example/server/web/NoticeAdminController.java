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
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.server.notice.NoticeService;
import com.example.server.notice.dto.NoticeAdminResponse;
import com.example.server.notice.dto.NoticeArchiveRequest;
import com.example.server.notice.dto.NoticeCreateRequest;
import com.example.server.notice.dto.NoticePublishRequest;
import com.example.server.notice.dto.NoticeUpdateRequest;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/admin/notices")
@Tag(name = "Notice Admin", description = "공지 관리자 API")
public class NoticeAdminController {

    private final NoticeService noticeService;

    public NoticeAdminController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PostMapping
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return NoticeAdminResponse.apply(noticeService.createNotice(request, currentUsername()), masker);
    }

    @PutMapping("/{id}")
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse updateNotice(@PathVariable UUID id,
                                            @Valid @RequestBody NoticeUpdateRequest request) {
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return NoticeAdminResponse.apply(noticeService.updateNotice(id, request, currentUsername()), masker);
    }

    @PostMapping("/{id}/publish")
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse publishNotice(@PathVariable UUID id,
                                             @Valid @RequestBody NoticePublishRequest request) {
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return NoticeAdminResponse.apply(noticeService.publishNotice(id, request, currentUsername()), masker);
    }

    @PostMapping("/{id}/archive")
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public NoticeAdminResponse archiveNotice(@PathVariable UUID id,
                                             @RequestBody(required = false) NoticeArchiveRequest request) {
        NoticeArchiveRequest payload = request == null ? new NoticeArchiveRequest(null) : request;
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return NoticeAdminResponse.apply(noticeService.archiveNotice(id, payload, currentUsername()), masker);
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.UPDATE)
    public List<NoticeAdminResponse> listNotices() {
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return noticeService.listNotices().stream()
                .map(n -> NoticeAdminResponse.apply(n, masker))
                .toList();
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(context -> context.username())
                .orElse("system");
    }
}
