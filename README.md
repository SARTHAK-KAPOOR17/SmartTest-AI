# SmartTest AI — Self-Healing QA Automation Platform

## Overview

**SmartTest AI** is an intelligent, self-healing quality assurance and test automation platform designed to simplify web application testing, eliminate fragile locator maintenance, and automatically heal broken test scripts during execution.

This repository represents the production-grade modular monolith backbone securing multi-tenant test automation projects through **Phase 1: Project Foundation** and **Phase 2: User Authentication, Authorization & Project Ownership**.

---

## Implemented Phases

### Phase 1 — Project Foundation
- **Modular Monolith Backbone**: Spring Boot 3.3.4, Java 17, Maven.
- **RESTful Domain Core**: Full CRUD operations for QA automation **Projects**.
- **Robust Exception Handling**: Uniform API error schemas with structured validation and resource lookup handling.
- **Production Observability**: Spring Boot Actuator health checks and metrics.
- **Self-Documenting API**: Springdoc OpenAPI 3 / Swagger UI specification.
- **Containerization**: Multi-stage Dockerfile and Docker Compose orchestration.

### Phase 2 — User Authentication, Authorization & Project Ownership
- **Stateless JWT Security**: Spring Security 6 + JJWT (HMAC-SHA256) stateless token-based authentication.
- **Password Security**: Cryptographically secure hashing with `BCryptPasswordEncoder` (10 rounds). Passwords are never stored in plaintext, never logged, and never returned in API payloads.
- **Role-Based Access Control (RBAC)**: Distinct permissions for `ROLE_USER` (standard tenant) and `ROLE_ADMIN` (system administrator).
- **Multi-Tenant Project Ownership**: Every project belongs to an authenticated user (`owner`). Standard users can only view, manage, and delete their own projects. Accessing an unowned project yields `404 Not Found` (preventing ID enumeration / IDOR vulnerabilities).
- **Interactive Swagger Authentication**: OpenAPI 3 Bearer HTTP scheme configured for seamless testing directly from Swagger UI.
- **Database Safety & Migration**: Automatic startup bootstrap (`DataInitializer`) that seeds an administrative account and adopts orphaned projects from earlier stages.
- **Comprehensive Test Suite**: 34 unit, controller, and end-to-end integration tests with 100% pass rate.

---

## Architecture

SmartTest AI follows Clean Architecture / Layered Architecture within a Modular Monolith:

