package com.example.common.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelUtilRowContextTest {

    @Test
    @DisplayName("RowContext getter 들이 헤더/인덱스로 값을 조회한다")
    void rowContextAccessors() throws Exception {
        var cols = List.of(
                ExcelUtil.ColumnConfig.<Map<String,Object>>builder().header("id").extractor(m -> m.get("id")).build(),
                ExcelUtil.ColumnConfig.<Map<String,Object>>builder().header("name").extractor(m -> m.get("name")).build(),
                ExcelUtil.ColumnConfig.<Map<String,Object>>builder().header("date").extractor(m -> m.get("date")).build()
        );
        var data = List.of(Map.<String,Object>of("id", 3, "name", "Lee", "date", java.sql.Date.valueOf(LocalDate.of(2024,1,2))));
        var res = ExcelUtil.writeResource(cols, data);

        try (InputStream in = new ByteArrayInputStream(res.getInputStream().readAllBytes())) {
            var list = ExcelUtil.stream(in, ctx -> ctx).toList();
            var ctx = list.getFirst();
            assertThat(ctx.getRowIndex()).isEqualTo(1); // header = 0, first data = 1
            assertThat(ctx.get("name")).isEqualTo("Lee");
            assertThat(ctx.get("id")).isEqualTo(3.0d); // POI stores numeric as double
            assertThat(ctx.get(1)).isEqualTo("Lee");
            assertThat(ctx.get("missing")).isNull();
            assertThat(ctx.get(5)).isNull();
        }
    }
}
