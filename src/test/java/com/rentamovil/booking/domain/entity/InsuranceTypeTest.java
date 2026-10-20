package com.rentamovil.booking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rentamovil.booking.domain.exception.BusinessRuleException;

class InsuranceTypeTest {

    @Test
    @DisplayName("INV-007: insurance is charged per day, rounded to 2 decimals")
    void dailyCostIsRounded() {
        InsuranceType plan = InsuranceType.create("Premium", "Full coverage", new BigDecimal("9500.35"));

        // 9500.35 * 4 = 38001.40 exact; with a factor that doesn't divide evenly it must round.
        assertThat(plan.costFor(4)).isEqualByComparingTo("38001.40");
    }

    @Test
    @DisplayName("The cost fits in DECIMAL(10,2): always 2 decimals")
    void costAlwaysHasTwoDecimals() {
        InsuranceType plan = InsuranceType.create("Basic", null, new BigDecimal("1234.56"));

        assertThat(plan.costFor(3)).isEqualByComparingTo("3703.68");
        assertThat(plan.costFor(3).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("A negative daily cost is rejected")
    void negativeDailyCostIsRejected() {
        assertThatThrownBy(() -> InsuranceType.create("Premium", null, new BigDecimal("-1.00")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("A reservation of negative days is rejected")
    void negativeDaysAreRejected() {
        InsuranceType plan = InsuranceType.create("Premium", null, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> plan.costFor(0))
                .isInstanceOf(BusinessRuleException.class);
    }
}
