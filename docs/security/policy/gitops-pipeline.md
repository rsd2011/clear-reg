# Policy GitOps Automation

## 1. GitHub Actions Workflow
- 파일: `.github/workflows/policy-gitops.yml`
- **validate job**: Gradle schema test + `yamllint` 실행, 변경 diff 아티팩트 업로드.
- **bundle job**: `scripts/policy-bundle/package-policy-bundle.sh`로 tarball 생성 후 아티팩트 저장.
- **deploy job**: `main` 브랜치 push 시, 최신 번들을 다운로드해 `POLICY_GATEWAY_URL` Webhook으로 POST.
- **rollback job**: `workflow_dispatch` 입력으로 전달된 tarball URL을 재적용.

## 2. Scripts
- `scripts/policy-bundle/package-policy-bundle.sh`: manifest(`docs/permission-bundle-digests.json`)에 선언된 정책 파일을 tar.gz로 묶고 SHA-256 버전 생성.
- `scripts/policy-bundle/publish-policy-bundle.sh`: `POLICY_GATEWAY_URL` / `POLICY_GATEWAY_TOKEN`을 사용하여 번들을 업로드 (Jenkins/Actions 공용 스크립트).

## 3. Jenkins Integration
1. Jenkins job은 GitOps repo(`policy-configs`)를 watch하여 main merge 시 CI/CD step을 호출.
2. Jenkins pipeline stages:
   - **Checkout** policy repo + application repo as submodule.
   - **Validate**: `./gradlew :backend:auth:test --tests "*PermissionDefinitionSchemaTest"` 실행.
   - **Package**: `scripts/policy-bundle/package-policy-bundle.sh $WORKSPACE/artifacts` 호출.
   - **Deploy**: `scripts/policy-bundle/publish-policy-bundle.sh artifacts/*.tar.gz` 실행.
   - **Rollback**: parameterized build가 tarball URL을 받아 동일 스크립트 재실행.
3. Jenkins outputs(artifacts + build metadata)은 GitHub Actions rollback job이 참조할 수 있도록 S3에 업로드.

## 4. Runbook Snippets
- 새 정책 PR: Actions 자동 실행 → diff artifact 링크 PR 코멘트.
- 운영 배포: `main` 머지 후 deploy job 실행, Slack Webhook에 `POLICY_GATEWAY_URL` 응답 코드 전송.
- 긴급 롤백: Jenkins or Actions `workflow_dispatch`에 rollback bundle URL 입력 → 동일 endpoint 재호출.
