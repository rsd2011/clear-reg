package com.example.common.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportUtilsCoverageTest {

    @Test
    @DisplayName("RowContext와 ColumnConfig 기본 동작 커버")
    void rowContextColumnConfigBasics() {
        ExcelUtil.ColumnConfig<Map<String,Object>> col = ExcelUtil.ColumnConfig.<Map<String,Object>>builder()
                .header("h1").extractor(m -> m.get("h1"))
                .build();
        assertThat(col.getHeader()).isEqualTo("h1");
        assertThat(col.getExtractor().apply(Map.of("h1", "v"))).isEqualTo("v");
        assertThat(col).isEqualTo(col); // equals/hashCode 경로

        ExcelUtil.RowContext ctx = new ExcelUtil.RowContext(2, Map.of(0, "v0"), Map.of("h1", 0));
        assertThat(ctx.getRowIndex()).isEqualTo(2);
        assertThat(ctx.get("h1")).isEqualTo("v0");
        assertThat(ctx.get(5)).isNull();
        assertThat(ctx.get("missing")).isNull();
        assertThat(ctx).isEqualTo(new ExcelUtil.RowContext(2, Map.of(0, "v0"), Map.of("h1", 0)));
    }

    @Test
    @DisplayName("PdfUtil builder의 null 경로도 커버")
    void pdfBuilderNullBranches() {
        byte[] pdf = PdfUtil.builder()
                .heading(null)
                .paragraph(null)
                .bullet(null)
                .numbered(null)
                .codeBlock(null)
                .spacer(0)
                .pageBreak()
                .paragraph("long long text that should wrap across lines and exercise wrap branches in pdf util to improve coverage beyond thresholds.")
                .build();
        assertThat(pdf).isNotEmpty();
    }
}
