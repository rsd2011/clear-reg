# Backend Architecture Analysis (clear-reg)

## 1. 모듈 구조 및 역할

### 핵심 모듈 (15개)

```
backend/
├── platform/        # ✅ 핵심 기초 모듈
│   ├── com.example.common.user.spi
│   │   ├── UserAccountInfo (인터페이스)
│   │   ├── UserAccountProvider (인터페이스)
│   ├── com.example.common.api.dto
│   │   ├── ErrorResponse
│   ├── com.example.common.error
│   │   ├── ErrorCode, BusinessException, CommonErrorCode
│   ├── com.example.common.security
│   │   ├── FeatureCode, ActionCode, RowScope
│   └── test-fixtures (java-test-fixtures)
│
├── auth/            # 인증/인가 (Authentication & Authorization)
│   ├── domain/
│   │   ├── RefreshToken, RefreshTokenRepository
│   │   ├── RefreshTokenService
│   │   ├── PasswordHistory, PasswordHistoryRepository
│   ├── dto/
│   │   ├── LoginRequest, LoginResponse
│   │   ├── TokenResponse, TokenRefreshRequest
│   │   ├── PasswordChangeRequest, AccountStatusChangeRequest
│   ├── security/
│   │   ├── JwtTokenProvider (JWT 생성/검증)
│   │   ├── AccountStatusPolicy (계정 상태 관리)
│   │   ├── PasswordHistoryService
│   │   ├── PasswordPolicyValidator
│   │   ├── UserAccountDetailsService
│   ├── strategy/
│   │   ├── AuthenticationStrategy (인터페이스)
│   │   ├── PasswordAuthenticationStrategy
│   │   ├── SsoAuthenticationStrategy
│   │   ├── ActiveDirectoryAuthenticationStrategy
│   │   ├── AuthenticationStrategyResolver
│   ├── ad/        # Active Directory
│   ├── sso/       # Single Sign-On
│   ├── config/
│   │   ├── AuthDataInitializer
│   └── AuthService (메인 비즈니스 로직)
│
├── admin/          # 관리 기능 (Permission, Policy, Config, User 관리)
│   ├── permission/ (권한 관리)
│   │   ├── context/
│   │   │   ├── AuthContext (ThreadLocal 기반 인증 정보)
│   │   │   ├── AuthContextHolder
│   │   │   ├── PermissionDecision
│   │   │   ├── AuthCurrentUserProvider
│   │   │   ├── AuthContextPropagator (비동기 컨텍스트 전파)
│   │   │   ├── AuthContextTaskDecorator
│   │   ├── aop/
│   │   │   ├── RequirePermissionAspect (@RequirePermission 처리)
│   │   ├── repository/
│   │   │   ├── PermissionGroupRepository
│   │   │   ├── PermissionGroupRootRepository
│   │   │   ├── PermissionMenuRepository
│   │   ├── service/
│   │   │   ├── PermissionEvaluator (SpEL 기반 권한 평가)
│   │   ├── dto/
│   │   │   ├── PermissionGroupRootResponse
│   │   │   ├── PermissionAssignmentDto
│   │   │   ├── PermissionGroupRootRequest
│   │   │   └── 기타 PermissionGroup*Response/Request
│   │   ├── domain/
│   │   │   ├── PermissionGroup (권한 그룹)
│   │   │   ├── PermissionGroupRoot (권한 그룹 루트)
│   │   │   ├── PermissionAssignment (권한 할당)
│   │   │   ├── PermissionMenu
│   │   ├── spi/
│   │   │   └── UserInfo (interface, @Deprecated)
│   │   └── exception/
│   │       └── PermissionDeniedException
│   │
│   ├── maskingpolicy/ (데이터 마스킹 정책)
│   │   └── @Sensitive("TAG") 기반 필드 레벨 마스킹
│   │
│   ├── rowaccesspolicy/ (행 레벨 접근 제어)
│   │   ├── RowAccessPolicyProvider
│   │   ├── RowAccessQuery, RowAccessMatch
│   │
│   ├── draft/       (기안 템플릿 및 양식)
│   │   └── DraftFormTemplateResponse
│   │
│   ├── approval/    (결재 템플릿 및 그룹)
│   │   ├── ApprovalTemplateRootResponse
│   │   ├── ApprovalGroupResponse
│   │
│   ├── systemconfig/ (시스템 설정)
│   │   └── SystemConfigController
│   │
│   ├── menu/        (메뉴 관리)
│   │
│   ├── codegroup/   (코드 그룹 관리)
│   │
│   ├── orggroup/    (조직 그룹 관리)
│   │
│   └── user/        (사용자 관리)
│       ├── UserAccount (엔티티)
│       └── UserAccountService
│
├── draft/          # 기안 도메인 로직
│   ├── application/
│   │   ├── DraftApplicationService
│   │   ├── dto/
│   │   │   ├── DraftCreateRequest
│   │   │   ├── DraftDecisionRequest
│   │   │   ├── DraftResponse
│   │   │   ├── DraftHistoryResponse
│   │   │   └── DraftReferenceResponse
│   ├── domain/
│   │   ├── Draft (엔티티)
│   │   └── exception/ DraftNotFoundException
│
├── approval/       # 결재 워크플로우 엔진
│   ├── domain/
│   ├── application/
│   │   └── ApprovalAuthorizationService
│
├── audit/          # 감사 로깅 및 SIEM 통합
│   ├── AuditPort (포트/인터페이스)
│   ├── AuditEvent
│   ├── Actor, ActorType
│   ├── RiskLevel
│   └── AuditMode (ASYNC_FALLBACK 등)
│
├── file-core/      # 파일 저장소 추상화
│   ├── FileStorageException
│   ├── FilePolicyViolationException
│   └── StoredFileNotFoundException
│
├── server/         # 🔴 메인 Spring Boot 애플리케이션
│   ├── web/ (20개 RestController)
│   │   ├── AuthController (/api/auth)
│   │   ├── DraftController (/api/drafts)
│   │   ├── DraftFormTemplateController
│   │   ├── ApprovalGroupController
│   │   ├── ApprovalTemplateRootController
│   │   ├── PermissionGroupRootController
│   │   ├── RowAccessPolicyRootController
│   │   ├── MaskingPolicyRootController
│   │   ├── MenuController
│   │   ├── SystemConfigController
│   │   ├── NotificationController, NotificationAdminController
│   │   ├── NoticeController, NoticeAdminController
│   │   ├── FileController
│   │   ├── CodeGroupController
│   │   ├── DwOrganizationController
│   │   ├── HelloController
│   │   ├── GlobalExceptionHandler (@RestControllerAdvice)
│   │   └── PolicyDebugController
│   │
│   ├── security/
│   │   ├── JwtAuthenticationFilter
│   │   ├── RestAuthenticationEntryPoint
│   │   ├── RestAccessDeniedHandler
│   │
│   ├── config/
│   │   ├── SecurityConfig
│   │   ├── PasswordEncoderConfig
│   │   └── 기타 설정
│   │
│   ├── readmodel/
│   │   ├── PermissionMenuReadModelSourceImpl
│   │
│   └── Application.java (진입점)
│
├── batch/          # Spring Batch 처리
│   ├── BatchApplication.java (진입점)
│   ├── audit/
│   │   └── AuditMetricsExposure, AuditPartitionScheduler
│
├── dw-gateway/     # Data Warehouse 게이트웨이
│   └── DwGatewayApplication.java (진입점)
│
├── dw-worker/      # 백그라운드 DW 수집 워커
│   └── DwWorkerApplication.java (진입점)
│
├── dw-gateway-api/ # DW 게이트웨이 API 계약
│
├── dw-gateway-client/ # DW 게이트웨이 클라이언트 라이브러리
│
├── dw-ingestion-core/ # DW 수집 핵심 로직
│
└── data-integration/ # HR/외부 데이터 커넥터
```

