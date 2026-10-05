package com.rentamovil.booking.infrastructure.web.exception;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.ForbiddenException;
import com.rentamovil.booking.domain.exception.InvalidDateRangeException;
import com.rentamovil.booking.domain.exception.InvalidTokenException;
import com.rentamovil.booking.domain.exception.ResourceNotFoundException;
import com.rentamovil.booking.infrastructure.web.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

/** Convierte las excepciones en el formato de error de booking-reservation.yaml. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ErrorResponse("VALIDATION_ERROR", "Datos de entrada inválidos", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadJson() {
        return new ErrorResponse("VALIDATION_ERROR", "El cuerpo de la petición no es un JSON válido");
    }

    /** Un evento de /internal/events sin la estructura esperada (tipo desconocido, campo faltante). */
    @ExceptionHandler(InvalidEventException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidEvent(InvalidEventException e) {
        if (e.getField() == null) {
            return new ErrorResponse(e.getCode(), e.getMessage());
        }
        return new ErrorResponse(e.getCode(), "El evento no tiene la estructura esperada",
                List.of(new ErrorResponse.FieldError(e.getField(), e.getMessage())));
    }

    /** HU-BOOKING-003 escenario 2, e INV-003 al intentar acortar. */
    @ExceptionHandler(InvalidDateRangeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidDateRange(InvalidDateRangeException e) {
        return new ErrorResponse(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidToken(InvalidTokenException e) {
        return new ErrorResponse("INVALID_TOKEN", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException e) {
        return new ErrorResponse("FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException e) {
        return new ErrorResponse("NOT_FOUND", e.getMessage());
    }

    /** INV-004, INV-012, INV-013, reservas no modificables y las transiciones de estado inválidas. */
    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleBusinessRule(BusinessRuleException e) {
        return new ErrorResponse(e.getCode(), e.getMessage());
    }

    /** La base frena algo que la aplicación dejó pasar. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(DataIntegrityViolationException e) {
        String cause = String.valueOf(e.getMostSpecificCause().getMessage());
        log.error("Violación de integridad no esperada", e);
        if (cause.contains("ck_reservation_total")) {
            return new ErrorResponse("INVALID_AMOUNTS", "El total no coincide con los subtotales");
        }
        if (cause.contains("ck_reservation_status")) {
            return new ErrorResponse("INVALID_STATUS", "El estado de la reserva no es válido");
        }
        if (cause.contains("uq_rental_reservation_id")) {
            return new ErrorResponse("RENTAL_ALREADY_STARTED",
                    "Esta reserva ya tiene una renta registrada");
        }
        if (cause.contains("ck_rental_mileage")) {
            return new ErrorResponse("INVALID_MILEAGE", "El kilometraje no es válido");
        }
        return new ErrorResponse("CONFLICT", "El dato entra en conflicto con uno existente");
    }

    /** Errores propios de Spring que ya traen su código HTTP (405, 415, 404 de ruta). */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleSpringWebError(ErrorResponseException e) {
        return springWebError(e.getStatusCode());
    }

    /** El detalle solo va al log, nunca en la respuesta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            return springWebError(springError.getStatusCode());
        }
        log.error("Error no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Ocurrió un error inesperado"));
    }

    private ResponseEntity<ErrorResponse> springWebError(HttpStatusCode status) {
        String code = switch (status.value()) {
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "REQUEST_ERROR";
        };
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, "La petición no es válida para esta ruta"));
    }
}