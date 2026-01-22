#!/bin/bash

#########################################
# Maruweb Server Restart Script
# Graceful Shutdown + Start
#
# 용도: cron을 통한 정기 재시작
# 스케줄: 매일 00:05 (자정 5분 후)
#########################################

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="/var/logs/trading/restart.log"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" >> "$LOG_FILE"
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log "========================================="
log "Starting scheduled restart..."
log "========================================="

cd "$SCRIPT_DIR"

# Graceful shutdown
log "Step 1: Stopping server (graceful shutdown)..."
./stop-maruweb.sh >> "$LOG_FILE" 2>&1
STOP_RESULT=$?

if [ $STOP_RESULT -eq 0 ]; then
    log "Server stopped successfully"
else
    log "Warning: Stop script returned code $STOP_RESULT"
fi

# Wait a moment before restart
sleep 3

# Start server
log "Step 2: Starting server..."
./run-maruweb.sh >> "$LOG_FILE" 2>&1
START_RESULT=$?

if [ $START_RESULT -eq 0 ]; then
    log "Server started successfully"
    log "Restart completed!"
else
    log "ERROR: Server failed to start (code: $START_RESULT)"
fi

log "========================================="