---

## 2. Server 모듈의 Controller & API 엔드포인트

### AuthController (/api/auth)

```java
POST   /api/auth/login              // 로그인 (SSO/AD/Password)
POST   /api/auth/refresh            // 액세스 토큰 갱신
POST   /api/auth/logout             // 로그아웃 (리프레시 토큰 폐기)
PATCH  /api/auth/password           // 비밀번호 변경
PATCH  /api/auth/accounts/status    // 계정 활성화/비활성화
```

### 기안 관련 API

```java
GET    /api/drafts                  // 기안 목록 조회 (페이지네이션, 필터)
POST   /api/drafts                  // 기안 생성
POST   /api/drafts/{id}/submit      // 기안 제출
POST   /api/drafts/{id}/approve     // 기안 승인
POST   /api/drafts/{id}/reject      // 기안 반려
GET    /api/drafts/{id}             // 기안 상세 조회
GET    /api/draft-form-templates    // 기안 양식 템플릿 조회
```

### 결재 관련 API

```java
GET    /api/approval-groups         // 결재 그룹 조회
POST   /api/approval-groups         // 결재 그룹 생성
PUT    /api/approval-groups/{id}    // 결재 그룹 수정
GET    /api/approval-templates      // 결재 템플릿 조회
```

### 권한/정책 관리 API

