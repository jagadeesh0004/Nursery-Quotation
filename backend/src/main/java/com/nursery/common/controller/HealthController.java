package com.nursery.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health check controller to keep the service warm and monitor status.
 */
@RestController
@RequestMapping("/ping")
public class HealthController {

    @GetMapping
    public String ping() {
        return "ok";
    }
}

