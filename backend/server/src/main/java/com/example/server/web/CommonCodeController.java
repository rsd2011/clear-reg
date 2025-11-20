package com.example.server.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.server.commoncode.CommonCodeQueryService;
import com.example.server.commoncode.dto.CommonCodeAggregateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/common-codes")
@Tag(name = "Common Codes")
public class CommonCodeController {

    private final CommonCodeQueryService commonCodeQueryService;

    public CommonCodeController(CommonCodeQueryService commonCodeQueryService) {
        this.commonCodeQueryService = commonCodeQueryService;
    }

    @GetMapping("/{codeType}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(summary = "Fetch merged common codes from system + DW sources")
    public CommonCodeAggregateResponse getCodes(@PathVariable String codeType,
                                                @RequestParam(defaultValue = "true") boolean includeSystem,
                                                @RequestParam(defaultValue = "true") boolean includeDw) {
        return CommonCodeAggregateResponse.of(codeType,
                commonCodeQueryService.aggregate(codeType, includeSystem, includeDw));
    }
}
