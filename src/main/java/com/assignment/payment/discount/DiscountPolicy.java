package com.assignment.payment.discount;

import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;

import java.math.BigDecimal;

public interface DiscountPolicy {

    boolean supports(MemberGrade grade);

    int discount(Order order);

    String getPolicyName();

    // null = 고정 금액 할인, non-null = 비율 할인
    BigDecimal getDiscountRate();
}
