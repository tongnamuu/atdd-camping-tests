# atdd-camping-tests

## 빠른 시작 — Step 1 스모크 테스트

```bash
# 키오스크 컨테이너 기동 + @smoke 테스트 + 종료 (원커맨드)
JAVA_HOME=/Users/rook/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home \
  ./gradlew smokeTest --no-daemon
```

## 개요
캠핑 키오스크·결제 서비스의 ATDD(인수 테스트) 및 통합 테스트 전용 프로젝트.
Cucumber + REST Assured 기반으로 외부 실행 중인 서비스에 HTTP 요청을 보내 검증한다.

## 프로젝트 구성
- **빌드 도구**: Gradle (Kotlin DSL), Java 17 필수
- **테스트 프레임워크**: Cucumber 7.14 + JUnit Platform Suite
- **HTTP 클라이언트**: REST Assured 5.3.2
- **DI**: cucumber-picocontainer (TestContext 공유)

## 빌드 및 테스트 실행

> **주의**: 로컬에서는 반드시 **Java 17**을 사용해야 한다.
> `JAVA_HOME`이 Java 17로 설정되어 있지 않으면 빌드가 실패한다.

```bash
# Java 17로 테스트 빌드 확인
JAVA_HOME=/Users/rook/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home \
  ./gradlew testClasses --no-daemon

# 전체 테스트 실행 (서비스가 먼저 구동되어야 함)
JAVA_HOME=... ./gradlew test --no-daemon
```

## Step 1: 스모크 테스트 (키오스크 단독)

```bash
# 컨테이너 기동 (빌드 포함)
docker compose -f infra/docker/docker-compose.yml up -d --build

# 상태 확인
docker compose -f infra/docker/docker-compose.yml ps

# @smoke 테스트만 실행 (컨테이너가 이미 구동된 경우)
JAVA_HOME=... ./gradlew test -Dcucumber.filter.tags="@smoke" --no-daemon

# 종료
docker compose -f infra/docker/docker-compose.yml down
```

### 환경 변수
| 변수 | 기본값 | 설명 |
|------|--------|------|
| `KIOSK_BASE_URL` | `http://localhost:8080` | 키오스크 서비스 URL |
| `PAYMENTS_BASE_URL` | `http://localhost:9090` | 결제 서비스 URL |

> `SmokeHooks.@Before("@smoke")` — 첫 번째 @smoke 시나리오 실행 전 `/health` 엔드포인트에 최대 2분 폴링 (5초 간격 × 24회)

## 전체 서비스 구동 (Docker Compose)

```bash
# 인프라 전체 구동 (DB + WireMock + payments + kiosk)
docker compose -f infra/docker-compose.yml up -d --build

# 서비스 상태 확인
docker compose -f infra/docker-compose.yml ps

# 로그 확인
docker compose -f infra/docker-compose.yml logs -f

# 종료
docker compose -f infra/docker-compose.yml down
```

### 포트 매핑
| 서비스 | 컨테이너 포트 | 호스트 포트 |
|--------|------------|-----------|
| MySQL  | 3306 | 3306 |
| WireMock (관리자 서비스 모의) | 8080 | 9000 |
| payments | 9090 | 9090 |
| kiosk | 8080 | 8080 |

## 디렉토리 구조
```
infra/
  docker-compose.yml          # 전체 테스트 환경
  docker-compose-infra.yml    # DB만 구동 시
  db/init.sql                 # MySQL 초기 스키마 및 시드 데이터
  wiremock/mappings/
    payment-approve.json      # 결제 서비스 모의 (WireMock 직접 테스트용)
    admin-login.json          # POST /auth/login 모의
    admin-products.json       # GET /admin/products 모의
    admin-sales.json          # POST /api/sales 모의
src/test/
  java/com/camping/tests/
    RunCucumberTest.java       # Cucumber 스위트 진입점
    support/TestContext.java   # 시나리오 내 공유 상태 (PicoContainer DI)
    steps/
      CommonSteps.java         # 공통 검증 스텝
      PaymentSteps.java        # 결제 서비스 통합 테스트 스텝
      KioskSteps.java          # 키오스크 ATDD 스텝
  resources/
    cucumber.properties        # glue·plugin 설정
    features/
      payments.feature         # 결제 서비스 통합 테스트
      kiosk.feature            # 키오스크 ATDD
```

## 테스트 시나리오 요약

### payments.feature (통합 테스트)
- 결제 생성 → `INITIATED`
- 결제 승인 → `APPROVED`
- 결제 취소 → `CANCELED`
- 멱등 처리: 동일 paymentKey 중복 요청
- 인증 없는 요청 → 401

### kiosk.feature (ATDD)
- 상품 목록 조회
- 결제 생성 성공 (WireMock admin + 실제 payments)
- 결제 생성 → 승인 전체 흐름

## 관련 서비스
- `atdd-camping-payments` — 결제 서비스 (포트 9090)
- `atdd-camping-kiosk` — 키오스크 POS (포트 8080)
