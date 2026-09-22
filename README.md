# SmartTest AI — Self-Healing QA Automation Platform

## Overview

**SmartTest AI** is an intelligent, self-healing quality assurance and test automation platform designed to simplify web application testing, eliminate fragile locator maintenance, and automatically heal broken test scripts during execution.

This repository represents **Phase 1: Project Foundation** — the production-grade modular monolith backbone upon which future AI-driven and self-healing automation components will be built.

---

## Phase 1 — Project Foundation

Phase 1 establishes a clean, scalable architectural baseline with:
- **Modular Monolith Architecture**: High cohesion, low coupling, and zero premature microservices complexity.
- **RESTful Domain Core**: Full CRUD operations for QA automation **Projects**.
- **Robust Exception Handling**: Uniform API error schemas with structured validation and resource lookup handling.
- **Production Observability**: Spring Boot Actuator health checks and metrics.
- **Self-Documenting API**: Springdoc OpenAPI 3 / Swagger UI specification.
- **Isolated Testing Suite**: Fast, zero-dependency unit and integration testing powered by JUnit 5, Mockito, and H2 (PostgreSQL compatibility mode).
- **Container-Ready**: Multi-stage Dockerfile and Docker Compose orchestration.

---

## Architecture

SmartTest AI follows standard Clean Architecture / Layered Architecture principles:

```text
[ Client / API Consumer / Swagger UI ]
                   │
                   ▼ (HTTP / JSON)
        ┌─────────────────────┐
        │  ProjectController  │   <- REST Presentation Layer (Validation & OpenAPI)
        └──────────┬──────────┘
                   │ DTOs (Request / Response)
                   ▼
        ┌─────────────────────┐
        │   ProjectService    │   <- Business Logic & Transaction Management
        └──────────┬──────────┘
                   │ Domain Entities
                   ▼
        ┌─────────────────────┐
        │  ProjectRepository  │   <- Spring Data JPA Persistence Layer
        └──────────┬──────────┘
                   │ JDBC
                   ▼
        ┌─────────────────────┐
        │  PostgreSQL 16 DB   │   <- Persistent Relational Store (or H2 in test)
        └─────────────────────┘
```

---

## Technology Stack

| Layer / Concern | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java | 17 (LTS) |
| **Framework** | Spring Boot | 3.3.4 |
| **Persistence** | Spring Data JPA / Hibernate | 3.3.4 |
| **Database (Runtime)** | PostgreSQL | 16-alpine |
| **Database (Tests)** | H2 (PostgreSQL Mode) | In-memory |
| **Validation** | Jakarta Bean Validation | Hibernate Validator |
| **Observability** | Spring Boot Actuator | 3.3.4 |
| **API Documentation**| Springdoc OpenAPI / Swagger UI | 2.6.0 |
| **Build Tool** | Apache Maven | 3.9+ |
| **Testing** | JUnit 5, Mockito, AssertJ | Spring Boot Starter Test |
| **Containerization** | Docker & Docker Compose | Multi-stage build |

---

## Project Structure

```text
smarttest-ai/
├── src/
│   ├── main/
│   │   ├── java/com/smarttestai/
│   │   │   ├── SmartTestAiApplication.java    # Application entry point
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java         # OpenAPI / Swagger configuration
│   │   │   ├── controller/
│   │   │   │   └── ProjectController.java     # REST API Controller
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   └── CreateProjectRequest.java # Inbound creation DTO with validation
│   │   │   │   └── response/
│   │   │   │       ├── ErrorResponse.java     # Standardized error payload
│   │   │   │       └── ProjectResponse.java   # Outbound project presentation DTO
│   │   │   ├── entity/
│   │   │   │   └── Project.java               # JPA Domain Entity
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java# Centralized @RestControllerAdvice
│   │   │   │   └── ResourceNotFoundException.java # Domain 404 Exception
│   │   │   ├── repository/
│   │   │   │   └── ProjectRepository.java     # Spring Data JPA Repository
│   │   │   └── service/
│   │   │       └── ProjectService.java        # Core business operations
│   │   └── resources/
│   │       └── application.yml                # Main runtime configuration
│   └── test/
│       ├── java/com/smarttestai/
│       │   ├── controller/
│       │   │   └── ProjectControllerTest.java # MockMvc web-layer tests
│       │   ├── integration/
│       │   │   └── ProjectIntegrationTest.java# Full HTTP-to-Database integration tests
│       │   └── service/
│       │       └── ProjectServiceTest.java    # Service unit tests with Mockito
│       └── resources/
│           └── application-test.yml           # H2 PostgreSQL-compatible test configuration
├── Dockerfile                                 # Multi-stage container definition
├── docker-compose.yml                         # Orchestration for PostgreSQL + App
├── .env.example                               # Environment template
├── .gitignore                                 # Git ignore definitions
├── pom.xml                                    # Maven build configuration
└── README.md                                  # Platform documentation
```

---

## Prerequisites

