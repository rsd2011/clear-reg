package com.example.backend.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hr.application.HrOrganizationQueryService;

@RestController
@RequestMapping("/api/hr/organizations")
public class HrOrganizationController {

    private final HrOrganizationQueryService queryService;

    public HrOrganizationController(HrOrganizationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public Page<HrOrganizationResponse> organizations(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return queryService.getOrganizations(pageable)
                .map(HrOrganizationResponse::fromEntity);
    }
}
