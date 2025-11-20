# DraftBusinessPolicy 연동 예시

목적: 업무 모듈(예: 비용정산/휴가 등)에서 기안 가능 여부를 검증하고, 기안 상태 변화에 따라 업무 엔티티 상태를 갱신한다.

## 핵심 포인트
- `DraftBusinessPolicy`는 Draft 엔티티에 의존하지 않고 최소 컨텍스트만 전달한다: draftId, businessFeatureCode, status, action, actor.
- 기본 구현(`NoOpDraftBusinessPolicy`)은 draft 모듈에 포함되어 있으며, 업무 모듈에서 @Primary 구현을 제공하면 자동으로 교체된다.
- 의존성 역전: 업무 모듈은 draft 모듈에 의존하지만 draft는 업무 모듈을 모른다.

## 예시 구현 (expense 모듈)
```java
@Component
@Primary
@Slf4j
public class ExpenseDraftPolicy implements DraftBusinessPolicy {

    private final ExpenseService expenseService;

    public ExpenseDraftPolicy(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Override
    public void assertCreatable(String businessFeatureCode, String organizationCode, String actor) {
        if (!"EXPENSE".equalsIgnoreCase(businessFeatureCode)) return;
        boolean allowed = expenseService.canSubmitExpense(organizationCode, actor);
        if (!allowed) {
            throw new IllegalStateException("해당 조직에서 비용 기안을 제출할 수 없습니다.");
        }
    }

    @Override
    public void afterStateChanged(UUID draftId, String businessFeatureCode, DraftStatus newStatus, DraftAction action, String actor) {
        if (!"EXPENSE".equalsIgnoreCase(businessFeatureCode)) return;
        expenseService.markDraftState(draftId, newStatus.name(), action.name(), actor);
    }
}
```

## 스프링 빈 우선순위
- draft 모듈의 `NoOpDraftBusinessPolicy`는 `@ConditionalOnMissingBean`이 아니므로, 커스텀 구현을 `@Primary`로 선언하거나 draft 모듈 빈을 제외(scan filter)하는 방식으로 대체한다.
- 추천: 커스텀 구현 클래스에 `@Primary`를 붙여 가장 우선 주입되도록 한다.

## 연동 체크리스트
- 모듈 의존성: 업무 모듈은 `backend/draft`에 대한 compileOnly 또는 implementation 의존을 추가해 `DraftBusinessPolicy`, `DraftAction`, `DraftStatus`만 사용한다.
- 예외 정책: `assertCreatable`에서 비즈니스 불가 상태 시 도메인 예외를 던지면 DraftApplicationService가 전파한다.
- 관측성: 필요한 경우 `afterStateChanged`에서 로그/메트릭을 기록하거나 별도 이벤트를 발행한다.
```
