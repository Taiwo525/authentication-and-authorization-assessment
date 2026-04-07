# Senior Backend Engineer Assessment

A production-ready authentication and authorization system built with Spring Boot, demonstrating clean architecture, modular design, and security best practices.

## Architecture

This project consists of two **independently buildable** modules:

### 1. `core-security-starter` (Reusable Library)
A standalone Spring Boot Starter that can be published to any Maven repository and consumed by any Spring Boot application. It provides:
- JWT authentication filter and token utilities
- Role-based authorization support
- 401/403 exception handlers with consistent error responses
- Authenticated request logging
- Externalized configuration properties

### 2. `sample-application` (Demo Application)
A sample Spring Boot application demonstrating the starter with:
- PostgreSQL database with JPA entities
- Flyway database migrations
- Clean layered architecture (Controller → Service → Repository)

## Key Features

- **BCrypt password hashing** for secure credential storage
- **Signed JWT tokens** containing userId, username, roles, and expiry
- **Role-based access control** with method-level security (`@PreAuthorize`)
- **Cross-cutting concerns** abstracted into the reusable starter
- **Database migrations** with Flyway
- **Integration tests** using H2 in-memory database

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (for running the sample application)

## Build

### Build the entire project:
```bash
mvn clean install
```

### Build only the starter (for publishing):
```bash
cd core-security-starter
mvn clean install
```

### Build only the sample application:
```bash
cd sample-application
mvn clean package
```

## Database Setup

Create a PostgreSQL database:
```sql
CREATE DATABASE sample_db;
```

## Configuration

The sample application uses environment variables with sensible defaults:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | postgres | Database password |
| `JWT_SECRET` | (default) | JWT signing key (min 32 chars) |
| `JWT_EXPIRY_SECONDS` | 3600 | Token expiration time |
| `JWT_ISSUER` | sample-application | Token issuer claim |

## Run Sample Application

```bash
cd sample-application
mvn spring-boot:run
```

Or with custom configuration:
```bash
DB_USERNAME=myuser DB_PASSWORD=mypass JWT_SECRET=my-super-secret-key-at-least-32-chars mvn spring-boot:run
```

## Demo Users

| Username | Password | Roles |
|----------|----------|-------|
| john | password123 | ROLE_USER |
| admin | admin123 | ROLE_USER, ROLE_ADMIN |

## API Endpoints

### Public Health Check
```bash
curl http://localhost:8089/api/v1/public/health
```

**Response:**
```json
{"status": "UP"}
```

### Login (Get JWT Token)
```bash
curl -X POST http://localhost:8089/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "password123"}'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwiaXNzIjoic2FtcGxlLWFwcGxpY2F0aW9uIiwiaWF0IjoxNzEyNDg1NjAwLCJleHAiOjE3MTI0ODkyMDAsInVzZXJJZCI6MSwidXNlcm5hbWUiOiJqb2huIiwicm9sZXMiOlsiUk9MRV9VU0VSIl19.xxx",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "userId": 1,
  "username": "john",
  "roles": ["ROLE_USER"]
}
```

### Get Current User (Requires Authentication)
```bash
# First, login and extract the token
TOKEN=$(curl -s -X POST http://localhost:8089/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "password123"}' | jq -r '.accessToken')

# Then use the token
curl http://localhost:8089/api/v1/user/me \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "id": 1,
  "username": "john",
  "roles": ["ROLE_USER"]
}
```

### List All Users (Requires ROLE_ADMIN)
```bash
# Login as admin
TOKEN=$(curl -s -X POST http://localhost:8089/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' | jq -r '.accessToken')

# Access admin endpoint
curl http://localhost:8089/api/v1/admin/users \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
[
  {"id": 1, "username": "john", "roles": ["ROLE_USER"]},
  {"id": 2, "username": "admin", "roles": ["ROLE_USER", "ROLE_ADMIN"]}
]
```

### Error Responses

**401 Unauthorized (Invalid/Missing Token):**
```json
{
  "timestamp": "2024-04-07T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/user/me"
}
```

**403 Forbidden (Insufficient Role):**
```json
{
  "timestamp": "2024-04-07T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/admin/users"
}
```

### Swagger UI
Access the interactive API documentation at:
```
http://localhost:8089/swagger-ui.html
```

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database with separate Flyway migrations.

## Using the Starter in Your Project

Add the dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>com.assessment</groupId>
    <artifactId>core-security-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Configure in `application.yml`:
```yaml
security:
  starter:
    jwt-secret: your-secret-key-at-least-32-characters
    token-expiry-seconds: 3600
    token-issuer: your-application
```

Implement `UserDetailsService` to load users from your data source.

## Design Decisions & Trade-offs

### Architecture Decisions