```java
GET    /api/permission-groups       // 권한 그룹 조회
POST   /api/permission-groups       // 권한 그룹 생성/관리
GET    /api/row-access-policies     // 행 접근 정책 조회
PUT    /api/masking-policies        // 마스킹 정책 수정
GET    /api/menus                   // 메뉴 조회
```

### 시스템 관리 API

```java
GET    /api/system-config           // 시스템 설정 조회
PUT    /api/system-config           // 시스템 설정 수정
GET    /api/notifications           // 알림 조회
GET    /api/notices                 // 공지사항 조회
```

### 파일 관련 API

```java
POST   /api/files                   // 파일 업로드
GET    /api/files/{id}              // 파일 다운로드
DELETE /api/files/{id}              // 파일 삭제
```

### 기타

```java
GET    /api/dw-organizations        // DW 조직 조회
GET    /api/greeting                // 테스트 엔드포인트
```

---

## 3. Auth 모듈: 인증/인가 시스템

### 3.1 JWT 기반 인증

**JwtTokenProvider**
```
액세스 토큰:  15분 (900초)
리프레시 토큰: 30일 (2,592,000초)
발급자: "clear-reg-backend"
비밀키: 설정 필요 (현재 더미값)
```

**TokenResponse (토큰 응답)**
```json
{
  "accessToken": "eyJhbGc...",
  "accessTokenExpiresAt": "2024-12-03T10:20:00Z",
  "refreshToken": "ref_xxx...",
  "refreshTokenExpiresAt": "2025-01-02T10:05:00Z"
}
```

### 3.2 로그인 요청/응답

**LoginRequest**
```json
{
  "type": "PASSWORD|SSO|AD",  // 필수
  "username": "user123",
  "password": "***",
  "token": "sso_token"        // SSO/AD 토큰
}
```

**LoginResponse**
```json
{
  "username": "user123",
  "type": "PASSWORD|SSO|AD",
  "tokens": {
    "accessToken": "...",
    "accessTokenExpiresAt": "...",
    "refreshToken": "...",
    "refreshTokenExpiresAt": "..."
  }
}
```

### 3.3 인증 전략 패턴

```
AuthenticationStrategyResolver
  ├─ PasswordAuthenticationStrategy
  ├─ SsoAuthenticationStrategy (싱글 사인온)
  └─ ActiveDirectoryAuthenticationStrategy (LDAP/AD)

각 전략이 UserAccountInfo를 반환
```

### 3.4 권한 시스템 (@RequirePermission)

**AOP 기반 권한 검사**
```java
@RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
public Page<DraftResponse> listDrafts(...) { ... }
```

**FeatureCode (기능)**
- `DRAFT`: 기안
- `APPROVAL`: 결재
- `PERMISSION`: 권한 관리
- `AUDIT`: 감사
- `POLICY`: 정책 관리

**ActionCode (동작)**
- `READ`, `CREATE`, `UPDATE`, `DELETE`
- `DRAFT_READ`, `DRAFT_CREATE`, `DRAFT_SUBMIT`, `DRAFT_APPROVE`

### 3.5 권한 평가 흐름

