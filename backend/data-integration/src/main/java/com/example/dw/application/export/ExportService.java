package com.example.dw.application.export;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 실제 export 로직과 Audit 연계를 위한 래퍼 서비스.
 * 데이터 추출/포맷팅은 caller가 제공하고, 이 서비스는 성공/실패 감사만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportAuditService auditService;

    /**
     * @param command export 메타 정보
     * @param exporter 실제 export를 수행해 byte[] 또는 String 등을 반환하는 함수
     * @return exporter가 반환한 결과
     */
    public <T> T export(ExportCommand command, Supplier<T> exporter) {
        try {
            T result = exporter.get();
            auditService.auditExport(command.exportType(),
                    command.recordCount(),
                    true,
                    mergeMeta(command));
            return result;
        } catch (Exception ex) {
            auditService.auditExport(command.exportType(),
                    command.recordCount(),
                    false,
                    mergeMeta(command));
            throw ex;
        }
    }

    private Map<String, Object> mergeMeta(ExportCommand command) {
        Map<String, Object> meta = command.meta() != null ? command.meta() : Map.of();
        if (!meta.containsKey("fileName") && command.fileName() != null) {
            meta = new java.util.HashMap<>(meta);
            meta.put("fileName", command.fileName());
        }
        return meta;
    }
}
