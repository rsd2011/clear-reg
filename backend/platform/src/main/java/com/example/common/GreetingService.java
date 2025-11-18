package com.example.common;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    @Cacheable(cacheNames = "greetings", key = "#name?:'__default__'")
    public String greet(String name) {
        return "Hello, " + (name == null || name.isBlank() ? "World" : name) + "!";
    }
}