```text
                                 ┌──────────────────────────────────────┐
                                 │       Client / UI / Swagger UI       │
                                 └──────────────────┬───────────────────┘
                                                    │ HTTP Request
                                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Spring Security Filter Chain                                                                          │
│                                                                                                       │
│   ┌───────────────────────────┐      Public Endpoint?                                                 │
│   │  JwtAuthenticationFilter  │ ───────────────────────────► Permit Access                            │
│   └─────────────┬─────────────┘      (/api/v1/auth/**, /swagger-ui/**, etc.)                          │
│                 │ (Extract Bearer Token)                                                              │
│                 ▼                                                                                     │
│   ┌───────────────────────────┐                                                                       │
│   │        JwtService         │ ◄── Validates signature, claims & expiration                          │
│   └─────────────┬─────────────┘                                                                       │
│                 │                                                                                     │
│                 ▼                                                                                     │
│   ┌───────────────────────────┐                                                                       │
│   │ CustomUserDetailsService  │ ◄── Loads UserDetails from DB                                         │
│   └─────────────┬─────────────┘                                                                       │
│                 │                                                                                     │
│                 ▼                                                                                     │
│   ┌───────────────────────────┐                                                                       │
│   │      SecurityContext      │ ─── Sets Authenticated Principal into SecurityContextHolder           │
│   └───────────────────────────┘                                                                       │
└───────────────────────────────────────────────────┬───────────────────────────────────────────────────┘
                                                    │
                                                    ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Presentation & Business Layer                                                                        │
│                                                                                                       │
│    ┌─────────────────────────┐               ┌────────────────────────────────────────────────┐       │
│    │     AuthController      │               │               ProjectController                │       │
│    └────────────┬────────────┘               └───────────────────────┬────────────────────────┘       │
│                 │                                                    │ @AuthenticationPrincipal       │
│                 ▼                                                    ▼                                │
│    ┌─────────────────────────┐               ┌────────────────────────────────────────────────┐       │
│    │       AuthService       │               │                 ProjectService                 │       │
│    └────────────┬────────────┘               └───────────────────────┬────────────────────────┘       │
│                 │                                                    │ Scoped by Owner / Role         │
│                 ▼                                                    ▼                                │
│    ┌─────────────────────────┐               ┌────────────────────────────────────────────────┐       │
│    │     UserRepository      │               │               ProjectRepository                │       │
│    └────────────┬────────────┘               └───────────────────────┬────────────────────────┘       │
│                 │                                                    │                                │
│                 └────────────────────────────┬───────────────────────┘                                │
│                                              ▼                                                        │
│                                   PostgreSQL 16 Database                                              │
│                                   ┌────────────────────┐                                              │
│                                   │       users        │                                              │
│                                   └─────────┬──────────┘                                              │
│                                             │ 1                                                       │
│                                             │                                                         │
│                                             │ N                                                       │
│                                   ┌─────────▼──────────┐                                              │
│                                   │      projects      │ (owner_id FK -> users.id)                    │
│                                   └────────────────────┘                                              │
└───────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer / Concern | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java | 17 (LTS) |
| **Framework** | Spring Boot | 3.3.4 |
| **Security** | Spring Security | 6.3.3 |
| **JWT Library** | JJWT (io.jsonwebtoken) | 0.12.6 |
| **Persistence** | Spring Data JPA / Hibernate | 3.3.4 |
| **Database (Runtime)** | PostgreSQL | 16-alpine |
| **Database (Tests)** | H2 (PostgreSQL Mode) | In-memory |
| **Validation** | Jakarta Bean Validation | Hibernate Validator |
| **Observability** | Spring Boot Actuator | 3.3.4 |
| **API Documentation**| Springdoc OpenAPI / Swagger UI | 2.6.0 |
| **Build Tool** | Apache Maven | 3.9+ |
| **Testing** | JUnit 5, Mockito, AssertJ, Spring Security Test | 3.3.4 |
| **Containerization** | Docker & Docker Compose | Multi-stage build |

---

## Project Structure

```text
smarttest-ai/
├── src/
│   ├── main/
│   │   ├── java/com/smarttestai/
│   │   │   ├── SmartTestAiApplication.java       # Application entry point
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java          # Admin seed & project migration runner
│   │   │   │   └── OpenApiConfig.java            # OpenAPI 3 with Bearer JWT config
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java           # Authentication REST Controller
│   │   │   │   └── ProjectController.java        # Project management REST Controller
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── CreateProjectRequest.java # Inbound project creation DTO
│   │   │   │   │   ├── LoginRequest.java         # Inbound login DTO
│   │   │   │   │   └── RegisterRequest.java      # Inbound registration DTO
│   │   │   │   └── response/
│   │   │   │       ├── AuthResponse.java         # Outbound JWT & user DTO
│   │   │   │       ├── ErrorResponse.java        # Standardized error payload
│   │   │   │       ├── ProjectResponse.java      # Outbound project presentation DTO
│   │   │   │       └── UserResponse.java         # Safe user presentation DTO
│   │   │   ├── entity/
│   │   │   │   ├── Project.java                  # JPA Project Entity with owner relation
│   │   │   │   ├── Role.java                     # Role enum (ROLE_USER, ROLE_ADMIN)
│   │   │   │   └── User.java                     # JPA User Entity
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java   # Centralized @RestControllerAdvice
│   │   │   │   ├── ResourceNotFoundException.java# Domain 404 Exception
│   │   │   │   └── UserAlreadyExistsException.java# Domain 409 Exception
│   │   │   ├── repository/
│   │   │   │   ├── ProjectRepository.java        # Spring Data JPA Repository for Projects
│   │   │   │   └── UserRepository.java           # Spring Data JPA Repository for Users
│   │   │   ├── security/
│   │   │   │   ├── CustomAccessDeniedHandler.java# 403 Forbidden handler
│   │   │   │   ├── CustomUserDetails.java        # Spring Security UserDetails adapter
│   │   │   │   ├── CustomUserDetailsService.java # User loader service
│   │   │   │   ├── JwtAuthenticationEntryPoint.java# 401 Unauthorized entry point
│   │   │   │   ├── JwtAuthenticationFilter.java  # OncePerRequestFilter for Bearer JWT
│   │   │   │   ├── JwtService.java               # HMAC-SHA256 token operations
│   │   │   │   └── SecurityConfig.java           # Spring Security filter chain configuration
│   │   │   └── service/
│   │   │       ├── AuthService.java              # User registration and login logic
│   │   │       └── ProjectService.java           # Tenant-isolated project operations
│   │   └── resources/
│   │       └── application.yml                   # Main runtime configuration
│   └── test/
│       ├── java/com/smarttestai/
│       │   ├── controller/
│       │   │   ├── AuthControllerTest.java       # Auth web-layer tests
│       │   │   └── ProjectControllerTest.java    # Project web-layer tests with security context
│       │   ├── integration/
│       │   │   ├── AuthIntegrationTest.java      # Full end-to-end authentication tests
│       │   │   ├── ProjectIntegrationTest.java   # Full HTTP-to-Database lifecycle tests
│       │   │   └── ProjectSecurityIntegrationTest.java # Tenant isolation & IDOR tests
│       │   ├── security/
│       │   │   └── JwtServiceTest.java           # JWT token generation & validation tests
│       │   └── service/
│       │       ├── AuthServiceTest.java          # Auth service unit tests with Mockito
│       │       └── ProjectServiceTest.java       # Project service ownership unit tests
│       └── resources/
│           └── application-test.yml              # H2 PostgreSQL-compatible test configuration
├── Dockerfile                                    # Multi-stage container definition
├── docker-compose.yml                            # Orchestration for PostgreSQL + App
├── .env.example                                  # Environment template
├── pom.xml                                       # Maven build configuration with Security & JJWT
└── README.md                                     # Platform documentation
```

---

## Environment Variables

| Variable | Description | Default (Local) |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `smarttest_ai` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | Application HTTP port | `8080` |
| `JWT_SECRET` | 256-bit secret key for signing JWTs | Base64/Hex 256-bit key |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | `86400000` (24 hours) |

---

## Getting Started

### 1. Build the application
```bash
mvn clean package -DskipTests
```

### 2. Run with Docker Compose (Recommended)
```bash
docker compose up --build -d
```

### 3. Run Locally
```bash
# Start PostgreSQL via docker:
docker compose up -d postgres

