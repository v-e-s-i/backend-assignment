package com.assignment.payment.discount;

import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import org.springframework.stereotype.Component;

@Component
public class VvipDiscountPolicy implements DiscountPolicy {

    private static final double DISCOUNT_RATE = 0.10;

    @Override
    public boolean supports(MemberGrade grade) {
        return grade == MemberGrade.VVIP;
    }

    @Override
    public int discount(Order order) {
        return (int) (order.getOriginalPrice() * DISCOUNT_RATE);
    }
}
