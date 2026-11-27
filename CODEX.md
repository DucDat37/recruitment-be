# CODEX.md - OpenAI Codex CLI Guidelines for `recruitment-be`

> **Note**: This document provides operational guidelines and constraints for **OpenAI Codex CLI**, Codex agents, and related AI developer tools working on `recruitment-be`. For full specifications, consult [`AGENTS.md`](./AGENTS.md).

---

## 1. Mandatory Git Workflow & Branching Policy

- **NEVER COMMIT DIRECTLY TO `main`**:
  - Always branch off `main`: `feat/<feature-name>`, `fix/<bug-name>`, `refactor/<module-name>`, `chore/<task-name>` or `docs/<topic>`.
  - Create a **Pull Request (PR)** for review before merging into `main`.
- **English Language Requirement**:
  - All source code, identifier names, inline comments, Javadoc, commit messages, and PR descriptions MUST be written in English.
  - Exception: user-facing messages (`message` in `ApiResponse` / `ErrorResponse`, validation messages, emails) stay in Vietnamese.
  - Use Conventional Commits in English (e.g., `feat(auth): add refresh token endpoint`).

---

## 2. Tech Stack & Environment

- **Language & Runtime**: Java 21 (LTS).
- **Framework**: Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation).
- **Database & Migration**: MySQL 8.0, Flyway.
  - *Never modify existing Flyway migration files*; always create a new `V<number>__<description>.sql`.
- **Security & JWT**: `jjwt` 0.12.6, BCrypt.
- **Build Tool**: Maven Wrapper (`./mvnw`).

---

## 3. Architecture & Conventions (`com.example.recruitment`)

- Maintain strict layered architecture:
  `controller` -> `service` -> `repository` -> `entity`.
- All successful controller endpoints must wrap responses in `ApiResponse<T>` (`common/ApiResponse.java`).
- DTOs are records grouped by feature in `dto/<feature>/` (e.g., `dto/auth/`). Use Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) on request records.
- Handle exceptions centrally via `GlobalExceptionHandler` (`@RestControllerAdvice`); errors are returned as `ErrorResponse`, not `ApiResponse`.
- Maintain API alignment with the shared spec: [`recruitment-docs/docs/api_specifications.md`](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/api_specifications.md).

---

## 4. Verification Commands

Before opening any PR, Codex must execute and verify:

```bash
# 1. Compile project and verify Lombok annotation processing
./mvnw clean compile

# 2. Run tests (needs MySQL and the DB_PASSWORD, JWT_SECRET env vars)
./mvnw test
```

There is no CI pipeline yet; run these locally and report the result in the PR.

---

## 5. Cross References

- Multi-Agent Guidelines: [`AGENTS.md`](./AGENTS.md)
- Claude Code Rules: [`CLAUDE.md`](./CLAUDE.md)
- Cursor Rules: [`.cursorrules`](./.cursorrules)
- Shared project docs: [`recruitment-docs`](https://github.com/yopaz-ducdt/recruitment-docs)
- API Specifications: [`api_specifications.md`](https://github.com/yopaz-ducdt/recruitment-docs/blob/main/docs/api_specifications.md)
