package com.rentamovil.booking.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.rentamovil.booking.infrastructure.web.exception.InvalidEventException;

/**
 * Lee los datos de un evento con validación. Cada error dice el campo exacto
 * ("payload.reservationId") para que el servicio que envía sepa qué corregir.
 *
 * <p>Los números se aceptan como número JSON (12) o como texto numérico ("12"), pero no con
 * decimales (12.5) ni fuera del rango de un entero.
 */
public final class InternalEventPayload {

    private final Map<String, Object> values;

    public InternalEventPayload(Map<String, Object> values) {
        this.values = values == null ? Map.of() : values;
    }

    public Integer requiredInt(String name) {
        Integer value = optionalInt(name);
        if (value == null) {
            throw missing(name);
        }
        return value;
    }

    public Integer optionalInt(String name) {
        Object raw = values.get(name);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Integer number) {
            return number;
        }
        String text = raw instanceof Number || raw instanceof String ? raw.toString().trim() : null;
        if (text == null) {
            throw invalid(name, "debe ser un número entero");
        }
        try {
            return new BigDecimal(text).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw invalid(name, "debe ser un número entero");
        }
    }

    public String requiredText(String name) {
        String value = optionalText(name);
        if (value == null) {
            throw missing(name);
        }
        return value;
    }

    public String optionalText(String name) {
        Object raw = values.get(name);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String text)) {
            throw invalid(name, "debe ser texto");
        }
        return text.isBlank() ? null : text.trim();
    }

    private static InvalidEventException missing(String name) {
        return new InvalidEventException("INVALID_EVENT", "payload." + name, "obligatorio");
    }

    private static InvalidEventException invalid(String name, String message) {
        return new InvalidEventException("INVALID_EVENT", "payload." + name, message);
    }
}
