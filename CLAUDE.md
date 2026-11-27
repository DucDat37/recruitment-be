# CLAUDE.md - Rules for Claude Code in recruitment-be

Refer to [`AGENTS.md`](./AGENTS.md) for complete guidelines.

## Git Workflow & Branching
- **Never commit directly to `main`**: Always create a feature branch (`feat/*`, `fix/*`, `refactor/*`, `chore/*`, `docs/*`) and open a Pull Request (PR).
- Use Conventional Commits in English (e.g., `feat(auth): add refresh token endpoint`).

## Language Standards
- **English Only** for Java code, identifiers, comments, Javadoc, commits and PR descriptions.
- **Exception**: user-facing messages (`message` in `ApiResponse` / `ErrorResponse`, validation messages, emails) stay in Vietnamese.

## Tech Stack
- Java 21, Spring Boot 4.1.1, Spring Data JPA, Hibernate, MySQL 8, Flyway, JJWT, Lombok.

## Build & Test Commands
- Compile project: `./mvnw clean compile`
- Run tests: `./mvnw test` (needs MySQL and the `DB_PASSWORD`, `JWT_SECRET` env vars)
- Run application: `./mvnw spring-boot:run`

## Architecture & Conventions
- Layered architecture: `controller` -> `service` -> `repository` -> `entity`.
- DTOs are records grouped by feature in `dto/<feature>/` (e.g., `dto/auth/`), not `dto/request` / `dto/response`.
- Wrap every successful response in `ApiResponse<T>` (`common/ApiResponse.java`); errors use `ErrorResponse` via `GlobalExceptionHandler`.
- Never modify existing Flyway migration files; create a new `V*__description.sql`.
- Run `./mvnw clean compile` before finishing tasks to ensure zero compilation or Lombok issues.
