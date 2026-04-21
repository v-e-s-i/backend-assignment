package com.assignment.payment.discount;

import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import org.springframework.stereotype.Component;

@Component
public class VipDiscountPolicy implements DiscountPolicy {

    private static final int FIXED_DISCOUNT_AMOUNT = 1_000;

    @Override
    public boolean supports(MemberGrade grade) {
        return grade == MemberGrade.VIP;
    }

    @Override
    public int discount(Order order) {
        return FIXED_DISCOUNT_AMOUNT;
    }

    @Override
    public String getPolicyName() {
        return "VIP_FIXED_1000";
    }

    @Override
    public java.math.BigDecimal getDiscountRate() {
        return null; // 고정 금액 할인
    }
}
