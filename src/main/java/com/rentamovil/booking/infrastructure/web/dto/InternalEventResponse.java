package com.rentamovil.booking.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de procesar un evento.
 *
 * <p>result = PROCESSED si cambió algo, o DUPLICATE_IGNORED si el evento ya estaba aplicado. Las
 * dos respuestas son 200: un evento repetido no es un error y quien lo reenvía no debe reintentar.
 */
@Getter
@AllArgsConstructor
public class InternalEventResponse {

    public static final String PROCESSED = "PROCESSED";
    public static final String DUPLICATE_IGNORED = "DUPLICATE_IGNORED";

    private String type;
    private String eventId;
    private String result;
}
