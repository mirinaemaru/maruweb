# Nginx Reverse Proxy 설정 가이드

Mac Mini 홈 서버에서 Nginx를 reverse proxy로 사용하여 HTTPS를 지원합니다.

## 설치

```bash
# Nginx 설치
brew install nginx

# Certbot 설치 (Let's Encrypt SSL)
brew install certbot
```

## 설정

### 1. Nginx 설정 파일 복사

```bash
cp maruweb.conf /opt/homebrew/etc/nginx/servers/maruweb.conf
```

### 2. 도메인 설정

`maruweb.conf`에서 `your-domain.com`을 실제 도메인으로 변경합니다.

### 3. SSL 인증서 발급

```bash
# Certbot으로 인증서 발급 (DNS 인증 방식)
sudo certbot certonly --manual --preferred-challenges dns -d your-domain.com

# 또는 standalone 방식 (80 포트가 비어있을 때)
sudo certbot certonly --standalone -d your-domain.com
```

### 4. Nginx 시작

```bash
# 설정 테스트
nginx -t

# 시작
brew services start nginx

# 재시작
brew services restart nginx
```

## SSL 인증서 자동 갱신

```bash
# crontab에 추가
# 매달 1일 새벽 3시에 인증서 갱신 시도
0 3 1 * * certbot renew --quiet && brew services restart nginx
```

## 포트 포워딩

공유기에서 다음 포트를 Mac Mini IP로 포워딩해야 합니다:
- **80** (HTTP → HTTPS 리다이렉트)
- **443** (HTTPS)

## 상태 확인

```bash
# Nginx 프로세스 확인
brew services list | grep nginx

# 설정 문법 확인
nginx -t

# 로그 확인
tail -f /opt/homebrew/var/log/nginx/error.log
tail -f /opt/homebrew/var/log/nginx/access.log
```

## 문제 해결

### 502 Bad Gateway
- Spring Boot 앱이 8090 포트에서 실행 중인지 확인: `lsof -ti:8090`

### SSL 인증서 만료
- `certbot renew` 실행 후 `brew services restart nginx`
