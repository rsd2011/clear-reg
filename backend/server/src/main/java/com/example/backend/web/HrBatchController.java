package com.example.backend.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hr.application.HrBatchQueryService;

@RestController
@RequestMapping("/api/hr/batches")
public class HrBatchController {

    private final HrBatchQueryService batchQueryService;

    public HrBatchController(HrBatchQueryService batchQueryService) {
        this.batchQueryService = batchQueryService;
    }

    @GetMapping
    public Page<HrBatchResponse> getBatches(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return batchQueryService.getBatches(pageable)
                .map(HrBatchResponse::fromEntity);
    }

    @GetMapping("/latest")
    public ResponseEntity<HrBatchResponse> latest() {
        return batchQueryService.latestBatch()
                .map(HrBatchResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
