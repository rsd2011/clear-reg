package com.example.dw.application.export;

import java.util.Map;

/**
 * 대량 export 요청 파라미터.
 */
public record ExportCommand(
        String exportType,          // CSV, EXCEL, PDF 등
        String fileName,            // 생성될 파일명
        long recordCount,           // 내보낼 레코드 수
        Map<String, Object> meta    // 추가 메타데이터 (rowScope, orgCode 등)
) {
}
