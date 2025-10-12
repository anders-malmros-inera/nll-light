# Logout Fix - OIDC Session Termination

## Problem
When clicking "Logout" in the web application, the local session was cleared but the Keycloak SSO (Single Sign-On) session remained active. This meant:
- Users appeared to be logged out locally
- But navigating back to the app would automatically re-authenticate them without requiring credentials
- Users could not switch accounts without closing the browser

## Root Cause
The application was only performing a **local logout** (clearing the Spring Security session) but not performing an **OIDC RP-Initiated Logout** to terminate the Keycloak session.

## Solution
Implemented proper OIDC logout using Spring Security's `OidcClientInitiatedLogoutSuccessHandler`.

### Changes Made

#### 1. SecurityConfig.java
Updated the logout configuration to use OIDC logout handler:

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
    .invalidateHttpSession(true)
    .clearAuthentication(true)
    .deleteCookies("JSESSIONID")
);
```

Added the OIDC logout success handler:

```java
private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    OidcClientInitiatedLogoutSuccessHandler successHandler = 
        new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    
    // Set the post-logout redirect URI (where to go after Keycloak logout)
    successHandler.setPostLogoutRedirectUri("http://localhost:8080/login?logout");
    
    return successHandler;
}
```

#### 2. application.properties
Added the issuer-uri configuration for OIDC provider discovery:

```properties
# Issuer URI for OIDC logout - uses browser-accessible URL
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8082/auth/realms/nll-light
```

#### 3. login.html
Enhanced the login page to show a success message after logout:

```html
<div th:if="${param.logout}" class="success-message">
  <p>Du har loggats ut. Logga in igen för att fortsätta.</p>
</div>
```

## How It Works

### Logout Flow
1. User clicks "Logout" button (POST to `/logout`)
2. Spring Security's LogoutFilter processes the request
3. `OidcClientInitiatedLogoutSuccessHandler` is invoked
4. Handler redirects to Keycloak's `end_session_endpoint`:
   - URL: `http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/logout`
   - Parameter: `post_logout_redirect_uri=http://localhost:8080/login?logout`
5. Keycloak terminates the SSO session
6. Keycloak redirects back to the application's login page with `?logout` parameter
7. Login page displays "Du har loggats ut" success message

### Technical Details

**OIDC RP-Initiated Logout Specification**: 
- Defined in [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html)
- Allows Relying Parties (RP) to request logout at the OpenID Provider (OP)
- Uses the `end_session_endpoint` from OIDC provider metadata

**OidcClientInitiatedLogoutSuccessHandler**:
- Spring Security class that implements OIDC RP-Initiated Logout
- Automatically discovers the `end_session_endpoint` from the provider's issuer-uri
- Constructs the logout URL with `post_logout_redirect_uri` parameter
- Handles the redirect to Keycloak

**Issuer URI**:
- Must be accessible from the user's browser (not internal Docker network)
- Uses `http://localhost:8082/auth/realms/nll-light` instead of `http://keycloak:8080/...`
- Spring Security fetches OIDC discovery metadata from `{issuer}/.well-known/openid-configuration`

## Testing

### Test Logout Functionality
1. Login as any user (patient001, prescriber001, or pharmacist001)
2. Navigate to your role-specific dashboard
3. Click the "Logout" button
4. **Expected behavior**:
   - You are redirected to Keycloak's logout page (briefly visible)
   - Then redirected back to login page with green success message
   - If you click "Logga in med Keycloak", you must enter credentials again
   - Your previous session is completely terminated

### Test Session Isolation
1. Login as patient001
2. Note the dashboard (Blue theme, patient prescriptions)
3. Logout
4. Login as prescriber001
5. **Expected**: Green prescriber dashboard (NOT blue patient dashboard)

### Test Browser Session
1. Login as pharmacist001
2. Open a new tab, navigate to http://localhost:8080
3. **Expected**: Purple pharmacist dashboard (SSO - no re-authentication needed)
4. Go back to first tab, click Logout
5. Refresh the second tab
6. **Expected**: Redirected to login page (session terminated in both tabs)

## Security Benefits

1. **Session Termination**: Keycloak session is properly terminated, not just hidden
2. **Account Switching**: Users can switch accounts without closing browser
3. **Security Compliance**: Follows OIDC RP-Initiated Logout specification
4. **Multi-Tab Consistency**: Logout affects all browser tabs with the application
5. **SSO Integration**: Works correctly with Keycloak's Single Sign-On architecture

## Configuration

### Environment-Specific Settings

For **production** environments, update the `post_logout_redirect_uri`:

```java
successHandler.setPostLogoutRedirectUri("https://your-domain.com/login?logout");
```

And update the issuer-uri in application.properties:

```properties
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://keycloak.your-domain.com/auth/realms/your-realm
```

### HTTPS Requirements
In production:
- Both the application and Keycloak should use HTTPS
- The `post_logout_redirect_uri` must match a registered redirect URI in Keycloak client settings
- Cookie security flags should be enabled (secure, httpOnly, sameSite)

## Troubleshooting

### Issue: Logout redirects but session persists
**Solution**: 
- Check that `issuer-uri` is correctly configured
- Verify Keycloak's `end_session_endpoint` is accessible from browser
- Check browser console for redirect errors

### Issue: CORS errors during logout
**Solution**:
- Ensure `post_logout_redirect_uri` is registered in Keycloak client
- Add the redirect URI to "Valid Post Logout Redirect URIs" in Keycloak admin console

### Issue: "Invalid redirect uri" error from Keycloak
**Solution**:
- In Keycloak admin console, go to: Clients → medication-web → Settings
- Add `http://localhost:8080/login?logout` to "Valid Post Logout Redirect URIs"
- Or use `http://localhost:8080/*` for development

## Files Modified

1. `medication-web/src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java`
   - Added `OidcClientInitiatedLogoutSuccessHandler`
   - Updated logout configuration
   - Added imports for OIDC logout classes

2. `medication-web/src/main/resources/application.properties`
   - Added `spring.security.oauth2.client.provider.keycloak.issuer-uri`

3. `medication-web/src/main/resources/templates/login.html`
   - Added success message styling
   - Added logout success message display
   - Updated error message to use param detection

## References

- [OpenID Connect RP-Initiated Logout 1.0](https://openid.net/specs/openid-connect-rpinitiated-1_0.html)
- [Spring Security OAuth2 Client Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oidc-logout)
- [Keycloak OIDC Logout Documentation](https://www.keycloak.org/docs/latest/securing_apps/#logout)
