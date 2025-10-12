# ID Token Hint Fix for Logout

## Problem
When clicking the "Logout" button, users encountered a Keycloak error page:
```
We are sorry...
Missing parameters: id_token_hint
```

## Root Cause
Keycloak's OIDC RP-Initiated Logout endpoint requires the `id_token_hint` parameter to identify which user session to terminate. This is a security measure defined in the [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) specification.

Without the `id_token_hint`:
- Keycloak cannot verify which user session to terminate
- The logout request is rejected for security reasons
- User remains logged in (session not terminated)

## Solution
Updated the logout handler in `SecurityConfig.java` to extract the ID token from the authenticated OIDC user and include it as a query parameter in the logout URL.

### Code Changes

**File**: `medication-web/src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java`

#### Before (Missing ID Token):
```java
private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
        String logoutUrl = UriComponentsBuilder
            .fromUriString("http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout")
            .queryParam("post_logout_redirect_uri", "http://localhost:8080/login?logout")
            .build()
            .toUriString();
        
        response.sendRedirect(logoutUrl);
    };
}
```

#### After (With ID Token Hint):
```java
private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
        String logoutUrl;
        
        // Extract ID token if user is authenticated via OIDC
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String idToken = oidcUser.getIdToken().getTokenValue();
            
            // Construct Keycloak logout URL with id_token_hint
            logoutUrl = UriComponentsBuilder
                .fromUriString("http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout")
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", "http://localhost:8080/login?logout")
                .build()
                .toUriString();
        } else {
            // Fallback if not OIDC authenticated
            logoutUrl = "http://localhost:8080/login?logout";
        }
        
        try {
            response.sendRedirect(logoutUrl);
        } catch (IOException e) {
            // If redirect fails, fall back to local logout
            try {
                response.sendRedirect("/login?logout");
            } catch (IOException ex) {
                // Log error if needed
            }
        }
    };
}
```

## How It Works

### Complete Logout Flow
1. **User clicks "Logout" button**
   - Sends POST request to `/logout`

2. **Spring Security processes logout**
   - Invalidates HTTP session
   - Clears authentication
   - Deletes JSESSIONID cookie
   - Calls custom logout success handler

3. **Logout handler extracts ID token**
   - Checks if user is authenticated (`authentication != null`)
   - Verifies principal is an `OidcUser` instance
   - Extracts ID token value: `oidcUser.getIdToken().getTokenValue()`

4. **Constructs logout URL with parameters**
   - Base URL: `http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout`
   - Parameters:
     - `id_token_hint`: The JWT ID token (identifies the user session)
     - `post_logout_redirect_uri`: Where to redirect after logout

5. **Browser redirects to Keycloak**
   - Keycloak validates the `id_token_hint`
   - Keycloak terminates the SSO session
   - Keycloak redirects back to `post_logout_redirect_uri`

6. **User arrives at login page**
   - Login page shows success message: "Du har loggats ut"
   - SSO session is completely terminated
   - Next login requires credentials ✅

## Security Benefits

### Why ID Token Hint is Required

1. **Session Identification**
   - The ID token uniquely identifies the user's session
   - Prevents ambiguity when multiple sessions exist

2. **Logout Request Validation**
   - Keycloak verifies the token is valid and active
   - Ensures the logout request is legitimate

3. **Protection Against Malicious Logout**
   - Without the token, anyone could craft a logout request
   - Attackers could force users to be logged out maliciously
   - The token proves the request comes from an authenticated session

4. **Compliance with OIDC Specification**
   - Follows OpenID Connect RP-Initiated Logout 1.0 standard
   - Ensures interoperability with compliant OIDC providers

## What is the ID Token?

The ID token is a **JSON Web Token (JWT)** issued by Keycloak during login that contains:

### Example ID Token Claims:
```json
{
  "exp": 1697116800,
  "iat": 1697116500,
  "auth_time": 1697116500,
  "jti": "94109481-d291-f07f-2432-d3ca8d54e327",
  "iss": "http://localhost:8082/auth/realms/nll-light",
  "aud": "medication-web",
  "sub": "5f327b2b-ec56-4641-a389-6eb3f18cc682",
  "typ": "ID",
  "azp": "medication-web",
  "sid": "6f3bb53d-ec88-8236-7a3c-fdcf4277fae4",
  "preferred_username": "patient001",
  "name": "Patient One",
  "email": "patient001@example.com",
  "email_verified": true,
  "realm_access": {
    "roles": ["PATIENT"]
  }
}
```

**Key claims used for logout**:
- `iss` (Issuer): Identifies Keycloak as the token issuer
- `sub` (Subject): Unique user identifier
- `sid` (Session ID): Identifies the specific session
- `aud` (Audience): The client application (medication-web)
- `exp` (Expiration): When the token expires
- `jti` (JWT ID): Unique token identifier

## Testing

### Verify Logout Works Correctly

