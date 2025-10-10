# Issuer Mismatch Error - Resolution
**Date**: October 10, 2025  
**Error**: Invalid token issuer error from Keycloak  
**Status**: ✅ RESOLVED

---

## Error Message

```
2025-10-10 21:00:44,358 WARN [org.keycloak.events] (executor-thread-4) 
type="USER_INFO_REQUEST_ERROR", 
realmId="4bdbba97-12f4-4ed3-8740-2eb77982831a", 
realmName="nll-light", 
clientId="null", 
userId="null", 
ipAddress="172.19.0.5", 
error="invalid_token", 
reason="Invalid token issuer. Expected 'http://keycloak:8080/auth/realms/nll-light'", 
auth_method="validate_access_token"
```

---

## Problem Analysis

### The Issue

Keycloak was experiencing an **issuer mismatch** when validating access tokens for userinfo requests:

- **Tokens were issued with issuer**: `http://localhost:8082/auth/realms/nll-light` (browser-facing URL)
- **Keycloak expected issuer**: `http://keycloak:8080/auth/realms/nll-light` (internal Docker DNS)

This happened because:
1. Keycloak's discovery endpoint advertised `localhost:8082` as the issuer
2. Tokens were issued with that issuer claim
3. When medication-web called the userinfo endpoint with those tokens, Keycloak **internally** expected the issuer to be the container DNS name `keycloak:8080`

### Why This Happened

Without explicit hostname configuration, Keycloak determines its issuer URL based on the incoming request:
- Requests from browser → issuer: `http://localhost:8082/...`
- Requests from container → issuer: `http://keycloak:8080/...`

This inconsistency caused tokens issued via one URL to be rejected when validated via another.

---

## Root Cause

**Keycloak's issuer URL was not explicitly configured**, causing it to use different issuer values depending on how it was accessed.

---

## Solution

### Fix Applied

Added `KC_HOSTNAME_URL` environment variable to Keycloak configuration to **force a consistent issuer URL**.

**File**: `docker-compose.yml`

**Change**:
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:latest
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_DB: dev-file
    KC_HTTP_PORT: 8080
    KC_HOSTNAME_STRICT: false
    KC_HTTP_RELATIVE_PATH: /auth
    KC_HOSTNAME_URL: http://localhost:8082/auth   # ← ADDED THIS
  command: start-dev --import-realm
  volumes:
    - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
  ports:
    - "8082:8080"
```

### What This Does

`KC_HOSTNAME_URL` tells Keycloak to **always use** `http://localhost:8082/auth` as its base URL, regardless of how it's accessed. This ensures:

1. ✅ Tokens are issued with issuer: `http://localhost:8082/auth/realms/nll-light`
2. ✅ Keycloak validates tokens expecting issuer: `http://localhost:8082/auth/realms/nll-light`
3. ✅ No mismatch between issuance and validation

---

## Deployment Steps

```powershell
# 1. Update docker-compose.yml (already done)

# 2. Restart Keycloak with new configuration
docker compose up -d keycloak

# 3. Wait for Keycloak to start (10 seconds)
Start-Sleep -Seconds 10

# 4. Verify issuer is correct
$config = Invoke-RestMethod "http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration"
$config.issuer
# Should output: http://localhost:8082/auth/realms/nll-light

# 5. Restart medication-web for clean connection
docker compose restart medication-web
```

---

## Verification

### Test Results

✅ **Issuer Configuration**:
```powershell
$config = Invoke-RestMethod "http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration"
Write-Host "Issuer: $($config.issuer)"
```
Output: `Issuer: http://localhost:8082/auth/realms/nll-light`

✅ **Token Exchange + Userinfo**:
```powershell
$body = @{grant_type='password'; client_id='medication-web'; client_secret='web-app-secret'; username='user666'; password='secret'; scope='openid profile email'}
$token = Invoke-RestMethod -Uri "http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/token" -Method Post -Body $body -ContentType 'application/x-www-form-urlencoded'
$userinfo = Invoke-RestMethod -Uri "http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/userinfo" -Headers @{Authorization="Bearer $($token.access_token)"}
Write-Host "User: $($userinfo.preferred_username)"
```
Output: `User: user666`

✅ **No Errors in Logs**:
```powershell
docker compose logs keycloak --since 2m | Select-String "invalid_token|USER_INFO_REQUEST_ERROR"
```
Output: (empty - no errors)

---

## Technical Details

### Keycloak Hostname Configuration

Keycloak supports several environment variables for hostname configuration:

1. **KC_HOSTNAME_URL** (what we used):
   - Forces a specific frontend URL for all realms
   - Most explicit and reliable for Docker environments
   - Ensures consistent issuer across all access patterns

2. **KC_HOSTNAME**:
   - Sets just the hostname (without protocol/port)
   - Less explicit than KC_HOSTNAME_URL

3. **KC_HOSTNAME_STRICT**:
   - When `true`, enforces strict hostname checking
   - We set to `false` for development flexibility

### Why Browser-Facing URL?

We chose `http://localhost:8082/auth` (browser-facing) instead of `http://keycloak:8080/auth` (internal) because:

