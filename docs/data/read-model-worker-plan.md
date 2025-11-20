# Read Model Worker Plan (Org/Menu/Permission)

## 목표
- 조직/메뉴/퍼미션 메뉴 Read Model을 Redis에 재생성하는 워커 프로파일을 제공하고, API/이벤트 트리거 기반으로 재빌드할 수 있게 한다.

## 구현 체크리스트
- [ ] profile `read-model-worker` 활성화 시: OrganizationReadModelPort/MenuReadModelPort/PermissionMenuReadModelPort 빈이 필요(현재 organization/menu 소스는 placeholder).
- [x] PermissionMenuReadModelSource: PermissionGroup 기반 생성(`PermissionMenuReadModelSourceImpl`).
- [x] Worker/controller: `/internal/read-model/**/rebuild` 엔드포인트, worker bean 생성(readmodel.worker.enabled=true).
- [ ] Organization/Menu ReadModelSource를 실제 데이터로 교체 (현재 Default*Source placeholder).
- [ ] E2E: principal로 rebuild 호출 → Redis 키/값 확인 (embedded Redis) + 권한 그룹 변경 이벤트 후 read model 갱신 확인.

## 운영 플래그 예시
```yaml
readmodel:
  worker:
    enabled: true
  organization:
    enabled: true
  menu:
    enabled: true
  permission-menu:
    enabled: true
```

## 후속
- Organization/Menu 소스 구현 후, CacheInvalidation/PermissionSetChanged 이벤트와 연계해 자동 재빌드 E2E 테스트 추가.
