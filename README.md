# SmartTest AI — Self-Healing QA Automation Platform

## Overview

**SmartTest AI** is an intelligent, self-healing quality assurance and test automation platform designed to simplify web application testing, eliminate fragile locator maintenance, and automatically heal broken test scripts during execution.

This repository represents the production-grade modular monolith securing multi-tenant test automation projects and user stories through:
- **Phase 1: Project Foundation**
- **Phase 2: User Authentication, Authorization & Project Ownership**
- **Phase 1: Project Foundation**
- **Phase 2: User Authentication, Authorization & Project Ownership**
- **Phase 3: User Story Management**
- **Phase 4: AI Test Case Generation** (LangChain4j + LLM Structured Output)

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
- **Password Security**: Cryptographically secure hashing with `BCryptPasswordEncoder` (10 rounds).
- **Role-Based Access Control (RBAC)**: Distinct permissions for `ROLE_USER` (standard tenant) and `ROLE_ADMIN` (system administrator).
- **Multi-Tenant Project Ownership**: Every project belongs to an authenticated user (`owner`). Standard users can only view, manage, and delete their own projects. Accessing an unowned project yields `404 Not Found` (preventing ID enumeration / IDOR vulnerabilities).
- **Interactive Swagger Authentication**: OpenAPI 3 Bearer HTTP scheme configured for seamless testing directly from Swagger UI.

### Phase 3 — User Story Management
- **Hierarchical Domain Structure**: 
  $$\text{User} \longrightarrow \text{Project} \longrightarrow \text{User Story} \longrightarrow \text{TestCase} \longrightarrow \text{TestStep}$$
- **User Story Domain Model**:
  - `id`: Auto-generated identifier.
  - `project`: Strongly typed `@ManyToOne` association linking each story to its project.
  - `title`: Short descriptive title (3-150 chars).
  - `description`: Narrative description (`TEXT`, 5-2000 chars).
  - `acceptanceCriteria`: Detailed scenarios and acceptance criteria (`TEXT`, 5-4000 chars).
  - `priority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
  - `status`: `DRAFT`, `READY`, `IN_PROGRESS`, `COMPLETED`.
  - `createdAt` / `updatedAt`: Audited timestamps.
- **Two-Tier IDOR Protection**: Server enforces project ownership before accessing stories; mismatch or foreign probes always return `404 Not Found`.
- **Filtering & Phase 4 AI Bridge**: List endpoint supports query parameters `?status=READY&priority=HIGH`. When stories transition to `status = READY`, they serve as clean context for Phase 4's LLM test generator.

### Phase 4 — AI Test Case Generation
- **LangChain4j Integration**: Clean, vendor-agnostic chat model abstraction (`ChatLanguageModel`) supporting OpenAI (`gpt-4o-mini`), Groq, local Ollama, and a zero-network `MockTestCaseGenerator` for offline development and CI/CD pipelines.
- **Decoupled Test Case Model**:
  - `TestCase`: Independent `TestPriority` (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), `TestCaseType` (`POSITIVE`, `NEGATIVE`, `EDGE_CASE`, `SECURITY`), preconditions, and expected overall result.
  - `TestStep`: Structured `@Embeddable` value objects with `stepNumber`, `action`, and `expectedResult` cleanly separating interactions from assertions for Phase 5 Selenium generation.
- **Prompt Injection Defense & Guardrails**: Strict XML boundary tagging (`<user_story_input>`) and system prompt security instructions to prevent untrusted user story content from overriding AI directives.
- **5-Stage Validation Pipeline**: Validates raw JSON schema, bean constraints, required scenarios, and title deduplication before persisting to PostgreSQL.
- **Three-Tier IDOR Protection**: Scoped enforcement across `Project` $\rightarrow$ `UserStory` $\rightarrow$ `TestCase` returning `404 Not Found` for unauthorized cross-tenant requests.
- **Human-in-the-Loop Lifecycle**: Explicit state transitions (`GENERATED` $\rightarrow$ `REVIEWED` $\rightarrow$ `APPROVED` / `REJECTED`) allowing QA engineers to edit and verify test cases before automation.
- **Automated Test Suite**: 87 automated unit, web-layer, security, and integration tests with **100% pass rate**.

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
│  ┌────────────────────┐   ┌─────────────────────────┐   ┌──────────────────────────────────────────┐  │
│  │   AuthController   │   │    ProjectController    │   │           UserStoryController            │  │
│  └─────────┬──────────┘   └────────────┬────────────┘   └────────────────────┬─────────────────────┘  │
│            │                           │                                     │                        │
│            ▼                           ▼                                     ▼                        │
│  ┌────────────────────┐   ┌─────────────────────────┐   ┌──────────────────────────────────────────┐  │
│  │    AuthService     │   │     ProjectService      │   │             UserStoryService             │  │
│  └─────────┬──────────┘   └────────────┬────────────┘   └────────────────────┬─────────────────────┘  │
│            │                           │                                     │                        │
│            ▼                           ▼                                     ▼                        │
│  ┌────────────────────┐   ┌─────────────────────────┐   ┌──────────────────────────────────────────┐  │
│  │   UserRepository   │   │    ProjectRepository    │   │           UserStoryRepository            │  │
│  └─────────┬──────────┘   └────────────┬────────────┘   └────────────────────┬─────────────────────┘  │
│            │                           │                                     │                        │
│            └───────────────────────────┼─────────────────────────────────────┘                        │
│                                        ▼                                                              │
│                             PostgreSQL 16 Database                                                    │
│                             ┌────────────────────┐                                                    │
│                             │       users        │                                                    │
│                             └─────────┬──────────┘                                                    │
│                                       │ 1                                                             │
│                                       │                                                               │
│                                       │ N                                                             │
│                             ┌─────────▼──────────┐                                                    │
│                             │      projects      │ (owner_id FK -> users.id)                          │
│                             └─────────┬──────────┘                                                    │
│                                       │ 1                                                             │
│                                       │                                                               │
│                                       │ N                                                             │
│                             ┌─────────▼──────────┐                                                    │
│                             │    user_stories    │ (project_id FK -> projects.id)                     │
│                             └────────────────────┘                                                    │
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

## Running Automated Tests

All tests run against an in-memory H2 database in PostgreSQL mode (`application-test.yml`), with zero external dependencies.

```bash
mvn clean test
```

### Test Coverage Highlights (87 Tests / 100% Pass Rate):
* **Unit Tests (43 Tests)**:
  * [`JwtServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/security/JwtServiceTest.java) (4): Token issuance, claim parsing (`sub`, `userId`, `role`), expiry, tampering rejection.
  * [`AuthServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/service/AuthServiceTest.java) (4): User registration with BCrypt hashing, duplicate check (409), login validation.
  * [`ProjectServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/service/ProjectServiceTest.java) (9): Owner assignment, tenant isolation, admin cross-tenant listing, 404 on unowned.
  * [`UserStoryServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/service/UserStoryServiceTest.java) (9): Story creation under project, status/priority filtering, IDOR boundary validation, updates, deletes.
  * [`PromptBuilderServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/ai/PromptBuilderServiceTest.java) (3): XML boundary formatting, prompt injection guard verification, version tagging.
  * [`LangChain4jTestCaseGeneratorTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/ai/LangChain4jTestCaseGeneratorTest.java) (5): Structured JSON parsing, markdown fence stripping, schema validation, exception mapping.
  * [`TestCaseServiceTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/service/TestCaseServiceTest.java) (9): READY status check, AI generation outside DB transaction, title deduplication, manual CRUD, status transition.
