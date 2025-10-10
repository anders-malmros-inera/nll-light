# nll-light

Secure medication management application demonstrating modern OAuth2/OpenID Connect architecture with Spring Boot, Keycloak, and Kong API Gateway.

## Overview

Java multi-module project with:
- **medication-api**: Spring Boot REST API for medications (H2 in-memory DB)
- **medication-web**: Spring Boot web app with Keycloak OIDC authentication
- **kong**: API Gateway routing requests between web and API
- **keycloak**: OIDC identity provider with pre-configured realm and users

## Architecture

```
┌─────────────────┐
│   Web Browser   │
│                 │
│ localhost:8080  │ ◄───── User accesses web app
└────────┬────────┘
         │
         │ OAuth2/OIDC Flow
         ▼
┌─────────────────┐
│  Medication Web │
│  (Spring Boot)  │ ◄───── OAuth2 Client
│   port :8080    │        (authorization_code)
└────────┬────────┘
         │
         │ ┌──────────────────────────┐
         │ │ Browser: localhost:8082  │ ◄─ Authorization endpoint
         ├─┤ Container: keycloak:8080 │ ◄─ Token/userinfo/JWKS
         │ └──────────────────────────┘
         │          ▼
         │   ┌─────────────┐
         │   │  Keycloak   │
         │   │    OIDC     │
         │   │ Provider    │
         │   └─────────────┘
         │
         │ API calls
         ▼
┌─────────────┐         ┌─────────────────┐
│    Kong     │────────►│  Medication API │
│  Gateway    │         │   (REST API)    │
│  :8000      │         │     :8081       │
└─────────────┘         └────────┬────────┘
                                 │
                                 ▼
                          ┌─────────────┐
                          │ H2 Database │
                          │ (In-memory) │
                          └─────────────┘
```

## Features

### Authentication & Authorization
- **OAuth2/OpenID Connect** via Keycloak
- **Authorization Code Flow** with PKCE support
- **Spring Security** OAuth2 client integration
- **Custom JWT validation** handling Docker network/host URL resolution
- **Session management** with secure cookie handling

### API Endpoints
- `GET /api/medications` - List all medications
- `GET /api/medications/{id}` - Get medication by ID
- `GET /api/medications/search?name={query}` - Search medications by name

### Pre-seeded Data
- **Alimemazin**: Antihistamin. Exempelindikation: allergiska besvär.
- **Elvanse**: CNS-stimulerande läkemedel. Exempelindikation: ADHD.
- **Melatonin**: Hormonpreparat. Exempelindikation: sömnstörningar.

## Quick Start

### Prerequisites
- **Docker** & **Docker Compose**
- **Node.js** 16+ (for E2E tests, optional)
- **Maven 3.8+** (for local development without Docker)

### Start All Services (Docker)
```bash
# Build and start all services
docker compose up --build

# Access points:
# - Web App: http://localhost:8080
# - API Gateway (Kong): http://localhost:8000
# - API Direct: http://localhost:8081
# - Kong Admin: http://localhost:8001
# - Keycloak Admin: http://localhost:8082/auth/admin (admin/admin)
# - Keycloak Account: http://localhost:8082/auth/realms/nll-light/account
```

### Test Login Flow
1. Open browser: http://localhost:8080
2. Click **"Logga in med Keycloak"**
3. Enter credentials: `user666` / `secret`
4. You'll be redirected back and see a personalized greeting

## OAuth2 / Keycloak Configuration

### Keycloak Pre-configured Setup
The application includes a fully configured Keycloak realm:

- **Realm**: `nll-light`
- **Client ID**: `medication-web`
- **Client Secret**: `web-app-secret`
- **Redirect URI**: `http://localhost:8080/login/oauth2/code/keycloak`
- **Scopes**: `openid`, `profile`, `email`
- **Test User**: 
  - Username: `user666`
  - Password: `secret`
  - Email: `user666@example.com`

