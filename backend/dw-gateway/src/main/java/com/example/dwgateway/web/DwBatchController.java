package com.example.dwgateway.web;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.dwgateway.dw.DwBatchPort;
import com.example.dwgateway.web.dto.DwBatchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dw/batches")
@Tag(name = "DW Batches")
@RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.READ)
public class DwBatchController {

    private final DwBatchPort batchPort;

    public DwBatchController(DwBatchPort batchPort) {
        this.batchPort = batchPort;
    }

    @GetMapping
    @Operation(summary = "List DW ingestion batches")
    public List<DwBatchResponse> getBatches() {
        Pageable pageable = Pageable.unpaged();
        return batchPort.getBatches(pageable)
                .getContent()
                .stream()
                .map(DwBatchResponse::fromRecord)
                .toList();
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recent DW ingestion batch")
    public ResponseEntity<DwBatchResponse> latest() {
        return batchPort.latestBatch()
                .map(DwBatchResponse::fromRecord)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
