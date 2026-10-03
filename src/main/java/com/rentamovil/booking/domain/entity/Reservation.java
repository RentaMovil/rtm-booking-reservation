package com.rentamovil.booking.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.InvalidDateRangeException;
import com.rentamovil.booking.domain.exception.InvalidStatusTransitionException;

import lombok.Getter;

/**
 * Una reserva: el registro creado antes de recoger físicamente el vehículo.
 *
 * <p>Entidad de dominio pura — no usa Spring ni JPA. Las invariantes vienen de
 * 02-domain/entities-and-rules.md (INV-001..INV-016); las que la base impone (CHECK, FK, UNIQUE)
 * están duplicadas en rtm-booking-reservation-db a propósito: cada capa frena por su lado.
 */
@Getter
public class Reservation {

    private final Integer id;
    /** Titular de la reserva: una Person de iam, no necesariamente un User. */
    private final Integer clientId;
    private final Integer vehicleId;
    /** El seguro es opcional: null significa "sin seguro" (INV-006, como máximo uno). */
    private final Integer insuranceTypeId;
    private final Integer pickupBranchId;
    private final Integer returnBranchId;
    private final LocalDateTime reservationDate;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final BigDecimal vehicleSubtotal;
    private final BigDecimal insuranceSubtotal;
    private final ReservationStatus status;

    private Reservation(Integer id, Integer clientId, Integer vehicleId, Integer insuranceTypeId,
                        Integer pickupBranchId, Integer returnBranchId, LocalDateTime reservationDate,
                        LocalDateTime startDate, LocalDateTime endDate, BigDecimal vehicleSubtotal,
                        BigDecimal insuranceSubtotal, ReservationStatus status) {
        this.id = id;
        this.clientId = clientId;
        this.vehicleId = vehicleId;
        this.insuranceTypeId = insuranceTypeId;
        this.pickupBranchId = pickupBranchId;
        this.returnBranchId = returnBranchId;
        this.reservationDate = reservationDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.vehicleSubtotal = vehicleSubtotal;
        this.insuranceSubtotal = insuranceSubtotal;
        this.status = status;
    }

    /**
     * Crea una reserva nueva. Siempre arranca en PENDING_PAYMENT: la confirmación la produce un
     * Administrador al aprobar el pago (INV-009), nunca el cliente al crearla.
     */
    public static Reservation create(Integer clientId, Integer vehicleId, Integer insuranceTypeId,
                                     Integer pickupBranchId, Integer returnBranchId,
                                     LocalDateTime startDate, LocalDateTime endDate,
                                     BigDecimal vehicleSubtotal, BigDecimal insuranceSubtotal) {
        validateDates(startDate, endDate);

        BigDecimal insurance = insuranceSubtotal == null ? BigDecimal.ZERO : insuranceSubtotal;
        if (vehicleSubtotal == null || vehicleSubtotal.signum() < 0) {
            throw new BusinessRuleException("INVALID_AMOUNTS", "El subtotal del vehículo no puede ser negativo");
        }
        if (insurance.signum() < 0) {
            throw new BusinessRuleException("INVALID_AMOUNTS", "El subtotal del seguro no puede ser negativo");
        }

        return new Reservation(null, clientId, vehicleId, insuranceTypeId, pickupBranchId,
                returnBranchId, LocalDateTime.now(), startDate, endDate, vehicleSubtotal, insurance,
                ReservationStatus.PENDING_PAYMENT);
    }

    /**
     * Reconstruye una reserva ya persistida. No revalida: la base ya garantizando que estos
     * datos son válidos, y las operaciones de negocio validan antes de escribir.
     */
    public static Reservation restore(Integer id, Integer clientId, Integer vehicleId, Integer insuranceTypeId,
                                      Integer pickupBranchId, Integer returnBranchId,
                                      LocalDateTime reservationDate, LocalDateTime startDate,
                                      LocalDateTime endDate, BigDecimal vehicleSubtotal,
                                      BigDecimal insuranceSubtotal, ReservationStatus status) {
        return new Reservation(id, clientId, vehicleId, insuranceTypeId, pickupBranchId, returnBranchId,
                reservationDate, startDate, endDate, vehicleSubtotal, insuranceSubtotal, status);
    }

