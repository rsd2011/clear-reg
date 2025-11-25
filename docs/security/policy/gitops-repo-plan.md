# Policy GitOps Repository Adoption Plan

## 1. 목표
- 권한/정책 정의(`permission-groups.yml`, 마스킹 정책, RowScope 템플릿 등)를 애플리케이션 코드와 분리된 전용 GitOps 저장소로 이전해 변경 추적성과 롤백 속도를 높인다.
- 정책 배포는 Git 이벤트(merge) 기반 파이프라인으로만 수행하도록 강제해 휴먼오류를 줄이고 감사 가능성을 확보한다.

## 2. 저장소 구조 제안
```
policy-configs/
 ├── tenants/
 │   ├── default/
 │   │   ├── permission-groups.yml
 │   │   ├── masking-policies.yml
 │   │   └── row-scope.yml
 │   └── <tenantId>/...
 ├── bundles/
 │   ├── feature-metadata.json
 │   └── action-files/
 └── scripts/
     └── validate-bundle.sh
```
- `main` 브랜치: 운영 환경 기준. `release/*` 태그로 배포 버전을 정의.
- `environments/{dev,staging,prod}` 브랜치 혹은 디렉터리로 멀티 환경 배포 매핑.

## 3. 파이프라인 구성 (GitHub Actions 예시)
1. **Validate Stage**
   - `schema-validation` job: Gradle 테스트(`:backend:auth:test --tests *PermissionDefinitionSchemaTest`) 실행.
   - `lint-yaml` job: `yamllint`, custom script.
   - `bundle-digest` job: 변경된 파일 해시 산출 후 `docs/permission-bundle-digests.json`에 기록.
2. **Preview Stage**
   - `render-diff` job: 정책 변경 diff → HTML/S3 업로드, PR 코멘트.
   - `drift-check` job: 운영 환경 Redis/DB 값과 비교.
3. **Deploy Stage**
   - 조건: `main` 브랜치 merge 및 `deploy:prod` 라벨.
   - Steps:
     1. 아티팩트 패키징 (`policy-bundle-<hash>.tar.gz`).
     2. S3 업로드.
     3. `backend/server`의 `PolicyBundleLoader` API 호출 (GitOps Webhook) → 각 환경에 적용.
     4. 적용 성공 시 태그 생성(`policy-release-<date>`).
4. **Rollback Stage**
   - `workflow_dispatch` 입력값으로 롤백 대상 태그 선택.
   - 이전 번들 다운로드 → 동일 API 호출.

## 4. 애플리케이션 연계
- `PolicyAdminPort`에 `publishBundleMetadata(BundleMetadata metadata)` 메서드 추가해 적용된 번들 정보를 로그/DB에 저장.
- `docs/permission-bundle-digests.json`을 GitOps repo로 이동하고, 애플리케이션 repo에서는 read-only 서브모듈로 참조.
- `AuthContextHolder` 및 `SensitiveDataMaskingModule`은 특정 번들 버전을 캐시에 저장해 요청마다 검증하도록 개선.

## 5. 운영 고려사항
- **권한 분리**: GitOps repo는 보안팀/정책팀에 write 권한 부여, 애플리케이션 repo와 PR 리뷰어 분리.
- **비상 수정**: `hotfix/*` 브랜치 템플릿 + 자동 lint; 승인 외 긴급 배포 시에도 기록 강제.
- **감사 로그**: CI 가 `docs/permission-bundle-digests.json` (GitOps repo 경로) 업데이트 시 CloudTrail/SIEM 로깅.
- **비밀 관리**: 배포 파이프라인은 AWS IAM Role 혹은 GitHub OIDC로 권한을 획득; 자격 증명 노출 방지.

## 6. 단계별 TODO
- [ ] GitOps repo 생성 및 기본 디렉터리/README 추가.
- [ ] Schema lint, Gradle 테스트, bundle digest 스크립트를 GitHub Actions로 분리.
- [ ] `policy-bundle-loader` 엔드포인트 및 인증 방식 확정 (`backend/server`).
- [ ] 운영/스테이징 환경 연결, 롤백 매뉴얼 작성.
- [ ] 애플리케이션 repo에서 정책 파일 제거 후 Git submodule/Artifact 참조로 전환.
