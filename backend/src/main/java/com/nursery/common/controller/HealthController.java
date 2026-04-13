package com.nursery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @RequestMapping(value = "/ping", method = RequestMethod.HEAD)
    public ResponseEntity<Void> pingHead() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public String health() {
        return "healthy";
    }
}