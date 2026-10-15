package com.rentamovil.booking.domain.exception;

/** Error de negocio. 409 CONFLICT con el código que indique cada subclase. */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}