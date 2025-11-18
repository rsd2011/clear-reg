package com.example.batch.ingestion;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.hr.dto.HrOrganizationRecord;

@Component
public class HrOrganizationCsvParser {

    private static final int EXPECTED_COLUMNS = 6;

    public List<HrOrganizationRecord> parse(String payload) {
        List<HrOrganizationRecord> records = new ArrayList<>();
        if (!StringUtils.hasText(payload)) {
            return records;
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(payload))) {
            String header = reader.readLine();
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
                if (columns.length < EXPECTED_COLUMNS) {
                    continue;
                }
                HrOrganizationRecord record = new HrOrganizationRecord(trim(columns[0]),
                        trim(columns[1]),
                        trim(columns[2]),
                        trim(columns[3]),
                        parseDate(columns[4]),
                        parseDate(columns[5]),
                        line,
                        lineNumber);
                records.add(record);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse organization payload", ex);
        }
        return records;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static LocalDate parseDate(String value) {
        String trimmed = trim(value);
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        return LocalDate.parse(trimmed);
    }
}
