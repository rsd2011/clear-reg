package com.example.dw.application.export.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * 경량 Excel 입출력 유틸. Apache POI(XSSF) 기반.
 * - stream(): 헤더 행을 키로 사용해 DTO 매핑
 * - writeResource(): 컬럼 설정과 데이터로 XLSX Resource 생성
 */
@UtilityClass
public class ExcelUtil {

    public <T> Stream<T> stream(InputStream in, Function<RowContext, T> mapper) {
        return stream(in, 0, mapper);
    }

    public <T> Stream<T> stream(InputStream in, int sheetIndex, Function<RowContext, T> mapper) {
        try {
            Workbook wb = new XSSFWorkbook(in);
            Sheet sheet = wb.getNumberOfSheets() > sheetIndex ? wb.getSheetAt(sheetIndex) : wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) {
                wb.close();
                return Stream.empty();
            }
            Row header = rows.next();
            Map<String, Integer> headerIndex = new LinkedHashMap<>();
            header.forEach(c -> headerIndex.put(getCellValue(c).toString(), c.getColumnIndex()));

            Iterator<T> dtoIter = new Iterator<>() {
                @Override public boolean hasNext() { return rows.hasNext(); }
                @Override public T next() {
                    Row row = rows.next();
                    Map<Integer, Object> rowValues = new LinkedHashMap<>();
                    row.forEach(c -> rowValues.put(c.getColumnIndex(), getCellValue(c)));
                    RowContext ctx = new RowContext(row.getRowNum(), rowValues, headerIndex);
                    return mapper.apply(ctx);
                }
            };
            Spliterator<T> spl = Spliterators.spliteratorUnknownSize(dtoIter, Spliterator.ORDERED);
            return StreamSupport.stream(spl, false).onClose(() -> closeQuietly(wb));
        } catch (IOException e) {
            throw new UncheckedIOException("excel stream read error", e);
        }
    }

    public <T> Resource writeResource(List<ColumnConfig<T>> columns, Iterable<T> data) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sheet1");
            applyHeaderRow(wb, sheet, columns);
            int rowIdx = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int col = 0; col < columns.size(); col++) {
                    Object val = columns.get(col).getExtractor().apply(item);
                    if (val == null) continue;
                    Cell cell = row.createCell(col);
                    setCellValue(cell, val);
                }
            }
            autosize(sheet, columns.size());
            wb.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("excel write error", e);
        }
    }

    private void applyHeaderRow(Workbook wb, Sheet sheet, List<? extends ColumnConfig<?>> columns) {
        Row header = sheet.createRow(0);
        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int col = 0; col < columns.size(); col++) {
            Cell cell = header.createCell(col, CellType.STRING);
            cell.setCellValue(columns.get(col).getHeader());
            cell.setCellStyle(style);
        }
    }

    private void autosize(Sheet sheet, int cols) {
        for (int c = 0; c < cols; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            case BLANK -> "";
            default -> cell.toString();
        };
    }

    private void setCellValue(Cell cell, Object val) {
        if (val instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (val instanceof Boolean b) {
            cell.setCellValue(b);
        } else {
            cell.setCellValue(val.toString());
        }
    }

    private void closeQuietly(Workbook wb) {
        try { wb.close(); } catch (IOException ignored) { }
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RowContext {
        int rowIndex;
        Map<Integer, Object> rowValues;
        Map<String, Integer> headerIndex;

        public Object get(String header) {
            Integer idx = headerIndex.get(header);
            return idx == null ? null : rowValues.get(idx);
        }
    }

    @Builder
    @Value
    public static class ColumnConfig<T> {
        String header;
        Function<T, ?> extractor;
    }

    public static Workbook toWorkbook(Resource resource) throws IOException {
        try (InputStream in = new ByteArrayInputStream(resource.getInputStream().readAllBytes())) {
            return new XSSFWorkbook(in);
        }
    }
}