1. **Standalone Modules over Parent POM Inheritance**
   - *Decision*: Each module has its own complete POM and can be built/published independently.
   - *Trade-off*: Slight duplication of dependency versions vs. ability to publish `core-security-starter` to any Maven repository without requiring the parent POM.
   - *Benefit*: The starter can be consumed by any Spring Boot project, not just this monorepo.

2. **Spring Boot Starter Pattern**
   - *Decision*: The security library follows Spring Boot Starter conventions with `@AutoConfiguration` and `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
   - *Trade-off*: More boilerplate setup vs. seamless integration for consumers.
   - *Benefit*: Zero-config integration—just add the dependency and configure properties.

3. **`@ConditionalOnMissingBean` for All Beans**
   - *Decision*: Every bean in the auto-configuration uses `@ConditionalOnMissingBean`.
   - *Trade-off*: Consumers can accidentally override critical beans vs. full customization flexibility.
   - *Benefit*: Consumers can replace any component (e.g., custom `PasswordEncoder`, custom `JwtTokenService`).

### Security Decisions

4. **JWT in Authorization Header (not Cookies)**
   - *Decision*: Tokens are passed via `Authorization: Bearer <token>` header.
   - *Trade-off*: Requires client-side token storage vs. automatic cookie handling.
   - *Benefit*: Stateless, works well with SPAs and mobile apps, no CSRF concerns.

5. **BCrypt with Default Strength (10)**
   - *Decision*: Use `BCryptPasswordEncoder` with default cost factor.
   - *Trade-off*: ~100ms per hash vs. faster but weaker hashing.
   - *Benefit*: Industry-standard security, resistant to brute-force attacks.

6. **No Refresh Token Mechanism**
   - *Decision*: Single access token with configurable expiry (default 1 hour).
   - *Trade-off*: Users must re-login after token expiry vs. complexity of refresh token rotation.
   - *Benefit*: Simpler implementation, suitable for assessment scope. Production systems should add refresh tokens.

7. **Roles Stored in JWT**
   - *Decision*: User roles are embedded in the JWT claims.
   - *Trade-off*: Role changes don't take effect until token refresh vs. database lookup on every request.
   - *Benefit*: Truly stateless authentication, no database hit for authorization.

### Database Decisions

8. **Flyway for Schema Migrations**
   - *Decision*: Use Flyway with versioned SQL scripts.
   - *Trade-off*: Manual SQL vs. Hibernate auto-DDL.
   - *Benefit*: Reproducible, auditable schema changes across environments.

9. **Lazy Fetch for Roles with JOIN FETCH Queries**
   - *Decision*: `@ManyToMany(fetch = FetchType.LAZY)` with explicit `JOIN FETCH` in repository queries.
   - *Trade-off*: Must remember to use `findByUsernameWithRoles()` vs. eager loading everywhere.
   - *Benefit*: Avoids N+1 queries, better performance at scale.

10. **Runtime Data Initialization (not Flyway Seed Data)**
    - *Decision*: Demo users are created via `CommandLineRunner` with `PasswordEncoder`.
    - *Trade-off*: Data created on every fresh start vs. static SQL seed data.
    - *Benefit*: Passwords are properly BCrypt-encoded at runtime, avoiding hash compatibility issues.

### Testing Decisions

11. **H2 for Integration Tests**
    - *Decision*: Use H2 in-memory database with separate migrations for tests.
    - *Trade-off*: Slight SQL dialect differences vs. PostgreSQL dependency in CI.
    - *Benefit*: Fast tests, no external database required, CI/CD friendly.

12. **Cross-Cutting Concerns in Starter Only**
    - *Decision*: All security filters, exception handlers, and logging live in `core-security-starter`.
    - *Trade-off*: Sample app cannot customize error responses without overriding beans.
    - *Benefit*: Clean separation, sample app contains only business logic.

### Additional Production Considerations

-  **refresh token** mechanism with token rotation
-  **rate limiting** on login endpoint (e.g., using Bucket4j or Redis)
-  **account lockout** after failed login attempts
-  **asymmetric keys (RS256)** instead of symmetric HMAC for JWT signing
-  **audit logging** for security events
-  **Redis** for token blacklisting on logout

## Project Structure

```
├── core-security-starter/          # Reusable security library
│   ├── pom.xml                     # Standalone POM
│   └── src/main/java/
│       └── com/assessment/securitystarter/
│           ├── config/             # Auto-configuration
│           ├── security/           # JWT filter, token service
│           ├── error/              # Exception handlers
│           └── logging/            # Request logging
│
├── sample-application/             # Demo application
│   ├── pom.xml                     # Standalone POM
│   └── src/
│       ├── main/java/
│       │   └── com/assessment/sample/
│       │       ├── api/            # Controllers & DTOs
│       │       ├── service/        # Business logic
│       │       └── domain/         # Entities & Repositories
│       └── main/resources/
│           ├── application.yml
│           └── db/migration/       # Flyway migrations
│
└── pom.xml                         # Aggregator POM (optional)
```