* **Web Layer Tests (29 Tests)**:
  * [`AuthControllerTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/controller/AuthControllerTest.java) (5): MockMvc validation for `/register` and `/login`.
  * [`ProjectControllerTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/controller/ProjectControllerTest.java) (8): MockMvc tests for all Project CRUD endpoints.
  * [`UserStoryControllerTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/controller/UserStoryControllerTest.java) (8): MockMvc validation for User Story CRUD endpoints.
  * [`TestCaseControllerTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/controller/TestCaseControllerTest.java) (8): MockMvc tests for AI generation, manual creation, filtering, get, update, status patch, delete.
* **Integration Tests (15 Tests)**:
  * [`AuthIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/AuthIntegrationTest.java) (1): End-to-end registration, BCrypt in DB, login, and error checks.
  * [`ProjectSecurityIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/ProjectSecurityIntegrationTest.java) (2): Multi-user project isolation, 401 unauthenticated check, Admin management.
  * [`ProjectIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/ProjectIntegrationTest.java) (1): Complete project CRUD lifecycle with JWT Bearer authentication.
  * [`UserStorySecurityIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/UserStorySecurityIntegrationTest.java) (2): Multi-tenant story isolation, cross-project IDOR probe rejection (returns 404), Admin management.
  * [`UserStoryIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/UserStoryIntegrationTest.java) (1): Complete story CRUD lifecycle, status/priority filtering, and delete verification.
  * [`TestCaseGenerationIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/TestCaseGenerationIntegrationTest.java) (3): End-to-end AI test case generation, DRAFT rejection, review status transition, and manual CRUD.
  * [`TestCaseSecurityIntegrationTest`](file:///c:/Users/kapoo/OneDrive/Desktop/SmartTest-AI/src/test/java/com/smarttestai/integration/TestCaseSecurityIntegrationTest.java) (5): Three-tier IDOR prevention across tenants and admin cross-tenant inspection.

---

## API Endpoints

### 1. Authentication APIs (Public)

| Method | Path | Request Body | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | `RegisterRequest` | Register new user (`ROLE_USER`), returns JWT. |
| `POST` | `/api/v1/auth/login` | `LoginRequest` | Authenticate credentials, returns JWT. |

### 2. Project APIs (Protected — Requires `Authorization: Bearer <JWT>`)

| Method | Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/projects` | Create project. Owner is automatically set to authenticated user. |
| `GET` | `/api/v1/projects` | List projects (`ROLE_USER` sees owned; `ROLE_ADMIN` sees all). |
| `GET` | `/api/v1/projects/{id}` | Get project by ID (`ROLE_USER` can only access owned; else `404`). |
| `DELETE`| `/api/v1/projects/{id}` | Delete project (`ROLE_USER` can only delete owned; `ROLE_ADMIN` can delete any). |

### 3. User Story APIs (Protected — Requires `Authorization: Bearer <JWT>`)

| Method | Path | Request Body | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/projects/{projectId}/stories` | `CreateUserStoryRequest` | Create user story under project. |
| `GET` | `/api/v1/projects/{projectId}/stories` | None (`?status=&priority=`) | List user stories with optional status and priority filters. |
| `GET` | `/api/v1/projects/{projectId}/stories/{storyId}` | None | Retrieve user story by ID within project. |
| `PUT` | `/api/v1/projects/{projectId}/stories/{storyId}` | `UpdateUserStoryRequest` | Update user story fields or status (`READY`). |
| `DELETE`| `/api/v1/projects/{projectId}/stories/{storyId}` | None | Delete user story from project. |

### 4. Test Case APIs (Protected — Requires `Authorization: Bearer <JWT>`)

| Method | Path | Request Body | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/projects/{projectId}/stories/{storyId}/test-cases/generate` | None | AI-generate test cases from a READY user story using LangChain4j. |
| `POST` | `/api/v1/projects/{projectId}/stories/{storyId}/test-cases` | `CreateTestCaseRequest` | Manually create a test case under the user story. |
| `GET` | `/api/v1/projects/{projectId}/stories/{storyId}/test-cases` | None (`?type=&priority=&status=`) | List test cases with optional filters. |
| `GET` | `/api/v1/projects/{projectId}/stories/{storyId}/test-cases/{testCaseId}` | None | Retrieve a specific test case by ID. |
| `PUT` | `/api/v1/projects/{projectId}/stories/{storyId}/test-cases/{testCaseId}` | `UpdateTestCaseRequest` | Update test case fields or steps. |
| `PATCH`| `/api/v1/projects/{projectId}/stories/{storyId}/test-cases/{testCaseId}/status` | `UpdateTestCaseStatusRequest` | Transition review status (`GENERATED` $\rightarrow$ `APPROVED`). |
| `DELETE`| `/api/v1/projects/{projectId}/stories/{storyId}/test-cases/{testCaseId}` | None | Delete a test case from the user story. |

---

## Using Swagger UI with Authentication

1. Open **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
2. Use `POST /api/v1/auth/register` or `POST /api/v1/auth/login` to obtain a JWT token.
3. Copy the `token` string from the JSON response.
4. Click the green **Authorize 🔓** button at the top right of the Swagger UI page.
5. Paste the token into the value field and click **Authorize**.
6. All requests to `/api/v1/projects/**` will automatically include the `Authorization: Bearer <token>` header.

---

## Future Roadmap

- [x] **Phase 1 — Project Foundation**: Project entity, CRUD APIs, Swagger, Docker, baseline tests.
- [x] **Phase 2 — User Authentication, Authorization & Project Ownership**: JWT, BCrypt, RBAC, multi-tenant isolation.
- [x] **Phase 3 — User Story Management**: Requirements domain, Gherkin acceptance criteria, status filtering.
- [x] **Phase 4 — AI Test Case Generation**: LangChain4j integration, anti-prompt injection, structured JSON output, human review workflow, 87 automated tests.
- [ ] **Phase 5 — Selenium Test Automation**: Automated executable Selenium Java test script generation and execution runner.
- [ ] **Phase 6 — DOM Tree Analysis & Locator Engine**: Real-time webpage DOM capture, semantic tagging, and resilient selector generation.
- [ ] **Phase 7 — Self-Healing Engine**: Dynamic fallback locator matching, automatic script healing, and healing audit records.
- [ ] **Phase 8 — Analytics & CI/CD Integrations**: Test run metrics, flaky test detection, and webhook notifications.
