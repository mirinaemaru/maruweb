# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 필요한 가이드를 제공합니다.

## 개발 명령어

### 서버 시작하기
```bash
./run-maruweb.sh              # 개발 서버 시작 (포트 8090)
./stop-maruweb.sh             # 서버 중지
tail -f /tmp/maruweb.log      # 서버 로그 모니터링
```

### 빌드 및 테스트
```bash
./mvnw clean package          # 프로젝트 빌드
./mvnw test                   # 모든 테스트 실행
./mvnw spring-boot:run        # Maven으로 직접 실행
```

### 데이터베이스 접근
데이터베이스 접속 정보는 `.env` 파일에서 로드됩니다. `mysql` 명령어 사용:
```bash
mysql -u $DB_USERNAME -p$DB_PASSWORD maruweb
```

## 아키텍처 개요

### 모듈 구조
애플리케이션은 기능별로 구분된 모듈식 아키텍처를 따릅니다:

- **dashboard**: 모든 모듈의 통합 대시보드
- **kanban**: 프로젝트 기반 태스크 관리 및 워크플로우 (파일 첨부 지원)
- **todo**: 상태 추적이 가능한 작업 관리
- **calendar**: Google Calendar 양방향 동기화를 지원하는 이벤트 관리
- **note**: 노트 작성 및 정리
- **shortcut**: 카테고리별 빠른 링크 관리
- **dday**: D-Day 카운트다운 추적
- **habit**: 습관 추적 기능
- **trading**: 외부 Trading System API 연동 (포트 8099)
- **strategy**: 조건 기반 자동매매 전략 관리

각 모듈은 Spring MVC 패턴을 따릅니다: `controller/` → `service/` → `repository/` → `entity/`

### 주요 아키텍처 패턴

**소프트 삭제 패턴**
모든 엔티티는 하드 삭제 대신 `deleted` 컬럼('Y'/'N')을 사용합니다. 조회 시:
- Repository 메서드: `findByDeletedOrderBy...("N")`
- 삭제 작업: `entity.setDeleted("Y")`로 설정 후 저장

**폼에서 날짜 바인딩**
LocalDate 필드는 HTML 폼 바인딩을 위해 `@DateTimeFormat(pattern = "yyyy-MM-dd")` 어노테이션이 필요합니다.

**Thymeleaf 템플릿 레이아웃**
모든 페이지는 `layout:decorate="~{layout/main}"`을 사용하고 `<div layout:fragment="content">`에 내용을 정의합니다. 메인 레이아웃은 모든 모듈로 이동할 수 있는 사이드바를 포함합니다.

### UI 스타일 가이드라인

**다크모드 스타일 규칙:**
- 텍스트 색상: 흰색 (`var(--text-primary)` 사용)
- 버튼 배경색: 그레이 (`var(--bg-tertiary)` 또는 `#4a5568` 사용)
- 카드/컨테이너 배경: 다크 그레이 (`var(--bg-secondary)` → `#2d2d2d`)
- 페이지 배경: 더 어두운 그레이 (`var(--bg-primary)` → `#1a1a1a`)

**CSS 변수 사용:**
- 항상 하드코딩된 색상 대신 CSS 변수 사용
- 다크모드 변수는 `style.css`의 `[data-theme="dark"]` 섹션에 정의됨
- 새 컴포넌트 추가 시 기존 변수 재사용 권장

**버튼 스타일:**
- 라이트모드: 각 버튼별 고유 색상 (primary=보라색, edit=노란색, delete=빨간색, save=초록색)
- **다크모드: 모든 버튼 배경색은 그레이** (`var(--bg-tertiary)` 또는 `#4a5568`)
- 다크모드에서 버튼 hover: `#4a5568`
- 다크모드 버튼 텍스트: 흰색 (`var(--text-primary)`)
- 적용 대상: `.btn-primary`, `.btn-edit`, `.btn-delete`, `.btn-save`, `.btn-secondary`

### 자동매매 시스템 연동

**strategy** 모듈은 외부 Trading System API(포트 8099)와 연동되는 조건 기반 자동매매를 구현합니다.

**2단계 전략 설정:**
1. Strategy 엔티티의 `strategyType` 필드: `GENERAL` 또는 `AUTO_TRADING`
2. `AUTO_TRADING` 전략만 "Configure Trading", "Activate", "Monitor" 버튼 표시
3. 매매 설정은 `strategies` 테이블의 24개 이상의 추가 컬럼에 저장됨

**핵심 서비스:**
- `TradingApiService`: 외부 Trading System API의 REST 클라이언트 래퍼
- `TradingStrategyService`: 로컬 전략을 API 페이로드로 변환, 동기화/활성화 처리
- `StrategyService`: 소프트 삭제를 지원하는 전략 CRUD 작업

