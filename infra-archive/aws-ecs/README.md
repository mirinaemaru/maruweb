# AWS ECS Fargate 배포 가이드

이 디렉토리에는 maruweb 애플리케이션을 AWS ECS Fargate에 배포하기 위한 CloudFormation 템플릿이 포함되어 있습니다.

## 아키텍처

```
Internet → Route53 → ALB (HTTPS) → ECS Fargate
                                      ├── maruweb (포트 8090) ──┐
                                      └── trading-api (8099) ───┼──→ EC2 MariaDB
                                                                │
                                      EFS (파일 업로드) ←───────┘
```

## 예상 월 비용

| 리소스 | 비용 |
|--------|------|
| NAT Instance (t3.micro) | ~$8 |
| EC2 MariaDB (t3.medium) | ~$35 |
| ECS Fargate (maruweb 2개) | ~$40 |
| ECS Fargate (trading-api 1개) | ~$15 |
| ALB | ~$20 |
| EFS | ~$5 |
| CloudWatch | ~$5 |
| 기타 | ~$10 |
| **합계** | **~$140** |

> Fargate Spot 사용 시 추가 절감 가능

## 배포 순서

### 1. 사전 준비

```bash
# AWS CLI 설정
aws configure

# 리전 설정 (서울)
export AWS_REGION=ap-northeast-2
```

### 2. Secrets Manager에 시크릿 생성

```bash
# 데이터베이스 자격 증명
aws secretsmanager create-secret \
    --name maruweb/db-credentials \
    --secret-string '{
        "url": "jdbc:mariadb://YOUR_DB_HOST:3306/maruweb",
        "username": "maruweb",
        "password": "YOUR_DB_PASSWORD"
    }'

# Trading API 데이터베이스 (새로 시작)
aws secretsmanager create-secret \
    --name maruweb/trading-db-credentials \
    --secret-string '{
        "url": "jdbc:mariadb://YOUR_DB_HOST:3306/trading",
        "username": "trading",
        "password": "YOUR_TRADING_DB_PASSWORD"
    }'

# Google OAuth 자격 증명
aws secretsmanager create-secret \
    --name maruweb/google-oauth \
    --secret-string '{
        "clientId": "YOUR_GOOGLE_CLIENT_ID",
        "clientSecret": "YOUR_GOOGLE_CLIENT_SECRET",
        "redirectUri": "https://your-domain.com/calendar/oauth2/callback"
    }'

# 암호화 키
aws secretsmanager create-secret \
    --name maruweb/encryption-key \
    --secret-string '{"value": "YOUR_32_CHAR_ENCRYPTION_KEY"}'
```

### 3. CloudFormation 스택 배포

```bash
# 1. VPC 및 네트워크
aws cloudformation create-stack \
    --stack-name maruweb-vpc \
    --template-body file://01-vpc.yaml \
    --capabilities CAPABILITY_NAMED_IAM

# 완료 대기
aws cloudformation wait stack-create-complete --stack-name maruweb-vpc

# 2. 보안 그룹
aws cloudformation create-stack \
    --stack-name maruweb-security-groups \
    --template-body file://02-security-groups.yaml

aws cloudformation wait stack-create-complete --stack-name maruweb-security-groups

# 3. ECS 클러스터 및 ECR
aws cloudformation create-stack \
    --stack-name maruweb-ecs-cluster \
    --template-body file://03-ecs-cluster.yaml \
    --capabilities CAPABILITY_NAMED_IAM

aws cloudformation wait stack-create-complete --stack-name maruweb-ecs-cluster

# 4. ALB (SSL 인증서 없이 먼저 배포)
aws cloudformation create-stack \
    --stack-name maruweb-alb \
    --template-body file://04-alb.yaml

aws cloudformation wait stack-create-complete --stack-name maruweb-alb
```

### 4. EC2 MariaDB 설정

DB 서브넷에 EC2 인스턴스를 수동으로 생성하고 MariaDB를 설치합니다.

```bash
# EC2에 SSH 접속 후
sudo yum install -y mariadb105-server
sudo systemctl start mariadb
sudo systemctl enable mariadb
sudo mysql_secure_installation

# 데이터베이스 및 사용자 생성
mysql -u root -p << EOF
CREATE DATABASE maruweb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE trading CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'maruweb'@'10.0.%' IDENTIFIED BY 'YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON maruweb.* TO 'maruweb'@'10.0.%';

CREATE USER 'trading'@'10.0.%' IDENTIFIED BY 'YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON trading.* TO 'trading'@'10.0.%';

FLUSH PRIVILEGES;
EOF
```

### 5. Docker 이미지 빌드 및 푸시

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
    docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com

# maruweb 이미지 빌드 및 푸시
docker build -t maruweb/app .
docker tag maruweb/app:latest YOUR_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/maruweb/app:latest
docker push YOUR_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/maruweb/app:latest

# trading-api도 동일하게 (별도 저장소에서)
```

### 6. ECS 서비스 배포

```bash
aws cloudformation create-stack \
    --stack-name maruweb-ecs-services \
    --template-body file://05-ecs-services.yaml \
    --capabilities CAPABILITY_NAMED_IAM

aws cloudformation wait stack-create-complete --stack-name maruweb-ecs-services
```

### 7. SSL 인증서 설정 (선택)

```bash
# ACM에서 인증서 요청
aws acm request-certificate \
    --domain-name your-domain.com \
    --validation-method DNS

# DNS 검증 완료 후 ALB 스택 업데이트
aws cloudformation update-stack \
    --stack-name maruweb-alb \
    --template-body file://04-alb.yaml \
    --parameters ParameterKey=CertificateArn,ParameterValue=arn:aws:acm:ap-northeast-2:xxx:certificate/xxx
```

### 8. DNS 설정

Route53 또는 외부 DNS에서 ALB DNS를 가리키는 A 레코드(별칭) 또는 CNAME을 추가합니다.

```bash
# ALB DNS 확인
aws cloudformation describe-stacks --stack-name maruweb-alb \
    --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text
```

## 배포 후 검증

```bash
# ECS 서비스 상태 확인
aws ecs describe-services \
    --cluster maruweb-cluster \
    --services maruweb-service trading-api-service

# 로그 확인
aws logs tail /ecs/maruweb --follow

# 헬스체크
curl https://your-domain.com/actuator/health
```

## 문제 해결

### ECS 태스크가 시작되지 않는 경우

1. CloudWatch 로그 확인: `/ecs/maruweb`
2. Secrets Manager 권한 확인
3. 보안 그룹 규칙 확인

### 데이터베이스 연결 실패

1. EC2 MariaDB 보안 그룹에서 ECS 보안 그룹 허용 확인
2. Secrets Manager의 DB URL이 올바른지 확인
3. 데이터베이스 사용자 권한 확인

### Trading API 연결 실패

1. Cloud Map DNS 확인: `trading-api.maruweb.local`
2. ECS 보안 그룹 간 통신 규칙 확인

## GitHub Actions 설정

GitHub Secrets에 다음 값을 추가하세요:

- `AWS_ACCESS_KEY_ID`: IAM 사용자 Access Key
- `AWS_SECRET_ACCESS_KEY`: IAM 사용자 Secret Key

배포 권한이 있는 IAM 정책:
- AmazonEC2ContainerRegistryPowerUser
- AmazonECS_FullAccess
- SecretsManagerReadWrite
