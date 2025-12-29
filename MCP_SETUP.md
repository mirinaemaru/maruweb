# MCP Server 설정 가이드

이 프로젝트에 설치된 MCP (Model Context Protocol) 서버 목록과 사용법입니다.

## 설치된 MCP 서버

### 1. MySQL MCP ✅
**상태**: 설치됨 (Claude Code 재시작 필요)

**기능**:
- 데이터베이스 쿼리 자동 실행
- 테이블 구조 조회
- 데이터 검색 및 수정

**사용 예시**:
```
"maruweb 데이터베이스의 todos 테이블에서 완료되지 않은 항목 조회해줘"
"calendar_events 테이블 구조 보여줘"
```

**연결 정보**:
- Host: localhost:3306
- Database: maruweb
- User: nextman

### 2. Git MCP ✅
**상태**: 설치됨 (Claude Code 재시작 필요)

**기능**:
- Git 상태 조회
- 커밋 히스토리 분석
- 브랜치 관리
- 변경사항 diff 조회
- 더 스마트한 커밋 메시지 생성

**사용 예시**:
```
"최근 10개 커밋 보여줘"
"현재 브랜치의 변경사항 확인해줘"
"main 브랜치와의 차이점 보여줘"
```

**자동 승인 설정됨**:
- `git status`, `git log`, `git diff`, `git show`
- `git branch`, `git checkout`, `git pull`
- `git add`, `git commit`, `git push`

## MCP 활성화 방법

⚠️ **중요**: MCP를 사용하려면 Claude Code를 재시작해야 합니다.

```bash
# 현재 Claude Code 세션 종료 후 다시 시작
# 또는 터미널에서:
claude --restart
```

## MCP 확인 방법

```bash
# 설치된 MCP 목록 확인
claude mcp list

# MCP 상태 확인
cat ~/.claude.json
```

## 추천 MCP 서버 (미설치)

### Filesystem MCP
대량의 파일 작업 시 유용

```bash
claude mcp add filesystem "npx -y @modelcontextprotocol/server-filesystem"
```

### Brave Search MCP
기술 문서 검색, Stack Overflow 답변 찾기

```bash
claude mcp add brave-search "npx -y @modelcontextprotocol/server-brave-search"
```

**주의**: Brave Search API 키 필요
- https://brave.com/search/api/

### GitHub MCP
GitHub 이슈, PR 관리

```bash
claude mcp add github "npx -y @modelcontextprotocol/server-github"
```

**주의**: GitHub Personal Access Token 필요

## 문제 해결

### MCP가 작동하지 않을 때

1. **Claude Code 재시작**
   ```bash
   # 세션 종료 후 다시 시작
   ```

2. **MCP 설정 확인**
   ```bash
   cat ~/.claude.json
   ```

3. **Node.js 버전 확인**
   ```bash
   node --version  # v18 이상 필요
   ```

4. **MCP 로그 확인**
   ```bash
   # Claude Code의 디버그 모드로 실행
   claude --verbose
   ```

### MySQL MCP 연결 실패

1. **MariaDB 실행 확인**
   ```bash
   brew services list | grep mariadb
   mysql -u nextman -p1111 maruweb -e "SELECT 1"
   ```

2. **연결 문자열 확인**
   ```bash
   grep mysql ~/.claude.json
   ```

## MCP 성능 팁

### 1. 자주 사용하는 명령어는 자동 승인 설정
`.claude/settings.local.json`에 추가:
```json
{
  "permissions": {
    "allow": [
      "Bash(your-command:*)"
    ]
  }
}
```

### 2. MCP 캐시 활용
MCP는 쿼리 결과를 캐싱하므로 반복 쿼리가 빠릅니다.

### 3. 복잡한 작업은 MCP 조합
Git + MySQL MCP를 함께 사용:
```
"최근 커밋에서 변경된 파일의 관련 데이터베이스 레코드 보여줘"
```

## 참고 자료

- [MCP 공식 문서](https://modelcontextprotocol.io/)
- [MCP 서버 목록](https://github.com/modelcontextprotocol/servers)
- [Claude Code 문서](https://claude.com/claude-code)
