# CODEX.md - OpenAI Codex CLI Guidelines for `recruitment-be`

> **Note**: This document provides operational guidelines and constraints for **OpenAI Codex CLI**, Codex agents, and related AI developer tools working on `recruitment-be`. For full specifications, consult [`AGENTS.md`](file:///home/ducdt/Other/recruitment-be/AGENTS.md).

---

## 1. Mandatory Git Workflow & Branching Policy

- **NEVER COMMIT DIRECTLY TO `main`**:
  - Always branch off `main`: `feat/<feature-name>`, `fix/<bug-name>`, or `chore/<task-name>`.
  - Create a **Pull Request (PR)** for review and CI checks before merging into `main`.
- **English Language Requirement**:
  - All source code, identifier names, inline comments, Javadoc, commit messages, and PR descriptions MUST be written in English.
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
  `controller` $\rightarrow$ `service` $\rightarrow$ `repository` $\rightarrow$ `entity`.
- All successful controller endpoints must wrap responses in `ApiResponse<T>`.
- Use Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) on inbound DTOs in `dto/request/`.
- Handle exceptions centrally via `GlobalExceptionHandler` (`@RestControllerAdvice`).
- Maintain API alignment with `docs/BACKEND_AUDIT_AND_FEEDBACK.md` and `../docs/api_specifications.md`.

---

## 4. Verification Commands

Before opening any PR, Codex must execute and verify:

```bash
# 1. Compile project and verify Lombok annotation processing
./mvnw clean compile

# 2. Run unit and integration tests
./mvnw test
```

---

## 5. Cross References

- Multi-Agent Guidelines: [`AGENTS.md`](file:///home/ducdt/Other/recruitment-be/AGENTS.md)
- Claude Code Rules: [`CLAUDE.md`](file:///home/ducdt/Other/recruitment-be/CLAUDE.md)
- Cursor Rules: [`.cursorrules`](file:///home/ducdt/Other/recruitment-be/.cursorrules)
- Backend Audit & Feedback: [`docs/BACKEND_AUDIT_AND_FEEDBACK.md`](file:///home/ducdt/Other/recruitment-be/docs/BACKEND_AUDIT_AND_FEEDBACK.md)
- API Specifications: [`../docs/api_specifications.md`](file:///home/ducdt/Other/docs/api_specifications.md)
