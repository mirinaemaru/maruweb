---
name: restart
description: Restart the local development server. Use when user asks to restart, reload, refresh, or 재시작.
allowed-tools: Bash
---

# Restart Skill

로컬 개발 서버를 빠르게 재시작합니다.

## Arguments

- `build`: 빌드 후 재시작 (코드 변경 시)
- (없음): 재시작만

## Default (재시작만)

```bash
./stop-maruweb.sh && ./run-maruweb.sh
```

## With Build

인수에 `build`가 포함되면:

```bash
./mvnw clean package -DskipTests -q && ./stop-maruweb.sh && ./run-maruweb.sh
```

## Verification

5초 대기 후 상태 확인:

```bash
sleep 5
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/
```

## Expected Result

- HTTP 200: "Server restarted successfully on http://localhost:8090"
- Otherwise: Show last 30 lines of log for debugging

```bash
tail -30 /var/logs/trading/maruweb.log
```

## Instructions for Claude

1. 인수 확인: `build` 포함 여부
2. 빌드 필요 시 `./mvnw clean package -DskipTests -q` 먼저 실행
3. `./stop-maruweb.sh && ./run-maruweb.sh` 실행
4. 5초 대기 후 health check
5. 성공/실패 결과 보고
