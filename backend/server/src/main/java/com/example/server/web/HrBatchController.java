package com.example.server.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.hr.application.HrBatchQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/hr/batches")
@Tag(name = "HR Batches")
@RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.READ)
public class HrBatchController {

    private final HrBatchQueryService batchQueryService;

    public HrBatchController(HrBatchQueryService batchQueryService) {
        this.batchQueryService = batchQueryService;
    }

    @GetMapping
    @Operation(summary = "List HR ingestion batches with pagination")
    public Page<HrBatchResponse> getBatches(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return batchQueryService.getBatches(pageable)
                .map(HrBatchResponse::fromEntity);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recent HR ingestion batch")
    public ResponseEntity<HrBatchResponse> latest() {
        return batchQueryService.latestBatch()
                .map(HrBatchResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
