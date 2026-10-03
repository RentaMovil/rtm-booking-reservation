package com.rentamovil.booking.domain.exception;

/** Token ausente, vencido, mal firmado o sin un claim obligatorio. 401. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}