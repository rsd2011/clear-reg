package com.example.common.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import lombok.experimental.UtilityClass;
import lombok.SneakyThrows;

/**
 * PDFBox 기반 간단 Markdown 스타일 PDF 유틸.
 * 지원 블록: heading(h1~h3), paragraph, bullet/numbered list, code block, table, hr, spacer, pageBreak.
 * Builder 체이닝으로 문서를 쌓은 뒤 build()로 byte[] 반환.
 */
@UtilityClass
public class PdfUtil {

    public byte[] createDocument(String title, List<String> paragraphs) {
        return builder().heading(title).paragraphs(paragraphs).build();
    }

    public byte[] createTable(String title, List<List<String>> rows) {
        return builder().heading(title).table(rows).build();
    }

    /** 단순 bullet 문서를 생성한다. */
    public byte[] createBullets(String title, List<String> bullets) {
        return builder().heading(title).bullet(bullets).build();
    }

    /** 코드 블록이 포함된 문서를 생성한다. */
    public byte[] createCodeBlock(String title, String code) {
        return builder().heading(title).codeBlock(code).build();
    }

    /** 텍스트 폭을 계산하는 공개 헬퍼(테스트/유틸 용). */
    static float stringWidth(PDType1Font font, int size, String text) {
        if (font == null || text == null) {
            return text == null ? 0 : text.length() * size * 0.6f;
        }
        try {
            return font.getStringWidth(text) / 1000 * size;
        } catch (IOException | IllegalArgumentException e) {
            return text.length() * size * 0.6f;
        }
    }

    /** Markdown-like Builder */
    public Builder builder() {
        return new Builder();
    }

    /** 제네릭 테이블 DSL 진입점 */
    public <T> TableBuilder<T> table(Class<T> type) {
        return new TableBuilder<>();
    }

    public record TableColumn<T>(String header,
                                 Function<T, String> extractor,
                                 float width) {
    }

    public static class TableBuilder<T> {
        private final List<TableColumn<T>> columns = new ArrayList<>();

        public TableBuilder<T> column(String header, Function<T, ?> extractor, float width) {
            columns.add(new TableColumn<>(header, t -> {
                Object v = extractor.apply(t);
                return v == null ? "" : v.toString();
            }, width));
            return this;
        }

        public TableBuilder<T> column(String header, Function<T, ?> extractor) {
            return column(header, extractor, 120f);
        }

        public List<TableColumn<T>> columns() {
            return List.copyOf(columns);
        }
    }

    public static class Builder {
        private final PDDocument doc = new PDDocument();
        private PDPageContentStream cs;
        private PDPage page;
        private float y;
        private final float margin = 50f;
        private final PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final PDType1Font monoFont = new PDType1Font(Standard14Fonts.FontName.COURIER);

        public Builder() {
            newPage();
        }

        public Builder heading(String text) { return heading(text, 1); }

        public Builder heading(String text, int level) {
            if (text == null) return this;
            int size = switch (Math.max(1, Math.min(level, 3))) {
                case 1 -> 18; case 2 -> 16; default -> 14;
            };
            float spacing = 8;
            writeWrapped(text, boldFont, size, true, spacing);
            return this;
        }

        public Builder paragraph(String text) {
            if (text == null) return this;
            writeWrapped(text, bodyFont, 12, false, 6);
            return this;
        }

        public Builder paragraphs(List<String> lines) {
            if (lines != null) lines.forEach(this::paragraph);
            return this;
        }

        public Builder bullet(List<String> items) {
            if (items == null) return this;
            for (String item : items) {
                writeWrapped("• " + item, bodyFont, 12, false, 4);
            }
            y -= 4;
            return this;
        }

        public Builder numbered(List<String> items) {
            if (items == null) return this;
            int idx = 1;
            for (String item : items) {
                writeWrapped(idx++ + ". " + item, bodyFont, 12, false, 4);
            }
            y -= 4;
            return this;
        }

        public Builder codeBlock(String text) {
            if (text == null) return this;
            float padding = 4;
            String[] lines = text.split("\n");
            for (String line : lines) {
                writeWrapped(line, monoFont, 11, false, 2, margin + padding, PDRectangle.A4.getWidth() - 2*margin - padding);
            }
            y -= 6;
            return this;
        }

