# Trading Dashboard 통합 완료!

## ✅ 추가된 파일

### 1. Backend (Java)

#### Config
- `src/main/java/com/maru/config/RestTemplateConfig.java`
  - Trading System API 호출용 RestTemplate Bean 설정
  - Base URL: http://localhost:8099
  - Timeout 설정

#### Service
- `src/main/java/com/maru/trading/service/TradingApiService.java`
  - Trading System API 호출 서비스
  - Health Check
  - 계좌 목록 조회
  - Kill Switch 상태 조회
  - 전략 목록 조회
  - 주문 목록 조회
  - 포지션 목록 조회

#### Controller
- `src/main/java/com/maru/trading/controller/TradingController.java`
  - `/trading/dashboard` - Trading 대시보드
  - `/trading/accounts` - 계좌 관리
  - `/trading/strategies` - 전략 관리
  - `/trading/orders` - 주문 조회

### 2. Frontend (Thymeleaf)

#### Templates
- `src/main/resources/templates/trading/dashboard.html`
  - Trading System 대시보드
  - System Health 표시
  - Kill Switch 상태 표시
  - 계좌 목록 표시
  - 전략 목록 표시
  - 30초마다 자동 새로고침

- `src/main/resources/templates/trading/error.html`
  - Trading System 연결 오류 페이지
  - 에러 메시지 및 해결 방법 안내

### 3. Configuration

#### Modified Files
- `src/main/resources/application.properties`
  - Trading API 설정 추가
  ```properties
  trading.api.base-url=http://localhost:8099
  trading.api.connect-timeout=5000
  trading.api.read-timeout=10000
  ```

- `src/main/resources/templates/dashboard/index.html`
  - 네비게이션 메뉴에 Trading 링크 추가

---

## 🚀 실행 방법

### 1. Trading System 실행 (필수!)

```bash
cd /Users/changsupark/projects/cautostock
./run-with-env.sh
```

**확인**: http://localhost:8099/health
```json
{
  "status": "UP",
  "components": {
    "db": "UP",
    "kisRest": "UP",
    "kisWs": "UP",
    "token": "VALID"
  }
}
```

### 2. MaruWeb 실행

```bash
cd /Users/changsupark/projects/maruweb
mvn spring-boot:run
```

**확인**: http://localhost:8090

### 3. Trading Dashboard 접속

http://localhost:8090/trading/dashboard

---

## 📊 Trading Dashboard 기능

### System Health
- Database 상태
- KIS REST API 상태
- KIS WebSocket 상태
- Token 유효성 상태

### Kill Switch
- 현재 상태 (OFF/ON)
- 거래 차단 여부 표시

### Accounts
- 등록된 계좌 수
- 계좌 목록 (환경, 상태 포함)

### Strategies
- 활성 전략 수
- 전체 전략 수
- 전략 목록 (상태 포함)

### Quick Links
- 계좌 관리 페이지
- 전략 관리 페이지
- 주문 조회 페이지

---

## 🎨 UI 특징

- **반응형 디자인**: 모바일/데스크톱 지원
- **자동 새로고침**: 30초마다 데이터 갱신
- **에러 처리**: Trading System 미실행 시 에러 페이지 표시
- **색상 코딩**:
  - 🟢 녹색: 정상 상태 (UP, OFF)
  - 🔴 빨간색: 비정상 상태 (DOWN, ON)
  - 🔵 파란색: PAPER 환경
  - 🟢 초록색: ACTIVE 상태
  - 🔴 빨간색: INACTIVE 상태

---

## 🧪 테스트 시나리오

### Scenario 1: 정상 작동
1. Trading System 실행 확인 (8099 포트)
2. MaruWeb 접속 (8090 포트)
3. 네비게이션에서 "Trading" 메뉴 클릭
4. Dashboard에 다음 정보 표시 확인:
   - System Health: UP
   - Kill Switch: OFF
   - Accounts: 등록된 계좌 표시
   - Strategies: 등록된 전략 표시

### Scenario 2: Trading System 미실행
1. Trading System 중지
2. MaruWeb에서 Trading Dashboard 접속
3. 에러 페이지 표시 확인
4. 해결 방법 안내 확인

### Scenario 3: 자동 새로고침
1. Trading Dashboard 접속
2. Trading System에서 계좌 추가
3. 30초 대기
4. Dashboard 자동 갱신 확인

---

## 🔧 트러블슈팅

### 1. "Trading System API is unavailable" 오류

**원인**: Trading System API 서버가 실행되지 않음

**해결**:
```bash
cd /Users/changsupark/projects/cautostock
./run-with-env.sh
```

확인: `curl http://localhost:8099/health`

### 2. 빈 데이터 표시

**원인**: Trading System에 데이터가 없음

**해결**: Trading System API를 통해 계좌/전략 등록
```bash
curl -X POST http://localhost:8099/api/v1/admin/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "broker": "KIS",
    "environment": "PAPER",
    "cano": "50068999",
    "acntPrdtCd": "01",
    "alias": "demo-account"
  }'
```

### 3. Connection Timeout

**원인**: Trading System 응답 지연

**해결**: application.properties에서 timeout 조정
```properties
trading.api.read-timeout=15000  # 10초 → 15초
```

---

## 📁 프로젝트 구조

```
maruweb/
├── src/main/java/com/maru/
│   ├── config/
│   │   └── RestTemplateConfig.java          ← 새로 추가
│   └── trading/                              ← 새로 추가
│       ├── controller/
│       │   └── TradingController.java
│       └── service/
│           └── TradingApiService.java
│
└── src/main/resources/
    ├── templates/
    │   ├── dashboard/
    │   │   └── index.html                    ← 수정됨 (메뉴 추가)
    │   └── trading/                          ← 새로 추가
    │       ├── dashboard.html
    │       └── error.html
    └── application.properties                ← 수정됨 (설정 추가)
```

---

## 🎯 다음 단계 (선택사항)

### 추가 기능 구현
1. **계좌 관리 페이지** (`/trading/accounts`)
   - 계좌 등록/수정/삭제
   - 권한 설정

2. **전략 관리 페이지** (`/trading/strategies`)
   - 전략 생성/수정/삭제
   - 파라미터 설정
   - 활성화/비활성화

3. **주문 조회 페이지** (`/trading/orders`)
   - 주문 내역 조회
   - 필터링 (계좌, 종목, 상태, 기간)
   - 체결 상세 정보

4. **포지션/손익 페이지**
   - 현재 포지션 조회
   - 실시간 손익 표시
   - 차트 시각화

5. **Kill Switch 토글 기능**
   - 긴급 정지 버튼
   - 확인 대화상자
   - POST 요청으로 상태 변경

---

## 📚 참고 문서

- Trading System API 명세: `/projects/cautostock/md/docs/04_API_OPENAPI.md`
- 통합 가이드: `/projects/cautostock/MARUWEB_INTEGRATION.md`
- Trading System 실행 가이드: `/projects/cautostock/RUN_GUIDE.md`

---

## ✨ 완료!

MaruWeb에 Trading Dashboard가 성공적으로 통합되었습니다!

**접속 URL**: http://localhost:8090/trading/dashboard
