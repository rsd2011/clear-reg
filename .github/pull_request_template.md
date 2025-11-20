## 변경 요약
- 관련 모듈/기능과 변경 목적을 간단히 기술하세요.

## 체크리스트
- [ ] 권한/정책 YAML(`permission-groups.yml`, `docs/policies/*.yml`) 변경 시 `docs/permissions-gitops.md`에 정의된 절차를 완료했습니다.
- [ ] `./scripts/check-permission-bundle.sh` 실행 로그를 PR에 첨부하고, `docs/permission-bundle-digests.json`을 최신 해시로 갱신했습니다.
- [ ] `./gradlew test jacocoTestCoverageVerification` (또는 영향 범위에 맞는 모듈 테스트) 결과를 공유했습니다.
- [ ] 보안/감사 관련 변경 내용은 `docs/permissions.md` 혹은 관련 문서를 업데이트했습니다.

## 테스트 노트
- 수행한 수동/자동 테스트를 적어주세요. 실패 케이스가 있다면 이유와 후속 계획을 함께 적습니다.
