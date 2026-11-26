# GitHub Copilot Instructions for recruitment-be

Please refer to and follow the comprehensive project rules in `AGENTS.md`.

## Key Directives:
- **Git Branching**: Never commit directly to `main`. Create feature branches (`feat/*`, `fix/*`, `chore/*`) and submit Pull Requests.
- **Language**: English is mandatory for all code, comments, Javadoc, and Git commit messages.
- **Git Commits**: Conventional Commits in English (e.g., `feat(auth): add refresh token endpoint`).
- **Framework**: Java 21 + Spring Boot 4.1.1 + Spring Data JPA + MySQL 8 + Flyway.
- **Response Format**: Wrap responses in `ApiResponse<T>`.
- **Flyway Rule**: Never edit existing migration scripts; create new versioned migrations.
- **Verification**: Run `./mvnw clean compile` to ensure compilation and Lombok generation succeed.
