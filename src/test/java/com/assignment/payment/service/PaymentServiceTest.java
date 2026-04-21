package com.assignment.payment.service;

import com.assignment.payment.domain.member.Member;
import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import com.assignment.payment.domain.payment.Payment;
import com.assignment.payment.domain.payment.PaymentMethod;
import com.assignment.payment.exception.AlreadyPaidException;
import com.assignment.payment.repository.MemberRepository;
import com.assignment.payment.repository.OrderRepository;
import com.assignment.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PaymentServiceTest {

    @Autowired PaymentService paymentService;
    @Autowired MemberRepository memberRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;

    @Test
    @DisplayName("NORMAL 회원 결제 시 할인 없음")
    void normalMemberPaysFullPrice() {
        Member member = memberRepository.save(member("일반회원", MemberGrade.NORMAL));
        Order order = orderRepository.save(order("노트북", 50_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        assertThat(payment.getFinalAmount()).isEqualTo(50_000);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(payment.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("VIP 회원 결제 시 1,000원 할인")
    void vipMemberGets1000Discount() {
        Member member = memberRepository.save(member("VIP회원", MemberGrade.VIP));
        Order order = orderRepository.save(order("키보드", 30_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        assertThat(payment.getFinalAmount()).isEqualTo(29_000);
    }

    @Test
    @DisplayName("VVIP 회원 결제 시 10% 할인")
    void vvipMemberGets10PercentDiscount() {
        Member member = memberRepository.save(member("VVIP회원", MemberGrade.VVIP));
        Order order = orderRepository.save(order("모니터", 100_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        assertThat(payment.getFinalAmount()).isEqualTo(90_000);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    @DisplayName("결제 완료 주문을 다시 결제하면 AlreadyPaidException")
    void duplicatePaymentThrowsException() {
        Member member = memberRepository.save(member("회원", MemberGrade.NORMAL));
        Order order = orderRepository.save(order("상품", 10_000, member));

        paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        assertThatThrownBy(() -> paymentService.pay(order, PaymentMethod.POINT))
                .isInstanceOf(AlreadyPaidException.class);
    }

    @Test
    @DisplayName("결제 저장 후 주문 정보, 금액, 결제수단, 일시 모두 기록됨")
    void paymentRecordsAllRequiredFields() {
        Member member = memberRepository.save(member("VIP회원", MemberGrade.VIP));
        Order order = orderRepository.save(order("마우스", 20_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        Payment saved = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(saved.getOrder().getId()).isEqualTo(order.getId());
        assertThat(saved.getFinalAmount()).isEqualTo(19_000);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(saved.getPaidAt()).isNotNull();
    }

    private Member member(String name, MemberGrade grade) {
        return Member.builder().name(name).grade(grade).build();
    }

    private Order order(String productName, int price, Member member) {
        return Order.builder().productName(productName).originalPrice(price).member(member).build();
    }
}
