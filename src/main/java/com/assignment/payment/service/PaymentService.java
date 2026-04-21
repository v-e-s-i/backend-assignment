package com.assignment.payment.service;

import com.assignment.payment.discount.DiscountPolicy;
import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import com.assignment.payment.domain.payment.Payment;
import com.assignment.payment.domain.payment.PaymentMethod;
import com.assignment.payment.exception.AlreadyPaidException;
import com.assignment.payment.exception.UnsupportedGradeException;
import com.assignment.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final List<DiscountPolicy> discountPolicies;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment pay(Order order, PaymentMethod paymentMethod) {
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new AlreadyPaidException(order.getId());
        }

        MemberGrade grade = order.getMember().getGrade();
        DiscountPolicy policy = findGradePolicy(grade);

        int discountAmount = policy.discount(order);
        int finalAmount = order.getOriginalPrice() - discountAmount;

        return paymentRepository.save(Payment.builder()
                .order(order)
                .finalAmount(finalAmount)
                .paymentMethod(paymentMethod)
                .paidAt(LocalDateTime.now())
                .build());
    }

    private DiscountPolicy findGradePolicy(MemberGrade grade) {
        return discountPolicies.stream()
                .filter(p -> p.supports(grade))
                .findFirst()
                .orElseThrow(() -> new UnsupportedGradeException(grade));
    }
}
