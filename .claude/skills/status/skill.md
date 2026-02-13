---
name: status
description: Check system status. Use when user asks for status, health, 상태, or wants to verify all services are running.
allowed-tools: Bash
---

# Status Skill

전체 시스템 상태를 한눈에 확인합니다.

## Checks

### 1. Local Development Server (8090)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/ 2>/dev/null || echo "000"
```

### 2. Production (Mac Mini, 8090)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/ 2>/dev/null || echo "000"
```

### 3. Nginx (80/443)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:80/ 2>/dev/null || echo "000"
```

### 4. Trading API (8099)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8099/health 2>/dev/null || echo "000"
```

### 5. Database Connection

```bash
mysql -u $DB_USERNAME -p$DB_PASSWORD -e "SELECT 1" maruweb 2>/dev/null && echo "OK" || echo "FAIL"
```

### 6. Git Status

```bash
git status --short
```

## Output Format

결과를 테이블 형식으로 표시:

| 서비스 | 상태 | 포트/정보 |
|--------|------|-----------|
| Local Dev | ✓ Running / ✗ Down | 8090 |
| Production (Mac Mini) | ✓ Running / ✗ Down | 8090 |
| Nginx | ✓ Running / ✗ Down | 80/443 |
| Trading API | ✓ Running / ✗ Down | 8099 |
| Database | ✓ Connected / ✗ Failed | maruweb |
| Git | Clean / n changes | branch명 |

## Status Codes

- `200`: ✓ Running
- `000` 또는 timeout: ✗ Down
- 기타: ⚠ Issues

## Instructions for Claude

1. 모든 체크를 병렬로 실행 (가능한 경우)
2. 결과를 테이블 형식으로 정리
3. 문제가 있는 서비스는 강조 표시
4. 문제 발견 시 해결 방법 제안

## Quick Commands

모든 상태를 한 번에 확인:

```bash
echo "=== System Status ===" && \
echo -n "Local/Prod (8090): " && (curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/ 2>/dev/null || echo "DOWN") && \
echo -n "Nginx (80): " && (curl -s -o /dev/null -w "%{http_code}" http://localhost:80/ 2>/dev/null || echo "DOWN") && \
echo -n "Trading API (8099): " && (curl -s -o /dev/null -w "%{http_code}" http://localhost:8099/health 2>/dev/null || echo "DOWN") && \
echo -n "Git: " && git branch --show-current && git status --short
```
