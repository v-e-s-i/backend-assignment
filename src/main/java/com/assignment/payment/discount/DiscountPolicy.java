package com.assignment.payment.discount;

import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;

public interface DiscountPolicy {

    boolean supports(MemberGrade grade);

    int discount(Order order);
}
