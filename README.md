# Maru Web - Management System

A comprehensive web-based management system built with Spring Boot and Thymeleaf.

## Features

- 📝 **Todo Management**: Task tracking with status and description
- 📅 **Calendar**: Event management with Google Calendar integration
- 📒 **Notes**: Note-taking and organization
- 🔗 **Shortcuts**: Quick access links management
- 📊 **Trading Dashboard**: Trading system monitoring and management
  - System health monitoring
  - Account management
  - Strategy management
  - Order tracking

## Tech Stack

- **Backend**: Spring Boot 3.x, Java 17
- **Frontend**: Thymeleaf, HTML5, CSS3
- **Database**: MariaDB
- **Build Tool**: Maven

## Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- MariaDB 10.x or higher

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/mirinaemaru/maruweb.git
cd maruweb
```

### 2. Setup Environment Variables

```bash
# Copy the environment template
cp .env.example .env

# Generate a secure encryption key
openssl rand -base64 32

# Edit .env and fill in your actual values
nano .env
```

Required environment variables:
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password (use a strong password)
- `GOOGLE_CLIENT_ID`: Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth2 client secret
- `CALENDAR_ENCRYPTION_KEY`: 32-character encryption key

### 3. Configure Database

Create a MariaDB database:

```sql
CREATE DATABASE maruweb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'your_username'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON maruweb.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;
```

### 4. Run the Application

**Development mode:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Production mode:**
```bash
./mvnw clean package -DskipTests
java -jar -Dspring.profiles.active=prod target/todo-0.0.1-SNAPSHOT.jar
```

### 5. Access the Application

- **Development**: http://localhost:8090
- **Production**: http://localhost:9080

## Project Structure

```
maruweb/
├── src/
│   ├── main/
│   │   ├── java/com/maru/
│   │   │   ├── calendar/      # Calendar module
│   │   │   ├── config/        # Configuration classes
│   │   │   ├── dday/          # D-Day module
│   │   │   ├── habit/         # Habit tracking
│   │   │   ├── note/          # Notes module
│   │   │   ├── shortcut/      # Shortcuts module
│   │   │   ├── todo/          # Todo module
│   │   │   └── trading/       # Trading system integration
│   │   └── resources/
│   │       ├── static/        # CSS, JS, images
│   │       ├── templates/     # Thymeleaf templates
│   │       └── application*.properties
│   └── test/
├── .env.example              # Environment variables template
├── SECURITY.md              # Security guidelines
└── pom.xml                  # Maven configuration
```

## Security

**⚠️ IMPORTANT: Never commit sensitive information to Git!**

This project uses environment variables for sensitive data. See [SECURITY.md](SECURITY.md) for:
- Security best practices
- Environment setup guide
- Credential rotation policies
- Incident response procedures

## Development

### Running Tests

```bash
./mvnw test
```

### Building

```bash
./mvnw clean package
```

### Profiles

- `local`: Local development (uses environment variables)
- `dev`: Development server
- `prod`: Production environment

## Deployment

### Using Jenkins

The project includes a Jenkins deployment skill. See `.claude/skills/deploy/` for configuration.

### Manual Deployment

1. Build the application:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. Set environment variables on the server

3. Run the JAR:
   ```bash
   java -jar -Dspring.profiles.active=prod target/todo-0.0.1-SNAPSHOT.jar
   ```

## Trading System Integration

The application integrates with an external Trading System API (default: http://localhost:8099).

Features:
- Real-time system health monitoring
- Account management (PAPER/REAL accounts)
- Strategy management with statistics
- Order tracking and filtering

## Google Calendar Integration

To enable Google Calendar sync:

1. Create OAuth2 credentials at [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Set authorized redirect URIs:
   - Development: `http://localhost:8090/calendar/oauth2/callback`
   - Production: `http://localhost:9080/calendar/oauth2/callback`
3. Add credentials to `.env` file

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is private and proprietary.

## Support

For issues and questions, please contact the project maintainer.
