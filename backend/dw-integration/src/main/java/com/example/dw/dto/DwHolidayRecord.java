package com.example.dw.dto;

import java.time.LocalDate;

public record DwHolidayRecord(LocalDate date,
                              String countryCode,
                              String localName,
                              String englishName,
                              boolean workingDay) {
}
