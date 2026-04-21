package com.assignment.payment.discount;

import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import org.springframework.stereotype.Component;

@Component
public class NormalDiscountPolicy implements DiscountPolicy {

    @Override
    public boolean supports(MemberGrade grade) {
        return grade == MemberGrade.NORMAL;
    }

    @Override
    public int discount(Order order) {
        return 0;
    }

    @Override
    public String getPolicyName() {
        return "NORMAL_NO_DISCOUNT";
    }

    @Override
    public java.math.BigDecimal getDiscountRate() {
        return java.math.BigDecimal.ZERO;
    }
}
