# 결제 시스템 구현 — Step 1 (기본 요구사항)

## 실행 방법

```bash
mvn test              # 전체 테스트 실행
mvn spring-boot:run   # 애플리케이션 실행 (H2 in-memory DB)
```

---

## 구현 범위

| 기능 | 내용 |
|------|------|
| 회원 등급 할인 | NORMAL(없음) / VIP(1,000원 고정) / VVIP(10%) |
| 중복 결제 방지 | 동일 주문 재결제 시 `AlreadyPaidException` |
| 결제 저장 | 주문·금액·결제수단·일시 모두 기록 |

---

## 프로젝트 구조

```
src/main/java/com/assignment/payment/
├── domain/
│   ├── member/      Member, MemberGrade
│   ├── order/       Order
│   └── payment/     Payment, PaymentMethod
├── discount/
│   ├── DiscountPolicy (interface)
│   ├── NormalDiscountPolicy
│   ├── VipDiscountPolicy
│   └── VvipDiscountPolicy
├── service/         PaymentService
├── repository/      MemberRepository, OrderRepository, PaymentRepository
└── exception/       UnsupportedGradeException, AlreadyPaidException
```

---

## 중요하게 생각한 설계 결정

### 1. 할인 정책에 전략 패턴(Strategy Pattern) 적용

요구사항에 "정책은 수시로 추가/변경 가능"이라는 조건이 명시되어 있습니다.  
이를 `if (grade == VIP)` 같은 분기문으로 처리하면, 등급이 추가될 때마다 `PaymentService` 내부를 수정해야 합니다. 이는 OCP(개방-폐쇄 원칙) 위반입니다.

`DiscountPolicy` 인터페이스에 `supports(MemberGrade)` 메서드를 두고, 각 정책이 자신이 처리할 등급을 스스로 선언하도록 설계했습니다.

```java
// PaymentService는 구체적인 정책을 전혀 알지 못합니다
private DiscountPolicy findGradePolicy(MemberGrade grade) {
    return discountPolicies.stream()
            .filter(p -> p.supports(grade))
            .findFirst()
            .orElseThrow(() -> new UnsupportedGradeException(grade));
}
```

새 등급(예: `PLATINUM`)이 추가되더라도 `PlatinumDiscountPolicy` 하나만 `@Component`로 등록하면 됩니다. `PaymentService`는 수정이 필요 없습니다.

---

### 2. 도메인 객체의 불변성 확보

`Payment`, `Order`, `Member` 엔티티는 모두 setter를 제공하지 않습니다.  
`@Builder`를 통해 생성 시점에 필드를 확정하고, 이후 상태 변경을 막았습니다.

결제는 본질적으로 **기록(record)** 이지 **상태(state)** 가 아닙니다. 결제 완료 후 금액이나 수단이 바뀌는 일은 없어야 합니다. 객체 수준에서 이를 강제함으로써 데이터 정합성 버그를 원천 차단했습니다.

---

### 3. 중복 결제 방어를 서비스 레이어에서 명시적으로 처리

`Payment.order_id`에 `unique` 제약을 걸어 DB 레벨에서도 중복을 막을 수 있지만, DB 예외는 메시지가 불분명하고 상위 레이어에서 처리하기 어렵습니다.

서비스 레이어에서 `existsByOrderId`로 사전에 확인하고 `AlreadyPaidException`을 던지도록 했습니다. DB 제약은 최후 안전망으로 유지하되, 비즈니스 예외는 코드로 명확히 표현하는 것이 의도입니다.

---

### 4. 테스트는 행위(behavior) 기준으로 작성

단순히 메서드 호출 여부가 아니라, **실제 금액 계산 결과**와 **DB에 저장된 값**을 검증하도록 테스트를 작성했습니다.

- `DiscountPolicyTest`: 각 정책의 계산 로직을 단위 테스트로 독립 검증
- `PaymentServiceTest`: Spring 컨텍스트 + H2를 통해 저장/조회까지 통합 검증

---

## 테스트 결과

```
Tests run: 11, Failures: 0, Errors: 0
├── DiscountPolicyTest      6건  (등급별 할인 계산 단위 테스트)
└── PaymentServiceTest      5건  (결제 흐름 통합 테스트)
```
