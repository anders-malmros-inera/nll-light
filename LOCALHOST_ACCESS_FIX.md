# Fixing localhost:8080 Access Issue

## Problem
The medication-web service was failing to start with the error:
```
Unable to resolve Configuration with the provided Issuer of "http://localhost:8082/auth/realms/nll-light"
Connection refused
```

## Root Cause
When we added `issuer-uri` configuration for OIDC logout, Spring Boot's auto-configuration tried to fetch the OIDC discovery document (`.well-known/openid-configuration`) from `http://localhost:8082` during application startup.

However, the medication-web container runs inside Docker's internal network where:
- ❌ `localhost:8082` is NOT accessible (refers to the container itself, not the host)
- ✅ `keycloak:8080` IS accessible (internal Docker DNS)
- ✅ `localhost:8082` IS only accessible from the host machine (your browser)

This created a conflict:
- Browser needs: `http://localhost:8082` (for authorization and logout redirect)
- Container needs: `http://keycloak:8080` (for token exchange and JWK fetching)

## Solution

### 1. Removed issuer-uri Configuration
**File**: `application.properties`

Removed the problematic line:
```properties
# REMOVED: spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8082/auth/realms/nll-light
```

Added explanation comment:
```properties
# Note: issuer-uri is NOT set here because it would try to fetch .well-known/openid-configuration at startup
# from localhost:8082 which is not accessible from inside the container. Instead, we manually configure
# all required endpoints above. The issuer-uri for logout is constructed dynamically in SecurityConfig.
```

### 2. Implemented Custom Logout Handler
**File**: `SecurityConfig.java`

Replaced `OidcClientInitiatedLogoutSuccessHandler` (which requires issuer-uri) with a custom implementation that manually constructs the Keycloak logout URL:

```java
private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
        // Construct Keycloak logout URL manually
        // Browser needs to access Keycloak at localhost:8082 (not internal keycloak:8080)
        String logoutUrl = UriComponentsBuilder
            .fromUriString("http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout")
            .queryParam("post_logout_redirect_uri", "http://localhost:8080/login?logout")
            .build()
            .toUriString();
        
        try {
            response.sendRedirect(logoutUrl);
        } catch (IOException e) {
            // If redirect fails, fall back to local logout
            response.sendRedirect("/login?logout");
        }
    };
}
```

### 3. Updated Imports
Added necessary imports for the custom handler:
```java
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import java.io.IOException;
```

Removed unused import:
```java
// Removed: import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
```

## How It Works Now

### Startup Flow
1. Application starts in Docker container
2. No issuer-uri configured → no attempt to fetch `.well-known/openid-configuration`
3. Manually configured endpoints are used:
   - Authorization: `http://localhost:8082` (browser-accessible)
   - Token exchange: `http://keycloak:8080` (container-accessible)
   - JWK Set: `http://keycloak:8080` (container-accessible)
4. Application starts successfully ✅

### Logout Flow
1. User clicks "Logout"
2. Custom logout handler executes
3. Manually constructed logout URL redirects browser to:
   - `http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout?post_logout_redirect_uri=http://localhost:8080/login?logout`
4. Keycloak terminates session
5. Browser redirected back to login page ✅

## Network Architecture

```
┌─────────────────┐
│  Browser/Host   │
│                 │
│  localhost:8080 ──────► medication-web (external access)
│  localhost:8082 ──────► keycloak (external access)
└─────────────────┘
         │
         │ Docker Network
         ▼
┌──────────────────────────────────────┐
│  Docker Internal Network             │
│                                      │
│  ┌────────────────┐                 │
│  │ medication-web │                 │
│  │                │                 │
│  │ Can access:    │                 │
│  │ - keycloak:8080 ───► Keycloak   │
│  │                │     (internal)  │
│  │ Cannot access: │                 │
│  │ - localhost:8082 ✗               │
│  └────────────────┘                 │
│                                      │
│  ┌────────────────┐                 │
│  │   Keycloak     │                 │
│  │   :8080        │                 │
│  └────────────────┘                 │
└──────────────────────────────────────┘
```

## Testing

### Verify Application is Accessible
1. Open browser to http://localhost:8080
2. Should see the login page ✅

### Verify OAuth2 Login Works
1. Click "Logga in med Keycloak"
2. Should redirect to Keycloak login page ✅
3. Enter credentials (e.g., patient001/patient001)
4. Should redirect back to application dashboard ✅

### Verify Logout Works
1. Click "Logout" button
2. Should redirect to Keycloak logout
3. Should redirect back to login page with success message ✅
4. Click "Logga in med Keycloak" again
5. Should require credentials (not auto-login) ✅

## Files Modified

1. **medication-web/src/main/resources/application.properties**
   - Removed `issuer-uri` configuration
   - Added explanatory comment

2. **medication-web/src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java**
   - Replaced `OidcClientInitiatedLogoutSuccessHandler` with custom implementation
   - Updated imports
   - Added manual logout URL construction

## Key Learnings

### Docker Networking
- Containers cannot access `localhost` on the host machine
- Use service names (e.g., `keycloak`) for container-to-container communication
- Use `localhost` or `host.docker.internal` for container-to-host communication (not reliable across platforms)

### OAuth2/OIDC Configuration
- `issuer-uri` triggers automatic discovery of provider metadata
- Discovery requires network access to the issuer at startup
- Manual endpoint configuration avoids discovery and is more flexible for Docker environments

### Hybrid URL Strategy
- Browser requests use host-accessible URLs (`localhost:8082`)
- Server requests use container-accessible URLs (`keycloak:8080`)
- This is necessary when running OAuth2 providers in Docker

## Production Considerations

For production deployment:

1. **Use proper DNS names** instead of localhost:
   ```properties
   spring.security.oauth2.client.provider.keycloak.authorization-uri=https://auth.yourdomain.com/auth/realms/production/protocol/openid-connect/auth
   ```

2. **Enable HTTPS** for all endpoints:
   ```java
   String logoutUrl = UriComponentsBuilder
       .fromUriString("https://auth.yourdomain.com/auth/realms/production/protocol/openid-connect/logout")
       .queryParam("post_logout_redirect_uri", "https://app.yourdomain.com/login?logout")
       .build()
       .toUriString();
   ```

3. **Use environment variables** for configuration:
   ```properties
   spring.security.oauth2.client.provider.keycloak.authorization-uri=${KEYCLOAK_AUTH_URI}
   ```

4. **Consider using issuer-uri** if all services can access the same URL:
   ```properties
   # Works if both browser AND container can access the same URL
   spring.security.oauth2.client.provider.keycloak.issuer-uri=https://auth.yourdomain.com/auth/realms/production
   ```

## Troubleshooting

### Issue: Still can't access localhost:8080
**Check**:
- Run `docker compose ps` - ensure medication-web is "Up"
- Check logs: `docker compose logs medication-web`
- Verify port not in use: `netstat -ano | findstr :8080`

### Issue: Logout doesn't work
**Check**:
- Browser can access `http://localhost:8082`
- Keycloak is running: `docker compose ps keycloak`
- Check browser network tab for redirect chain

### Issue: Login fails
**Check**:
- Keycloak is accessible from browser
- medication-web can reach keycloak:8080
- Test: `docker exec nll-light-medication-web-1 curl http://keycloak:8080/auth/realms/nll-light/.well-known/openid-configuration`
