package com.example.server.web.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.dw.application.port.DwHolidayPort;

public record DwHolidayResponse(UUID id,
                                LocalDate holidayDate,
                                String countryCode,
                                String localName,
                                String englishName,
                                boolean workingDay,
                                OffsetDateTime syncedAt) {

    public static DwHolidayResponse fromRecord(DwHolidayPort.DwHolidayRecord record) {
        return fromRecord(record, UnaryOperator.identity());
    }

    public static DwHolidayResponse fromRecord(DwHolidayPort.DwHolidayRecord record, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DwHolidayResponse(record.id(), record.holidayDate(), record.countryCode(),
                fn.apply(record.localName()), fn.apply(record.englishName()),
                record.workingDay(), record.syncedAt());
    }
}
