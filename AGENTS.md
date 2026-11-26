# AGENTS.md - Multi-Agent Synchronization Guidelines (`recruitment-be`)

> **Objective**: This is the mandatory specification for all AI Coding Agents (OpenAI Codex, Antigravity, Cursor, Claude Code, GitHub Copilot, Windsurf, etc.) and human developers working on `recruitment-be`. All agents and contributors must strictly adhere to these rules to maintain architectural consistency, database integrity, type safety, and seamless frontend integration.

---

## 1. Git Workflow, Branching Strategy & Pull Request (PR) Policy

> **CRITICAL RULE**: **NEVER commit directly to the `main` branch**. All changes must go through a dedicated branch and a Pull Request (PR).

1. **Branch Naming Standard**:
   - `feat/<feature-name>`: New feature implementations (e.g., `feat/interview-scheduling-api`)
   - `fix/<bug-name>`: Bug fixes (e.g., `fix/jwt-token-refresh-expiry`)
   - `refactor/<module-name>`: Code refactoring without behavior change
   - `chore/<task-name>`: Tooling, dependency, or configuration updates
2. **Pull Request Workflow**:
   - Always branch off the latest `main`.
   - Ensure the project builds cleanly via `./mvnw clean compile` (and `./mvnw test` passes).
   - Push the branch to remote and submit a **Pull Request (PR)** with a clear title and description in English.
   - Await code review and approval before merging into `main`.

---

## 2. Language & Communication Standards

- **Primary Language**: **English** is strictly required across the entire codebase.
- **Code & Comments**: All identifiers (classes, methods, variables, constants, packages), inline comments, and Javadoc annotations must be written in clear English.
- **Git Commits**: All commit messages must be in English following the **Conventional Commits** specification:
  - `feat(auth): implement refresh token endpoint with rotation`
  - `fix(job): resolve pagination query offset issue`
  - `chore(deps): update jjwt library version`
- **Documentation**: All Markdown documentation and PR descriptions within this repository must be in English.

---

## 3. Tech Stack & Version Constraints

- **Language**: Java 21 (LTS).
- **Framework**: Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation).
- **ORM & Database**: Hibernate 7, MySQL 8.0, Flyway for schema migrations.
- **Security & JWT**: `jjwt` 0.12.6, BCrypt password hashing.
- **Email Service**: Spring Boot Starter Mail (Gmail SMTP / MailHog).
- **API Documentation**: `springdoc-openapi` (Swagger UI).
- **Boilerplate Reduction**: Project Lombok.
- **Build Tool**: Maven Wrapper (`./mvnw`).

---

## 4. Directory & Package Layout (`com.example.recruitment`)

Agents must strictly respect the established architectural layers:

```text
src/main/java/com/example/recruitment/
├── common/             # Common constants, utility classes, and base entities
│   └── ApiResponse.java        # Standard unified REST envelope: { success, message, data, timestamp }
├── config/             # Spring configuration beans (Security, CORS, OpenAPI, PasswordEncoder)
├── controller/         # REST Controllers handling HTTP requests, routing & status codes
├── dto/                # Data Transfer Objects
│   ├── request/        # Inbound request payloads with Jakarta Validation annotations (@NotBlank, @Email...)
│   └── response/       # Outbound DTOs sent to Frontend
├── entity/             # JPA Entities mapped to MySQL tables via Hibernate
├── exception/          # Custom exceptions and GlobalExceptionHandler (@RestControllerAdvice)
├── repository/         # Spring Data JPA repositories with query methods and @Query annotations
├── security/           # JWT authentication filter, token provider, UserDetailsService implementation
└── service/            # Business logic layer (Service interfaces and @Service implementations)
```

---

## 5. API Contracts & Response Standards

- **Zero Hallucination Policy**: All endpoints, request fields, and response DTOs must align with `docs/BACKEND_AUDIT_AND_FEEDBACK.md` and `../docs/api_specifications.md`.
- **Unified Response Wrapper**:
  Every successful endpoint response must return `ApiResponse<T>`:
  ```java
  public record ApiResponse<T>(
      boolean success,
      String message,
      T data,
      Instant timestamp
  ) {
      public static <T> ApiResponse<T> success(T data, String message) {
          return new ApiResponse<>(true, message, data, Instant.now());
      }
  }
  ```
- **Error Handling**:
  Errors must be caught centrally in `GlobalExceptionHandler` returning structured error payloads (including field validation errors).
- **Phase 1 Registration Scope**:
  `RegisterRequest` is strictly limited to `{ email, password, role }` (`CANDIDATE` or `RECRUITER`). Profile details belong to later onboarding steps.

---

## 6. Database Migrations (Flyway)

- **Immutable Migration Rule**: **NEVER modify or delete existing migration scripts** in `src/main/resources/db/migration/` (e.g., `V1__init.sql`).
- **Version Increment**: Any schema modification must be created as a new versioned script (e.g., `V2__add_interviews_table.sql`).
- Always verify SQL syntax and ensure migration scripts run cleanly against MySQL 8.

---

## 7. Self-Verification Checklist (Mandatory for Agents)

Before opening any Pull Request, verify:
1. `./mvnw clean compile` — Confirm compilation succeeds without syntax or Lombok issues.
2. `./mvnw test` — Confirm all unit and integration tests pass.