```
@RequirePermission 어노테이션
    ↓
RequirePermissionAspect (AOP)
    ↓
PermissionEvaluator (SpEL 기반)
    ↓
AuthContextHolder (ThreadLocal)
    ↓
PermissionDecision (UserInfo + PermissionAssignment + PermissionGroup)
    ↓
AuthContext (username, orgCode, permissionGroupCode, feature, action)
```

### 3.6 계정 관리 정책

**AccountStatusPolicy**
- 계정 활성화/비활성화
- 로그인 실패 잠금 (max-failed-attempts: 5)
- 잠금 해제 시간 (lockout-seconds: 900초 = 15분)

**PasswordPolicyValidator**
- 최소 길이: 12자
- 대문자: 필수
- 소문자: 필수
- 숫자: 필수
- 특수문자: 필수

**PasswordHistoryService**
- 비밀번호 재사용 방지 (history-size: 5개)
- 비밀번호 만료 (password-expiry-days: 90일)

---

## 4. DTO/Request/Response 클래스 구조

### 4.1 인증 관련 DTO

| 클래스명 | 용도 | 필드 |
|---------|------|------|
| **LoginRequest** | 로그인 요청 | type(필수), username, password, token |
| **LoginResponse** | 로그인 응답 | username, type, tokens(TokenResponse) |
| **TokenResponse** | 토큰 응답 | accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt |
| **TokenRefreshRequest** | 토큰 갱신 요청 | refreshToken |
| **PasswordChangeRequest** | 비밀번호 변경 | currentPassword, newPassword |
| **AccountStatusChangeRequest** | 계정 상태 변경 | username, active |

### 4.2 기안 관련 DTO

| 클래스명 | 용도 |
|---------|------|
| **DraftCreateRequest** | 기안 생성 요청 |
| **DraftDecisionRequest** | 기안 결재 요청 (승인/반려) |
| **DraftResponse** | 기안 조회 응답 (UUID id, title, businessFeatureCode, status, ...) |
| **DraftHistoryResponse** | 기안 변경 이력 |
| **DraftReferenceResponse** | 기안 참조 정보 |
| **DraftFormTemplateResponse** | 기안 양식 템플릿 |

### 4.3 권한/정책 DTO

| 클래스명 | 용도 |
|---------|------|
| **PermissionGroupRootResponse** | 권한 그룹 조회 (id, groupCode, name, assignments, ...) |
| **PermissionAssignmentDto** | 개별 권한 할당 정보 |
| **PermissionGroupRootRequest** | 권한 그룹 생성/수정 요청 |
| **RowAccessPolicyRootResponse** | 행 접근 정책 응답 |
| **MaskingPolicyRootResponse** | 마스킹 정책 응답 |

### 4.4 공통 DTO

```java
// platform 모듈
record ErrorResponse(String code, String message, String traceId, String timestamp)

// server 모듈 
record ProblemResponse(String message)  // 간단한 에러 응답
```

### 4.5 사용자 정보 인터페이스

**UserAccountInfo** (platform 모듈에서 정의)
```java
interface UserAccountInfo {
  UUID getId()
  String getUsername()
  String getPassword()
  String getEmail()
  String getOrganizationCode()           // 소속 조직
  String getPermissionGroupCode()        // 권한 그룹
  String getSsoId()                      // SSO 시스템 ID
  String getActiveDirectoryDomain()      // AD 도메인
  Set<String> getRoles()                 // 역할 목록 (USER, ADMIN, ...)
  boolean isActive()
  boolean isLocked()
  Instant getLockedUntil()
  int getFailedLoginAttempts()
  Instant getPasswordChangedAt()
}
```

---

## 5. 에러 처리 방식

### 5.1 GlobalExceptionHandler

**위치**: `/backend/server/src/main/java/com/example/server/web/GlobalExceptionHandler.java`

**어노테이션**: `@RestControllerAdvice`

### 5.2 처리하는 예외와 HTTP 상태 코드

