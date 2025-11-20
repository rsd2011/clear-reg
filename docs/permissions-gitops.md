# Permissions GitOps Workflow

본 문서는 권한/데이터 정책 정의(`permission-groups.yml`, 정책 YAML 등)를 변경할 때 따라야 하는 GitOps 절차를 설명합니다.

## 1. 변경 사유 정의
- 어떤 Feature/Action/RowScope를 왜 바꾸는지 이슈나 PR 본문에 명시합니다.
- 사용자 영향, 롤아웃/롤백 플랜, 감사 항목을 함께 기록합니다.

## 2. 스키마 & 린트 실행
1. 필요한 변경을 적용한 뒤 `./gradlew :backend:auth:test --tests com.example.auth.permission.declarative.PermissionDefinitionSchemaTest` 로 기본 검증을 수행합니다.
2. 정책/권한 문서(`docs/permissions.md`)에 해당 변경 요약을 추가합니다.

## 3. 해시 갱신 & 로그 남기기
1. `sha256sum backend/auth/src/main/resources/permission-groups.yml` 처럼 변경된 파일의 SHA-256 값을 구합니다.
2. `docs/permission-bundle-digests.json`에 해당 경로, 해시, 변경 일자를 업데이트합니다.
3. `./scripts/check-permission-bundle.sh` 를 실행해 해시가 문서와 일치하는지 확인하고, 성공 로그를 PR에 첨부합니다.
4. 필요 시 `docs/permission-bundle-digests.json` 변경 내역에 코멘트를 남겨 감사 추적을 돕습니다.

## 4. PR 생성 시 체크리스트
- `.github/pull_request_template.md`의 체크박스를 모두 채웁니다.
- 권한/정책 변경이 포함된 경우 `git diff` 스니펫 혹은 스크린샷으로 이해관계자가 쉽게 파악하도록 합니다.
- 변경 내용이 운영에 미치는 영향을 설명하는 Release Note 초안을 PR에 포함합니다.

## 5. 배포 전/후 작업
- 승인 후 배포 전, 운영 환경에 로드될 파일 경로와 해시를 다시 비교합니다.
- 배포 완료 후에는 `docs/permission-bundle-digests.json`의 `updatedAt` 값이 실제 반영 일자와 맞는지 확인합니다.
- 문제가 발생하면 해당 PR/커밋을 롤백하고 이전 해시 값을 복원합니다 (Git 기록을 통해 손쉽게 복구 가능).

이 워크플로우를 통해 권한 정책 변경이 항상 리뷰·검증·감사 가능하도록 유지합니다.
