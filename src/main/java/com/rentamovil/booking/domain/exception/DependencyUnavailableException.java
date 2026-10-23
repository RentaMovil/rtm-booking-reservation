package com.rentamovil.booking.domain.exception;

import lombok.Getter;

/** Otro servicio del que depende la operación no responde (iam para las claves del token). No es
 * culpa de quien llama: se traduce a 503. */
@Getter
public class DependencyUnavailableException extends RuntimeException {

    // IAM_UNAVAILABLE
    private final String code;

    public DependencyUnavailableException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