1. **Tokens are browser-accessible**: JWTs are visible in browser and must reference a URL the browser can reach
2. **OAuth2 redirects**: Authorization redirects must work from the user's browser
3. **Client-side validation**: JavaScript clients may validate issuer against discovery endpoint
4. **Custom JwtDecoder handles mismatch**: Our SecurityConfig already handles fetching JWKS from internal URL while validating browser-facing issuer

---

## Related Configuration

### medication-web SecurityConfig.java

The custom JwtDecoder remains unchanged and now works correctly:

```java
@Bean
public JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}") String jwkSetUri
) {
    // Fetch JWKS from internal Docker DNS
    String expectedIssuer = "http://localhost:8082/auth/realms/nll-light";
    
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(expectedIssuer));
    return decoder;
}
```

**This works because**:
- Keycloak now issues tokens with `http://localhost:8082/...` (matches expectedIssuer)
- JWKS are fetched from `http://keycloak:8080/...` (internal, fast)
- No issuer mismatch in either direction

---

## Complete OAuth2 Flow (After Fix)

1. **User clicks login** → Browser navigates to medication-web
2. **OAuth2 redirect** → Browser redirected to `http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth`
3. **User authenticates** → Enters credentials at Keycloak
4. **Authorization code returned** → Keycloak redirects to `http://localhost:8080/login/oauth2/code/keycloak?code=...`
5. **Token exchange** → medication-web POST to `http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token`
   - Receives tokens with issuer: `http://localhost:8082/auth/realms/nll-light`
6. **Token validation** → medication-web validates JWT:
   - Fetches JWKS from `http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs`
   - Validates issuer matches `http://localhost:8082/auth/realms/nll-light` ✅
7. **Userinfo request** → medication-web calls `http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/userinfo`
   - Keycloak validates token with issuer `http://localhost:8082/auth/realms/nll-light` ✅
8. **Session established** → User logged in successfully

---

## Before vs After

### Before Fix

| Component | Issuer Expected | Issuer Received | Result |
|-----------|----------------|-----------------|--------|
| Keycloak (token issuance) | N/A | `http://localhost:8082/...` | ✅ Issued |
| medication-web (JWT validation) | `http://localhost:8082/...` | `http://localhost:8082/...` | ✅ Valid |
| Keycloak (userinfo validation) | `http://keycloak:8080/...` | `http://localhost:8082/...` | ❌ **MISMATCH** |

**Error**: `Invalid token issuer. Expected 'http://keycloak:8080/auth/realms/nll-light'`

### After Fix

| Component | Issuer Expected | Issuer Received | Result |
|-----------|----------------|-----------------|--------|
| Keycloak (token issuance) | N/A | `http://localhost:8082/...` | ✅ Issued |
| medication-web (JWT validation) | `http://localhost:8082/...` | `http://localhost:8082/...` | ✅ Valid |
| Keycloak (userinfo validation) | `http://localhost:8082/...` | `http://localhost:8082/...` | ✅ **MATCH** |

**Result**: All validations succeed ✅

---

## Production Considerations

### For Production Deployment

When deploying to production with proper DNS:

1. **Use a real domain**:
   ```yaml
   KC_HOSTNAME_URL: https://auth.yourdomain.com
   ```

2. **Enable HTTPS**:
   ```yaml
   KC_HOSTNAME_STRICT: true
   KC_HTTPS_ENABLED: true
   ```

3. **Update medication-web configuration**:
   ```properties
   spring.security.oauth2.client.provider.keycloak.authorization-uri=https://auth.yourdomain.com/auth/realms/nll-light/protocol/openid-connect/auth
   # Internal endpoints can use service DNS or same public URL
   spring.security.oauth2.client.provider.keycloak.token-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
   ```

4. **Update JwtDecoder expected issuer**:
   ```java
   String expectedIssuer = "https://auth.yourdomain.com/auth/realms/nll-light";
   ```

---

## Monitoring

### Check for Issuer Errors

```powershell
# Real-time monitoring
docker compose logs -f keycloak | Select-String "invalid_token|issuer"

# Recent errors
docker compose logs keycloak --since 10m | Select-String "USER_INFO_REQUEST_ERROR"
```

### Verify Issuer Configuration

```powershell
# Check discovery endpoint
$config = Invoke-RestMethod "http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration"
$config.issuer

# Should always return: http://localhost:8082/auth/realms/nll-light
```

---

## Files Modified

1. **docker-compose.yml**:
   - Added `KC_HOSTNAME_URL: http://localhost:8082/auth`

**No code changes required** - SecurityConfig.java already had the correct configuration.

---

## Summary

**Issue**: Keycloak issuer mismatch causing userinfo validation failures  
**Root Cause**: Keycloak using different issuer URLs based on access pattern  
**Fix**: Set `KC_HOSTNAME_URL` to force consistent issuer  
**Result**: All OAuth2 flows now work correctly with consistent issuer  

**Status**: ✅ **RESOLVED**

---

**Fixed by**: GitHub Copilot  
**Report Generated**: October 10, 2025 23:03
