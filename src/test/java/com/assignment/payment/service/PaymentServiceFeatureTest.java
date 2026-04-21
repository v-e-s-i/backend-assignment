package com.assignment.payment.service;

import com.assignment.payment.domain.history.DiscountType;
import com.assignment.payment.domain.history.PaymentDiscountHistory;
import com.assignment.payment.domain.member.Member;
import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import com.assignment.payment.domain.payment.Payment;
import com.assignment.payment.domain.payment.PaymentMethod;
import com.assignment.payment.repository.MemberRepository;
import com.assignment.payment.repository.OrderRepository;
import com.assignment.payment.repository.PaymentDiscountHistoryRepository;
import com.assignment.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentServiceFeatureTest {

    @Autowired PaymentService paymentService;
    @Autowired MemberRepository memberRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentDiscountHistoryRepository historyRepository;

    // ── 중복 할인 테스트 ─────────────────────────────────────────────────

    @Test
    @DisplayName("NORMAL 회원이 포인트 결제 시 포인트 5% 추가 할인만 적용")
    void normalMemberPointPaymentAppliesOnlyPointDiscount() {
        Member member = save(member("일반", MemberGrade.NORMAL));
        Order order = save(order("상품", 10_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.POINT);

        // 등급 할인 0 → 10,000 → 포인트 5% = 500 → 최종 9,500
        assertThat(payment.getFinalAmount()).isEqualTo(9_500);
    }

    @Test
    @DisplayName("VIP 회원이 포인트 결제 시 등급 할인 후 포인트 5% 중복 적용")
    void vipMemberPointPaymentAppliesBothDiscounts() {
        Member member = save(member("VIP", MemberGrade.VIP));
        Order order = save(order("상품", 20_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.POINT);

        // VIP 1,000 할인 → 19,000 → 포인트 5% = 950 → 최종 18,050
        assertThat(payment.getFinalAmount()).isEqualTo(18_050);
    }

    @Test
    @DisplayName("VVIP 회원이 포인트 결제 시 10% 등급 할인 후 포인트 5% 중복 적용")
    void vvipMemberPointPaymentAppliesBothDiscounts() {
        Member member = save(member("VVIP", MemberGrade.VVIP));
        Order order = save(order("상품", 100_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.POINT);

        // VVIP 10% = 10,000 → 90,000 → 포인트 5% = 4,500 → 최종 85,500
        assertThat(payment.getFinalAmount()).isEqualTo(85_500);
    }

    @Test
    @DisplayName("신용카드 결제 시 포인트 추가 할인 미적용")
    void creditCardPaymentDoesNotApplyPointDiscount() {
        Member member = save(member("VVIP", MemberGrade.VVIP));
        Order order = save(order("상품", 100_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        // VVIP 10% = 10,000 → 최종 90,000 (포인트 할인 없음)
        assertThat(payment.getFinalAmount()).isEqualTo(90_000);
    }

    // ── 이력 관리 테스트 ─────────────────────────────────────────────────

    @Test
    @DisplayName("결제 완료 시 등급 할인 이력이 저장됨")
    void gradeDiscountHistoryIsSaved() {
        Member member = save(member("VIP", MemberGrade.VIP));
        Order order = save(order("상품", 30_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);
        List<PaymentDiscountHistory> histories = historyRepository.findByPaymentId(payment.getId());

        assertThat(histories).hasSize(1);

        PaymentDiscountHistory gradeHistory = histories.get(0);
        assertThat(gradeHistory.getDiscountType()).isEqualTo(DiscountType.GRADE);
        assertThat(gradeHistory.getPolicyName()).isEqualTo("VIP_FIXED_1000");
        assertThat(gradeHistory.getDiscountAmount()).isEqualTo(1_000);
        assertThat(gradeHistory.getAppliedGrade()).isEqualTo("VIP");
    }

    @Test
    @DisplayName("포인트 결제 시 등급 + 결제수단 이력 두 건 저장")
    void twoHistoriesForPointPaymentWithGradeDiscount() {
        Member member = save(member("VVIP", MemberGrade.VVIP));
        Order order = save(order("상품", 100_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.POINT);
        List<PaymentDiscountHistory> histories = historyRepository.findByPaymentId(payment.getId());

        assertThat(histories).hasSize(2);

        PaymentDiscountHistory gradeH = histories.stream()
                .filter(h -> h.getDiscountType() == DiscountType.GRADE).findFirst().orElseThrow();
        PaymentDiscountHistory methodH = histories.stream()
                .filter(h -> h.getDiscountType() == DiscountType.PAYMENT_METHOD).findFirst().orElseThrow();

        assertThat(gradeH.getPolicyName()).isEqualTo("VVIP_RATE_10PCT");
        assertThat(gradeH.getDiscountAmount()).isEqualTo(10_000);

        assertThat(methodH.getPolicyName()).isEqualTo("POINT_EXTRA_5PCT");
        assertThat(methodH.getDiscountAmount()).isEqualTo(4_500);
    }

    @Test
    @DisplayName("과거 결제 이력은 정책 변경 후에도 원래 값 보존")
    void historiesPreserveSnapshotAfterPolicyWouldChange() {
        Member member = save(member("VIP", MemberGrade.VIP));
        Order order = save(order("상품", 50_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);

        List<PaymentDiscountHistory> histories = historyRepository.findByPaymentId(payment.getId());

        assertThat(histories).isNotEmpty();

        PaymentDiscountHistory snapshot = histories.get(0);
        assertThat(snapshot.getPolicyName()).isEqualTo("VIP_FIXED_1000");
        assertThat(snapshot.getDiscountAmount()).isEqualTo(1_000);
        assertThat(snapshot.getAppliedAt()).isNotNull();

        assertThat(paymentRepository.findById(payment.getId())).isPresent();
    }

    @Test
    @DisplayName("NORMAL 회원 신용카드 결제 시 이력 1건 (할인액 0)")
    void normalGradeHistoryStoredWithZeroDiscount() {
        Member member = save(member("일반", MemberGrade.NORMAL));
        Order order = save(order("상품", 15_000, member));

        Payment payment = paymentService.pay(order, PaymentMethod.CREDIT_CARD);
        List<PaymentDiscountHistory> histories = historyRepository.findByPaymentId(payment.getId());

        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getDiscountAmount()).isEqualTo(0);
        assertThat(histories.get(0).getDiscountType()).isEqualTo(DiscountType.GRADE);
    }

    // ── helpers ────────────────────────────────────────────────────────

    private Member save(Member m) { return memberRepository.save(m); }
    private Order save(Order o) { return orderRepository.save(o); }

    private Member member(String name, MemberGrade grade) {
        return Member.builder().name(name).grade(grade).build();
    }

    private Order order(String name, int price, Member member) {
        return Order.builder().productName(name).originalPrice(price).member(member).build();
    }
}
