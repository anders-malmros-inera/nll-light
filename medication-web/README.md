# Medication Web Application

Spring Boot web application with Keycloak OAuth2/OpenID Connect authentication.

## Overview

This module provides:
- **User Interface**: Thymeleaf-based web pages for medication browsing
- **OAuth2 Client**: Authorization code flow integration with Keycloak
- **Session Management**: Secure cookie-based sessions
- **API Integration**: Calls medication-api via Kong gateway

## Architecture

```
┌──────────────┐
│   Browser    │
└──────┬───────┘
       │ 1. GET /
       ▼
┌──────────────────┐
│ medication-web   │
│ (This module)    │
└──────┬───────────┘
       │ 2. Redirect to
       │    /oauth2/authorization/keycloak
       ▼
┌──────────────────┐
│    Keycloak      │ 3. User login
│  (Auth Server)   │
└──────┬───────────┘
       │ 4. Callback with
       │    authorization code
       ▼
┌──────────────────┐
│ medication-web   │ 5. Exchange code
│                  │    for tokens
└──────┬───────────┘
       │ 6. Call API with
       │    access token
       ▼
┌──────────────────┐
│     Kong         │
│   (Gateway)      │
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│ medication-api   │
└──────────────────┘
```

## OAuth2 Configuration

### Application Properties
File: `src/main/resources/application.properties`

```properties
# OAuth2 Client Registration
spring.security.oauth2.client.registration.keycloak.client-id=medication-web
spring.security.oauth2.client.registration.keycloak.client-secret=web-app-secret
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

# Provider Endpoints
# Note: Mixed browser-facing (localhost:8082) and container-internal (keycloak:8080) URLs
spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth
spring.security.oauth2.client.provider.keycloak.token-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
spring.security.oauth2.client.provider.keycloak.user-info-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/userinfo
spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
```

### Why Mixed URLs?

**Browser-facing (`localhost:8082`)**:
- The authorization endpoint must be accessible from the user's browser
- The browser redirects directly to Keycloak for login

**Container-internal (`keycloak:8080`)**:
- Token exchange, userinfo, and JWKS endpoints are called server-side
- The web container uses Docker network DNS to reach Keycloak
- More secure as these endpoints aren't exposed to the browser

### Custom JWT Decoder

File: `src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java`

```java
@Bean
public JwtDecoder jwtDecoder() {
    // Fetch JWKS from container-internal URL
    String jwkSetUri = "http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs";
    
    // Validate issuer as browser-facing URL (matches token issuer claim)
    String expectedIssuer = "http://localhost:8082/auth/realms/nll-light";
    
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(expectedIssuer));
    return decoder;
}
```

**Why this is needed:**
- Keycloak issues tokens with issuer `http://localhost:8082/auth/realms/nll-light`
- The web app runs in a container and reaches Keycloak at `keycloak:8080`
- Without custom decoder, Spring tries to auto-discover at `localhost:8082` from inside the container (fails)
- This bean fetches JWKS via container DNS while validating the browser-facing issuer

## Security Configuration

### SecurityFilterChain
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/error").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
        );
    return http.build();
}
```

**Access Control**:
- `/`, `/login`, `/error` → Public
- All other endpoints → Require authentication

**Login Flow**:
1. Unauthenticated user visits `/medications`
2. Redirected to `/login`
3. User clicks "Logga in med Keycloak"
4. Redirected to `/oauth2/authorization/keycloak`
5. Spring Security redirects to Keycloak authorization endpoint
6. User enters credentials at Keycloak
7. Keycloak redirects back to `/login/oauth2/code/keycloak`
8. Spring Security exchanges code for tokens
9. User session established, redirected to original URL

## Templates

### Login Page
File: `src/main/resources/templates/login.html`

```html
<a href="/oauth2/authorization/keycloak" class="keycloak-btn">
    Logga in med Keycloak
</a>
```

### Authenticated Pages
Access user information via Spring Security:

```java
@GetMapping("/medications")
public String medications(Model model, @AuthenticationPrincipal OAuth2User principal) {
    String username = principal.getAttribute("preferred_username");
    String email = principal.getAttribute("email");
    model.addAttribute("username", username);
    // ... fetch medications from API
    return "medications";
}
```

In Thymeleaf:
```html
<p>Welcome, <span th:text="${username}">User</span>!</p>
```

## API Integration

### Calling medication-api
```java
@Value("${api.base-url}")
private String apiBaseUrl;

public List<Medication> getMedications() {
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<Medication[]> response = restTemplate.getForEntity(
        apiBaseUrl + "/api/medications",
        Medication[].class
    );
    return Arrays.asList(response.getBody());
}
```

**Environment Variables**:
- `api.base-url` (default: `http://kong:8000` in Docker)

## Testing

