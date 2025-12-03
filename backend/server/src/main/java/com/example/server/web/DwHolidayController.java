package com.example.server.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.dw.application.port.DwHolidayPort;
import com.example.server.web.dto.DwHolidayResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dw/holidays")
@Tag(name = "DW Holidays", description = "DW 휴일 조회 API")
@RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.READ)
public class DwHolidayController {

    private final DwHolidayPort holidayPort;

    public DwHolidayController(DwHolidayPort holidayPort) {
        this.holidayPort = holidayPort;
    }

    @GetMapping
    @Operation(summary = "List holidays for a country", description = "특정 국가의 휴일 목록을 조회합니다.")
    public List<DwHolidayResponse> holidays(
            @Parameter(description = "Country code (e.g., KR, US)")
            @RequestParam String countryCode) {
        return holidayPort.findByCountry(countryCode)
                .stream()
                .map(DwHolidayResponse::fromRecord)
                .toList();
    }

    @GetMapping("/range")
    @Operation(summary = "List holidays in date range", description = "날짜 범위 내 휴일을 조회합니다.")
    public List<DwHolidayResponse> holidaysInRange(
            @Parameter(description = "Country code (optional)")
            @RequestParam(required = false) String countryCode,
            @Parameter(description = "Start date (inclusive)", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DwHolidayPort.DwHolidayRecord> records;
        if (countryCode != null && !countryCode.isBlank()) {
            records = holidayPort.findByCountryAndDateRange(countryCode, startDate, endDate);
        } else {
            records = holidayPort.findByDateRange(startDate, endDate);
        }
        return records.stream().map(DwHolidayResponse::fromRecord).toList();
    }

    @GetMapping("/check")
    @Operation(summary = "Check if a date is a holiday", description = "특정 날짜가 휴일인지 확인합니다.")
    public ResponseEntity<Boolean> isHoliday(
            @Parameter(description = "Date to check", example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Country code", example = "KR")
            @RequestParam String countryCode) {
        return ResponseEntity.ok(holidayPort.isHoliday(date, countryCode));
    }

    @GetMapping("/{date}/{countryCode}")
    @Operation(summary = "Get holiday details", description = "특정 날짜와 국가의 휴일 상세 정보를 조회합니다.")
    public ResponseEntity<DwHolidayResponse> holiday(
            @Parameter(description = "Holiday date", example = "2025-01-01")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Country code", example = "KR")
            @PathVariable String countryCode) {
        return holidayPort.findByDateAndCountry(date, countryCode)
                .map(DwHolidayResponse::fromRecord)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/countries")
    @Operation(summary = "List available country codes", description = "휴일이 등록된 국가 코드 목록을 조회합니다.")
    public List<String> availableCountryCodes() {
        return holidayPort.findAvailableCountryCodes();
    }
}