### Keycloak Docker Configuration
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0.4
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_DB: dev-file
    KC_HTTP_PORT: 8080
    KC_HOSTNAME_STRICT: false
    KC_HTTP_RELATIVE_PATH: /auth
  command: start-dev --import-realm
  volumes:
    - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
  ports:
    - "8082:8080"  # Host port 8082 → Container port 8080
```

### Spring OAuth2 Client Configuration
The web app (`medication-web/src/main/resources/application.properties`) uses explicit provider URIs to handle Docker networking:

```properties
# OAuth2 Client Registration
spring.security.oauth2.client.registration.keycloak.client-id=medication-web
spring.security.oauth2.client.registration.keycloak.client-secret=web-app-secret
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

# Provider endpoints (mixed browser-facing and container-internal URLs)
spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth
spring.security.oauth2.client.provider.keycloak.token-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
spring.security.oauth2.client.provider.keycloak.user-info-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/userinfo
spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
```

**Why mixed URLs?**
- **Browser-facing** (`localhost:8082`): Authorization endpoint must be reachable by the user's browser
- **Container-internal** (`keycloak:8080`): Token, userinfo, and JWK endpoints are called server-side from the web container

### Custom JWT Decoder
`medication-web` includes a custom `JwtDecoder` bean in `SecurityConfig.java` to handle issuer validation:

```java
@Bean
public JwtDecoder jwtDecoder() {
    String jwkSetUri = "http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs";
    String expectedIssuer = "http://localhost:8082/auth/realms/nll-light";
    
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(expectedIssuer));
    return decoder;
}
```

This allows tokens issued with `localhost:8082` issuer to be validated while fetching JWKS from the container-accessible URL.

## Keycloak Admin Console

Access the Keycloak admin console to manage realms, clients, and users:

- **URL**: http://localhost:8082/auth/admin
- **Credentials**: `admin` / `admin`

### Key Admin Tasks
- View/edit realm `nll-light`
- Manage client `medication-web` configuration
- Add/modify users and roles
- View authentication sessions
- Configure identity providers

## Testing

### Unit & Integration Tests
The application includes comprehensive JUnit tests:

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn -pl medication-api test
```

**Test Coverage:**
- **MedicationControllerTest**: Tests for medication API endpoints
  - `list_shouldReturnSeededMedications`: Verifies listing all medications
  - `search_shouldFindMelatoninByPartial`: Tests partial name search functionality
  - `get_shouldReturnSingleMedication`: Tests retrieving individual medication by ID

Tests run automatically during Docker build and must pass for successful deployment.

### End-to-End (E2E) Tests
Lightweight Playwright tests verify the full OAuth2 login flow:

**Location**: `medication-web/e2e/`

**Prerequisites**:
- Node.js 16+
- Docker services running

**Setup and Run**:
```powershell
cd medication-web\e2e

# Install dependencies
npm install

# Install Playwright browser
npx playwright install chromium

# Run tests
npm test
```

**What the E2E test does:**
1. Opens http://localhost:8080
2. Clicks "Logga in med Keycloak"
3. Fills Keycloak login form (user666/secret)
4. Submits credentials
5. Asserts successful redirect back to the app
6. Verifies logged-in state (logout link or username displayed)

**PowerShell Execution Policy Note:**
If you encounter `npm` execution errors due to PowerShell policy, use one of these approaches:

**Option A** (recommended): Run npm via Node directly
```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
node "C:\Program Files\nodejs\node_modules\npm\bin\npx-cli.js" playwright install chromium
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test
```

**Option B**: Temporarily enable script execution (requires admin)
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
npm install
# ... run other commands
```

### Manual API Testing
When the application is running, test endpoints manually:

**Via Kong Gateway (recommended):**
```bash
# List all medications
curl http://localhost:8000/api/medications

# Search medications
curl http://localhost:8000/api/medications/search?name=mel

