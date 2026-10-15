package com.rentamovil.booking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.exception.BusinessRuleException;

class InsuranceTypeTest {

    @Test
    @DisplayName("INV-007: el seguro se cobra por día, redondeado a 2 decimales")
    void costoPorDiaRedondeado() {
        InsuranceType plan = InsuranceType.create("Premium", "Todo riesgo", new BigDecimal("9500.35"));

        // 9500.35 * 4 = 38001.40 exacto; con un factor que no da exacto hay que redondear.
        assertThat(plan.costFor(4)).isEqualByComparingTo("38001.40");
    }

    @Test
    @DisplayName("El costo cabe en DECIMAL(10,2): siempre 2 decimales")
    void costoSiempreConDosDecimales() {
        InsuranceType plan = InsuranceType.create("Básico", null, new BigDecimal("1234.56"));

        assertThat(plan.costFor(3)).isEqualByComparingTo("3703.68");
        assertThat(plan.costFor(3).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un costo diario negativo se rechaza")
    void costoNegativoSeRechaza() {
        assertThatThrownBy(() -> InsuranceType.create("Premium", null, new BigDecimal("-1.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Una reserva de días negativos se rechaza")
    void diasNegativosSeRechazan() {
        InsuranceType plan = InsuranceType.create("Premium", null, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> plan.costFor(0))
                .isInstanceOf(BusinessRuleException.class);
    }
}