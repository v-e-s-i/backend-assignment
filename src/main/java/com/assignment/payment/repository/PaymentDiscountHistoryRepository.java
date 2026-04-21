package com.assignment.payment.repository;

import com.assignment.payment.domain.history.PaymentDiscountHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentDiscountHistoryRepository extends JpaRepository<PaymentDiscountHistory, Long> {
    List<PaymentDiscountHistory> findByPaymentId(Long paymentId);
}
