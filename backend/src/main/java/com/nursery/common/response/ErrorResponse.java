package com.nursery.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Structured error response for API failures
 *
 * Example:
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "errors": ["Price must be greater than 0", "Quantity must be positive"],
 *   "timestamp": "2024-04-12T10:30:00Z"
 * }
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private String message;
    private List<String> errors;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public static ErrorResponse of(String message) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static ErrorResponse of(String message, List<String> errors) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
}

