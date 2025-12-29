# Restart Server Skill

Restart the local development server (maruweb) with proper environment variables.

## When to Use

Use this skill when:
- User asks to "restart server", "서버 재시작", "재시작해줘"
- After making code changes that require server restart (Java, properties files)
- After committing and pushing changes for local testing
- When server is not responding or needs fresh start

## Environment

- **Server Port**: 8090 (local development)
- **Profile**: local
- **Log File**: /tmp/maruweb.log
- **Environment Variables**: Loaded from .env file in project root

## Steps

### 1. Stop Existing Server

```bash
# Find and kill process on port 8090
lsof -ti:8090 | xargs kill -9 2>/dev/null
sleep 2
```

### 2. Load Environment Variables

Read from `.env` file:
- `DB_USERNAME=nextman`
- `DB_PASSWORD=1111`
- `DB_URL=jdbc:mariadb://localhost:3306/maruweb?createDatabaseIfNotExist=true`
- `GOOGLE_CLIENT_ID` (OAuth credentials)
- `GOOGLE_CLIENT_SECRET` (OAuth credentials)
- `CALENDAR_ENCRYPTION_KEY` (Token encryption)
- `TRADING_API_BASE_URL=http://localhost:8099`
- `SPRING_PROFILES_ACTIVE=local`

### 3. Start Server

Read environment variables from `.env` file and start server:

```bash
# Read from .env file
source <(grep -v '^#' .env | sed 's/^/export /')

# Start server with all required environment variables
SPRING_PROFILES_ACTIVE=local \
DB_USERNAME="${DB_USERNAME}" \
DB_PASSWORD="${DB_PASSWORD}" \
DB_URL="${DB_URL}" \
GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
CALENDAR_ENCRYPTION_KEY="${CALENDAR_ENCRYPTION_KEY}" \
TRADING_API_BASE_URL="${TRADING_API_BASE_URL}" \
nohup ./mvnw spring-boot:run > /tmp/maruweb.log 2>&1 &
```

**IMPORTANT**: Do NOT hardcode credentials. Always read from `.env` file.

### 4. Wait and Verify

```bash
# Wait for server startup (15 seconds)
sleep 15

# Check if server responds with HTTP 200
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/
```

### 5. Report Status

- ✓ If HTTP 200: "Server started successfully on http://localhost:8090"
- ✗ If not 200: Show last 30 lines of `/tmp/maruweb.log`

## Troubleshooting

### Server Fails to Start

1. Check logs: `tail -50 /tmp/maruweb.log`
2. Common issues:
   - Database connection failure (check MariaDB is running)
   - Port already in use (kill process manually)
   - Missing environment variables
   - Google OAuth token decryption errors

### Database Connection Issues

```bash
# Check MariaDB is running
mysql -u nextman -p1111 -e "SELECT 1"

# If not running, start MariaDB
brew services start mariadb
```

## Notes

- **DO NOT** ask user for environment variables - they are in .env file
- **DO NOT** use `./mvnw spring-boot:run` without environment variables
- **ALWAYS** wait at least 15 seconds before checking server status
- **ALWAYS** run in background with `nohup` and redirect logs to /tmp/maruweb.log
