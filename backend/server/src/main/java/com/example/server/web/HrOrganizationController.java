package com.example.server.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.hr.application.HrOrganizationQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/hr/organizations")
@Tag(name = "HR Organizations")
public class HrOrganizationController {

    private final HrOrganizationQueryService queryService;

    public HrOrganizationController(HrOrganizationQueryService queryService) {
        this.queryService = queryService;
    }

    @RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)
    @GetMapping
    @Operation(summary = "List organizations visible to the requester")
    public Page<HrOrganizationResponse> organizations(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        AuthContext context = AuthContextHolder.current()
                .orElseThrow(() -> new AccessDeniedException("권한 정보가 없습니다."));
        if (context.rowScope() == null || context.organizationCode() == null) {
            throw new AccessDeniedException("조직 스코프 정보가 올바르지 않습니다.");
        }
        return queryService.getOrganizations(pageable, context.rowScope(), context.organizationCode())
                .map(HrOrganizationResponse::fromEntity);
    }
}
