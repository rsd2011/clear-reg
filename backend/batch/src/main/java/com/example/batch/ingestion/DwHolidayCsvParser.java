package com.example.batch.ingestion;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.example.dw.dto.DwHolidayRecord;

@Component
public class DwHolidayCsvParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public List<DwHolidayRecord> parse(String payload) {
        List<DwHolidayRecord> records = new ArrayList<>();
        if (payload == null || payload.isBlank()) {
            return records;
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(payload))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (!headerSkipped && looksLikeHeader(line)) {
                    headerSkipped = true;
                    continue;
                }
                String[] columns = line.split(",");
                if (columns.length < 3) {
                    continue;
                }
                LocalDate date = LocalDate.parse(columns[0].trim(), DATE_FORMAT);
                String country = columns[1].trim().toUpperCase(Locale.ROOT);
                String localName = columns[2].trim();
                String englishName = columns.length > 3 ? columns[3].trim() : null;
                boolean workingDay = columns.length > 4 ? Boolean.parseBoolean(columns[4].trim()) : false;
                records.add(new DwHolidayRecord(date, country, localName, englishName, workingDay));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("휴일 CSV를 파싱하지 못했습니다.", ex);
        }
        return records;
    }

    private boolean looksLikeHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("date") && lower.contains("country");
    }
}
