package com.assignment.payment.discount;

import com.assignment.payment.domain.payment.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentMethodDiscountPolicy {

    boolean supports(PaymentMethod method);

    int discount(int amountAfterGradeDiscount);

    String getPolicyName();

    BigDecimal getDiscountRate();
}
