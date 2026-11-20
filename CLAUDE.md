# CLAUDE.md - Rules for Claude Code in recruitment-be

Refer to `AGENTS.md` for complete guidelines.

## Git Workflow & Branching
- **Never commit directly to `main`**: Always create a feature branch (`feat/*`, `fix/*`, `chore/*`) and open a Pull Request (PR).
- Use Conventional Commits in English (e.g., `feat(auth): add refresh token endpoint`).

## Language Standards
- **English Only**: All Java code, identifiers, comments, Javadoc, and documentation MUST be in English.

## Tech Stack
- Java 21, Spring Boot 4.1.1, Spring Data JPA, Hibernate, MySQL 8, Flyway, JJWT, Lombok.

## Build & Test Commands
- Compile project: `./mvnw clean compile`
- Run tests: `./mvnw test`
- Run application: `./mvnw spring-boot:run`

## Architecture & Conventions
- Layered architecture: `controller` -> `service` -> `repository` -> `entity`.
- Unified response envelope: `ApiResponse<T>` for all successful responses.
- Never modify existing Flyway migration files; create a new `V*__description.sql`.
- Run `./mvnw clean compile` before finishing tasks to ensure zero compilation or Lombok issues.
