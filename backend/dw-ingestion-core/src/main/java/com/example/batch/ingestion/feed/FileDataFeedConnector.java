package com.example.batch.ingestion.feed;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.dw.dto.HrFileDescriptor;
import com.example.batch.ingestion.DwFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class FileDataFeedConnector implements DataFeedConnector {

    private final ConcurrentHashMap<String, HrFileDescriptor> descriptors = new ConcurrentHashMap<>();
    private final DwFileStorageService fileStorageService;

    @Override
    public Optional<DataFeed> nextFeed() {
        return fileStorageService.nextPendingFile()
                .flatMap(this::toFeed);
    }

    private Optional<DataFeed> toFeed(HrFileDescriptor descriptor) {
        try {
            String payload = fileStorageService.readPayload(descriptor);
            String feedId = UUID.randomUUID().toString();
            java.util.Map<String, String> attributes = new java.util.HashMap<>(descriptor.attributes());
            attributes.put("fileName", descriptor.fileName());
            DataFeed feed = new DataFeed(feedId,
                    descriptor.feedType(),
                    descriptor.businessDate(),
                    descriptor.sequenceNumber(),
                    payload,
                    "FILE",
                    java.util.Collections.unmodifiableMap(attributes));
            descriptors.put(feedId, descriptor);
            return Optional.of(feed);
        } catch (IOException ex) {
            log.error("Failed to read HR file {}", descriptor.path(), ex);
            fileStorageService.markProcessed(descriptor, false);
            return Optional.empty();
        }
    }

    @Override
    public void onSuccess(DataFeed feed) {
        HrFileDescriptor descriptor = descriptors.remove(feed.id());
        if (descriptor != null) {
            fileStorageService.markProcessed(descriptor, true);
        }
    }

    @Override
    public void onFailure(DataFeed feed, Exception exception) {
        HrFileDescriptor descriptor = descriptors.remove(feed.id());
        if (descriptor != null) {
            fileStorageService.markProcessed(descriptor, false);
        }
    }

    @Override
    public String name() {
        return "file";
    }
}
