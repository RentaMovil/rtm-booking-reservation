package com.rentamovil.booking.domain.exception;

/** Rango de fechas inválido. 400 BAD_REQUEST. */
public class InvalidDateRangeException extends RuntimeException {

    private final String code;

    public InvalidDateRangeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}