# AWS ECS Fargate 인프라 아카이브

## 아카이브 사유

2026-02-13: AWS ECS Fargate 기반 운영 배포를 취소하고 Mac Mini 홈 서버로 전환.

비용 절감 및 운영 단순화를 위해 AWS 인프라를 더 이상 사용하지 않습니다.
Jenkins 파이프라인을 통한 로컬 Mac Mini 배포로 전환되었습니다.

## 포함된 파일

| 파일 | 설명 |
|------|------|
| `01-vpc.yaml` | VPC, 서브넷, NAT Gateway CloudFormation |
| `02-security-groups.yaml` | 보안 그룹 CloudFormation |
| `03-ecs-cluster.yaml` | ECS Cluster CloudFormation |
| `04-alb.yaml` | Application Load Balancer CloudFormation |
| `05-ecs-services.yaml` | ECS Service/Task Definition CloudFormation |
| `README.md` | 원본 인프라 문서 |
| `setup-secrets.sh` | AWS Secrets Manager 설정 스크립트 |
| `deploy-aws.yml` | GitHub Actions AWS 배포 워크플로우 |
| `Dockerfile` | ECS용 Docker 이미지 빌드 |
| `.dockerignore` | Docker 빌드 제외 파일 목록 |

## 복원 방법

AWS 배포로 다시 전환이 필요한 경우, 이 디렉토리의 파일들을 원래 위치로 되돌리면 됩니다.
