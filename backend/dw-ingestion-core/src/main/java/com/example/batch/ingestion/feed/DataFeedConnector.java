package com.example.batch.ingestion.feed;

import java.util.Optional;

public interface DataFeedConnector {

    Optional<DataFeed> nextFeed();

    default void onSuccess(DataFeed feed) {
        // no-op
    }

    default void onFailure(DataFeed feed, Exception exception) {
        // no-op
    }

    String name();
}