| 예외 클래스 | HTTP 상태 | 응답 포맷 |
|-----------|----------|----------|
| `InvalidCredentialsException` | **401** (Unauthorized) | ProblemResponse |
| `NoticeNotFoundException` | **404** (Not Found) | ProblemResponse |
| `NoticeStateException` | **400** (Bad Request) | ProblemResponse |
| `UserNotificationNotFoundException` | **404** (Not Found) | ProblemResponse |
| `StoredFileNotFoundException` | **404** (Not Found) | ProblemResponse |
| `FileStorageException` | **500** (Internal Server Error) | ProblemResponse |
| `FilePolicyViolationException` | **400** (Bad Request) | ProblemResponse |
| `MethodArgumentNotValidException` | **400** (Bad Request) | ProblemResponse + 필드명 + 메시지 |
| `IllegalArgumentException` | **400** (Bad Request) | ProblemResponse |
| `BusinessException` | 동적 (ErrorCode 기반) | ErrorResponse |

### 5.3 에러 응답 포맷

**ProblemResponse** (간단한 에러)
```json
{
  "message": "Invalid credentials"
}
```

**ErrorResponse** (비즈니스 예외)
```json
{
  "code": "PERMISSION_DENIED",
  "message": "접근 권한이 없습니다",
  "traceId": "...",
  "timestamp": "2024-12-03T10:05:00Z"
}
```

### 5.4 BusinessException & ErrorCode

```java
class BusinessException extends RuntimeException {
  ErrorCode errorCode()
}

enum CommonErrorCode implements ErrorCode {
  PERMISSION_DENIED      → 403
  NOT_FOUND              → 404
  CONFLICT               → 409
  INVALID_REQUEST        → 400
}
```

### 5.5 커스텀 예외들

| 모듈 | 예외 클래스 | 설명 |
|-----|-----------|------|
| auth | `InvalidCredentialsException` | 인증 실패 |
| file-core | `FileStorageException` | 파일 저장 오류 |
| file-core | `FilePolicyViolationException` | 파일 정책 위반 |
| file-core | `StoredFileNotFoundException` | 저장된 파일 없음 |
| admin | `PermissionDeniedException` | 권한 거부 |
| admin | `PermissionGroupNotFoundException` | 권한 그룹 없음 |
| draft | `DraftNotFoundException` | 기안 없음 |
| 기타 | `*NotFoundException` | 리소스 없음 |

---

## 6. 보안 설정 (SecurityConfig)

### 6.1 필터 체인

```
1. CSRF 비활성화 (Stateless API)
2. 인가 규칙:
   - /h2-console/**, /v3/api-docs/**, /swagger-ui/** → 모두 허용
   - POST /api/auth/login, /refresh, /logout → 인증 불필요
   - GET /api/greeting → USER, ADMIN 역할 필요
   - 나머지 → 모두 인증 필요

3. 세션 관리: STATELESS (JWT 기반)
4. 예외 처리:
   - AuthenticationEntryPoint: RestAuthenticationEntryPoint
   - AccessDeniedHandler: RestAccessDeniedHandler

5. JWT 필터: JwtAuthenticationFilter (UsernamePasswordAuthenticationFilter 이전)
```

### 6.2 인증/인가 필터

**JwtAuthenticationFilter**
```
요청 헤더 Authorization: Bearer <token> 파싱
  ↓
JwtTokenProvider.validateToken()
  ↓
Spring Security Context에 Authentication 설정
```

---

## 7. 설정값 (application.yml)

### 7.1 JWT 설정

```yaml
security:
  jwt:
    secret: change-me-change-me-change-me-change-me-change-me-32bytes
    access-token-seconds: 900         # 15분
    refresh-token-seconds: 2592000    # 30일
    issuer: clear-reg-backend
```

### 7.2 인증 정책

```yaml
security:
  auth:
    password-min-length: 12
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special: true
    max-failed-attempts: 5
    lockout-seconds: 900              # 15분
    password-history-size: 5
    password-expiry-days: 90

  policy:
    password-policy-enabled: true
    password-history-enabled: true
    account-lock-enabled: true
    enabled-login-types:
      - PASSWORD
      - SSO
      - AD
```

