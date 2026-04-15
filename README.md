# sfn-sqa-appium-example

Android 앱 UI 자동화 테스트 예제 프로젝트입니다.  
Appium + TestNG + Allure Report 조합으로 구성되어 있으며, Page Object Model(POM) 패턴을 기반으로 합니다.

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 현재 JVM 버전 자동 감지 |
| Appium Java Client | 10.0.0 |
| TestNG | 7.11.0 |
| Allure TestNG | 2.24.0 |
| Gradle | 9.x |
| Lombok | 9.0.0 (freefair) |
| AssertJ | 3.27.5 |
| Logback | 1.5.18 |

---

## 프로젝트 구조

```
src/test/
├── java/com/appium/
│   ├── common/                     # 공통 인프라 클래스
│   │   ├── BaseTest.java           # TestNG 라이프사이클 관리 (Appium 서버 시작/종료, 자동 로그인)
│   │   ├── BasePage.java           # 공통 페이지 액션 (탭 클릭, 로딩 대기, 팝업 처리 등)
│   │   ├── AppiumUtils.java        # 정적 대기/조건 유틸
│   │   ├── AdbDevices.java         # ADB 기기 정보 조회 (UDID, 앱 버전, OS 버전 등)
│   │   ├── RetryAnalyzer.java      # 실패 테스트 자동 재시도 (최대 2회)
│   │   ├── RetryTransformer.java   # 전체 테스트에 RetryAnalyzer 자동 적용
│   │   └── ScreenshotListener.java # 실패/스킵 시 Allure에 스크린샷 자동 첨부
│   ├── page/                       # Page Object 클래스
│   │   ├── LoginPage.java          # 로그인 화면 요소 및 액션
│   │   └── HomePage.java           # 홈 화면 요소 및 액션
│   ├── uitest/                     # 테스트 클래스
│   │   └── LoginTest.java          # 로그인 기능 테스트
│   ├── testdata/                   # 테스트 데이터 모델
│   │   ├── TestData.java           # YAML 매핑 데이터 클래스
│   │   └── TestDataManager.java    # 환경별 YAML 데이터 싱글톤 로더
│   └── utils/
│       ├── ActionUtils.java        # 클릭, 입력, 스크롤, 팝업 처리 베이스 유틸
│       └── DataUtils.java          # 데이터 관련 유틸
└── resources/
    ├── yml-qa/testdata.yml         # QA 환경 테스트 데이터
    ├── suite/suite.xml             # TestNG 스위트 설정
    ├── allure.properties           # Allure 결과 경로 설정
    ├── categories.json             # Allure 실패 카테고리 분류 정의
    └── logback.xml                 # 로그 설정
```

---

## 사전 준비

1. **Android 기기** 또는 에뮬레이터를 PC에 연결하고 USB 디버깅을 활성화합니다.
2. **Appium Server** (`npm install -g appium`)와 **UiAutomator2 드라이버** (`appium driver install uiautomator2`)를 설치합니다.
3. `adb devices` 명령으로 기기가 정상 인식되는지 확인합니다.

---

## 환경 설정

테스트 실행 전 환경에 맞는 YAML 파일을 설정해야 합니다.

**`src/test/resources/yml-qa/testdata.yml` 예시:**

```yaml
ipAddress: 127.0.0.1
port: 4723
appPackage: com.example.app
id: your_test_id
pw: your_test_password
```

> 환경을 추가하려면 `yml-{env}/testdata.yml` 형태로 디렉토리와 파일을 생성합니다.

---

## 테스트 실행

### 기본 실행 (QA 환경)

```bash
./gradlew test -Psmoke_test
```

### 환경 지정 실행

```bash
./gradlew test -Psmoke_test -Denv=qa
```

### 특정 스위트 실행

```bash
./gradlew test -Ptest1
```

---

## Allure 리포트

### 리포트 서버 실행 (테스트 완료 후 브라우저에서 즉시 확인)

```bash
./gradlew allureServe
```

### 리포트 파일 생성 (날짜별 폴더로 저장)

```bash
./gradlew allureReport
```

> 생성 경로: `build/reports/allure-report/allureReport-{yyyy-MM-dd_HH-mm}/`

### 트렌드(히스토리) 누적

`allureReport` 실행 시 `history` 폴더가 자동으로 `allure-results`에 복사되어  
다음 실행 시 트렌드 그래프가 누적됩니다.

### 실패 카테고리 분류 (`categories.json`)

| 카테고리 | 설명 |
|----------|------|
| 검증 실패 | AssertionError — 실제 앱 버그로 인한 결함 |
| UI 요소 미노출 | NoSuchElementException — 특정 UI 요소를 찾지 못함 |
| 화면 로딩 타임아웃 | TimeoutException — 로딩 대기 시간 초과 |
| 테스트 데이터 부재 | 필요한 데이터가 없어 스킵된 경우 |
| 테스트 코드 오류 | RuntimeException 등 코드 자체의 버그 |
| 앱 서버 오류 | Appium 서버 연결 실패 또는 드라이버 오류 |

---

## 주요 설계 특징

### 자동 로그인 (`BaseTest`)
- `@BeforeTest`에서 앱 실행 후 로그인 상태를 자동으로 확인하고, 로그아웃 상태인 경우에만 로그인을 수행합니다.
- 로그인이 불필요한 테스트 클래스(예: `LoginTest`)는 `isLoginRequired()`를 `false`로 오버라이드합니다.

### 자동 재시도 (`RetryAnalyzer`)
- 테스트 실패 시 최대 **2회** 자동 재시도합니다.
- 드라이버 세션이 유효하지 않으면 재시도를 중단합니다.
- 재시도 회차별 스크린샷이 Allure 리포트에 첨부됩니다.

### 알림 팝업 자동 처리 (`ActionUtils`)
- `click()`, `isDisplayed()` 실행 중 오류 팝업이 감지되면 자동으로 닫고 재시도합니다.
- `@BeforeMethod`에서 매 테스트 시작 전에도 팝업 유무를 확인합니다.

### Allure 환경 정보 자동 기록
- 테스트 시작 시 ADB를 통해 기기 정보(제조사, 모델, OS 버전) 및 앱 버전을 자동으로 수집하여 Allure 리포트의 Environment 탭에 기록합니다.