    /** INV-008: el total es siempre la suma de los dos subtotales. Nunca se envía por separado. */
    public BigDecimal getTotalAmount() {
        return vehicleSubtotal.add(insuranceSubtotal);
    }

    /**
     * INV-003: end_date solo se puede ampliar, nunca acortar. La fecha de recogida y el vehículo
     * tampoco cambian (INV-002).
     */
    public Reservation extendEndDate(LocalDateTime newEndDate) {
        validateDates(this.startDate, newEndDate);
        if (!newEndDate.isAfter(this.endDate)) {
            throw new InvalidDateRangeException("INVALID_DATE_RANGE",
                    "La fecha de devolución solo puede ampliarse, no acortarse");
        }
        return with(newEndDate, this.returnBranchId);
    }

    /**
     * INV-013: la sucursal de devolución se cambia hasta 3 días antes de end_date — la misma
     * ventana que la cancelación (INV-004). El plazo se evalúa en la aplicación porque un CHECK
     * de SQL Server no puede confiar en GETDATE().
     */
    public Reservation changeReturnBranch(Integer newReturnBranchId, LocalDateTime now) {
        LocalDateTime deadline = this.endDate.minusDays(3);
        if (now.isAfter(deadline)) {
            throw new BusinessRuleException("RETURN_BRANCH_LOCKED",
                    "La sucursal de devolución ya no se puede cambiar: el plazo límite era 3 días antes");
        }
        return with(this.endDate, newReturnBranchId);
    }

    /** Transiciones permitidas (tabla de estados de entities-and-rules.md). */
    public boolean canTransitionTo(ReservationStatus target) {
        return switch (this.status) {
            case PENDING_PAYMENT -> target == ReservationStatus.PENDING_REVIEW
                    || target == ReservationStatus.CANCELLED;
            case PENDING_REVIEW -> target == ReservationStatus.CONFIRMED
                    || target == ReservationStatus.PENDING_PAYMENT
                    || target == ReservationStatus.CANCELLED;
            case CONFIRMED -> target == ReservationStatus.COMPLETED
                    || target == ReservationStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }

    /**
     * Cambia de estado validando la transición (409 si no está permitida).
     *
     * <p>Idempotente a propósito: aplicar el mismo evento dos veces no falla, porque varias
     * reacciones a eventos son "deja el estado en X" y el evento puede llegar dos veces
     * (garantía at-least-once, ver events.md).
     */
    public Reservation transitionTo(ReservationStatus target) {
        if (this.status == target) {
            return this;
        }
        if (!canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "No se puede pasar de " + status + " a " + target);
        }
        return withStatus(target);
    }

    private Reservation with(LocalDateTime newEndDate, Integer newReturnBranchId) {
        return new Reservation(id, clientId, vehicleId, insuranceTypeId, pickupBranchId,
                newReturnBranchId, reservationDate, startDate, newEndDate, vehicleSubtotal,
                insuranceSubtotal, status);
    }

    private Reservation withStatus(ReservationStatus newStatus) {
        return new Reservation(id, clientId, vehicleId, insuranceTypeId, pickupBranchId, returnBranchId,
                reservationDate, startDate, endDate, vehicleSubtotal, insuranceSubtotal, newStatus);
    }

    private static void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidDateRangeException("INVALID_DATE_RANGE", "Las fechas son obligatorias");
        }
        if (!endDate.isAfter(startDate)) {
            throw new InvalidDateRangeException("INVALID_DATE_RANGE",
                    "La fecha de devolución debe ser posterior a la de recogida");
        }
    }
}