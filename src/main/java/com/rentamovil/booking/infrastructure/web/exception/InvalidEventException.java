package com.rentamovil.booking.infrastructure.web.exception;

/**
 * Un evento recibido en /internal/events no tiene la estructura esperada. Responde 400.
 *
 * <p>Vive en infraestructura, no en el dominio: validar el formato de un mensaje entrante es un
 * asunto del adaptador web, no una regla de negocio.
 */
public class InvalidEventException extends RuntimeException {

    private final String code;
    /** Campo con el problema, por ejemplo "payload.reservationId". Null si no aplica a uno. */
    private final String field;

    public InvalidEventException(String code, String field, String message) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
