package com.example.batch.ingestion.feed;

import java.time.LocalDate;
import java.util.Map;

import com.example.dw.dto.DataFeedType;

public record DataFeed(String id,
                     DataFeedType feedType,
                     LocalDate businessDate,
                     int sequenceNumber,
                     String payload,
                     String source,
                     Map<String, String> attributes) {

    public String attribute(String key) {
        return attributes == null ? null : attributes.get(key);
    }
}
