# Development Guide

로컬 개발 환경 설정 및 서버 실행 가이드입니다.

## 환경 설정

### 1. 환경 변수 설정

`.env` 파일에 필요한 환경 변수를 설정하세요:

```bash
# Database
DB_USERNAME=nextman
DB_PASSWORD=1111
DB_URL=jdbc:mariadb://localhost:3306/maruweb?createDatabaseIfNotExist=true

# Google OAuth (Calendar 동기화용)
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# 암호화 키
CALENDAR_ENCRYPTION_KEY=your-encryption-key

# Trading API
TRADING_API_BASE_URL=http://localhost:8099

# Spring Profile
SPRING_PROFILES_ACTIVE=local
```

### 2. MariaDB 실행

```bash
# MariaDB 시작
brew services start mariadb

# 연결 확인
mysql -u nextman -p1111 maruweb
```

## 서버 실행

### 간편 실행 (추천)

```bash
# 서버 시작
./run-maruweb.sh

# 서버 중지
./stop-maruweb.sh
```

**`run-maruweb.sh` 기능:**
- ✓ 자동 환경변수 로딩 (.env 파일)
- ✓ 포트 충돌 자동 감지 및 해결
- ✓ 데이터베이스 연결 확인
- ✓ 서버 시작 및 Health Check
- ✓ 상세한 진행 상황 표시

### 수동 실행

```bash
# 환경변수 로딩
source setup-env.sh

# 서버 시작
./mvnw spring-boot:run
```

## 서버 정보

- **Local Development**: http://localhost:8090
- **Production**: http://localhost:9080
- **Log File**: `/tmp/maruweb.log`

## 개발 팁

### 로그 확인

```bash
# 실시간 로그 보기
tail -f /tmp/maruweb.log

# 에러만 필터링
grep -i error /tmp/maruweb.log
```

### 포트 확인

```bash
# 8090 포트 사용 중인 프로세스 확인
lsof -ti:8090

# 프로세스 강제 종료
lsof -ti:8090 | xargs kill -9
```

### 데이터베이스 백업

```bash
# 백업
mysqldump -u nextman -p1111 maruweb > backup.sql

# 복원
mysql -u nextman -p1111 maruweb < backup.sql
```

### Hot Reload

Spring DevTools가 활성화되어 있어 코드 변경 시 자동으로 재시작됩니다:
- Java 파일 변경: 자동 재시작
- Template 파일 변경: 즉시 반영
- Static 파일 변경: 즉시 반영

## 배포

```bash
# Jenkins를 통한 자동 배포 (권장)
git add .
git commit -m "your message"
git push origin master

# 수동 배포
./mvnw clean package -DskipTests
# JAR 파일을 서버로 복사 후 실행
```

## 문제 해결

### 서버가 시작되지 않을 때

1. 로그 확인: `tail -50 /tmp/maruweb.log`
2. 포트 확인: `lsof -ti:8090`
3. DB 연결: `mysql -u nextman -p1111 maruweb`
4. Java 버전: `java -version` (17 필요)

### Google Calendar 동기화 오류

1. `.env`의 OAuth 자격증명 확인
2. 토큰 재설정: `DELETE FROM google_oauth_tokens;`
3. 서버 재시작 후 재인증

### Trading API 연결 오류

1. Trading API 서버 실행 확인: `curl http://localhost:8099/health`
2. 포트 확인: `lsof -ti:8099`
3. 서버 시작: `cd /Users/changsupark/projects/cautostock && ./run-with-env.sh`

## 유용한 명령어

```bash
# Maven wrapper 재생성
mvn wrapper:wrapper

# 의존성 업데이트 확인
./mvnw versions:display-dependency-updates

# 테스트만 실행
./mvnw test

# 빌드 (테스트 스킵)
./mvnw clean package -DskipTests
```
