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
                .paragraph("This is a very long paragraph that should wrap across multiple lines because it exceeds the usual width of the page margin. It keeps going and going to trigger wrap logic.")
                .bullet(List.of("item1", "item2"))
                .numbered(List.of("num1", "num2"))
                .codeBlock("code line")
                .table(List.of(List.of("A", "B"), List.of("1", "2")))
                .hr()
                .spacer(6)
                .heading("Page2", 2)
                .pageBreak()
                .paragraph("After break")
                .build();

        try (var doc = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Title").contains("Paragraph line one").contains("item1").contains("num1")
                    .contains("A").contains("1").contains("code line").contains("After break");
        }
    }

    @Test
    @DisplayName("createDocument null title와 extractText를 함께 호출")
    void nullTitleAndExtract() {
        byte[] pdf = PdfUtil.createDocument(null, List.of("Only body"));
        String text = PdfUtil.extractText(pdf);
        assertThat(text).contains("Only body");
    }
    @Test
    @DisplayName("여러 페이지를 생성하여 newPage/ensureSpace 분기 커버")
    void multiPage() throws Exception {
        List<String> lines = java.util.stream.IntStream.range(0, 80)
                .mapToObj(i -> "line-" + i + " lorem ipsum dolor sit amet")
                .toList();
        byte[] pdf = PdfUtil.builder()
                .heading("Multi")
                .paragraphs(lines)
                .build();
        assertThat(pdf).isNotEmpty();
    }

    @Test
    @DisplayName("TableBuilder DSL로 제네릭 테이블을 작성한다")
    void tableBuilderDsl() {
        var rows = List.of(new Person("Alice", 30), new Person("Bob", 28));
        var columns = PdfUtil.<Person>table(Person.class)
                .column("name", Person::name, 100f)
                .column("age", p -> String.valueOf(p.age()), 60f)
                .columns();

        byte[] pdf = PdfUtil.builder()
                .heading("Table Test")
                .table(rows, columns)
                .build();

        String text = PdfUtil.extractText(pdf);
        assertThat(text).contains("Table Test", "Alice", "Bob", "age");
    }

    @Test
    @DisplayName("폭이 큰 테이블과 다량의 행을 렌더링해 scale<1 및 페이지 전환 분기를 커버한다")
    void largeScaledTable() {
        var columns = PdfUtil.<Person>table(Person.class)
                .column("name", Person::name, 400f)
                .column("age", p -> String.valueOf(p.age()), 400f)
                .columns();
        List<Person> rows = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> new Person("User" + i, 20 + (i % 5)))
                .toList();

        byte[] pdf = PdfUtil.builder()
                .heading("Large Table")
                .table(rows, columns)
                .build();

        String text = PdfUtil.extractText(pdf);
        assertThat(text).contains("Large Table", "User0", "User49");
    }

    @Test
    @DisplayName("제네릭 테이블의 빈/널/0폭/널셀 분기를 커버한다")
    void genericTableBranches() {
        var columnsDefault = PdfUtil.<Person>table(Person.class)
                .column("defaultWidth", Person::name) // 기본 width 메서드 분기
                .columns();
        var columnsZero = PdfUtil.<Person>table(Person.class)
                .column("nullable", p -> null, 0f)     // totalWidth=0, 셀 null 분기
                .columns();

        var builder = PdfUtil.builder();
        // rows null + columns null/empty 분기
        builder.table(null, columnsDefault)
                .table(List.of(new Person("Ignored", 1)), null)
                .table(List.of(), columnsDefault);

        // scale else-branch(totalWidth=0) + cell null + heading 레벨3 + 빈 문자열 래핑
        builder.heading("Level3", 3)
                .paragraph("") // wrap()의 current.isEmpty() 분기
                .table(List.of(new Person("Alice", 30)), columnsZero);

        // cs null 분기 커버 (먼저 닫은 뒤 null 세팅)
        try {
            var csField = PdfUtil.Builder.class.getDeclaredField("cs");
            csField.setAccessible(true);
            var cs = (org.apache.pdfbox.pdmodel.PDPageContentStream) csField.get(builder);
            if (cs != null) {
                cs.close();
            }
            csField.set(builder, null);
        } catch (Exception ignored) {
        }

        byte[] pdf = builder.build();
        assertThat(pdf).isNotEmpty();
    }

    private record Person(String name, int age) {}
}
