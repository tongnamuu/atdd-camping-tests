# 테스트 전략 및 가이드

## 1. 스모크 테스트란?

**스모크 테스트(Smoke Test)** 는 애플리케이션이 최소한으로 동작하는지 확인하는 **가장 얕은 수준의 테스트**다.
연기가 나는지(smoke) 확인한다는 의미에서 유래했다.

```
테스트 피라미드

        ▲
       /AT\       ← ATDD (인수 테스트): 전체 흐름, 비즈니스 시나리오
      /----\
     / INT  \     ← 통합 테스트: 실제 서비스 API 검증
    /--------\
   /  SMOKE   \   ← 스모크 테스트: 서비스가 기동되어 응답하는가?
  /____________\
```

### 이 프로젝트에서의 스모크 테스트

| 항목 | 내용 |
|------|------|
| 대상 | 키오스크 서비스 (포트 8080) |
| 검증 | `GET /health → 200 OK` |
| 태그 | `@smoke` |
| 목적 | 컨테이너가 정상 기동되었는지 확인 |

---

## 2. 전체 테스트 전략

```
Step 1  @smoke        키오스크 단독 기동, 헬스 체크 1건
Step 2  @integration  결제 서비스 API 통합 테스트 (실제 payments 서비스)
Step 3  @atdd         키오스크 → 결제 → 관리자 전체 흐름 (WireMock으로 admin 모의)
```

### 태그 체계

```gherkin
@smoke       # 기동 확인만 (외부 서비스 불필요)
@integration # payments 서비스 필요 (포트 9090)
@atdd        # kiosk + payments + WireMock(admin) 전체 필요
```

### 서비스 의존 관계

```
@smoke       kiosk
@integration payments
@atdd        kiosk ──→ payments (실제)
                  └──→ WireMock (admin 모의)
```

---

## 3. 테스트 실행 방법

### 3-1. 스모크 테스트 (Step 1)

```bash
# 한 줄 실행 — 컨테이너 기동/종료 자동 처리
JAVA_HOME=/Users/rook/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home \
  ./gradlew smokeTest --no-daemon
```

내부 실행 순서:
```
SmokeHooks.@Before("@smoke")
  1. GET /health 시도
     ├─ 200 응답 → 이미 기동 중, 바로 진행
     └─ 실패    → docker compose up -d --build (자동 기동)
                  JVM 종료 시 docker compose down 자동 등록
  2. ServiceWaiter: /health 폴링 (5초 간격, 최대 2분)
  3. 시나리오 실행
  4. JVM 종료 → docker compose down
```

### 3-2. 태그별 실행

```bash
# @smoke 만
./gradlew test -Dcucumber.filter.tags="@smoke" --no-daemon

# @integration 만 (payments 서비스 필요)
./gradlew test -Dcucumber.filter.tags="@integration" --no-daemon

# @atdd 만 (전체 스택 필요)
./gradlew test -Dcucumber.filter.tags="@atdd" --no-daemon

# 전체 실행
./gradlew test --no-daemon
```

> **주의**: `JAVA_HOME`을 Java 17로 설정해야 한다.
> 기본값이 Java 25인 경우 `JAVA_HOME=/Users/rook/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home` 를 앞에 붙인다.

### 3-3. 전체 스택 수동 기동

```bash
# 인프라 + 전체 서비스 기동
docker compose -f infra/docker-compose.yml up -d --build

# 스모크 테스트용 (키오스크만)
docker compose -f infra/docker/docker-compose.yml up -d --build

# 상태 확인
docker compose -f infra/docker/docker-compose.yml ps

# 로그
docker compose -f infra/docker/docker-compose.yml logs -f kiosk

# 종료
docker compose -f infra/docker/docker-compose.yml down
```

---

## 4. 테스트 케이스 추가 방법

### 4-1. Feature 파일에 시나리오 추가

`src/test/resources/features/` 아래 `.feature` 파일에 시나리오를 작성한다.

```gherkin
# language: ko
@smoke
기능: 키오스크 스모크 테스트

  시나리오: 헬스 체크 - 키오스크가 정상 응답한다     ← 기존
    만약 키오스크 헬스 체크를 요청한다
    그러면 응답 상태 코드는 200이다

  시나리오: 새로운 시나리오 추가 예시              ← 추가
    만약 <새로운 스텝>
    그러면 응답 상태 코드는 200이다
```

### 4-2. 스텝 정의 추가

`src/test/java/com/camping/tests/steps/` 아래 해당 스텝 클래스에 메서드를 추가한다.

```java
@만약("새로운 스텝")
public void 새로운_스텝() {
    Response response = RestAssured.given()
            .get(TestConfig.KIOSK_BASE_URL + "/some-endpoint");
    context.setLastResponse(response);
}
```

### 4-3. 스텝 클래스 역할 분리

| 클래스 | 역할 |
|--------|------|
| `SmokeSteps` | 키오스크 헬스 체크 |
| `KioskSteps` | 키오스크 API 호출 (`/api/products`, `/api/payments`) |
| `PaymentSteps` | 결제 서비스 API 직접 호출 (`/v1/payments`) |
| `CommonSteps` | 응답 코드, 상태 등 공통 검증 |

새 도메인이 필요하면 `XxxSteps.java`를 추가하고, `TestContext`를 생성자로 받으면 PicoContainer가 자동 주입한다.

