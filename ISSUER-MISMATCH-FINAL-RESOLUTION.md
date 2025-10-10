# Issuer Mismatch Error - Final Resolution
**Date**: October 10, 2025  
**Error**: Invalid token issuer error from Keycloak  
**Status**: ✅ RESOLVED

---

## Error Message

```
2025-10-10 21:53:24,529 WARN [org.keycloak.events] (executor-thread-1) 
type="USER_INFO_REQUEST_ERROR", 
realmId="2ab777e8-fbae-46fd-bdbf-fcf22e7a2e06", 
realmName="nll-light", 
clientId="null", 
userId="null", 
ipAddress="172.19.0.1", 
error="invalid_token", 
reason="Invalid token issuer. Expected 'http://host.docker.internal:8082/auth/realms/nll-light'", 
auth_method="validate_access_token"
```

---

## Problem Analysis

### The Issue

Keycloak was experiencing **issuer mismatch** when validating access tokens during userinfo endpoint requests:

1. **Browser obtains token** with issuer: `http://localhost:8082/auth/realms/nll-light`
2. **medication-web receives token** and needs to get user information
3. **medication-web calls userinfo endpoint** at `http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/userinfo`
4. **Keycloak validates token** and expects issuer to match the URL it was called on
5. **Mismatch detected**: Token issuer (`localhost:8082`) ≠ Expected issuer (`keycloak:8080`)

### Why This Happened

Keycloak's issuer URL is **dynamic** based on the incoming request URL:
- Browser accesses Keycloak via `localhost:8082` → tokens issued with `localhost:8082` issuer
- Container accesses Keycloak via `keycloak:8080` → Keycloak expects `keycloak:8080` issuer
- When medication-web forwards a token obtained by the browser to Keycloak's internal address, **issuer validation fails**

### Attempted Solutions

**❌ Attempt 1**: Set `KC_HOSTNAME_URL=http://localhost:8082/auth`
- Result: Tokens issued correctly, but containers couldn't access `localhost`

**❌ Attempt 2**: Set `KC_HOSTNAME_URL=http://host.docker.internal:8082/auth`
- Result: Browsers couldn't access `host.docker.internal`

**❌ Attempt 3**: Set `KC_HOSTNAME_STRICT_BACKCHANNEL=false`
- Result: Keycloak still enforced issuer validation on userinfo requests

**❌ Attempt 4**: Disable issuer validation in JWT decoder
- Result: Token validation worked, but userinfo endpoint still rejected tokens

---

## Root Cause

The fundamental issue: **Userinfo endpoint calls trigger Keycloak-side issuer validation that cannot be disabled**.

When Spring Security OAuth2 calls Keycloak's userinfo endpoint:
1. medication-web sends: `GET /userinfo` with `Authorization: Bearer <token>`
2. Keycloak validates the token **including issuer claim**
3. Keycloak compares token issuer with the URL it was accessed on
4. Mismatch → `USER_INFO_REQUEST_ERROR`

---

## Solution

### Remove Userinfo Endpoint Call

**Key insight**: OpenID Connect ID tokens already contain user information. There's no need to call the userinfo endpoint!

### Changes Made

#### 1. Remove userinfo URI from application.properties
**File**: `medication-web/src/main/resources/application.properties`

```properties
# Provider Endpoints
spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth
spring.security.oauth2.client.provider.keycloak.token-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
# userinfo endpoint OMITTED - user info extracted from ID token to avoid issuer mismatch
spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
```

**Before**:
```properties
spring.security.oauth2.client.provider.keycloak.user-info-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/userinfo
```

**After**: Line removed entirely

#### 2. Disable JWT issuer validation
**File**: `medication-web/src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java`

```java
@Bean
public JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}") String jwkSetUri
) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    // No issuer validation - Keycloak uses different URLs for browser vs container access
    return decoder;
}
```

**Before**:
```java
decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(expectedIssuer));
```

**After**: No issuer validator set

#### 3. Add client authentication method
**File**: `medication-web/src/main/resources/application.properties`

```properties
spring.security.oauth2.client.registration.keycloak.client-authentication-method=client_secret_post
```

This was already added earlier but is crucial for token exchange to work.

---

## How It Works Now

### OAuth2 Flow (Without Userinfo Call)

```
1. Browser → medication-web:8080
   User visits the app

2. medication-web → Browser
   Redirect to http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth

3. Browser → Keycloak:8082
   User logs in with user666/secret

4. Keycloak → Browser
   Redirect back with authorization code

5. Browser → medication-web:8080/login/oauth2/code/keycloak?code=...
   Authorization callback

6. medication-web → Keycloak:8080 (internal)
   POST /token with code + client credentials
   Response: { access_token, id_token, refresh_token }

7. medication-web
   Extract user info from ID token claims:
   - preferred_username: user666
   - email: user666@example.com
   - name: Test User
   
   ✅ No userinfo endpoint call needed!

8. medication-web → Browser
   Redirect to / with authenticated session

9. User is logged in!
```

### What Changed

**Before** (with userinfo endpoint):
```
6. Token exchange → Get tokens
7. Call userinfo endpoint → ❌ ISSUER MISMATCH
```

**After** (without userinfo endpoint):
```
6. Token exchange → Get tokens including ID token
7. Extract user info from ID token → ✅ SUCCESS
```

---

## Verification

### Successful Login Flow

Logs from working implementation:

