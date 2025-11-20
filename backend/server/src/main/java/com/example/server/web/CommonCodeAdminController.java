package com.example.server.web;

import java.util.List;

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
import com.example.server.commoncode.SystemCommonCodeService;
import com.example.server.commoncode.dto.SystemCommonCodeRequest;
import com.example.server.commoncode.dto.SystemCommonCodeResponse;
import com.example.server.commoncode.model.SystemCommonCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/admin/common-codes")
@Tag(name = "Common Code Admin")
public class CommonCodeAdminController {

    private final SystemCommonCodeService systemCommonCodeService;

    public CommonCodeAdminController(SystemCommonCodeService systemCommonCodeService) {
        this.systemCommonCodeService = systemCommonCodeService;
    }

    @GetMapping("/{codeType}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    @Operation(summary = "List system-managed common codes")
    public List<SystemCommonCodeResponse> list(@PathVariable String codeType) {
        List<SystemCommonCode> codes = systemCommonCodeService.findAll(codeType);
        return codes.stream().map(SystemCommonCodeResponse::from).toList();
    }

    @PostMapping("/{codeType}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    public SystemCommonCodeResponse create(@PathVariable String codeType,
                                           @Valid @RequestBody SystemCommonCodeRequest request) {
        SystemCommonCode code = systemCommonCodeService.create(codeType, request.toEntity(currentUsername()));
        return SystemCommonCodeResponse.from(code);
    }

    @PutMapping("/{codeType}/{codeValue}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    public SystemCommonCodeResponse update(@PathVariable String codeType,
                                           @PathVariable String codeValue,
                                           @Valid @RequestBody SystemCommonCodeRequest request) {
        SystemCommonCode code = systemCommonCodeService.update(codeType, codeValue,
                request.toEntity(currentUsername()));
        return SystemCommonCodeResponse.from(code);
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(context -> context.username())
                .orElse("system");
    }
}
