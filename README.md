# Maru Todo Web Application

Spring Boot 기반의 Todo 관리 웹 애플리케이션입니다.

## 기술 스택

- **Backend**: Spring Boot 3.2.1
- **Database**: MariaDB
- **ORM**: Spring Data JPA
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven
- **Language**: Java 17

## 프로젝트 구조

```
maruweb/
├── src/
│   ├── main/
│   │   ├── java/com/maru/todo/
│   │   │   ├── controller/      # 컨트롤러 (웹 요청 처리)
│   │   │   ├── service/          # 비즈니스 로직
│   │   │   ├── repository/       # 데이터베이스 접근
│   │   │   ├── entity/           # JPA 엔티티
│   │   │   └── TodoApplication.java
│   │   └── resources/
│   │       ├── templates/        # Thymeleaf 템플릿
│   │       ├── static/css/       # CSS 파일
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## 주요 기능

- ✅ Todo 추가 (제목, 설명)
- ✅ Todo 수정
- ✅ Todo 삭제
- ✅ Todo 완료 상태 토글
- ✅ Todo 필터링 (전체/활성/완료)
- ✅ 반응형 UI 디자인

## 설치 및 실행 방법

### 1. MariaDB 설정

MariaDB가 설치되어 있어야 합니다. 데이터베이스는 자동으로 생성됩니다.

기본 설정:
- Host: localhost
- Port: 3306
- Database: tododb (자동 생성)
- Username: root
- Password: (비어있음)

비밀번호나 사용자명이 다른 경우 `src/main/resources/application.properties` 파일을 수정하세요:

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 2. 프로젝트 실행

```bash
# Maven으로 빌드 및 실행
./mvnw spring-boot:run

# 또는 직접 빌드 후 실행
./mvnw clean package
java -jar target/todo-0.0.1-SNAPSHOT.jar
```

### 3. 애플리케이션 접속

브라우저에서 다음 주소로 접속:
```
http://localhost:8080
```

## API 엔드포인트

### 웹 페이지
- `GET /` - 홈 (todos로 리다이렉트)
- `GET /todos` - Todo 목록 (필터: all/active/completed)
- `GET /todos/{id}/edit` - Todo 수정 페이지

### Todo 작업
- `POST /todos` - Todo 생성
- `POST /todos/{id}` - Todo 수정
- `POST /todos/{id}/toggle` - Todo 완료 상태 토글
- `POST /todos/{id}/delete` - Todo 삭제

## 확장 계획

이 프로젝트는 추후 다른 기능의 메뉴를 추가할 수 있도록 확장 가능한 구조로 설계되었습니다.

새로운 기능 추가 시:
1. `com.maru.todo` 패키지에 새로운 기능별 패키지 생성
2. Entity, Repository, Service, Controller 계층 구조 유지
3. Thymeleaf 템플릿을 `templates/` 하위에 기능별로 구성

## 개발 환경

- JDK 17 이상
- Maven 3.6 이상
- MariaDB 10.x 이상
- IDE: IntelliJ IDEA, Eclipse, VS Code 등

## 라이센스

MIT License
