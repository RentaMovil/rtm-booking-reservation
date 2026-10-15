package com.rentamovil.booking.domain.exception;

/** El recurso no existe. 404 NOT_FOUND. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}