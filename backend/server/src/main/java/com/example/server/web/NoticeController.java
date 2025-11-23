package com.example.server.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.server.notice.NoticeAudience;
import com.example.server.notice.NoticeResponse;
import com.example.server.notice.NoticeService;

@RestController
@Validated
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.NOTICE, action = ActionCode.READ)
    public List<NoticeResponse> getNotices(@RequestParam(required = false) NoticeAudience audience) {
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return noticeService.listActiveNotices(audience).stream()
                .map(n -> NoticeResponse.apply(n, masker))
                .toList();
    }
}
