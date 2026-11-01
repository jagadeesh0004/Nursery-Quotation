package com.nursery.customer;

import com.nursery.common.response.ApiResponse;
import com.nursery.config.SecurityUtils;
import com.nursery.customer.dto.CustomerDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.list(SecurityUtils.getCurrentUserId(), search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.get(SecurityUtils.getCurrentUserId(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Customer updated", customerService.update(SecurityUtils.getCurrentUserId(), id, request)));
    }
}
