package com.assignment.payment.discount;

import com.assignment.payment.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PointDiscountPolicy implements PaymentMethodDiscountPolicy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.POINT;
    }

    @Override
    public int discount(int amountAfterGradeDiscount) {
        return new BigDecimal(amountAfterGradeDiscount)
                .multiply(DISCOUNT_RATE)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    @Override
    public String getPolicyName() {
        return "POINT_EXTRA_5PCT";
    }

    @Override
    public BigDecimal getDiscountRate() {
        return DISCOUNT_RATE;
    }
}
