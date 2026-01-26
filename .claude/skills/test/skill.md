---
name: test
description: Run tests and analyze results. Use when user asks to test, run tests, 테스트, or verify code changes.
allowed-tools: Bash, Read, Grep, TaskOutput
---

# Test Skill

테스트를 실행하고 결과를 분석합니다. **실시간 진행 상황을 표시**합니다.

## Arguments

- (없음): 전체 테스트 실행
- `unit`: 단위 테스트만
- `it` 또는 `integration`: 통합 테스트만
- `e2e`: E2E 테스트만
- `<ClassName>`: 특정 테스트 클래스만

## Instructions for Claude (중요!)

### 실시간 테스트 실행 방법

**반드시 이 순서를 따를 것:**

1. **테스트를 백그라운드로 실행** (`run_in_background: true` 사용):
   ```bash
   ./mvnw test 2>&1 | tee /tmp/test-output.log
   ```

2. **실행 중 실시간 상태 확인** (3-5초 간격으로 반복):
   ```bash
   tail -30 /tmp/test-output.log 2>/dev/null || echo "테스트 시작 중..."
   ```

3. **테스트 완료 확인 방법**:
   - `BUILD SUCCESS` 또는 `BUILD FAILURE` 출력 확인
   - 또는 TaskOutput으로 백그라운드 작업 완료 확인

4. **완료 후 결과 요약 표시**

### Maven 명령어 (인수에 따라 선택)

| 인수 | 명령어 |
|------|--------|
| (없음) | `./mvnw test` (IT/E2E 자동 제외됨) |
| `unit` | `./mvnw test` (IT/E2E 자동 제외됨) |
| `it`, `integration` | `./mvnw failsafe:integration-test -Dit.test="*IT"` |
| `e2e` | `./mvnw failsafe:integration-test -Dit.test="*E2ETest"` |
| `<ClassName>` | `./mvnw test -Dtest="<ClassName>" -DfailIfNoTests=false` |

**참고:** pom.xml의 maven-surefire-plugin에서 `*IT.java`와 `*E2ETest.java`가 기본 제외됨

### 실시간 출력 형식

테스트 진행 중 다음 정보를 사용자에게 보여줄 것:

```
🧪 테스트 실행 중...

현재 실행 중인 테스트:
  [INFO] Running com.example.SomeTest

진행 상황:
  ✅ TodoServiceTest (3개 테스트 통과)
  ⏳ CalendarServiceTest (실행 중...)
```

### 결과 요약 테이블

테스트 완료 후:

| 항목 | 값 |
|------|-----|
| 총 테스트 | n개 |
| 성공 | n개 |
| 실패 | n개 |
| 스킵 | n개 |
| 소요 시간 | n초 |

## 실패 테스트 상세

실패한 테스트가 있으면:

```bash
grep -A 10 "FAILURE!" target/surefire-reports/*.txt 2>/dev/null
```

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

## 주의사항

- **절대로 `tail -50`으로 끝내지 말 것** - 실시간 진행 상황을 보여줘야 함
- 백그라운드 실행 + 주기적 tail 조합으로 실시간 모니터링
- 사용자에게 현재 어떤 테스트가 실행 중인지 알려줄 것
