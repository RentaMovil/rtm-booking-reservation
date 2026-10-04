package com.rentamovil.booking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.enums.ReservationStatus;
import com.rentamovil.booking.domain.exception.BusinessRuleException;
import com.rentamovil.booking.domain.exception.InvalidDateRangeException;
import com.rentamovil.booking.domain.exception.InvalidStatusTransitionException;

/**
 * Tests del dominio: sin Spring, sin base de datos (hexagonal-architecture.md, sección 8).
 * Verifican las invariantes de 02-domain/entities-and-rules.md.
 */
class ReservationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 5, 10, 0);

    private Reservation newReservation() {
        return Reservation.create(1, 301, null, 10, 11, START, END,
                new BigDecimal("120000.00"), BigDecimal.ZERO);
    }

    /** La reserva debe estar CONFIRMED para poder probar la mayoría de transiciones. */
    private Reservation confirmed() {
        return newReservation()
                .transitionTo(ReservationStatus.PENDING_REVIEW)
                .transitionTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Una reserva nueva nace en PENDING_PAYMENT, nunca CONFIRMED")
    void nuevaResanzaArrancaEnPENDINGPayment() {
        assertThat(newReservation().getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("INV-008: el total es siempre la suma de los dos subtotales")
    void totalEsLaSumaDeLosSubtotales() {
        Reservation conSeguro = Reservation.create(1, 301, 1, 10, 11, START, END,
                new BigDecimal("120000.00"), new BigDecimal("40000.00"));

        assertThat(conSeguro.getTotalAmount()).isEqualByComparingTo("160000.00");
    }

    @Test
    @DisplayName("El seguro ausente se guarda como 0, no como null")
    void seguroAusenteEsCero() {
        Reservation sinSeguro = Reservation.create(1, 301, null, 10, 11, START, END,
                new BigDecimal("120000.00"), null);

        assertThat(sinSeguro.getInsuranceSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("HU-BOOKING-003 escenario 2: la devolución debe ser posterior a la recogida")
    void fechaDeDevolucionInvalida() {
        assertThatThrownBy(() -> Reservation.create(1, 301, null, 10, 11, START, START,
                BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("Una fecha de devolución anterior a la de recogida también se rechaza")
    void fechaDeDevolucionAnteriorALaRecogida() {
        assertThatThrownBy(() -> Reservation.create(1, 301, null, 10, 11, START,
                START.minusDays(1), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    @DisplayName("INV-010: una sucursal de devolución distinta de la de recogida es válida")
    void sucursalDeDevolucionDistintaEsValida() {
        // pickup=10, return=99: sin recargo y sin error.
        Reservation reserva = Reservation.create(1, 301, null, 10, 99, START, END,
                new BigDecimal("120000.00"), BigDecimal.ZERO);

        assertThat(reserva.getPickupBranchId()).isNotEqualTo(reserva.getReturnBranchId());
        assertThat(reserva.getTotalAmount()).isEqualByComparingTo("120000.00");
    }

    @Test
    @DisplayName("PENDING_PAYMENT puede pasar a PENDING_REVIEW")
    void pendingPaymentAPendingReview() {
        assertThat(newReservation().transitionTo(ReservationStatus.PENDING_REVIEW).getStatus())
                .isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("INV-009: una reserva no se CONFIRMA sin pasar por PENDING_REVIEW")
    void noSePuedeConfirmarSinRevision() {
        assertThatThrownBy(() -> newReservation().transitionTo(ReservationStatus.CONFIRMED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("CANCELLED y COMPLETED son estados terminales")
    void estadosTerminales() {
        Reservation cancelada = newReservation().transitionTo(ReservationStatus.CANCELLED);

        assertThatThrownBy(() -> cancelada.transitionTo(ReservationStatus.PENDING_PAYMENT))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("Aplicar la misma transición dos veces no falla: es idempotente")
    void transicionIdempotente() {
        Reservation enRevision = newReservation().transitionTo(ReservationStatus.PENDING_REVIEW);

        // at-least-once: el mismo PaymentReceiptUploaded puede llegar dos veces.
        Reservation otraVez = enRevision.transitionTo(ReservationStatus.PENDING_REVIEW);

        assertThat(otraVez.getStatus()).isEqualTo(ReservationStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("INV-016: un rechazo con RETRY_ALLOWED devuelve la reserva a PENDING_PAYMENT")
    void retryAllowedVuelveAPendingPayment() {
        Reservation enRevision = newReservation().transitionTo(ReservationStatus.PENDING_REVIEW);

        assertThat(enRevision.transitionTo(ReservationStatus.PENDING_PAYMENT).getStatus())
                .isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("Cambiar la sucursal de devolución no toca las fechas ni el precio")
    void cambiarSucursalNoTocaFechasNiPrecio() {
        Reservation reserva = confirmed();

        Reservation cambiada = reserva.changeReturnBranch(99, END.minusDays(10));

        assertThat(cambiada.getStartDate()).isEqualTo(START);
        assertThat(cambiada.getEndDate()).isEqualTo(END);
        assertThat(cambiada.getTotalAmount()).isEqualByComparingTo(reserva.getTotalAmount());
    }

    @Test
    @DisplayName("Una reserva cancelada o completada ya no cambia de sucursal")
    void reservaCerradaNoCambiaDeSucursal() {
        Reservation cancelada = newReservation().transitionTo(ReservationStatus.CANCELLED);
        Reservation completada = confirmed().transitionTo(ReservationStatus.COMPLETED);

        assertThatThrownBy(() -> cancelada.changeReturnBranch(99, END.minusDays(10)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelada");
        assertThatThrownBy(() -> completada.changeReturnBranch(99, END.minusDays(10)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completada");
    }

    @Test
    @DisplayName("INV-013: la sucursal de devolución se cambia hasta 3 días antes de end_date")
    void sucursalSeCambiaDentroDelPlazo() {
        Reservation reserva = confirmed();
        LocalDateTime conTiempo = END.minusDays(10);

        assertThat(reserva.changeReturnBranch(99, conTiempo).getReturnBranchId()).isEqualTo(99);
    }

    @Test
    @DisplayName("INV-013: pasado el plazo la sucursal de devolución ya no se puede cambiar")
    void sucursalBloqueadaPasadoElPlazo() {
        Reservation reserva = confirmed();
        LocalDateTime despuesDelPlazo = END.minusDays(2);

        assertThatThrownBy(() -> reserva.changeReturnBranch(99, despuesDelPlazo))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 días");
    }

    @Test
    @DisplayName("Un subtotal negativo se rechaza")
    void subtotalNegativoSeRechaza() {
        assertThatThrownBy(() -> Reservation.create(1, 301, null, 10, 11, START, END,
                new BigDecimal("-1.00"), BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }
}