### E2E Tests (Playwright)
Location: `e2e/`

**Setup**:
```powershell
cd e2e
npm install
npx playwright install chromium
```

**Run**:
```powershell
npm test
```

**What it tests**:
1. Navigate to http://localhost:8080
2. Click "Logga in med Keycloak"
3. Fill credentials (user666/secret)
4. Submit login form
5. Verify redirect back to app
6. Assert logged-in state (logout link visible)

See `e2e/README.md` for detailed test documentation.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `API_BASE_URL` | `http://kong:8000` | Base URL for medication-api |
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring profile |
| `SERVER_PORT` | `8080` | Web server port |

## Running Locally

### With Docker (Recommended)
```bash
# From project root
docker compose up --build
```

Access: http://localhost:8080

### Without Docker (Development)
```bash
# Start Keycloak (requires Docker)
docker run -p 8082:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HTTP_RELATIVE_PATH=/auth \
  quay.io/keycloak/keycloak:26.0.4 start-dev

# Update application.properties to point to localhost for all endpoints
# (Change keycloak:8080 → localhost:8082 in token-uri, user-info-uri, jwk-set-uri)

# Start medication-web
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

Access: http://localhost:8080

## Debugging

### Enable Debug Logging
Already enabled in `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.web.client=DEBUG
```

### View Logs
```bash
# Docker
docker compose logs -f medication-web

# Local
# Logs appear in console where you ran mvn spring-boot:run
```

### Common Issues

#### Login redirects to Keycloak but fails to complete
**Check**:
1. Keycloak logs for errors: `docker compose logs keycloak`
2. Network tab in browser for failed requests
3. Issuer in JWT matches `expectedIssuer` in `SecurityConfig.java`

**Solution**: Verify custom `JwtDecoder` bean is configured correctly

#### "Invalid token issuer" error
**Cause**: JWT issuer claim doesn't match expected issuer

**Solution**:
1. Get actual issuer from Keycloak discovery:
   ```bash
   curl http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration | jq .issuer
   ```
2. Update `expectedIssuer` in `SecurityConfig.java`
3. Rebuild: `docker compose build medication-web`

#### Can't reach medication-api
**Check**:
```bash
# From inside web container
docker compose exec medication-web curl http://kong:8000/api/medications
```

**Solution**: Verify Kong is running and `api.base-url` is correct

## Security Best Practices

### Production Checklist
- [ ] Change client secret (generate in Keycloak admin console)
- [ ] Use environment variables for secrets (never hardcode)
- [ ] Enable HTTPS for all OAuth2 endpoints
- [ ] Configure PKCE (Keycloak client settings)
- [ ] Set short token expiration times
- [ ] Enable token refresh rotation
- [ ] Disable debug logging
- [ ] Configure CSRF protection
- [ ] Set secure cookie flags (SameSite, Secure, HttpOnly)
- [ ] Implement logout functionality
- [ ] Add session timeout

### Token Management
- **Access Token**: Short-lived (300s default), used for API calls
- **Refresh Token**: Longer-lived, used to obtain new access tokens
- **ID Token**: Contains user identity claims

**Spring Security handles**:
- Token storage (in session)
- Automatic refresh when access token expires
- Token validation (signature, expiration, issuer)

## Project Structure

```
medication-web/
├── src/
│   ├── main/
│   │   ├── java/se/inera/nll/nlllight/web/
│   │   │   ├── WebApplication.java          # Main entry point
│   │   │   ├── SecurityConfig.java          # OAuth2 & JWT config
│   │   │   └── controller/
│   │   │       ├── HomeController.java      # Public pages
│   │   │       └── MedicationController.java # Protected pages
│   │   └── resources/
│   │       ├── application.properties       # OAuth2 client config
│   │       ├── static/                      # CSS, JS
│   │       └── templates/                   # Thymeleaf templates
│   │           ├── login.html
│   │           └── medications.html
│   └── test/
│       └── java/                            # Unit tests (if any)
├── e2e/                                     # Playwright E2E tests
│   ├── tests/login.spec.js
│   ├── package.json
│   ├── playwright.config.js
│   └── README.md
├── Dockerfile
└── pom.xml
```

## Further Reading

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Keycloak Spring Security Integration](https://www.keycloak.org/docs/latest/securing_apps/#_spring_security_adapter)
- [OAuth2 Authorization Code Flow](https://oauth.net/2/grant-types/authorization-code/)
- [Thymeleaf + Spring Security](https://www.thymeleaf.org/doc/articles/springsecurity.html)

---

**Quick Commands**

```bash
# Build
mvn clean package

# Run (local, requires Keycloak)
mvn spring-boot:run

# Run (Docker)
cd .. && docker compose up medication-web

# Run E2E tests
cd e2e && npm test

# View logs
docker compose logs -f medication-web
```