1. **Login as any user**:
   - Navigate to http://localhost:8080
   - Click "Logga in med Keycloak"
   - Enter credentials (e.g., patient001/patient001)
   - Verify redirect to dashboard

2. **Click Logout button**:
   - Click "Logout" in the dashboard header
   - **Expected**: Brief redirect to Keycloak
   - **Expected**: Redirect back to login page with green success message
   - **NOT Expected**: "Missing parameters: id_token_hint" error ✅

3. **Verify session terminated**:
   - Click "Logga in med Keycloak" again
   - **Expected**: Keycloak login form appears (must enter credentials)
   - **NOT Expected**: Automatic re-authentication ✅

4. **Test in new tab**:
   - Login again
   - Open http://localhost:8080 in a new browser tab
   - **Expected**: See dashboard (SSO session active)
   - In first tab, click Logout
   - Refresh second tab
   - **Expected**: Redirected to login (session terminated everywhere) ✅

### Verify ID Token in URL

To see the ID token being sent:

1. Open Browser DevTools (F12)
2. Go to Network tab
3. Click "Logout"
4. Look for redirect to Keycloak logout endpoint
5. Check URL parameters - should see:
   ```
   http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout?
     id_token_hint=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEyMyJ9.eyJleHAiOjE2OTcxMTY4MDAsImlhdCI6MTY5NzExNjUwMCwic3ViIjoiNWYzMjdiMmItZWM1Ni00NjQxLWEzODktNmViM2YxOGNjNjgyIn0.xyz...&
     post_logout_redirect_uri=http://localhost:8080/login?logout
   ```

## Troubleshooting

### Issue: Still getting "Missing parameters: id_token_hint"

**Possible causes**:
1. Code not rebuilt/deployed
2. Browser cached old version
3. Authentication object is null

**Solutions**:
```bash
# Rebuild and restart
docker compose build medication-web
docker compose restart medication-web

# Clear browser cache
# Press Ctrl+Shift+Delete in browser

# Test in incognito/private window
# Open new private browsing window
```

### Issue: Logout redirects to login but doesn't show success message

**Check**:
- Login page should have `?logout` parameter in URL
- Example: `http://localhost:8080/login?logout`
- If missing, check redirect URL in code

### Issue: ID token is null or undefined

**Check application logs**:
```bash
docker compose logs medication-web | grep -i "oidc\|token\|logout"
```

**Verify**:
- User logged in via OIDC (not basic auth)
- Authentication principal is `OidcUser` type
- ID token was included in authentication response

### Issue: Keycloak rejects the id_token_hint

**Possible causes**:
- ID token expired
- ID token signature invalid
- Wrong issuer/audience

**Check Keycloak logs**:
```bash
docker compose logs keycloak | grep -i "logout\|end_session"
```

## Alternative: Using post_logout_redirect_uri Only

Some OIDC providers don't strictly require `id_token_hint`. If you encounter issues, you can make it optional:

```java
// Construct URL without id_token_hint (less secure)
logoutUrl = UriComponentsBuilder
    .fromUriString("http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout")
    .queryParam("post_logout_redirect_uri", "http://localhost:8080/login?logout")
    .build()
    .toUriString();
```

**⚠️ Warning**: This is **NOT recommended** as it:
- Reduces security
- May not work with all OIDC providers
- Violates OIDC specification for some providers
- Keycloak specifically requires it

## Production Considerations

### Environment-Specific Configuration

For production, use environment variables:

```java
@Value("${keycloak.logout-uri}")
private String keycloakLogoutUri;

@Value("${app.post-logout-redirect-uri}")
private String postLogoutRedirectUri;

// Then use in logout handler
logoutUrl = UriComponentsBuilder
    .fromUriString(keycloakLogoutUri)
    .queryParam("id_token_hint", idToken)
    .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
    .build()
    .toUriString();
```

**application.properties**:
```properties
keycloak.logout-uri=${KEYCLOAK_LOGOUT_URI:http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout}
app.post-logout-redirect-uri=${POST_LOGOUT_REDIRECT_URI:http://localhost:8080/login?logout}
```

### HTTPS in Production

Always use HTTPS for logout in production:
```java
logoutUrl = UriComponentsBuilder
    .fromUriString("https://auth.yourdomain.com/auth/realms/production/protocol/openid-connect/logout")
    .queryParam("id_token_hint", idToken)
    .queryParam("post_logout_redirect_uri", "https://app.yourdomain.com/login?logout")
    .build()
    .toUriString();
```

### Token Security

**Best practices**:
- ID tokens are sensitive - only send over HTTPS in production
- ID tokens are short-lived (typically 5-30 minutes)
- Never log ID token values in production
- Ensure proper CORS configuration for logout redirects

## References

- [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html)
- [Keycloak OIDC Logout Documentation](https://www.keycloak.org/docs/latest/securing_apps/#logout)
- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [Spring Security OAuth2 Client Logout](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oidc-logout)
