package com.example.common.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfUtilBuilderTest {

    @Test
    @DisplayName("Builder로 heading/paragraph/bullet/table를 쌓아 텍스트를 포함한다")
    void builderBuildsMarkdownLike() throws Exception {
        byte[] pdf = PdfUtil.builder()
                .heading("Title")
                .paragraph("Paragraph line one.")
                .bullet(List.of("item1", "item2"))
                .codeBlock("code line")
                .table(List.of(List.of("A", "B"), List.of("1", "2")))
                .build();

        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Title").contains("Paragraph line one").contains("item1").contains("A").contains("1").contains("code line");
        }
    }
}
