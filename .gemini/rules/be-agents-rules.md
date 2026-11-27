---
description: Rules and guidelines for Antigravity agents working on recruitment-be
globs: ["src/**/*", "pom.xml"]
---

# Antigravity Rules for recruitment-be

When editing or creating files in `recruitment-be`, follow `AGENTS.md` strictly:

1. **Git Workflow**: Never commit directly to `main`. Create a feature branch (`feat/*`, `fix/*`, `refactor/*`, `chore/*`, `docs/*`) and submit a Pull Request.
2. **Language Standards**: All code, identifiers, comments, and Git commits MUST be in **English**. User-facing messages (`message` in API responses, validation messages, emails) stay in Vietnamese.
3. **Commit Messages**: Use Conventional Commits in English (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`).
4. **Tech Stack**: Java 21, Spring Boot 4.1.1, Spring Data JPA, MySQL 8, Flyway, JJWT, Lombok.
5. **API & Architecture**: Keep strict layer separation (`controller` -> `service` -> `repository` -> `entity`). Successful responses are wrapped in `ApiResponse<T>`; errors use `ErrorResponse`. DTOs are records in `dto/<feature>/`.
6. **Flyway Migrations**: Never modify existing migrations in `db/migration/`. Always increment version (`V2__*.sql`).
7. **Verification**: Verify changes by running `./mvnw clean compile`.
