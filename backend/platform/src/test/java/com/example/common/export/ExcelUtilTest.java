package com.example.common.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.export.ExcelUtil;

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
}