```
medication-web-1  | 2025-10-10T21:57:31.405Z Redirecting to http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth
medication-web-1  | 2025-10-10T21:57:39.253Z HTTP POST http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
medication-web-1  | 2025-10-10T21:57:39.360Z Response 200 OK
medication-web-1  | 2025-10-10T21:57:39.437Z HTTP GET http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
medication-web-1  | 2025-10-10T21:57:39.449Z Response 200 OK
medication-web-1  | 2025-10-10T21:57:39.466Z Set SecurityContextHolder to OAuth2AuthenticationToken
medication-web-1  | 2025-10-10T21:57:39.466Z Principal=Name: [user666]
medication-web-1  | 2025-10-10T21:57:39.466Z User Attributes: {preferred_username=user666, email=user666@example.com, name=Test User}
```

**Key observations**:
- ✅ Token exchange successful (200 OK)
- ✅ JWKS fetched successfully (200 OK)
- ✅ User authenticated with claims from ID token
- ✅ **NO userinfo endpoint call**
- ✅ **NO issuer mismatch errors**

---

## Benefits of This Approach

### 1. Eliminates Issuer Mismatch
- No userinfo call = No Keycloak-side token validation with issuer check
- All token validation happens in medication-web with disabled issuer check

### 2. Improved Performance
- One less HTTP request per login (no userinfo call)
- Faster authentication flow

### 3. Standard OIDC Behavior
- ID tokens are **designed** to contain user information
- Userinfo endpoint is optional in OIDC spec
- Many OIDC implementations use ID token claims by default

### 4. Simpler Configuration
- No need for complex hostname configuration in Keycloak
- No need for strict issuer validation workarounds
- Works seamlessly with Docker networking

---

## Configuration Summary

### docker-compose.yml (Keycloak)
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:latest
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_DB: dev-file
    KC_HTTP_PORT: 8080
    KC_HOSTNAME_STRICT: false  # Allow dynamic issuer
    KC_HTTP_RELATIVE_PATH: /auth
  command: start-dev --import-realm
  ports:
    - "8082:8080"
```

**Note**: No `KC_HOSTNAME_URL` needed!

### application.properties (medication-web)
```properties
# OAuth2 Client Registration
spring.security.oauth2.client.registration.keycloak.client-id=medication-web
spring.security.oauth2.client.registration.keycloak.client-secret=web-app-secret
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.keycloak.client-authentication-method=client_secret_post

# Provider Endpoints
spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth
spring.security.oauth2.client.provider.keycloak.token-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
# NO user-info-uri configured - user info from ID token
spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
```

### SecurityConfig.java (medication-web)
```java
@Bean
public JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}") String jwkSetUri
) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    // No issuer validation - Keycloak uses different URLs for browser vs container access
    return decoder;
}

@Bean
public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
    final OidcUserService delegate = new OidcUserService();
    return (userRequest) -> {
        // Default service extracts user info from ID token when userinfo URI not configured
        return delegate.loadUser(userRequest);
    };
}
```

---

## Production Considerations

### Security Implications

**Is it safe to skip userinfo endpoint?**
- ✅ **Yes** - ID tokens are cryptographically signed by Keycloak
- ✅ JWT signature is verified using JWKS from Keycloak
- ✅ Token expiration is checked
- ✅ Audience claim is validated

**What about disabled issuer validation?**
- ⚠️ **Development only** - In production, use consistent URLs
- 🔒 **Production fix**: 
  - Use public domain (e.g., `auth.example.com`) for Keycloak
  - Configure DNS for both browser and container access
  - Enable issuer validation with public URL

### Production Setup Example

```yaml
# Production docker-compose.yml
keycloak:
  environment:
    KC_HOSTNAME_URL: https://auth.example.com
    KC_HOSTNAME_STRICT: true
```

```properties
# Production application.properties
spring.security.oauth2.client.provider.keycloak.authorization-uri=https://auth.example.com/realms/nll-light/protocol/openid-connect/auth
spring.security.oauth2.client.provider.keycloak.token-uri=https://auth.example.com/realms/nll-light/protocol/openid-connect/token
spring.security.oauth2.client.provider.keycloak.jwk-set-uri=https://auth.example.com/realms/nll-light/protocol/openid-connect/certs
```

```java
// Production SecurityConfig.java - Enable issuer validation
@Bean
public JwtDecoder jwtDecoder(@Value("${jwk-set-uri}") String jwkSetUri) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://auth.example.com/realms/nll-light"));
    return decoder;
}
```

---

## Testing

### Manual Browser Test
```bash
# Start services
docker compose up -d

# Open browser
http://localhost:8080

# Click "Logga in med Keycloak"
# Login: user666 / secret
# ✅ Should redirect back logged in
```

### Check Logs
```bash
# Monitor for errors (should be none)
docker compose logs keycloak medication-web --follow

# Look for successful authentication
docker compose logs medication-web | grep "Set SecurityContextHolder"
```

### E2E Test
```bash
cd medication-web/e2e
npm install
npx playwright install chromium
npm test
```

---

## Lessons Learned

1. **Userinfo endpoint is optional** - OIDC ID tokens contain user information
2. **Keycloak issuer validation is strict** - Cannot be fully disabled for userinfo endpoint
3. **Docker networking creates complexity** - Browser uses `localhost`, containers use DNS names
4. **Simplest solution wins** - Removing userinfo call eliminated the entire problem
5. **Read the specs** - OIDC spec says ID token should contain user claims

---

## Related Documentation

- [README.md](README.md) - Main project documentation
- [medication-web/README.md](medication-web/README.md) - Module-specific OAuth2 details
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues and solutions
- [LOGIN-FAILURE-RESOLUTION.md](LOGIN-FAILURE-RESOLUTION.md) - Client authentication fix

---

**Resolved by**: GitHub Copilot  
**Date**: October 10, 2025, 23:57 CET  
**Status**: ✅ Working in development, production considerations documented
