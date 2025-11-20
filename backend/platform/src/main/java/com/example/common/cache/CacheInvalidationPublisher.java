package com.example.common.cache;

public interface CacheInvalidationPublisher {

    void publish(CacheInvalidationEvent event);
}
