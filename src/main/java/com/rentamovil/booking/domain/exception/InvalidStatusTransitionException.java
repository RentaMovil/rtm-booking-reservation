package com.rentamovil.booking.domain.exception;

/**
 * Transición de estado no permitida (por ejemplo CONFIRMED → PENDING_PAYMENT). 409 CONFLICT.
 * Nota: aplicar la misma transición dos veces NO lanza esto — es idempotente a propósito.
 */
public class InvalidStatusTransitionException extends BusinessRuleException {

    public InvalidStatusTransitionException(String message) {
        super("INVALID_STATUS_TRANSITION", message);
    }
}