```java
public class NewSteps {
    private final TestContext context;

    public NewSteps(TestContext context) {   // PicoContainer 자동 주입
        this.context = context;
    }
}
```

### 4-4. 공통 검증 스텝 재사용

`CommonSteps`에 이미 정의된 스텝은 어떤 Feature 파일에서도 그대로 사용할 수 있다.

```gherkin
그러면 응답 상태 코드는 200이다         ← CommonSteps
그리고 결제 상태는 "APPROVED"이다       ← CommonSteps
그리고 상품이 1개 이상 포함되어 있다    ← CommonSteps
```

---

## 5. 키워드 규칙 (중요)

Cucumber에서 `@만약` / `@그리고` / `@그러면` 은 **문서화 용도**일 뿐,
스텝 매칭은 **텍스트 패턴만** 사용한다.

```java
// 이 세 가지는 모두 동일하게 동작한다
@만약("헬스 체크를 요청한다")
@그리고("헬스 체크를 요청한다")   // 같은 텍스트 → DuplicateStepDefinitionException!
@그러면("헬스 체크를 요청한다")   // 같은 텍스트 → DuplicateStepDefinitionException!
```

> **규칙**: 같은 텍스트 패턴은 **하나의 메서드에만** 등록한다.

---

## 6. DB 설정 및 초기화

현재 Step 1(스모크 테스트)에서는 **DB가 필요 없다**.
키오스크와 결제 서비스 모두 인메모리 또는 외부 서비스에 의존하기 때문이다.

### 6-1. DB가 필요한 경우 (전체 스택)

DB는 `infra/docker-compose.yml`에 포함되어 있다.

```bash
docker compose -f infra/docker-compose.yml up -d
```

| 항목 | 값 |
|------|----|
| 이미지 | MySQL 8.0 |
| 포트 | 3306 |
| DB명 | `atdd` |
| root 비밀번호 | `secret` |
| 접속 | `mysql -h 127.0.0.1 -uroot -psecret atdd` |

### 6-2. 초기 스키마 및 시드 데이터

`infra/db/init.sql` 이 MySQL 컨테이너 기동 시 자동 실행된다.

```
infra/db/init.sql
  ├── 테이블 생성: products, campsites, reservations, sales_records, rental_records
  └── 시드 데이터:
        products  — 랜턴, 장작팩, 코펠 세트 등 12종
        campsites — A-1 ~ A-20, B-1 ~ B-15 (총 35개)
        reservations — 과거/현재/미래 예약 샘플
        sales_records, rental_records — 과거 판매/대여 샘플
```

### 6-3. 테스트 전 DB 초기화가 필요한 경우

통합 테스트에서 DB 상태를 초기화하려면 Cucumber `@Before` 훅에서 JDBC로 직접 실행한다.

```java
import io.cucumber.java.Before;
import java.sql.*;

public class DbHooks {

    private static final String URL = "jdbc:mysql://localhost:3306/atdd";
    private static final String USER = "root";
    private static final String PASS = "secret";

    @Before("@db-reset")   // 해당 태그가 붙은 시나리오 전에만 실행
    public void resetDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM sales_records");
            stmt.execute("DELETE FROM rental_records");
            // 필요한 테이블만 초기화
        }
    }
}
```

Feature 파일에서 사용:

```gherkin
@atdd @db-reset
시나리오: 판매 기록이 없는 상태에서 첫 결제
  ...
```

---

## 7. 디렉토리 구조 요약

```
atdd-camping-tests/
│
├── infra/
│   ├── docker/
│   │   └── docker-compose.yml       # Step 1: 키오스크 단독
│   ├── docker-compose.yml           # 전체 스택 (DB + WireMock + payments + kiosk)
│   ├── docker-compose-infra.yml     # DB만
│   ├── db/
│   │   └── init.sql                 # 스키마 + 시드 데이터
│   ├── dockerfiles/
│   │   └── Dockerfile-kiosk         # 키오스크 Dockerfile 레퍼런스
│   └── wiremock/mappings/
│       ├── admin-login.json          # POST /auth/login 모의
│       ├── admin-products.json       # GET /admin/products 모의
│       └── admin-sales.json          # POST /api/sales 모의
│
└── src/test/
    ├── java/com/camping/tests/
    │   ├── RunCucumberTest.java       # JUnit Suite 진입점
    │   ├── hooks/
    │   │   └── SmokeHooks.java        # @smoke: 자동 docker compose up/down
    │   ├── support/
    │   │   ├── TestConfig.java        # 베이스 URL 환경 변수 읽기
    │   │   ├── TestContext.java       # 시나리오 내 공유 상태
    │   │   └── ServiceWaiter.java     # 서비스 준비 대기 폴링
    │   └── steps/
    │       ├── SmokeSteps.java        # @smoke 스텝
    │       ├── KioskSteps.java        # 키오스크 API 스텝
    │       ├── PaymentSteps.java      # 결제 서비스 API 스텝
    │       └── CommonSteps.java       # 공통 검증 스텝
    └── resources/
        ├── cucumber.properties        # glue, plugin 설정
        └── features/
            ├── smoke.feature          # @smoke
            ├── kiosk.feature          # @atdd
            └── payments.feature       # @integration
```