# Get specific medication
curl http://localhost:8000/api/medications/1
```

**Direct API access:**
```bash
curl http://localhost:8081/api/medications
```

### Swagger UI Testing
Interactive API documentation and testing:
- **Via Kong**: http://localhost:8000/swagger-ui.html
- **Direct API**: http://localhost:8081/swagger-ui/index.html

### Test Documentation
Detailed test documentation with sequence diagrams: `docs/TEST-DOCUMENTATION.md`

## Configuration

### Environment Variables
- `API_BASE_URL`: Web app API endpoint (default: `http://kong:8000`)
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: `docker`)
- `APP_URL`: E2E test target URL (default: `http://localhost:8080`)
- `KEYCLOAK_USER`: E2E test username (default: `user666`)
- `KEYCLOAK_PASS`: E2E test password (default: `secret`)

### Kong Gateway Configuration
Kong is configured in declarative (DB-less) mode via `kong.yml`:

**Services:**
- `medication-api-service` → `http://medication-api:8080`

**Routes:**
- `/api/medications*` → medication API service
- `/v3/api-docs*` → OpenAPI specification
- `/swagger-ui.html` → Swagger UI entry
- `/swagger-ui/**` → Swagger UI resources

**Admin API**: http://localhost:8001

**Example Admin Queries:**
```bash
# View services
curl http://localhost:8001/services

# View routes
curl http://localhost:8001/routes

# Health check
curl http://localhost:8001/status
```

### Application Profiles
- **docker**: Used in containers; API accessed via Kong gateway (`http://kong:8000`)
- **default**: Local development; API accessed directly (`http://localhost:8081`)

## Development

### Project Structure
```
nll-light/
├── medication-api/              # REST API module
│   ├── src/main/java/          # Application code
│   │   └── se/inera/nll/       # Controllers, services, repositories
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── data.sql            # Pre-seeded medication data
│   └── src/test/java/          # Integration tests
├── medication-web/             # Web application module
│   ├── src/main/java/
│   │   └── se/inera/nll/       # Controllers, security config
│   ├── src/main/resources/
│   │   ├── application.properties  # OAuth2 client config
│   │   └── templates/          # Thymeleaf templates
│   └── e2e/                    # Playwright E2E tests
│       ├── package.json
│       ├── playwright.config.js
│       ├── tests/
│       │   └── login.spec.js
│       └── README.md
├── keycloak/
│   └── realm-export.json       # Pre-configured realm, client, user
├── docs/                       # Documentation & diagrams
│   ├── TEST-DOCUMENTATION.md
│   └── *.puml                  # Sequence diagrams
├── kong.yml                    # Kong gateway declarative config
├── docker-compose.yml          # Multi-service orchestration
└── pom.xml                     # Multi-module Maven config
```

### Database
- **Type**: H2 In-memory
- **Schema**: Auto-created by Hibernate
- **Data**: Seeded from `data.sql` on startup
- **Reset**: Data resets on application restart (ephemeral)
- **Console**: Not exposed by default (add `spring.h2.console.enabled=true` for debugging)

### Local Development (without Docker)
```bash
# Terminal 1: Start Keycloak (requires Docker)
docker run -p 8082:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HTTP_RELATIVE_PATH=/auth \
  quay.io/keycloak/keycloak:26.0.4 start-dev

# Terminal 2: Start API
cd medication-api
mvn spring-boot:run

# Terminal 3: Start Web App
cd medication-web
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

**Note**: When running locally, update `application.properties` OAuth2 URIs to match your local Keycloak instance.

## Monitoring & Logging

### Application Logs
```bash
# View all logs
docker compose logs -f

# View specific service
docker compose logs -f medication-web
docker compose logs -f medication-api
docker compose logs -f keycloak
docker compose logs -f kong

# Tail last N lines
docker compose logs --tail 50 medication-web
```

### Debug Logging
The web app has enhanced debug logging enabled for OAuth2 troubleshooting:

```properties
# In medication-web/src/main/resources/application.properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.web.client=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Kong Monitoring
```bash
# View services
curl http://localhost:8001/services

# View routes
curl http://localhost:8001/routes

# Health check
curl http://localhost:8001/status
```

## Troubleshooting

### Common Issues