**동기화 흐름:**
1. 사용자가 `/trading/strategies/{id}/trading`을 통해 자동매매 설정 생성/수정
2. `TradingStrategyService.syncToTradingSystem()`이 Strategy 엔티티를 API 페이로드로 변환
3. 외부 시스템의 `externalStrategyId`가 로컬 Strategy 엔티티에 저장됨
4. `syncStatus`로 상태 추적: NOT_SYNCED, SYNCED, OUT_OF_SYNC, ERROR

**활성화 흐름:**
1. 활성화 전 검증으로 필수 필드 확인 (계좌, 종목, 조건, 포지션 크기)
2. `TradingApiService.updateStrategyStatus(externalId, "ACTIVE")` 호출
3. 로컬 `tradingStatus`가 ACTIVE로 업데이트되고 `activatedAt` 타임스탬프 기록

**매매 조건 형식:**
진입/청산 조건은 TEXT 컬럼에 JSON 배열로 저장:
```json
[
  {
    "type": "PRICE",
    "operator": ">=",
    "value": 70000,
    "description": "가격이 70,000원 이상일 때"
  }
]
```

### Google Calendar 연동

**OAuth 흐름:**
- 커스텀 OAuth2 구현 (Spring Security OAuth 아님)
- `TokenEncryptionService`를 사용한 AES 암호화로 토큰 암호화
- `google_oauth_tokens` 테이블에 암호화된 access/refresh 토큰 저장
- Redirect URI: `http://localhost:8090/calendar/oauth2/callback`

**양방향 동기화:**
- 로컬 이벤트는 `calendar_events` 테이블에 동기화 추적을 위한 `google_event_id`와 함께 저장
- UI를 통한 수동 동기화 또는 예약된 백그라운드 작업으로 동기화 트리거 (설정 가능한 간격)
- `GoogleCalendarApiService`가 Google Calendar API v3 호출 처리
- 충돌 해결: 동기화된 이벤트의 경우 Google Calendar가 기준

### 환경 설정

