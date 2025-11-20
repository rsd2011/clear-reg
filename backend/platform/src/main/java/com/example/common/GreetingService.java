package com.example.common;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.common.cache.CacheNames;

/**
 * Simple greeting facade primarily used in smoke tests and health-check style endpoints.
 */
@Service
public class GreetingService {

    /**
     * Builds a cached greeting message, defaulting to {@code World} when the caller omits the name.
     *
     * @param name optional target name
     * @return greeting sentence
     */
    @Cacheable(cacheNames = CacheNames.GREETINGS, key = "#name?:'__default__'")
    public String greet(String name) {
        return "Hello, " + (name == null || name.isBlank() ? "World" : name) + "!";
    }
}
