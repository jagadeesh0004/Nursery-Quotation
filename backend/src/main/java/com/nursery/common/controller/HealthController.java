package com.nursery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @GetMapping("/health")
    public String health() {
        return "healthy";
    }
}