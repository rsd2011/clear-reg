package com.example.dw.application.export.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfUtilTest {

    @Test
    @DisplayName("제목과 문단이 포함된 PDF를 생성한다")
    void createDocument() throws Exception {
        byte[] pdf = PdfUtil.createDocument("Title", List.of("Hello", "World"));
        assertThat(pdf).isNotEmpty();
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Title").contains("Hello").contains("World");
        }
    }

    @Test
    @DisplayName("테이블 형태 PDF를 생성한다")
    void createTable() throws Exception {
        byte[] pdf = PdfUtil.createTable("Table", List.of(List.of("A", "B"), List.of("1", "2")));
        assertThat(pdf).isNotEmpty();
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Table").contains("A").contains("1");
        }
    }
}
