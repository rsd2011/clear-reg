package com.example.common.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelUtilTest {

    @Test
    @DisplayName("writeResource로 생성한 XLSX를 stream으로 다시 읽을 수 있다")
    void writeAndStream() throws Exception {
        var cols = List.of(
                ExcelUtil.ColumnConfig.<Map<String,Object>>builder().header("name").extractor(m -> m.get("name")).build(),
                ExcelUtil.ColumnConfig.<Map<String,Object>>builder().header("score").extractor(m -> m.get("score")).build()
        );
        var data = List.of(Map.<String,Object>of("name", "Kim", "score", 90));

        var res = ExcelUtil.writeResource(cols, data);
        assertThat(res.contentLength()).isGreaterThan(0);

        try (InputStream in = new ByteArrayInputStream(res.getInputStream().readAllBytes())) {
            var stream = ExcelUtil.stream(in, ctx -> Map.of(
                    "name", ctx.get("name"),
                    "score", ctx.get("score")
            ));
            var list = stream.toList();
            assertThat(list).hasSize(1);
            assertThat(list.get(0).get("name")).isEqualTo("Kim");
        }
    }

    @Test
    @DisplayName("writeResource 결과 Workbook을 읽을 수 있다")
    void toWorkbook() throws Exception {
        var cols = List.of(ExcelUtil.ColumnConfig.<Map<String,Object>>builder().header("id").extractor(m -> m.get("id")).build());
        var res = ExcelUtil.writeResource(cols, List.of(Map.of("id", 1)));
        Workbook wb = ExcelUtil.toWorkbook(res);
        assertThat(wb.getNumberOfSheets()).isGreaterThan(0);
        wb.close();
    }
    @Test
    @DisplayName("stream이 다양한 셀 타입(boolean/blank)도 처리한다")
    void streamHandlesBooleanAndBlank() throws Exception {
        // 수동으로 워크북 작성
        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = wb.createSheet();
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("flag");
            header.createCell(1).setCellValue("empty");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(true);
            row.createCell(1, org.apache.poi.ss.usermodel.CellType.BLANK);
            var out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            try (InputStream in = new ByteArrayInputStream(out.toByteArray())) {
                var list = ExcelUtil.stream(in, ctx -> Map.of("flag", ctx.get("flag"), "empty", ctx.get("empty"))).toList();
                assertThat(list).hasSize(1);
                assertThat(list.getFirst().get("flag")).isEqualTo(true);
                assertThat(list.getFirst().get("empty")).isEqualTo("");
            }
        }
    }
}
