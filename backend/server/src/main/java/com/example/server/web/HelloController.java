package com.example.server.web;

import com.example.common.GreetingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Greeting")
public class HelloController {

    private final GreetingService greetingService;

    public HelloController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/api/greeting")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Returns greeting text for authenticated user")
    public String greeting(@RequestParam(value = "name", required = false) String name) {
        return greetingService.greet(name);
    }
}