        public Builder table(List<List<String>> rows) {
            if (rows == null || rows.isEmpty()) return this;
            float xStart = margin;
            float colWidth = 120;
            float rowHeight = 18;
            for (List<String> row : rows) {
                ensureSpace(rowHeight + 4);
                float x = xStart;
                for (String cell : row) {
                    rect(x, y - rowHeight, colWidth, rowHeight);
                    writeText(cell, bodyFont, 12, x + 4, y - 14, false);
                    x += colWidth;
                }
                y -= rowHeight;
            }
            y -= 6;
            return this;
        }

        /**
            * 제네릭 DTO 리스트를 테이블로 렌더링한다.
            * 사용 예:
            * <pre>
            * var table = PdfUtil.table(Person.class)
            *     .column("이름", Person::name, 120)
            *     .column("나이", p -> String.valueOf(p.age()), 80);
            * pdfBuilder.table(people, table.columns());
            * </pre>
            */
        public <T> Builder table(List<T> rows, List<TableColumn<T>> columns) {
            if (rows == null || rows.isEmpty() || columns == null || columns.isEmpty()) {
                return this;
            }
            float totalWidth = columns.stream()
                    .map(TableColumn::width)
                    .reduce(0f, Float::sum);
            float available = PDRectangle.A4.getWidth() - 2 * margin;
            float scale = totalWidth > 0 ? Math.min(1f, available / totalWidth) : 1f;
            float rowHeight = 18;

            // 헤더
            ensureSpace(rowHeight + 6);
            float x = margin;
            for (TableColumn<T> col : columns) {
                float w = col.width() * scale;
                rect(x, y - rowHeight, w, rowHeight);
                writeText(col.header(), boldFont, 12, x + 4, y - 14, false);
                x += w;
            }
            y -= rowHeight;

            // 데이터
            for (T row : rows) {
                ensureSpace(rowHeight + 4);
                x = margin;
                for (TableColumn<T> col : columns) {
                    float w = col.width() * scale;
                    rect(x, y - rowHeight, w, rowHeight);
                    String cell = col.extractor().apply(row);
                    writeText(cell == null ? "" : cell, bodyFont, 12, x + 4, y - 14, false);
                    x += w;
                }
                y -= rowHeight;
            }
            y -= 6;
            return this;
        }

        public Builder hr() {
            ensureSpace(4);
            line(margin, y, PDRectangle.A4.getWidth() - margin, y);
            y -= 8;
            return this;
        }

        public Builder spacer(float points) {
            y -= points;
            ensureSpace(0);
            return this;
        }

        public Builder pageBreak() {
            newPage();
            return this;
        }

        @SneakyThrows public byte[] build() {
            if (cs != null) cs.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            doc.close();
            return out.toByteArray();
        }

        private void writeWrapped(String text, PDType1Font font, int size, boolean underline, float spacing) {
            writeWrapped(text, font, size, underline, spacing, margin, PDRectangle.A4.getWidth() - 2*margin);
        }

        private void writeWrapped(String text, PDType1Font font, int size, boolean underline, float spacing, float x, float width) {
            List<String> lines = wrap(text, font, size, width);
            for (String line : lines) {
                ensureSpace(size + spacing + 2);
                writeText(line, font, size, x, y, underline);
                y -= (size + 2);
            }
            y -= spacing;
        }

        private List<String> wrap(String text, PDType1Font font, int size, float width) {
            String[] words = text.split(" ");
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String w : words) {
                String candidate = current.isEmpty() ? w : current + " " + w;
                if (PdfUtil.stringWidth(font, size, candidate) > width && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(w);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
            return lines;
        }

        @SneakyThrows private void writeText(String text, PDType1Font font, int size, float x, float y, boolean underline) {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
            if (underline) {
                float width = PdfUtil.stringWidth(font, size, text);
                cs.moveTo(x, y - 2);
                cs.lineTo(x + width, y - 2);
                cs.setRenderingMode(RenderingMode.FILL);
                cs.stroke();
            }
        }

        @SneakyThrows private void rect(float x, float y, float w, float h) {
            cs.addRect(x, y, w, h);
            cs.stroke();
        }

        @SneakyThrows private void line(float x1, float y1, float x2, float y2) {
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private void ensureSpace(float needed) {
            if (y - needed < margin) {
                newPage();
            }
        }

        @SneakyThrows private void newPage() {
            if (cs != null) cs.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - margin;
        }
    }

    /** 테스트 편의: PDF 텍스트 추출 */
    @SneakyThrows public String extractText(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
