#!/bin/bash

# E2E 테스트 실행 스크립트
# 사용법: ./run-e2e-tests.sh [api|ui|all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 함수: cautostock 서버 대기
wait_for_cautostock() {
    echo -e "${YELLOW}Waiting for cautostock server...${NC}"
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:8099/health | grep -q "UP"; then
            echo -e "${GREEN}cautostock is ready!${NC}"
            return 0
        fi
        echo "Waiting... ($attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    echo -e "${RED}cautostock is not available!${NC}"
    return 1
}

# 함수: maruweb 서버 대기
wait_for_maruweb() {
    echo -e "${YELLOW}Waiting for maruweb server...${NC}"
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:8090 > /dev/null 2>&1; then
            echo -e "${GREEN}maruweb is ready!${NC}"
            return 0
        fi
        echo "Waiting... ($attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    echo -e "${RED}maruweb is not available!${NC}"
    return 1
}

# 함수: API E2E 테스트 실행
run_api_e2e() {
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}Running API E2E Tests${NC}"
    echo -e "${YELLOW}========================================${NC}"

    # cautostock 서버 확인
    if ! wait_for_cautostock; then
        echo -e "${RED}Please start cautostock server first:${NC}"
        echo "cd /Users/changsupark/projects/cautostock && ./gradlew bootRun"
        exit 1
    fi

    # API E2E 테스트 실행
    ./mvnw test -Dtest=*E2ETest -Dspring.profiles.active=e2e -B

    echo -e "${GREEN}API E2E Tests completed!${NC}"
}

# 함수: UI E2E 테스트 실행
run_ui_e2e() {
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}Running UI E2E Tests${NC}"
    echo -e "${YELLOW}========================================${NC}"

    # Node.js 의존성 확인
    if [ ! -d "node_modules" ]; then
        echo "Installing Node.js dependencies..."
        npm install
    fi

    # Playwright 브라우저 확인
    if [ ! -d "$HOME/.cache/ms-playwright" ]; then
        echo "Installing Playwright browsers..."
        npx playwright install chromium
    fi

    # maruweb 서버 확인
    if ! wait_for_maruweb; then
        echo -e "${RED}Please start maruweb server first:${NC}"
        echo "./run-maruweb.sh"
        exit 1
    fi

    # UI E2E 테스트 실행
    npx playwright test

    echo -e "${GREEN}UI E2E Tests completed!${NC}"
    echo -e "${YELLOW}Report: npx playwright show-report${NC}"
}

# 함수: 전체 E2E 테스트 실행
run_all_e2e() {
    run_api_e2e
    echo ""
    run_ui_e2e
}

# 메인 로직
case "${1:-all}" in
    api)
        run_api_e2e
        ;;
    ui)
        run_ui_e2e
        ;;
    all)
        run_all_e2e
        ;;
    *)
        echo "Usage: $0 [api|ui|all]"
        echo "  api  - Run API E2E tests only"
        echo "  ui   - Run UI E2E tests only"
        echo "  all  - Run all E2E tests (default)"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}E2E Tests Completed Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
