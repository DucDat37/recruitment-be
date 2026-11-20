---
description: Rules and guidelines for Antigravity agents working on recruitment-be
globs: ["src/**/*", "pom.xml"]
---

# Antigravity Rules for recruitment-be

When editing or creating files in `recruitment-be`, follow `AGENTS.md` strictly:

1. **Git Workflow**: Never commit directly to `main`. Create a feature branch (`feat/*`, `fix/*`, `chore/*`) and submit a Pull Request.
2. **Language Standards**: All code, identifiers, comments, documentation, and Git commits MUST be in **English**.
3. **Commit Messages**: Use Conventional Commits in English (`feat:`, `fix:`, `refactor:`, `chore:`, `test:`).
4. **Tech Stack**: Java 21, Spring Boot 4.1.1, Spring Data JPA, MySQL 8, Flyway, JJWT, Lombok.
5. **API & Architecture**: Keep strict layer separation (`controller` -> `service` -> `repository` -> `entity`). All responses wrapped in `ApiResponse<T>`.
6. **Flyway Migrations**: Never modify existing migrations in `db/migration/`. Always increment version (`V2__*.sql`).
7. **Verification**: Verify changes by running `./mvnw clean compile`.
