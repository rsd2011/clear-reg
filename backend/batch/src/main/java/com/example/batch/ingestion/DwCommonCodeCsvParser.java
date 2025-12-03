package com.example.batch.ingestion;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.dw.dto.DwCommonCodeRecord;

@Component
public class DwCommonCodeCsvParser {

    private static final int MIN_COLUMNS = 3;

    public List<DwCommonCodeRecord> parse(String payload) {
        List<DwCommonCodeRecord> records = new ArrayList<>();
        if (!StringUtils.hasText(payload)) {
            return records;
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(payload))) {
            String header = reader.readLine(); // skip header
            if (header == null) {
                return records;
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] columns = line.split(",", -1);
                if (columns.length < MIN_COLUMNS) {
                    continue;
                }
                String codeType = trim(columns[0]);
                String codeValue = trim(columns[1]);
                String codeName = trim(columns[2]);
                Integer displayOrder = columns.length > 3 ? parseInt(columns[3]) : 0;
                boolean active = columns.length > 4 ? parseBoolean(columns[4]) : true;
                String category = columns.length > 5 ? trim(columns[5]) : null;
                String description = columns.length > 6 ? trim(columns[6]) : null;
                String metadataJson = columns.length > 7 ? trim(columns[7]) : null;
                records.add(new DwCommonCodeRecord(codeType, codeValue, codeName, displayOrder, active, category,
                        description, metadataJson, lineNumber));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse DW common-code payload", ex);
        }
        return records;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static Integer parseInt(String value) {
        String trimmed = trim(value);
        if (!StringUtils.hasText(trimmed)) {
            return 0;
        }
        return Integer.parseInt(trimmed);
    }

    private static boolean parseBoolean(String value) {
        String trimmed = trim(value);
        if (!StringUtils.hasText(trimmed)) {
            return true;
        }
        return !"false".equalsIgnoreCase(trimmed);
    }
}
