package com.assignment.payment.service;

import com.assignment.payment.discount.DiscountPolicy;
import com.assignment.payment.discount.PaymentMethodDiscountPolicy;
import com.assignment.payment.domain.history.DiscountType;
import com.assignment.payment.domain.history.PaymentDiscountHistory;
import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import com.assignment.payment.domain.payment.Payment;
import com.assignment.payment.domain.payment.PaymentMethod;
import com.assignment.payment.exception.AlreadyPaidException;
import com.assignment.payment.exception.UnsupportedGradeException;
import com.assignment.payment.repository.PaymentDiscountHistoryRepository;
import com.assignment.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final List<DiscountPolicy> discountPolicies;
    private final List<PaymentMethodDiscountPolicy> paymentMethodDiscountPolicies;
    private final PaymentRepository paymentRepository;
    private final PaymentDiscountHistoryRepository historyRepository;

    @Transactional
    public Payment pay(Order order, PaymentMethod paymentMethod) {
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new AlreadyPaidException(order.getId());
        }

        MemberGrade grade = order.getMember().getGrade();
        DiscountPolicy gradePolicy = findGradePolicy(grade);

        // 1단계: 등급 할인
        int gradeDiscount = gradePolicy.discount(order);
        int afterGradeDiscount = order.getOriginalPrice() - gradeDiscount;

        // 2단계: 결제 수단 추가 할인 (최종 금액 기준)
        int paymentMethodDiscount = paymentMethodDiscountPolicies.stream()
                .filter(p -> p.supports(paymentMethod))
                .mapToInt(p -> p.discount(afterGradeDiscount))
                .sum();

        int finalAmount = afterGradeDiscount - paymentMethodDiscount;

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .finalAmount(finalAmount)
                .paymentMethod(paymentMethod)
                .paidAt(LocalDateTime.now())
                .build());

        saveHistories(payment, grade, gradePolicy, gradeDiscount, paymentMethod, afterGradeDiscount, paymentMethodDiscount);

        return payment;
    }

    private void saveHistories(Payment payment, MemberGrade grade,
                                DiscountPolicy gradePolicy, int gradeDiscount,
                                PaymentMethod paymentMethod, int afterGradeDiscount,
                                int paymentMethodDiscount) {
        LocalDateTime now = payment.getPaidAt();
        List<PaymentDiscountHistory> histories = new ArrayList<>();

        histories.add(PaymentDiscountHistory.builder()
                .payment(payment)
                .appliedGrade(grade.name())
                .policyName(gradePolicy.getPolicyName())
                .discountRate(gradePolicy.getDiscountRate())
                .discountAmount(gradeDiscount)
                .discountType(DiscountType.GRADE)
                .appliedAt(now)
                .build());

        if (paymentMethodDiscount > 0) {
            paymentMethodDiscountPolicies.stream()
                    .filter(p -> p.supports(paymentMethod))
                    .forEach(p -> histories.add(PaymentDiscountHistory.builder()
                            .payment(payment)
                            .appliedGrade(grade.name())
                            .policyName(p.getPolicyName())
                            .discountRate(p.getDiscountRate())
                            .discountAmount(p.discount(afterGradeDiscount))
                            .discountType(DiscountType.PAYMENT_METHOD)
                            .appliedAt(now)
                            .build()));
        }

        historyRepository.saveAll(histories);
    }

    private DiscountPolicy findGradePolicy(MemberGrade grade) {
        return discountPolicies.stream()
                .filter(p -> p.supports(grade))
                .findFirst()
                .orElseThrow(() -> new UnsupportedGradeException(grade));
    }
}
