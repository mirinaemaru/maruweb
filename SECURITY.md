# Security Guide

## Overview

This document provides security guidelines for the Maru Web application, including how to handle sensitive information, manage credentials, and follow security best practices.

## Sensitive Information

### Database Credentials
- **Location**: Environment variables (`DB_USERNAME`, `DB_PASSWORD`)
- **Risk Level**: 🔴 HIGH
- **Never** commit database credentials to Git
- Use strong passwords (minimum 16 characters with mixed case, numbers, and symbols)

### Google OAuth2 Credentials
- **Location**: Environment variables (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`)
- **Risk Level**: 🟡 MEDIUM
- Obtain from [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
- Restrict API usage to authorized domains only

### Encryption Keys
- **Location**: Environment variable (`CALENDAR_ENCRYPTION_KEY`)
- **Risk Level**: 🔴 HIGH
- Must be at least 32 characters long
- Use cryptographically secure random generation

## Setup Instructions

### 1. Initial Setup

```bash
# Copy the environment template
cp .env.example .env

# Generate a secure encryption key
openssl rand -base64 32

# Edit .env and fill in actual values
nano .env
```

### 2. Environment Variables

Create a `.env` file in the project root with the following structure:

```bash
# Database
DB_USERNAME=your_username
DB_PASSWORD=your_strong_password

# Google OAuth2
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret

# Encryption Key (generate with: openssl rand -base64 32)
CALENDAR_ENCRYPTION_KEY=Xy9mK2pQr8tUv3wY6zAb1cDe2fGh4iJk5lMn7oPq0rS
```

### 3. Spring Profiles

Use different profiles for different environments:

**Local Development:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Development Server:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Production:**
```bash
java -jar -Dspring.profiles.active=prod maruweb.jar
```

## Security Best Practices

### Password Requirements

✅ **Strong Password Checklist:**
- Minimum 16 characters
- Mix of uppercase and lowercase letters
- Include numbers and special characters
- Avoid dictionary words or personal information
- Use a password manager

### Key Rotation

Regularly rotate sensitive credentials:
- Database passwords: Every 90 days
- API keys: Every 6 months
- Encryption keys: Every year or after security incident

### Git Commit Safety

Before committing, always check:

```bash
# Check what files will be committed
git status

# Review changes
git diff

# Ensure no sensitive files are staged
git diff --cached
```

Files that should **NEVER** be committed:
- `.env`
- `application-local.properties`
- Any file with passwords, API keys, or tokens
- `*.key`, `*.pem`, `*.p12`, `*.jks`

## Environment-Specific Configuration

### Local Development (application-local.properties)
- Contains local development settings
- Uses environment variables with safe defaults
- **MUST NOT** be committed to Git

### Development (application-dev.properties)
- For shared development environment
- Contains non-sensitive configuration only
- Safe to commit to Git

### Production (application-prod.properties)
- For production environment
- Contains non-sensitive configuration only
- Uses environment variables for all secrets
- Safe to commit to Git

## Generating Secure Keys

### Encryption Key (32+ characters)

**Using OpenSSL:**
```bash
openssl rand -base64 32
```

**Using Python:**
```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Strong Password

**Using OpenSSL:**
```bash
openssl rand -base64 24
```

**Using pwgen (if installed):**
```bash
pwgen -s 24 1
```

## Security Checklist

### Before First Run
- [ ] Copy `.env.example` to `.env`
- [ ] Generate strong database password
- [ ] Generate 32-character encryption key
- [ ] Set up Google OAuth2 credentials
- [ ] Verify `.env` is in `.gitignore`
- [ ] Test application startup

### Before Deployment
- [ ] All secrets in environment variables
- [ ] Strong passwords in use
- [ ] Encryption keys generated
- [ ] No hardcoded credentials in code
- [ ] `.gitignore` properly configured
- [ ] Security scan passed

### Regular Maintenance
- [ ] Review access logs monthly
- [ ] Rotate credentials quarterly
- [ ] Update dependencies monthly
- [ ] Security audit annually

## Incident Response

If credentials are accidentally committed:

1. **Immediately** rotate the exposed credentials
2. Remove from Git history:
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch path/to/file" \
     --prune-empty --tag-name-filter cat -- --all
   ```
3. Force push to remote (coordinate with team)
4. Notify affected services
5. Document the incident

## Contact

For security concerns or to report vulnerabilities, contact the project maintainer.

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Boot Security Best Practices](https://spring.io/guides/topicals/spring-security-architecture)
- [Google OAuth2 Documentation](https://developers.google.com/identity/protocols/oauth2)
