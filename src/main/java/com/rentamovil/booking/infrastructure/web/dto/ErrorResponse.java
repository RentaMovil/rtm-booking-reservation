package com.rentamovil.booking.infrastructure.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Formato estándar de error de todos los endpoints (07-api/contracts/openapi/_shared.yaml). */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    private String error;
    private String message;
    private List<FieldError> details;

    public ErrorResponse(String error, String message) {
        this(error, message, List.of());
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}