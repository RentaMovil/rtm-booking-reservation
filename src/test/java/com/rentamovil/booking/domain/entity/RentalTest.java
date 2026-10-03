package com.rentamovil.booking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.enums.RentalStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;

class RentalTest {

    private static final LocalDateTime PICKUP = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime DEVOLUCION = LocalDateTime.of(2026, 11, 5, 18, 30);

    private Rental enCurso() {
        return Rental.startPickup(1, 301, 55, PICKUP, new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("El pickup arranca una renta IN_PROGRESS, sin fecha ni kilometraje final")
    void pickupArrancaEnCurso() {
        Rental rental = enCurso();

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.IN_PROGRESS);
        assertThat(rental.getActualEndDate()).isNull();
        assertThat(rental.getFinalMileage()).isNull();
    }

    @Test
    @DisplayName("La devolución cierra la renta con fecha y kilometraje final")
    void devolucionCierraLaRenta() {
        Rental cerrada = enCurso().completeReturn(DEVOLUCION, new BigDecimal("15500.00"));

        assertThat(cerrada.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(cerrada.getActualEndDate()).isEqualTo(DEVOLUCION);
        assertThat(cerrada.getFinalMileage()).isEqualByComparingTo("15500.00");
    }

    @Test
    @DisplayName("El kilometraje final no puede ser menor que el inicial")
    void kilometrajeNoPuedeBajar() {
        Rental rental = enCurso();

        assertThatThrownBy(() -> rental.completeReturn(DEVOLUCION, new BigDecimal("14000.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("menor");
    }

    @Test
    @DisplayName("El kilometraje final es obligatorio al devolver")
    void kilometrajeFinalObligatorio() {
        assertThatThrownBy(() -> enCurso().completeReturn(DEVOLUCION, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Registrar dos devoluciones no cambia nada: es idempotente")
    void devolucionIdempotente() {
        Rental cerrada = enCurso().completeReturn(DEVOLUCION, new BigDecimal("15500.00"));

        Rental otraVez = cerrada.completeReturn(DEVOLUCION.plusHours(1), new BigDecimal("15600.00"));

        assertThat(otraVez.getActualEndDate()).isEqualTo(DEVOLUCION);
        assertThat(otraVez.getFinalMileage()).isEqualByComparingTo("15500.00");
    }

    @Test
    @DisplayName("El kilometraje inicial es obligatorio en el pickup")
    void kilometrajeInicialObligatorio() {
        assertThatThrownBy(() -> Rental.startPickup(1, 301, 55, PICKUP, null))
                .isInstanceOf(BusinessRuleException.class);
    }
}