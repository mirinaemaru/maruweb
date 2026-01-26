---
name: logs
description: Show and analyze server logs. Use when user asks for logs, errors, debugging, 로그, or 에러.
allowed-tools: Bash, Grep
---

# Logs Skill

maruweb 서버 로그를 조회하고 분석합니다.

## Log Paths

| 환경 | 경로 |
|------|------|
| Development | /var/logs/trading/maruweb.log |
| Production | /opt/maruweb/application.log |

## Default Action

개발 서버 로그 최근 50줄:

```bash
tail -50 /var/logs/trading/maruweb.log
```

## Options

### 에러만 보기

사용자가 "error", "에러", "오류" 언급 시:

```bash
grep -i "ERROR\|WARN\|Exception" /var/logs/trading/maruweb.log | tail -30
```

### Production 로그

사용자가 "prod", "production", "프로덕션", "9080" 언급 시:

```bash
tail -50 /opt/maruweb/application.log
```

### 실시간 모드

사용자가 "follow", "실시간", "-f" 언급 시:

```bash
tail -f /var/logs/trading/maruweb.log
```

(주의: 연속 실행되므로 Ctrl+C로 중단 필요)

### 특정 줄 수

사용자가 숫자를 언급하면 해당 줄 수만큼:

```bash
tail -<숫자> /var/logs/trading/maruweb.log
```

### 패턴 검색

사용자가 특정 키워드 검색 요청 시:

```bash
grep -i "<pattern>" /var/logs/trading/maruweb.log | tail -30
```

## Log Analysis Guide

| 패턴 | 의미 |
|------|------|
| `Started TodoApplication` | 서버 시작 성공 |
| `Tomcat started on port` | 요청 수신 준비 완료 |
| `Connection refused` | DB 또는 외부 서비스 연결 실패 |
| `OAuth` 에러 | Google Calendar 인증 문제 |
| `TradingApiService` | Trading System API 연결 문제 |
| `HikariPool` | 데이터베이스 커넥션 풀 상태 |

## Instructions for Claude

1. 기본: 개발 로그 50줄 표시
2. "error/에러" 언급 → 에러만 필터링
3. "prod/프로덕션" 언급 → production 로그 경로 사용
4. 숫자 언급 → 해당 줄 수 표시
5. 로그 표시 후 주요 이슈 요약 제공