- **Java Development Kit (JDK)**: JDK 17 or higher
- **Apache Maven**: 3.9+ (or use IDE-bundled Maven)
- **Docker & Docker Compose**: (Optional for containerized execution)
- **PostgreSQL**: 16+ (or run via Docker Compose)

---

## Environment Variables

SmartTest AI uses environment variables for database connectivity and runtime parameters:

| Variable | Description | Default (Local) |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `smarttest_ai` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | Application HTTP port | `8080` |

---

## Local Development

### PostgreSQL Setup

You can spin up an isolated PostgreSQL container using Docker Compose:

```bash
docker compose up -d postgres
```

Or connect to an existing local PostgreSQL instance:

```sql
CREATE DATABASE smarttest_ai;
```

---

## Running the Application

### 1. Build the application
```bash
mvn clean package -DskipTests
```

### 2. Run locally
```bash
java -jar target/smarttest-ai-0.0.1-SNAPSHOT.jar
```
Or with Maven:
```bash
mvn spring-boot:run
```

The application starts on port `8080` by default.

---

## Running Tests

The test suite runs with an in-memory H2 database configured in PostgreSQL compatibility mode (`application-test.yml`), meaning **no external database is required to execute tests**.

```bash
mvn clean test
```

This executes:
- **Unit Tests (`ProjectServiceTest`)**: Fast business logic tests using Mockito mocks.
- **Controller Tests (`ProjectControllerTest`)**: Web layer tests validating HTTP status codes, headers, and Jakarta Bean Validation errors using `MockMvc`.
- **Integration Tests (`ProjectIntegrationTest`)**: Full end-to-end integration tests over real HTTP requests with `TestRestTemplate`.

---

## Docker Setup

### Running with Docker Compose (Recommended)

To start both PostgreSQL and the SmartTest AI backend in isolated containers:

```bash
docker compose up --build -d
```

Check logs:
```bash
docker compose logs -f smarttest-ai
```

Stop containers:
```bash
docker compose down
```

---

## Swagger Documentation

Interactive OpenAPI 3 documentation is available at:

- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Health Check

Spring Boot Actuator provides liveness and readiness monitoring:

- **Endpoint**: `GET /actuator/health`
- **URL**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Expected Response**:
  ```json
  {
    "status": "UP"
  }
  ```

---

## API Endpoints

All Project APIs are prefixed with `/api/v1/projects`.

### 1. Create Project
- **HTTP Method**: `POST`
- **Path**: `/api/v1/projects`
- **Request Body**:
  ```json
  {
    "name": "E-Commerce Testing",
    "description": "Automation testing project for retail web app"
  }
  ```
- **Response**: `201 Created`
  - Header: `Location: /api/v1/projects/{id}`
  - Body:
    ```json
    {
      "id": 1,
      "name": "E-Commerce Testing",
      "description": "Automation testing project for retail web app",
      "createdAt": "2026-09-22T06:40:00Z",
      "updatedAt": "2026-09-22T06:40:00Z"
    }
    ```

### 2. Get All Projects
- **HTTP Method**: `GET`
- **Path**: `/api/v1/projects`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": 1,
      "name": "E-Commerce Testing",
      "description": "Automation testing project for retail web app",
      "createdAt": "2026-09-22T06:40:00Z",
      "updatedAt": "2026-09-22T06:40:00Z"
    }
  ]
  ```

### 3. Get Project By ID
- **HTTP Method**: `GET`
- **Path**: `/api/v1/projects/{id}`
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "E-Commerce Testing",
    "description": "Automation testing project for retail web app",
    "createdAt": "2026-09-22T06:40:00Z",
    "updatedAt": "2026-09-22T06:40:00Z"
  }
  ```
- **Error Response (If missing)**: `404 Not Found`
  ```json
  {
    "timestamp": "2026-09-22T06:45:00Z",
    "status": 404,
    "error": "PROJECT_NOT_FOUND",
    "message": "Project not found with id: 999",
    "path": "/api/v1/projects/999"
  }
  ```

### 4. Delete Project
- **HTTP Method**: `DELETE`
- **Path**: `/api/v1/projects/{id}`
- **Response**: `204 No Content`
- **Error Response (If missing)**: `404 Not Found`

---

## Future Roadmap

The following advanced capabilities are scheduled for subsequent phases and are **NOT IMPLEMENTED in Phase 1**:

- [ ] **Phase 2 — AI Test Case Generation**: Natural language user story ingestion and automated scenario generation via LLM.
- [ ] **Phase 3 — Selenium Test Automation**: Automated executable Selenium Java test script generation and execution runner.
- [ ] **Phase 4 — DOM Tree Analysis & Locator Engine**: Real-time webpage DOM capture, semantic tagging, and resilient selector generation.
- [ ] **Phase 5 — Self-Healing Engine**: Dynamic fallback locator matching, automatic script healing, and healing audit records.
- [ ] **Phase 6 — Analytics & CI/CD Integrations**: Test run metrics, flaky test detection, and webhook notifications.
