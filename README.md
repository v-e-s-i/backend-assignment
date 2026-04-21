# 결제 시스템 구현 — Step 2 (심화 요구사항)

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
| 결제 수단 추가 할인 | 포인트 결제 시 등급 할인 후 금액에서 추가 5% |
| 중복 결제 방지 | 동일 주문 재결제 시 `AlreadyPaidException` |
| 결제 이력 저장 | 적용 정책명·할인율·할인액·등급·할인 유형을 스냅샷으로 기록 |
| 이력 불변성 | 정책 변경·삭제 후에도 과거 이력 데이터 그대로 보존 |

---

## 프로젝트 구조

```
src/main/java/com/assignment/payment/
├── domain/
│   ├── member/      Member, MemberGrade
│   ├── order/       Order
│   ├── payment/     Payment, PaymentMethod
│   └── history/     PaymentDiscountHistory, DiscountType   ← feature 추가
├── discount/
│   ├── DiscountPolicy (interface)                          ← getPolicyName/getDiscountRate 추가
│   ├── NormalDiscountPolicy
│   ├── VipDiscountPolicy
│   ├── VvipDiscountPolicy
│   ├── PaymentMethodDiscountPolicy (interface)             ← feature 추가
│   └── PointDiscountPolicy                                 ← feature 추가
├── service/         PaymentService                         ← 이력 저장·결제수단 할인 추가
├── repository/      MemberRepository, OrderRepository,
│                    PaymentRepository,
│                    PaymentDiscountHistoryRepository        ← feature 추가
└── exception/       UnsupportedGradeException, AlreadyPaidException
```

---

## main → feature 설계 변경 내용

### 변경 1: `DiscountPolicy` 인터페이스에 `getPolicyName()` · `getDiscountRate()` 추가

**이유:** 이력 저장 시 "어떤 정책이었는가", "몇 % 할인이었는가"를 기록해야 했습니다.  
기존 인터페이스는 계산된 금액(`discount()`)만 반환했기 때문에, 정책 식별자와 비율 정보를 이력에 남길 수 없었습니다.

`getPolicyName()`으로 정책 식별자를, `getDiscountRate()`로 할인율을 각 구현체가 직접 노출하도록 했습니다.  
VIP처럼 고정 금액 할인인 경우에는 `getDiscountRate()`가 `null`을 반환해 "비율 할인 없음"을 명시합니다.

---

### 변경 2: 결제 수단 할인을 별도 인터페이스(`PaymentMethodDiscountPolicy`)로 분리

처음에는 결제 수단 할인도 기존 `DiscountPolicy`에 넣는 것을 고려했습니다.  
그러나 등급 할인과 결제 수단 할인은 **적용 기준이 다릅니다.**

- 등급 할인: `Order`(원가)를 기준으로 계산
- 결제 수단 할인: 등급 할인 **이후의 금액**을 기준으로 계산

두 할인의 입력값이 다르므로 하나의 인터페이스로 묶으면 메서드 시그니처가 어색해집니다.  
책임을 명확히 분리해 `PaymentMethodDiscountPolicy.discount(int amountAfterGradeDiscount)`로 독립시켰습니다.

이 구조 덕분에 "포인트 외에 다른 결제 수단에도 별도 할인을 추가"하는 요구가 생겼을 때, `@Component` 하나만 추가하면 됩니다.

---

### 변경 3: `PaymentDiscountHistory`를 독립 엔티티로 설계 (정책과 비결합)

이력을 `DiscountPolicy`의 FK로 연결하는 방식을 고려했지만 채택하지 않았습니다.

**이유:** 정책이 삭제되거나 수정되면 과거 결제 이력이 깨집니다.  
이력의 목적은 "그 시점에 어떤 조건으로 결제되었는가"를 영구히 기록하는 것입니다.  
따라서 `PaymentDiscountHistory`는 결제 시점의 값(정책명, 할인율, 할인액)을 문자열/숫자로 **스냅샷** 저장합니다.  
정책 빈이 코드에서 사라지더라도 이력 레코드는 그대로 남습니다.

```
결제 시점                     미래
──────────────────────────────────────────────
VIP_FIXED_1000  (현재 정책)   → 삭제되거나 금액 변경
PaymentDiscountHistory        → "VIP_FIXED_1000 / 1000원" 그대로 보존
```

---

## 기본 설계 원칙 (main에서 이어받은 내용)

### 할인 정책에 전략 패턴(Strategy Pattern) 적용

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

### 도메인 객체의 불변성 확보

`Payment`, `Order`, `Member` 엔티티는 모두 setter를 제공하지 않습니다.  
`@Builder`를 통해 생성 시점에 필드를 확정하고, 이후 상태 변경을 막았습니다.

### 중복 결제 방어를 서비스 레이어에서 명시적으로 처리

서비스 레이어에서 `existsByOrderId`로 사전에 확인하고 `AlreadyPaidException`을 던지도록 했습니다. DB `unique` 제약은 최후 안전망으로 유지합니다.

---

## 테스트 결과

```
Tests run: 19, Failures: 0, Errors: 0
├── DiscountPolicyTest          6건  (등급별 할인 계산 단위 테스트)
├── PaymentServiceTest          5건  (결제 흐름 통합 테스트)
└── PaymentServiceFeatureTest   8건  (중복 할인 + 이력 보존 테스트)
```
