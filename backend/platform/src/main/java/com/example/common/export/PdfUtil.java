package com.example.common.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    /** Markdown-like Builder */
    public Builder builder() {
        return new Builder();
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

        public byte[] build() {
            try {
                if (cs != null) cs.close();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);
                doc.close();
                return out.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("pdf build failed", e);
            }
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
                if (stringWidth(font, size, candidate) > width && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(w);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
            return lines;
        }

        private float stringWidth(PDType1Font font, int size, String text) {
            try {
                return font.getStringWidth(text) / 1000 * size;
            } catch (IOException e) {
                return text.length() * size * 0.6f;
            }
        }

        private void writeText(String text, PDType1Font font, int size, float x, float y, boolean underline) {
            try {
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(x, y);
                cs.showText(text);
                cs.endText();
                if (underline) {
                    float width = stringWidth(font, size, text);
                    cs.moveTo(x, y - 2);
                    cs.lineTo(x + width, y - 2);
                    cs.setRenderingMode(RenderingMode.FILL);
                    cs.stroke();
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private void rect(float x, float y, float w, float h) {
            try {
                cs.addRect(x, y, w, h);
                cs.stroke();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private void line(float x1, float y1, float x2, float y2) {
            try {
                cs.moveTo(x1, y1);
                cs.lineTo(x2, y2);
                cs.stroke();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private void ensureSpace(float needed) {
            if (y - needed < margin) {
                newPage();
            }
        }

        private void newPage() {
            try {
                if (cs != null) cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = page.getMediaBox().getHeight() - margin;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** 테스트 편의: PDF 텍스트 추출 */
    public String extractText(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
