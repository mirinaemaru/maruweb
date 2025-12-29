#!/bin/bash

#########################################
# Maruweb Local Development Server
# 로컬 개발 서버 중지 스크립트
#########################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "  Maruweb Server Stop Script"
echo "========================================="
echo ""

# Find process on port 8090
PID=$(lsof -ti:8090 2>/dev/null || true)

if [ -z "$PID" ]; then
    echo -e "${YELLOW}⚠ No server running on port 8090${NC}"
    exit 0
fi

echo "Found server process: $PID"
echo "Stopping server..."

# Kill the process
kill -9 $PID 2>/dev/null || true
sleep 1

# Verify it's stopped
if lsof -ti:8090 &>/dev/null; then
    echo -e "${RED}✗ Failed to stop server${NC}"
    exit 1
else
    echo -e "${GREEN}✓ Server stopped successfully${NC}"
fi

echo ""
echo "Server logs are available at: /tmp/maruweb.log"