### 7.3 데이터베이스

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:cleardb;...
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.H2Dialect
```

### 7.4 LDAP (Active Directory)

```yaml
spring:
  ldap:
    urls: ""         # ldap://server:389
    base: ""         # ou=users,dc=example,dc=com
    username: ""     # 관리자 DN
    password: ""     # 관리자 비밀번호
```

---

## 8. 비동기/배치 컨텍스트 처리

### 8.1 AuthContextPropagator

ThreadLocal 기반 AuthContext를 비동기/배치 작업으로 전파

```java
// 메인 스레드
AuthContext context = AuthContextHolder.getCurrentContext()

// 비동기 작업 실행 전
AuthContextPropagator.propagate(context)

// 워커 스레드
AuthContext.of(username, orgCode, permissionGroupCode, feature, action)
```

### 8.2 Batch 작업용 시스템 컨텍스트

```java
DwBatchAuthContext.systemContext()
// 배치 작업에서 시스템 권한으로 실행
```

---

## 9. 감사 (Audit) 로깅

### 9.1 AuditPort (포트 패턴)

```
AuthService → auditPort.record(AuditEvent)
  ↓
비동기 감사 로깅 (ASYNC_FALLBACK 모드)
```

### 9.2 감사 이벤트 구조

```java
AuditEvent
  ├─ eventType: "AUTH", "DRAFT", "APPROVAL", ...
  ├─ moduleName: "auth", "draft", ...
  ├─ action: "LOGIN", "PASSWORD_CHANGE", "DRAFT_CREATE", ...
  ├─ actor: Actor(id, type: HUMAN, role, dept)
  ├─ subject: Subject(type: "USER", key: username)
  ├─ success: boolean
  ├─ resultCode: "OK", "FAILED"
  └─ riskLevel: LOW, MEDIUM, HIGH
```

---

## 10. 프론트엔드 Pinia 설정 시 필요 정보

### 10.1 저장해야 할 토큰

```javascript
// 로그인 응답에서
{
  accessToken,
  accessTokenExpiresAt,
  refreshToken,
  refreshTokenExpiresAt
}

// Pinia 스토어에 저장
auth: {
  tokens: { ... },
  username: string,
  loginType: 'PASSWORD' | 'SSO' | 'AD'
}
```

### 10.2 토큰 갱신 로직

```
액세스 토큰 만료 시
  ↓
POST /api/auth/refresh + refreshToken
  ↓
새 accessToken + refreshToken 받기
  ↓
Pinia 스토어 업데이트
```

### 10.3 API 호출 시 헤더

```
Authorization: Bearer <accessToken>
```

### 10.4 권한 기반 UI 렌더링

```javascript
// AuthContext에서 가져올 수 있는 정보
{
  username,
  organizationCode,
  permissionGroupCode,
  feature,
  action
}

// 특정 기능 활성화 여부 확인
if (context.action.includes('DRAFT_CREATE')) {
  // 기안 생성 버튼 표시
}
```

### 10.5 RowScope 기반 데이터 필터링

```javascript
// RowScope: OWN | ORG | ALL | CUSTOM
// GET /api/drafts?scope=ORG 으로 자동 필터링
```

---

## 11. API 호출 예제 (프론트엔드용)

### 로그인
```
POST /api/auth/login
Body: {
  "type": "PASSWORD",
  "username": "user123",
  "password": "..."
}
Response: {
  "username": "user123",
  "type": "PASSWORD",
  "tokens": { ... }
}
```

### 기안 조회
```
GET /api/drafts?page=0&size=20&status=PENDING
Header: Authorization: Bearer <token>
Response: Page<DraftResponse>
```

### 기안 생성
```
POST /api/drafts
Header: Authorization: Bearer <token>
Body: DraftCreateRequest
Response: DraftResponse
```

---

## 요약

- **인증**: JWT (15분) + 리프레시 토큰 (30일)
- **권한**: @RequirePermission + AOP + SpEL 기반 평가
- **에러**: GlobalExceptionHandler + 동적 HTTP 상태 코드
- **감사**: 비동기 AuditPort 통합
- **비동기**: AuthContextPropagator로 ThreadLocal 전파
- **정책**: 비밀번호 정책, 계정 잠금, 비밀번호 이력 관리