#### 1. OAuth2 Login Fails / Redirect Loop
**Symptoms**: Clicking login redirects to Keycloak, but after entering credentials the app doesn't complete authentication.

**Causes & Solutions**:
- **Issuer mismatch**: Tokens are issued with `http://localhost:8082/...` but app expects `http://keycloak:8080/...`
  - ✅ **Solution**: Use the custom `JwtDecoder` bean in `SecurityConfig.java` (already implemented)
  - Validates issuer as `localhost:8082` while fetching JWKS from `keycloak:8080`

- **Missing `client_id` parameter**:
  - Check Keycloak logs: `docker compose logs keycloak | grep client_id`
  - Ensure `application.properties` has correct `client-id` configuration

- **Wrong redirect URI**:
  - Verify in Keycloak admin console: Client `medication-web` → Valid Redirect URIs = `http://localhost:8080/*`
  - Check `application.properties`: `redirect-uri={baseUrl}/login/oauth2/code/{registrationId}`

#### 2. Keycloak Container Fails to Start
**Symptoms**: `docker compose up` shows Keycloak errors or exits immediately.

**Solutions**:
```bash
# Check logs
docker compose logs keycloak

# Remove volumes and restart fresh
docker compose down -v
docker compose up --build
```

Common Keycloak startup issues:
- Port 8082 already in use → Change host port in `docker-compose.yml`
- Realm import failed → Validate `keycloak/realm-export.json` syntax

#### 3. Kong Gateway Not Routing
**Symptoms**: `http://localhost:8000/api/medications` returns 404 or connection error.

**Solutions**:
```bash
# Verify Kong configuration loaded
curl http://localhost:8001/services

# Check if medication-api is reachable from Kong
docker compose exec kong curl http://medication-api:8080/api/medications

# Restart Kong
docker compose restart kong
```

#### 4. PowerShell npm Execution Errors
**Symptoms**: `npm : File ... npm.ps1 cannot be loaded because running scripts is disabled`

**Solutions** (choose one):

**Option A**: Run npm via Node (no policy change needed)
```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
```

**Option B**: Set execution policy for current session (admin required)
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
```

#### 5. Port Conflicts
**Symptoms**: `docker compose up` fails with "address already in use"

**Solutions**:
```bash
# Check which ports are in use
netstat -ano | findstr :8080
netstat -ano | findstr :8082
netstat -ano | findstr :8000

# Stop conflicting services or change ports in docker-compose.yml
```

Default ports used:
- 8080: medication-web
- 8081: medication-api
- 8082: Keycloak (host) → 8080 (container)
- 8000: Kong gateway
- 8001: Kong admin API

#### 6. Tests Failing During Build
**Symptoms**: `docker compose build` fails during test execution

**Solutions**:
```bash
# Run tests locally to see detailed output
mvn test

# Skip tests during Docker build (not recommended)
docker compose build --build-arg MAVEN_OPTS="-DskipTests"
```

#### 7. Browser Shows "Issuer Mismatch" Error
**Symptoms**: JWT validation fails with issuer error in logs

**Root Cause**: The custom `JwtDecoder` expects issuer `http://localhost:8082/auth/realms/nll-light` but Keycloak is configured differently.

**Solutions**:
1. Check Keycloak discovery endpoint:
   ```bash
   curl http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration
   ```
   Look for `"issuer"` field value

2. Update `SecurityConfig.java` `expectedIssuer` to match

3. Rebuild and restart:
   ```bash
   docker compose build medication-web
   docker compose up -d medication-web
   ```

### Reset Everything
If issues persist, perform a complete reset:

```bash
# Stop and remove all containers, networks, volumes
docker compose down -v

# Remove images (optional, forces rebuild)
docker compose down --rmi all

# Rebuild and start fresh
docker compose up --build
```

### Getting Help
When reporting issues, include:
1. Docker compose logs: `docker compose logs > logs.txt`
2. Browser network tab (for OAuth2 issues)
3. Keycloak admin console screenshots
4. Steps to reproduce

## API Documentation

