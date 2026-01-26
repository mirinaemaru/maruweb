---
name: test
description: Run tests and analyze results. Use when user asks to test, run tests, 테스트, or verify code changes.
allowed-tools: Bash, Read, Grep
---

# Test Skill

테스트를 실행하고 결과를 분석합니다.

## Arguments

- (없음): 전체 테스트 실행
- `unit`: 단위 테스트만
- `it` 또는 `integration`: 통합 테스트만
- `e2e`: E2E 테스트만
- `<ClassName>`: 특정 테스트 클래스만

## Default (전체 테스트)

```bash
./mvnw test 2>&1 | tail -50
```

## Unit Tests Only

```bash
./mvnw test -Dtest="!*IT,!*E2ETest" 2>&1 | tail -50
```

## Integration Tests Only

```bash
./mvnw test -Dtest="*IT" 2>&1 | tail -50
```

## E2E Tests Only

```bash
./mvnw test -Dtest="*E2ETest" 2>&1 | tail -50
```

## Specific Test Class

```bash
./mvnw test -Dtest="<ClassName>" 2>&1 | tail -50
```

## Result Analysis

테스트 완료 후 결과 요약:

```bash
grep -E "Tests run:|Failures:|Errors:|BUILD" target/surefire-reports/*.txt 2>/dev/null | tail -20
```

## Output Format

| 항목 | 값 |
|------|-----|
| 총 테스트 | n개 |
| 성공 | n개 |
| 실패 | n개 |
| 스킵 | n개 |
| 소요 시간 | n초 |

## Failed Test Details

실패한 테스트가 있으면:

```bash
grep -A 10 "FAILURE!" target/surefire-reports/*.txt 2>/dev/null
```

## Instructions for Claude

1. 인수 확인: unit/it/e2e/클래스명
2. 적절한 Maven 명령어 실행
3. 결과 요약 테이블 생성
4. 실패한 테스트 있으면 상세 내용 표시
5. 실패 원인 분석 및 해결 방법 제안

## Common Test Patterns

| 패턴 | 설명 |
|------|------|
| `*Test.java` | 단위 테스트 |
| `*IT.java` | 통합 테스트 |
| `*E2ETest.java` | E2E 테스트 |

## Quick Check

빠른 빌드 검증 (테스트 스킵):

```bash
./mvnw clean compile -q && echo "BUILD OK" || echo "BUILD FAILED"
```
