#!/bin/bash
# AWS Secrets Manager 설정 스크립트
# 사용법: ./setup-secrets.sh

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Maruweb AWS Secrets Manager 설정 ===${NC}"
echo ""

# AWS 리전 확인
AWS_REGION=${AWS_REGION:-ap-northeast-2}
echo -e "AWS Region: ${YELLOW}$AWS_REGION${NC}"
echo ""

# 입력 받기
echo -e "${GREEN}1. 데이터베이스 설정${NC}"
read -p "MariaDB 호스트 (예: 10.0.21.x): " DB_HOST
read -p "maruweb DB 사용자명 [maruweb]: " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-maruweb}
read -sp "maruweb DB 비밀번호: " DB_PASSWORD
echo ""
read -p "trading DB 사용자명 [trading]: " TRADING_DB_USERNAME
TRADING_DB_USERNAME=${TRADING_DB_USERNAME:-trading}
read -sp "trading DB 비밀번호: " TRADING_DB_PASSWORD
echo ""
echo ""

echo -e "${GREEN}2. Google OAuth 설정${NC}"
read -p "Google Client ID: " GOOGLE_CLIENT_ID
read -sp "Google Client Secret: " GOOGLE_CLIENT_SECRET
echo ""
read -p "OAuth Redirect URI (예: https://your-domain.com/calendar/oauth2/callback): " OAUTH_REDIRECT_URI
echo ""

echo -e "${GREEN}3. 암호화 키 설정${NC}"
read -p "암호화 키 (32자 이상, 엔터시 자동 생성): " ENCRYPTION_KEY
if [ -z "$ENCRYPTION_KEY" ]; then
    ENCRYPTION_KEY=$(openssl rand -base64 32)
    echo -e "생성된 암호화 키: ${YELLOW}$ENCRYPTION_KEY${NC}"
fi
echo ""

# 확인
echo -e "${YELLOW}=== 입력 내용 확인 ===${NC}"
echo "DB 호스트: $DB_HOST"
echo "maruweb DB 사용자: $DB_USERNAME"
echo "trading DB 사용자: $TRADING_DB_USERNAME"
echo "Google Client ID: ${GOOGLE_CLIENT_ID:0:20}..."
echo "OAuth Redirect URI: $OAUTH_REDIRECT_URI"
echo ""

read -p "위 정보로 시크릿을 생성하시겠습니까? (y/N): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "취소되었습니다."
    exit 0
fi

echo ""
echo -e "${GREEN}시크릿 생성 중...${NC}"

# maruweb DB 자격 증명
echo "  - maruweb/db-credentials 생성..."
aws secretsmanager create-secret \
    --name maruweb/db-credentials \
    --region $AWS_REGION \
    --secret-string "{
        \"url\": \"jdbc:mariadb://${DB_HOST}:3306/maruweb?createDatabaseIfNotExist=true\",
        \"username\": \"${DB_USERNAME}\",
        \"password\": \"${DB_PASSWORD}\"
    }" 2>/dev/null || \
aws secretsmanager update-secret \
    --secret-id maruweb/db-credentials \
    --region $AWS_REGION \
    --secret-string "{
        \"url\": \"jdbc:mariadb://${DB_HOST}:3306/maruweb?createDatabaseIfNotExist=true\",
        \"username\": \"${DB_USERNAME}\",
        \"password\": \"${DB_PASSWORD}\"
    }"

# trading DB 자격 증명
echo "  - maruweb/trading-db-credentials 생성..."
aws secretsmanager create-secret \
    --name maruweb/trading-db-credentials \
    --region $AWS_REGION \
    --secret-string "{
        \"url\": \"jdbc:mariadb://${DB_HOST}:3306/trading?createDatabaseIfNotExist=true\",
        \"username\": \"${TRADING_DB_USERNAME}\",
        \"password\": \"${TRADING_DB_PASSWORD}\"
    }" 2>/dev/null || \
aws secretsmanager update-secret \
    --secret-id maruweb/trading-db-credentials \
    --region $AWS_REGION \
    --secret-string "{
        \"url\": \"jdbc:mariadb://${DB_HOST}:3306/trading?createDatabaseIfNotExist=true\",
        \"username\": \"${TRADING_DB_USERNAME}\",
        \"password\": \"${TRADING_DB_PASSWORD}\"
    }"

# Google OAuth
echo "  - maruweb/google-oauth 생성..."
aws secretsmanager create-secret \
    --name maruweb/google-oauth \
    --region $AWS_REGION \
    --secret-string "{
        \"clientId\": \"${GOOGLE_CLIENT_ID}\",
        \"clientSecret\": \"${GOOGLE_CLIENT_SECRET}\",
        \"redirectUri\": \"${OAUTH_REDIRECT_URI}\"
    }" 2>/dev/null || \
aws secretsmanager update-secret \
    --secret-id maruweb/google-oauth \
    --region $AWS_REGION \
    --secret-string "{
        \"clientId\": \"${GOOGLE_CLIENT_ID}\",
        \"clientSecret\": \"${GOOGLE_CLIENT_SECRET}\",
        \"redirectUri\": \"${OAUTH_REDIRECT_URI}\"
    }"

# 암호화 키
echo "  - maruweb/encryption-key 생성..."
aws secretsmanager create-secret \
    --name maruweb/encryption-key \
    --region $AWS_REGION \
    --secret-string "{\"value\": \"${ENCRYPTION_KEY}\"}" 2>/dev/null || \
aws secretsmanager update-secret \
    --secret-id maruweb/encryption-key \
    --region $AWS_REGION \
    --secret-string "{\"value\": \"${ENCRYPTION_KEY}\"}"

echo ""
echo -e "${GREEN}=== 완료! ===${NC}"
echo ""
echo "생성된 시크릿 목록:"
aws secretsmanager list-secrets --region $AWS_REGION \
    --filter Key=name,Values=maruweb \
    --query 'SecretList[].Name' --output table
