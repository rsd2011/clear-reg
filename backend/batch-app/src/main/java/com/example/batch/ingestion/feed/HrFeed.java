package com.example.batch.ingestion.feed;

import java.time.LocalDate;
import java.util.Map;

import com.example.hr.dto.HrFeedType;

public record HrFeed(String id,
                     HrFeedType feedType,
                     LocalDate businessDate,
                     int sequenceNumber,
                     String payload,
                     String source,
                     Map<String, String> attributes) {

    public String attribute(String key) {
        return attributes == null ? null : attributes.get(key);
    }
}
