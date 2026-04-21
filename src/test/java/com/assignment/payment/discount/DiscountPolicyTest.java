package com.assignment.payment.discount;

import com.assignment.payment.domain.member.Member;
import com.assignment.payment.domain.member.MemberGrade;
import com.assignment.payment.domain.order.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountPolicyTest {

    private final NormalDiscountPolicy normalPolicy = new NormalDiscountPolicy();
    private final VipDiscountPolicy vipPolicy = new VipDiscountPolicy();
    private final VvipDiscountPolicy vvipPolicy = new VvipDiscountPolicy();

    @Test
    @DisplayName("NORMAL 등급은 할인 없음")
    void normalGradeNoDiscount() {
        Order order = orderWithGrade(MemberGrade.NORMAL, 10_000);
        assertThat(normalPolicy.discount(order)).isEqualTo(0);
    }

    @Test
    @DisplayName("VIP 등급은 1,000원 고정 할인")
    void vipGradeFixed1000Discount() {
        Order order = orderWithGrade(MemberGrade.VIP, 10_000);
        assertThat(vipPolicy.discount(order)).isEqualTo(1_000);
    }

    @Test
    @DisplayName("VIP 할인은 주문 금액과 무관하게 고정 1,000원")
    void vipDiscountIsAlwaysFixed() {
        Order cheapOrder = orderWithGrade(MemberGrade.VIP, 500);
        Order expensiveOrder = orderWithGrade(MemberGrade.VIP, 1_000_000);

        assertThat(vipPolicy.discount(cheapOrder)).isEqualTo(1_000);
        assertThat(vipPolicy.discount(expensiveOrder)).isEqualTo(1_000);
    }

    @Test
    @DisplayName("VVIP 등급은 주문 금액의 10% 할인")
    void vvipGrade10PercentDiscount() {
        Order order = orderWithGrade(MemberGrade.VVIP, 20_000);
        assertThat(vvipPolicy.discount(order)).isEqualTo(2_000);
    }

    @Test
    @DisplayName("VVIP 할인은 주문 금액에 비례")
    void vvipDiscountIsProportional() {
        Order order5000 = orderWithGrade(MemberGrade.VVIP, 5_000);
        Order order100000 = orderWithGrade(MemberGrade.VVIP, 100_000);

        assertThat(vvipPolicy.discount(order5000)).isEqualTo(500);
        assertThat(vvipPolicy.discount(order100000)).isEqualTo(10_000);
    }

    @Test
    @DisplayName("각 정책은 자신이 지원하는 등급만 supports")
    void eachPolicySupportsCorrectGrade() {
        assertThat(normalPolicy.supports(MemberGrade.NORMAL)).isTrue();
        assertThat(normalPolicy.supports(MemberGrade.VIP)).isFalse();
        assertThat(normalPolicy.supports(MemberGrade.VVIP)).isFalse();

        assertThat(vipPolicy.supports(MemberGrade.VIP)).isTrue();
        assertThat(vipPolicy.supports(MemberGrade.NORMAL)).isFalse();
        assertThat(vipPolicy.supports(MemberGrade.VVIP)).isFalse();

        assertThat(vvipPolicy.supports(MemberGrade.VVIP)).isTrue();
        assertThat(vvipPolicy.supports(MemberGrade.NORMAL)).isFalse();
        assertThat(vvipPolicy.supports(MemberGrade.VIP)).isFalse();
    }

    private Order orderWithGrade(MemberGrade grade, int price) {
        Member member = Member.builder()
                .name("테스트회원")
                .grade(grade)
                .build();
        return Order.builder()
                .productName("상품")
                .originalPrice(price)
                .member(member)
                .build();
    }
}
