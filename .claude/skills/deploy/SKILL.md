---
name: deploy
description: Deploy the application through Jenkins pipeline. Use when user asks to deploy, push to production, check deployment status, or verify app health.
allowed-tools: Bash, Read, Grep, Glob
---

# Deploy Skill

Mac Mini 홈 서버에 Jenkins 파이프라인을 통해 Maru Web 애플리케이션을 배포합니다.

## 환경 구성

| 환경 | 포트 | 프로파일 | 용도 |
|------|------|----------|------|
| dev | 9080 | dev | 개발/테스트 |
| prod | 8090 | prod | 프로덕션 (Mac Mini) |

**배포 디렉토리**: `/opt/maruweb/`
**업로드 디렉토리**: `/opt/maruweb/uploads/kanban/`
**로그 디렉토리**: `/opt/maruweb/logs/`

## Deployment Process

### 1. Pre-deployment Checks
- Verify current git status
- Check if there are uncommitted changes
- Ensure we're on the master branch

### 2. Build & Test
```bash
# Clean and build
./mvnw clean package -DskipTests

# Run tests
./mvnw test
```

### 3. Push to GitHub (triggers Jenkins)
```bash
# Push changes to trigger Jenkins pipeline
git push origin master
```

### 4. Monitor Jenkins Build
- Jenkins URL: http://localhost:9090/job/maruweb-local/
- Jenkins automatically:
  - Checks out code
  - Builds with Maven
  - Runs tests
  - Stops previous instance
  - Creates required directories (`/opt/maruweb/uploads/kanban`, `/opt/maruweb/logs`)
  - Deploys new JAR to /opt/maruweb/
  - Performs health check

### 5. Verify Deployment

**dev 환경:**
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:9080/
```

**prod 환경:**
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/
```

## Health Check Commands

```bash
# prod 헬스체크
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/actuator/health

# dev 헬스체크
curl -s -o /dev/null -w "%{http_code}" http://localhost:9080/

# Application logs
tail -f /opt/maruweb/application.log

# Check process
lsof -ti:8090  # prod
lsof -ti:9080  # dev

# Nginx status (prod)
brew services list | grep nginx
```

## Deployment Configuration

**Build Tool**: Maven 3.9
**Java Version**: JDK 17
**Artifact**: todo-0.0.1-SNAPSHOT.jar

## Environment Variables (Jenkins)
- `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`: Database credentials
- `GOOGLE_CLIENT_ID`: Google OAuth credentials
- `GOOGLE_CLIENT_SECRET`: Google OAuth credentials
- `CALENDAR_ENCRYPTION_KEY`: Calendar data encryption key

## Common Commands

### Quick Deploy
```bash
# Build, commit, push (triggers Jenkins)
./mvnw clean package -DskipTests && git add . && git commit -m "Deploy" && git push
```

### Manual Deploy (bypass Jenkins)
```bash
# Build
./mvnw clean package -DskipTests

# Stop current instance (prod)
kill -9 $(lsof -ti:8090)

# Start new instance (prod)
nohup java -jar target/todo-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8090 > /opt/maruweb/application.log 2>&1 &
```

## Troubleshooting

### Build Fails
- Check `mvn -v` for correct Java version (17)
- Review error logs in Jenkins console output
- Verify all tests pass: `./mvnw test`

### Deployment Fails
- Check port is not blocked: `lsof -ti:8090`
- Verify Jenkins credentials are configured
- Check disk space: `df -h`
- Review application logs: `tail -f /opt/maruweb/application.log`

### App Not Responding
- Check process is running: `ps aux | grep maruweb`
- Check logs: `tail -100 /opt/maruweb/application.log`
- Verify database connectivity
- Check environment variables are set
- Check Nginx: `nginx -t && brew services list | grep nginx`

## Instructions for Claude

When user asks to deploy:

1. **Check git status** - ensure all changes are committed
2. **Ask user confirmation** - "Ready to deploy to production?"
3. **Ask environment** - dev (9080) or prod (8090)?
4. **Run tests** - `./mvnw test` and verify they pass
5. **Push to GitHub** - This triggers Jenkins pipeline
6. **Wait 2-3 minutes** - Jenkins needs time to build and deploy
7. **Verify deployment** - Check appropriate port returns 200
8. **Report results** - Tell user deployment status and provide Jenkins URL

When user asks to check deployment:

1. **Check app health** - `curl http://localhost:8090/` (prod) or `curl http://localhost:9080/` (dev)
2. **Check Jenkins** - Provide Jenkins build URL
3. **Check logs** - `tail /opt/maruweb/application.log`
4. **Report status** - Running/Not Running, last build info
