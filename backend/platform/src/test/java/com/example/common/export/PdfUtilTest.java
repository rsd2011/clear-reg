package com.example.common.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfUtilTest {

    @Test
    @DisplayName("제목과 문단이 포함된 PDF를 생성한다")
    void createDocument() throws Exception {
        byte[] pdf = PdfUtil.createDocument("Title", List.of("Hello", "World"));
        assertThat(pdf).isNotEmpty();
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Title").contains("Hello").contains("World");
        }
    }

    @Test
    @DisplayName("테이블 형태 PDF를 생성한다")
    void createTable() throws Exception {
        byte[] pdf = PdfUtil.createTable("Table", List.of(List.of("A", "B"), List.of("1", "2")));
        assertThat(pdf).isNotEmpty();
        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Table").contains("A").contains("1");
        }
    }

    @Test
    @DisplayName("extractText 헬퍼가 PDF 텍스트를 반환한다")
    void extractText() {
        byte[] pdf = PdfUtil.createDocument("Hello", List.of("World"));
        String text = PdfUtil.extractText(pdf);
        assertThat(text).contains("Hello").contains("World");
    }

    @Test
    @DisplayName("편의 메서드 createBullets/createCodeBlock도 정상 생성된다")
    void convenienceFactories() {
        byte[] bullets = PdfUtil.createBullets("B", List.of("one", "two"));
        byte[] code = PdfUtil.createCodeBlock("C", "line1\nline2");
        assertThat(bullets).isNotEmpty();
        assertThat(code).isNotEmpty();
        String text = PdfUtil.extractText(code);
        assertThat(text).contains("line1").contains("line2");
    }

    @Test
    @DisplayName("hr/spacer/codeBlock/table/pageBreak 조합도 생성된다")
    void mixedBlocks() {
        byte[] pdf = PdfUtil.builder()
                .heading("Misc")
                .hr()
                .spacer(8)
                .codeBlock("line1\nline2withaverylongwordthatshouldwrapcorrectly")
                .table(List.of(List.of("c1", "c2")))
                .pageBreak()
                .paragraph("after break")
                .build();
        String text = PdfUtil.extractText(pdf);
        assertThat(text).contains("Misc").contains("line1").contains("after break");
    }

    @Test
    @DisplayName("여러 행 테이블로 페이지 전환과 래핑을 강제한다")
    void largeTableTriggersNewPage() {
        List<List<String>> rows = java.util.stream.IntStream.range(0, 40)
                .mapToObj(i -> List.of("row" + i, "value" + i))
                .toList();
        byte[] pdf = PdfUtil.builder()
                .heading("LargeTable")
                .table(rows)
                .build();
        String text = PdfUtil.extractText(pdf);
        assertThat(text).contains("LargeTable").contains("row0").contains("value39");
    }

    @Test
    @DisplayName("null/빈 입력은 건너뛰고 heading 레벨 클램프를 적용한다")
    void nullAndEmptyBlocksAreSkipped() {
        byte[] pdf = PdfUtil.builder()
                .heading(null, 5)          // null 제목 + 레벨 클램프(>3) 분기
                .paragraph(null)            // 단일 null 문단 건너뜀
                .paragraphs(null)           // 리스트 null 건너뜀
                .bullet(null)               // bullet null 건너뜀
                .numbered(null)             // numbered null 건너뜀
                .table((List<List<String>>) null) // 2차원 테이블 null 건너뜀
                .table(List.of())           // 빈 테이블 건너뜀
                .codeBlock(null)            // 코드블록 null 건너뜀
                .build();

        assertThat(pdf).isNotEmpty();
        // heading null이므로 본문 텍스트는 비어 있을 수 있지만 PDF는 생성되어야 한다.
    }

    @Test
    @DisplayName("stringWidth 공개 헬퍼가 null과 정상 텍스트를 처리한다")
    void stringWidthPublic() {
        float nullFont = PdfUtil.stringWidth(null, 12, "abc");
        float nullText = PdfUtil.stringWidth(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                12, null);
        float normal = PdfUtil.stringWidth(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                10, "width");
        float illegal = PdfUtil.stringWidth(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                10, "\uD83D\uDE00"); // unsupported glyph -> IllegalArgumentException catch

        assertThat(nullFont).isGreaterThan(0);
        assertThat(nullText).isZero();
        assertThat(normal).isGreaterThan(0);
        assertThat(illegal).isGreaterThan(0);
    }
}