**필수 환경 변수** (`.env` 파일에 설정):
- `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`: 데이터베이스 접속 정보
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: Google Cloud Console의 OAuth2 자격 증명
- `CALENDAR_ENCRYPTION_KEY`: OAuth 토큰 저장용 32자 암호화 키 (`openssl rand -base64 32`로 생성)
- `TRADING_API_BASE_URL`: 외부 Trading System API URL (기본값: http://localhost:8099)

**Spring 프로파일:**
- `local`: 로컬 개발 (.env 변수 사용, 포트 8090)
- `dev`: 개발 서버
- `prod`: 프로덕션 (포트 9080)

### 데이터베이스 스키마 규칙

**공통 컬럼:**
- `id`: BIGINT PRIMARY KEY AUTO_INCREMENT
- `created_at`: DATETIME (생성 시 설정)
- `updated_at`: DATETIME (업데이트 시 설정, 사용하는 경우)
- `deleted`: VARCHAR(1) DEFAULT 'N' (소프트 삭제 플래그)

**Strategy 테이블 확장:**
`strategies` 테이블은 24개 이상의 자동매매 컬럼을 가지고 있습니다:
- `strategy_type`: GENERAL 또는 AUTO_TRADING
- `external_strategy_id`: Trading System의 ID
- `sync_status`, `trading_status`: 동기화 및 매매 상태 추적
- `target_account_id`, `symbol`, `asset_type`: 매매 설정
- `entry_conditions`, `exit_conditions`: JSON TEXT 컬럼
- `stop_loss_type`, `stop_loss_value`, `take_profit_type`, `take_profit_value`: 리스크 관리
- `position_size_type`, `position_size_value`, `max_positions`: 포지션 크기 설정
- `total_trades`, `winning_trades`, `losing_trades`, `total_profit_loss`: 통계

### URL 라우팅 패턴

**표준 라우트:**
- `/todos`: Todo 목록 및 관리
- `/calendar`: 캘린더 뷰 및 이벤트 관리
- `/calendar/oauth2/callback`: Google OAuth 콜백
- `/notes`: 노트 관리
- `/shortcuts`: 카테고리별 단축키 관리
- `/dday`: D-Day 관리
- `/habits`: 습관 추적
- `/dashboard`: 대시보드 개요

**Trading 라우트:**
- `/trading`: 트레이딩 시스템 상태 및 개요
- `/trading/accounts`: 계좌 관리
- `/trading/strategies`: 전략 목록 (레거시 라우트)
- `/trading/strategies/{id}`: 전략 업데이트
- `/trading/strategies/{id}/edit`: 전략 수정 폼
- `/trading/strategies/{id}/delete`: 전략 삭제 (POST)
- `/trading/strategies/{id}/trading`: 자동매매 설정 페이지
- `/trading/strategies/{id}/activate`: 자동매매 활성화 (POST)
- `/trading/strategies/{id}/deactivate`: 자동매매 비활성화 (POST)
- `/trading/strategies/{id}/sync`: Trading System에 수동 동기화 (POST)
- `/trading/strategies/{id}/monitor`: 전략 모니터링 대시보드
- `/trading/orders`: 주문 추적 및 필터링

**카테고리 관리 라우트:**
- `/trading/strategies/categories`: 카테고리 추가 (POST)
- `/trading/strategies/categories/{id}/delete`: 카테고리 삭제 (POST)

### 중요한 Thymeleaf 패턴

**전략 유형에 따른 조건부 렌더링:**
```html
<!-- AUTO_TRADING 전략에만 표시 -->
<div th:if="${strategy.strategyType == 'AUTO_TRADING'}">
    <a th:href="@{/trading/strategies/{id}/trading(id=${strategy.id})}">Configure Trading</a>
</div>
```

**Null 안전 조건:**
SpEL 평가 오류를 피하기 위해 복합 조건에서 항상 null 체크:
```html
<!-- 잘못된 예: syncStatus가 null이면 오류 발생 -->
<span th:if="${strategy.strategyType == 'AUTO_TRADING' && strategy.syncStatus}">

<!-- 올바른 예: Null 안전 체크 -->
<span th:if="${strategy.strategyType == 'AUTO_TRADING' && strategy.syncStatus != null}">
```

### 자주 발생하는 문제

1. **LocalDate 필드에 @DateTimeFormat 누락**: 어노테이션이 없으면 폼에서 400 Bad Request 오류 발생
2. **deleted='N' 체크 잊기**: Repository 쿼리에서 항상 소프트 삭제된 레코드 필터링 필요
3. **Strategy type이 업데이트되지 않음**: `StrategyService.updateStrategy()`에 `strategyType` 필드 업데이트 포함 필요
4. **Thymeleaf의 Null 체크**: null일 수 있는 필드는 truthy 평가 대신 `!= null` 사용
5. **Trading System API 사용 불가**: TradingApiService 메서드는 예외를 던지지 않고 오류 맵을 반환하므로 응답 구조 확인 필요

### 배포

**개발 환경:**
- `.env`를 로드하고 포트 8090에서 시작하는 `./run-maruweb.sh` 사용
- 로그는 `/tmp/maruweb.log`에 기록됨

**프로덕션:**
- WAR 파일 빌드: `./mvnw clean package -DskipTests`
- Tomcat에 배포하거나 독립 실행형으로 실행: `java -jar -Dspring.profiles.active=prod target/todo-0.0.1-SNAPSHOT.jar`
- Jenkins 배포 스킬이 `.claude/skills/deploy/`에 있음

### 외부 의존성

**Trading System API** (포트 8099):
- trading/strategy 기능이 작동하려면 실행 중이어야 함
- Health check 엔드포인트: `GET /health`
- Strategy 엔드포인트: `/api/strategies/*`
- Account 엔드포인트: `/api/accounts/*`
- Order 엔드포인트: `/api/orders/*`

**Google Calendar API:**
- Google Cloud Console의 OAuth2 자격 증명 필요
- 각 환경에 대해 인증된 리디렉션 URI 구성 필요
- API 스코프: `https://www.googleapis.com/auth/calendar`

## 칸반 보드 워크플로우

### 태스크 상태 관리

칸반 보드는 4단계 워크플로우를 사용합니다:

1. **등록 (REGISTERED)**: 새로 생성된 태스크
2. **응답대기중 (WAITING_RESPONSE)**: 사용자의 의견/결정 필요
3. **진행중 (IN_PROGRESS)**: 현재 작업 중인 태스크
4. **완료 (COMPLETED)**: 작업 완료

### Claude의 태스크 처리 규칙

**Claude는 다음 상태의 태스크를 개발합니다:**
- **등록 (REGISTERED)** 상태의 태스크
- **응답대기중 (WAITING_RESPONSE)** 상태에서 사용자 응답을 받은 태스크

**워크플로우:**
1. 등록된 태스크 개발 시작 → **진행중**으로 이동
2. 개발 중 사용자 의견/결정 필요 → **응답대기중**으로 이동
3. 사용자 응답 확인 → 다시 **진행중**으로 이동하여 개발 계속
4. 개발 완료 → **완료**로 이동

### 파일 첨부 기능

칸반 보드는 태스크당 1개 파일 첨부를 지원합니다:

**업로드 방법:**
- 파일 선택 버튼 클릭
- **Ctrl+V** (Mac: Cmd+V)로 스크린샷 직접 붙여넣기
- 드래그 앤 드롭

**지원 형식:**
- 문서: `.txt`, `.md`, `.pdf`, `.doc`, `.docx`
- 이미지: `.png`, `.jpg`, `.jpeg`
- 최대 크기: 10MB

**저장 위치:**
- `/uploads/kanban/{taskId}/{UUID_filename}`
- UUID 기반 파일명으로 중복 방지

### 프로젝트 디렉토리 매핑

각 프로젝트는 실제 디렉토리 경로와 연결됩니다:
- 프로젝트 생성 시 `directory_path` 설정 (예: `~/projects/maruweb`)
- Claude 실행 노트에 작성된 지시사항을 해당 디렉토리에서 수동 실행
- 향후 자동 실행 기능 추가 예정
