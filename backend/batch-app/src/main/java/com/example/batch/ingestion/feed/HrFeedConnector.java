package com.example.batch.ingestion.feed;

import java.util.Optional;

public interface HrFeedConnector {

    Optional<HrFeed> nextFeed();

    default void onSuccess(HrFeed feed) {
        // no-op
    }

    default void onFailure(HrFeed feed, Exception exception) {
        // no-op
    }

    String name();
}
