package com.rentamovil.booking.domain.exception;

/** El usuario autenticado no tiene el permiso requerido. 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}