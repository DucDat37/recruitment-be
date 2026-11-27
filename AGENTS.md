# AGENTS.md - Multi-Agent Synchronization Guidelines (`recruitment-be`)

> **Objective**: This is the mandatory specification for all AI Coding Agents (OpenAI Codex, Antigravity, Cursor, Claude Code, GitHub Copilot, Windsurf, etc.) and human developers working on `recruitment-be`. All agents and contributors must strictly adhere to these rules to maintain architectural consistency, database integrity, type safety, and seamless frontend integration.
>
> **Source of truth**: When this document and the code disagree, inspect the existing code first, follow the patterns it already uses, and raise the mismatch in the PR description instead of silently picking one.

---

## 1. Git Workflow, Branching Strategy & Pull Request (PR) Policy

> **CRITICAL RULE**: **NEVER commit directly to the `main` branch**. All changes must go through a dedicated branch and a Pull Request (PR).

1. **Branch Naming Standard**:
   - `feat/<feature-name>`: New feature implementations (e.g., `feat/interview-scheduling-api`)
   - `fix/<bug-name>`: Bug fixes (e.g., `fix/jwt-token-refresh-expiry`)
   - `refactor/<module-name>`: Code refactoring without behavior change
   - `chore/<task-name>`: Tooling, dependency, or configuration updates
   - `docs/<topic>`: Documentation-only changes
2. **Pull Request Workflow**:
   - Always branch off the latest `main`.
   - Ensure the project builds cleanly via `./mvnw clean compile` (and `./mvnw test` passes, see Section 7).
   - Push the branch to remote and submit a **Pull Request (PR)** with a clear title and description in English.
   - Await code review and approval before merging into `main`.

---

## 2. Language & Communication Standards

- **Primary Language**: **English** is required for everything developers read in the codebase.
- **Code & Comments**: All identifiers (classes, methods, variables, constants, packages), inline comments, and Javadoc annotations must be written in clear English.
- **Exception - user-facing text stays in Vietnamese**: Strings shown to end users are written in Vietnamese. This covers the `message` field of `ApiResponse` and `ErrorResponse`, validation messages, and email content. Do **not** translate these to English. Error `code` values stay in English `UPPER_SNAKE_CASE` (e.g., `EMAIL_ALREADY_EXISTS`).
- **Legacy Vietnamese comments**: Some Phase 1 files still contain Vietnamese comments. Translate them to English only in files you are already changing; do not open a PR just to translate.
- **Git Commits**: All commit messages must be in English following the **Conventional Commits** specification:
  - `feat(auth): implement refresh token endpoint with rotation`
  - `fix(job): resolve pagination query offset issue`
  - `chore(deps): update jjwt library version`
- **Documentation**: Agent rule files and PR descriptions must be in English. `README.md` and the use-case specs in `docs/` are currently written in Vietnamese for the team; keep them in Vietnamese unless asked otherwise.

---

## 3. Tech Stack & Version Constraints

- **Language**: Java 21 (LTS).
- **Framework**: Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation).
- **ORM & Database**: Hibernate 7, MySQL 8.0, Flyway for schema migrations.
- **Security & JWT**: `jjwt` 0.12.6, BCrypt password hashing.
- **Email Service**: Spring Boot Starter Mail over Gmail SMTP. For local development, set `APP_MAIL_MODE=log` to print emails to the console instead of sending them.
- **API Documentation**: `springdoc-openapi` (Swagger UI at `/swagger-ui.html`).
- **Boilerplate Reduction**: Project Lombok.
- **Build Tool**: Maven Wrapper (`./mvnw`).

---

## 4. Directory & Package Layout (`com.example.recruitment`)

Agents must respect the established architectural layers (`controller` -> `service` -> `repository` -> `entity`). This is the layout that exists today:

```text
src/main/java/com/example/recruitment/
├── common/             # Shared utilities and types
│   ├── ApiResponse.java        # Unified success envelope: { success, message, data, timestamp }
│   └── TokenUtils.java
├── config/             # Spring configuration (SecurityConfig, OpenApiConfig, AppProperties)
├── controller/         # REST controllers: routing, status codes, wrapping results in ApiResponse
├── dto/                # Data Transfer Objects, grouped by feature (NOT by request/response)
│   ├── auth/           # e.g. LoginRequest, RegisterRequest, TokenResponse, MeResponse
│   └── admin/          # e.g. UpdateUserStatusRequest
├── entity/             # JPA entities mapped to MySQL tables
│   └── enums/          # Enum types used by entities (UserRole, UserStatus, ...)
├── exception/          # ApiException, ErrorResponse, GlobalExceptionHandler (@RestControllerAdvice)
├── repository/         # Spring Data JPA repositories
├── security/           # JwtService, JwtAuthenticationFilter, AuthUser principal
└── service/            # Business logic as concrete @Service classes
```

Conventions:
- **DTOs** are Java `record`s. Name them `<Action>Request` / `<Thing>Response` and place them in `dto/<feature>/`. Put Jakarta Validation annotations (`@NotBlank`, `@Email`, `@Size`...) on request records.
- **Services** are concrete classes. Introduce an interface only when there are several implementations (e.g., `EmailSender` with `SmtpEmailSender` and `LoggingEmailSender`).
- **New features** get their own `dto/<feature>/` package and their own controller.

---

## 5. API Contracts & Response Standards

- **Zero Hallucination Policy**: All endpoints, request fields, and response DTOs must align with the shared API specification in the [`recruitment-docs`](https://github.com/yopaz-ducdt/recruitment-docs) repository: [`docs/api_specifications.md`](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/api_specifications.md) and [`docs/database_schema.md`](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/database_schema.md). If the spec is unavailable or unclear, ask instead of inventing fields.
- **Unified Success Wrapper**: Every successful endpoint returns `ApiResponse<T>` (`common/ApiResponse.java`):
  ```java
  public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {
      public static <T> ApiResponse<T> success(T data, String message) { ... }
      public static ApiResponse<Void> success(String message) { ... } // no payload, data = null
  }
  ```
  Usage in controllers:
  ```java
  return ApiResponse.success(authService.login(request), "Đăng nhập thành công.");
  return ApiResponse.success("Đăng xuất thành công.");
  ```
- **Error Handling**: Errors are **not** wrapped in `ApiResponse`. Throw `ApiException(HttpStatus, code, message)` from services; `GlobalExceptionHandler` turns it into `ErrorResponse`:
  ```json
  { "code": "EMAIL_ALREADY_EXISTS", "message": "Email đã được sử dụng", "fields": null }
  ```
  `fields` is only set for validation errors (`VALIDATION_ERROR`). The 401/403 handlers in `SecurityConfig` write the same shape.
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
1. `./mvnw clean compile` - Confirm compilation succeeds without syntax or Lombok issues.
2. `./mvnw test` - Confirm all tests pass. Note: the only test today is the Spring context-load test, which needs a running MySQL database plus the `DB_PASSWORD` and `JWT_SECRET` environment variables. If you cannot run it, say so in the PR description.
3. There is no CI pipeline yet, so these checks must be run locally.
