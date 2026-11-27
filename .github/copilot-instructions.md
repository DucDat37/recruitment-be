# GitHub Copilot Instructions for recruitment-be

Please refer to and follow the comprehensive project rules in `AGENTS.md`.

## Key Directives:
- **Git Branching**: Never commit directly to `main`. Create feature branches (`feat/*`, `fix/*`, `refactor/*`, `chore/*`, `docs/*`) and submit Pull Requests.
- **Language**: English is mandatory for all code, comments, Javadoc, and Git commit messages. User-facing messages (`message` in API responses, validation messages, emails) stay in Vietnamese.
- **Git Commits**: Conventional Commits in English (e.g., `feat(auth): add refresh token endpoint`).
- **Framework**: Java 21 + Spring Boot 4.1.1 + Spring Data JPA + MySQL 8 + Flyway.
- **Response Format**: Wrap successful responses in `ApiResponse<T>` (`common/ApiResponse.java`); errors use `ErrorResponse` via `GlobalExceptionHandler`.
- **DTOs**: Records grouped by feature in `dto/<feature>/` (e.g., `dto/auth/`).
- **Flyway Rule**: Never edit existing migration scripts; create new versioned migrations.
- **Verification**: Run `./mvnw clean compile` to ensure compilation and Lombok generation succeed.
