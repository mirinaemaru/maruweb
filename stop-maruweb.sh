#!/bin/bash

#########################################
# Maruweb Local Development Server
# 로컬 개발 서버 중지 스크립트
# Graceful Shutdown 지원
#########################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Graceful shutdown timeout (seconds)
SHUTDOWN_TIMEOUT=35

echo "========================================="
echo "  Maruweb Server Stop Script"
echo "  (Graceful Shutdown)"
echo "========================================="
echo ""

# Find process on port 8090
PID=$(lsof -ti:8090 2>/dev/null || true)

if [ -z "$PID" ]; then
    echo -e "${YELLOW}⚠ No server running on port 8090${NC}"
    exit 0
fi

echo "Found server process: $PID"
echo "Initiating graceful shutdown..."
echo ""

# Send SIGTERM for graceful shutdown
kill -TERM $PID 2>/dev/null

# Wait for graceful shutdown
echo "Waiting for server to finish processing requests..."
for i in $(seq 1 $SHUTDOWN_TIMEOUT); do
    if ! lsof -ti:8090 &>/dev/null; then
        echo ""
        echo -e "${GREEN}✓ Server stopped gracefully${NC}"
        echo ""
        echo "Server logs are available at: /var/logs/trading/maruweb.log"
        exit 0
    fi
    echo -n "."
    sleep 1
done

# If graceful shutdown didn't work, force kill
echo ""
echo -e "${YELLOW}⚠ Graceful shutdown timeout. Force stopping...${NC}"
kill -9 $PID 2>/dev/null || true
sleep 1

# Verify it's stopped
if lsof -ti:8090 &>/dev/null; then
    echo -e "${RED}✗ Failed to stop server${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Server force stopped${NC}"
fi

echo ""
echo "Server logs are available at: /var/logs/trading/maruweb.log"
