package com.example.server.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.admin.codemanage.CodeManageQueryService;
import com.example.admin.codemanage.dto.CodeManageAggregateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/common-codes")
@Tag(name = "Common Codes")
public class CommonCodeController {

    private final CodeManageQueryService codeManageQueryService;

    public CommonCodeController(CodeManageQueryService codeManageQueryService) {
        this.codeManageQueryService = codeManageQueryService;
    }

    @GetMapping("/{codeType}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(summary = "Fetch merged common codes from system + DW sources")
    public CodeManageAggregateResponse getCodes(@PathVariable String codeType,
                                                @RequestParam(defaultValue = "true") boolean includeSystem,
                                                @RequestParam(defaultValue = "true") boolean includeDw) {
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        return CodeManageAggregateResponse.of(codeType,
                codeManageQueryService.aggregate(codeType, includeSystem, includeDw),
                masker);
    }
}
