package com.example.dw.application.export.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import lombok.experimental.UtilityClass;

/**
 * PDFBox 기반 간단 PDF 생성 유틸.
 * - 제목, 단락, 단순 테이블 지원
 */
@UtilityClass
public class PdfUtil {

    public byte[] createDocument(String title, List<String> paragraphs) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - 50;
                if (title != null) {
                    y = writeText(cs, title, 20, 50, y, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), true);
                    y -= 10;
                }
                for (String p : paragraphs) {
                    y = writeText(cs, p, 14, 50, y, new PDType1Font(Standard14Fonts.FontName.HELVETICA), false) - 6;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("pdf build failed", e);
        }
    }

    public byte[] createTable(String title, List<List<String>> rows) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - 40;
                if (title != null) {
                    y = writeText(cs, title, 18, 50, y, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), true) - 10;
                }
                float xStart = 50;
                float colWidth = 120;
                float rowHeight = 18;
                for (List<String> row : rows) {
                    float x = xStart;
                    for (String cell : row) {
                        cs.addRect(x, y - rowHeight, colWidth, rowHeight);
                        cs.stroke();
                        writeText(cs, cell, 12, x + 4, y - 14, new PDType1Font(Standard14Fonts.FontName.HELVETICA), false);
                        x += colWidth;
                    }
                    y -= rowHeight;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("pdf table build failed", e);
        }
    }

    private float writeText(PDPageContentStream cs, String text, int size, float x, float y, PDType1Font font, boolean underline) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        if (underline) {
            float width = font.getStringWidth(text) / 1000 * size;
            cs.moveTo(x, y - 2);
            cs.lineTo(x + width, y - 2);
            cs.setRenderingMode(RenderingMode.FILL);
            cs.stroke();
        }
        return y - size - 2;
    }
}