# Run Spring Boot:
mvn spring-boot:run
```

---

## Running Automated Tests

All tests run against an in-memory H2 database in PostgreSQL mode (`application-test.yml`), with zero external dependencies.

```bash
mvn clean test
```

### Test Coverage Highlights (34 Tests / 100% Pass Rate):
* **Unit Tests**:
  * [`JwtServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/security/JwtServiceTest.java): Token issuance, claim parsing (`sub`, `userId`, `role`), tampering rejection.
  * [`AuthServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/service/AuthServiceTest.java): User creation, password hashing, duplicate email detection, credential verification.
  * [`ProjectServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/service/ProjectServiceTest.java): Automatic owner binding, user tenant filtering, admin overarching access.
* **Web Layer Tests**:
  * [`AuthControllerTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/controller/AuthControllerTest.java): MockMvc validation on registration/login endpoints.
  * [`ProjectControllerTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/controller/ProjectControllerTest.java): Web-layer status codes, header validation, and error serialization with security context.
* **Integration Tests**:
  * [`AuthIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/AuthIntegrationTest.java): End-to-end registration, verification of BCrypt hashes in DB, and login authentication.
  * [`ProjectSecurityIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/ProjectSecurityIntegrationTest.java): Multi-user tenant isolation, IDOR prevention (User A gets 404 accessing User B's project), unauthenticated 401 checks, and Admin cross-tenant management.
  * [`ProjectIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/ProjectIntegrationTest.java): Complete project CRUD lifecycle under authenticated session.

---

## API Endpoints

### Authentication APIs (Public)

#### 1. Register User
- **Method**: `POST`
- **Path**: `/api/v1/auth/register`
- **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@smarttestai.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 2,
      "name": "Jane Doe",
      "email": "jane@smarttestai.com",
      "role": "ROLE_USER",
      "createdAt": "2026-09-23T04:15:00Z"
    }
  }
  ```

#### 2. Login User
- **Method**: `POST`
- **Path**: `/api/v1/auth/login`
- **Request Body**:
  ```json
  {
    "email": "jane@smarttestai.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response**: `200 OK` (returns JWT `token` and `user` profile)

---

### Project APIs (Protected — Requires `Authorization: Bearer <JWT>`)

| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/projects` | Authenticated | Create project. Owner is automatically set to current user. |
| `GET` | `/api/v1/projects` | Authenticated | List projects (`ROLE_USER` sees owned; `ROLE_ADMIN` sees all). |
| `GET` | `/api/v1/projects/{id}` | Authenticated | Get project by ID (`ROLE_USER` can only access owned; else `404`). |
| `DELETE`| `/api/v1/projects/{id}` | Authenticated | Delete project (`ROLE_USER` can only delete owned; `ROLE_ADMIN` can delete any). |

---

## Using Swagger UI with Authentication

1. Open **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
2. Use `POST /api/v1/auth/register` or `POST /api/v1/auth/login` to obtain a JWT token.
3. Copy the `token` string from the JSON response.
4. Click the green **Authorize 🔓** button at the top right of the Swagger UI page.
5. Paste the token into the value field and click **Authorize**.
6. All subsequent requests to `/api/v1/projects` will automatically include the `Authorization: Bearer <token>` header.

---

## Future Roadmap

- [ ] **Phase 3 — AI Test Case Generation**: Natural language user story ingestion and scenario generation via LLM.
- [ ] **Phase 4 — Selenium Test Automation**: Automated executable Selenium Java test script generation and execution runner.
- [ ] **Phase 5 — DOM Tree Analysis & Locator Engine**: Real-time webpage DOM capture, semantic tagging, and resilient selector generation.
- [ ] **Phase 6 — Self-Healing Engine**: Dynamic fallback locator matching, automatic script healing, and healing audit records.
- [ ] **Phase 7 — Analytics & CI/CD Integrations**: Test run metrics, flaky test detection, and webhook notifications.