### Swagger / OpenAPI
Interactive API documentation available at:
- **Via Kong**: http://localhost:8000/swagger-ui.html
- **Direct**: http://localhost:8081/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8000/v3/api-docs

### Medication API Endpoints

#### List Medications
```http
GET /api/medications
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Alimemazin",
    "category": "Antihistamin",
    "indication": "Exempelindikation: allergiska besvär."
  },
  ...
]
```

#### Get Medication by ID
```http
GET /api/medications/{id}
```

**Parameters**:
- `id` (path, integer): Medication ID

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Alimemazin",
  "category": "Antihistamin",
  "indication": "Exempelindikation: allergiska besvär."
}
```

#### Search Medications
```http
GET /api/medications/search?name={query}
```

**Parameters**:
- `name` (query, string): Partial or full medication name

**Response** (200 OK):
```json
[
  {
    "id": 3,
    "name": "Melatonin",
    "category": "Hormonpreparat",
    "indication": "Exempelindikation: sömnstörningar."
  }
]
```

## Security Considerations

### Production Deployment
Before deploying to production, ensure:

1. **Change default credentials**:
   - Keycloak admin: `admin/admin` → strong password
   - Client secret: Generate new secret in Keycloak admin console
   - Test user: Create proper user accounts

2. **Use HTTPS**:
   - Configure TLS certificates for Keycloak
   - Update OAuth2 URIs to use `https://`
   - Enable HSTS headers

3. **Environment-specific configuration**:
   - Use environment variables for secrets
   - Never commit `client-secret` to version control
   - Use Spring Cloud Config or similar for centralized config

4. **Database**:
   - Replace H2 in-memory with persistent database (PostgreSQL, MySQL)
   - Configure Keycloak with production-grade database
   - Enable database encryption at rest

5. **Kong security**:
   - Enable rate limiting
   - Add authentication plugins
   - Configure CORS policies
   - Use Kong's security plugins (JWT, OAuth2, etc.)

6. **Logging & Monitoring**:
   - Disable debug logging in production
   - Set up centralized logging (ELK stack, Splunk)
   - Configure health check endpoints
   - Enable application performance monitoring (APM)

### OAuth2 Security Best Practices
- ✅ Uses authorization code flow (more secure than implicit)
- ✅ PKCE support available (configure in Keycloak client)
- ✅ Short-lived access tokens (300s default)
- ✅ Refresh token rotation enabled
- ⚠️ Client secret in plaintext → Use environment variables in production
- ⚠️ No token encryption → Consider JWE for sensitive data

## Additional Resources

### Documentation
- **Test Documentation**: `docs/TEST-DOCUMENTATION.md`
- **E2E Test README**: `medication-web/e2e/README.md`
- **Sequence Diagrams**: `docs/*.puml`

### External Resources
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Kong Gateway Documentation](https://docs.konghq.com/gateway/latest/)
- [Playwright Testing](https://playwright.dev/)

### Architecture Patterns
This application demonstrates:
- **Backend for Frontend (BFF)** pattern with Kong gateway
- **OAuth2 Authorization Code Flow** with Keycloak
- **Microservices** architecture with Docker Compose
- **API Gateway** pattern for routing and security
- **Infrastructure as Code** with declarative configurations

## Contributing

### Code Style
- Java: Follow Spring Boot conventions
- Formatting: Use default IntelliJ/Eclipse formatters
- Tests: Write tests for new features

### Pull Request Process
1. Create a feature branch
2. Ensure all tests pass (`mvn test`)
3. Update documentation if needed
4. Submit PR with clear description

## License

This is a demonstration/educational project. Check with your organization for licensing terms.

---

**Quick Reference Commands**

```bash
# Start everything
docker compose up --build

# View logs
docker compose logs -f medication-web

# Run tests
mvn test

# Run E2E tests
cd medication-web/e2e && npm test

# Reset environment
docker compose down -v && docker compose up --build

# Access points
# Web: http://localhost:8080 (user666/secret)
# API: http://localhost:8000/api/medications
# Keycloak Admin: http://localhost:8082/auth/admin (admin/admin)
```