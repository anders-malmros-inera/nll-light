# Quick Troubleshooting Guide

## System Status Check

Run these commands to verify your system:

```powershell
# Check all containers
docker ps --format "table {{.Names}}\t{{.Status}}"

# Expected output: All nll-light services should show "Up"
```

## Common Issues & Fixes

### Issue 1: medication-api is Down

**Symptom**: `docker ps` shows medication-api as "Exited"

**Fix**:
```powershell
docker compose up -d medication-api
```

**Verify**:
```powershell
Invoke-RestMethod http://localhost:8081/api/medications
# Should return 3 medications
```

---

### Issue 2: Cannot Access Web Application

**Symptom**: http://localhost:8080 doesn't load

**Check**:
```powershell
# Verify container is running
docker ps | Select-String "medication-web"

# Check logs
docker compose logs --tail 50 medication-web
```

**Fix**:
```powershell
docker compose restart medication-web
```

---

### Issue 3: OAuth2 Login Fails

**Symptom**: Clicking "Logga in med Keycloak" shows error or doesn't redirect

**Diagnosis**:
```powershell
# Test Keycloak is responding
Invoke-RestMethod http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration

# Test OAuth2 redirect
$redirect = Invoke-WebRequest -Uri "http://localhost:8080/oauth2/authorization/keycloak" -MaximumRedirection 0 -ErrorAction SilentlyContinue
$redirect.Headers.Location
# Should contain: localhost:8082...auth...client_id=medication-web
```

**Fix**:
```powershell
# Restart both Keycloak and medication-web
docker compose restart keycloak medication-web
```

---

### Issue 3: OAuth2 Login Fails

**Symptom**: Clicking login redirects to Keycloak, but after entering credentials the app doesn't complete authentication

**Diagnosis**:
```powershell
# Test Keycloak is responding
Invoke-RestMethod http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration

# Test OAuth2 redirect
$redirect = Invoke-WebRequest -Uri "http://localhost:8080/oauth2/authorization/keycloak" -MaximumRedirection 0 -ErrorAction SilentlyContinue
$redirect.Headers.Location
# Should contain: localhost:8082...auth...client_id=medication-web

# Check logs for errors
docker compose logs medication-web | Select-String "OAuth2AuthenticationException|401"
docker compose logs keycloak | Select-String "invalid_client|invalid_token"
```

**Common Causes & Fixes**:

**A. Missing client authentication method** (401 Unauthorized):
```powershell
# Check if application.properties has client-authentication-method
docker compose exec medication-web cat /app/BOOT-INF/classes/application.properties | Select-String "client-authentication-method"
```

**Solution**: Add to `medication-web/src/main/resources/application.properties`:
```properties
spring.security.oauth2.client.registration.keycloak.client-authentication-method=client_secret_post
```

Then rebuild:
```powershell
docker compose build medication-web
docker compose up -d medication-web
```

**B. Issuer mismatch**:
- See "Issue 7: Browser Shows Issuer Mismatch Error" below

**C. Wrong client secret**:
- Verify in Keycloak admin console: http://localhost:8082/auth/admin
- Go to Realm nll-light → Clients → medication-web → Credentials
- Should match `web-app-secret`

---

### Issue 4: Kong Gateway Not Routing

**Symptom**: http://localhost:8000/api/medications returns 404 or error

**Check Kong Configuration**:
```powershell
# Verify service
Invoke-RestMethod http://localhost:8001/services

# Verify routes
Invoke-RestMethod http://localhost:8001/routes
```

**Fix**:
```powershell
docker compose restart kong
```

**Test Direct API**:
```powershell
# Bypass Kong to test API directly
Invoke-RestMethod http://localhost:8081/api/medications
# If this works, problem is Kong; if this fails, problem is medication-api
```

---

### Issue 5: "Issuer Mismatch" Error in Logs

**Symptom**: Logs show "Invalid token issuer" error

**Check Keycloak Issuer**:
```powershell
$config = Invoke-RestMethod http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration
$config.issuer
# Should be: http://localhost:8082/auth/realms/nll-light
```

**Verify SecurityConfig.java**:
```java
// In medication-web/src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java
String expectedIssuer = "http://localhost:8082/auth/realms/nll-light";
// Must match the issuer from Keycloak
```

**Fix**:
```powershell
# Rebuild if you changed SecurityConfig.java
docker compose build medication-web
docker compose up -d medication-web
```

---

### Issue 6: Playwright Tests Fail

**Symptom**: `npm test` in e2e folder fails

**Check Prerequisites**:
```powershell
# Verify Node.js installed
node -v  # Should show v24+ or v16+

# Verify services running
docker ps
```

**Common Fixes**:

**A. PowerShell Execution Policy**:
```powershell
# Option 1: Run npm via Node
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test

# Option 2: Allow scripts (requires admin)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
npm test
```

**B. Chromium Not Installed**:
```powershell
cd medication-web\e2e
npx playwright install chromium
npm test
```

**C. Selectors Don't Match**:
- Open browser manually and inspect Keycloak login page
- Update selectors in `tests/login.spec.js` if Keycloak UI changed

---

### Issue 7: Port Conflicts

**Symptom**: `docker compose up` fails with "port already in use"

**Find Conflicting Process**:
```powershell
# Check what's using the port
netstat -ano | findstr :8080
netstat -ano | findstr :8082
```

**Fix**:
```powershell
# Option 1: Kill the process (find PID from netstat)
Stop-Process -Id <PID> -Force

# Option 2: Change ports in docker-compose.yml
# Edit docker-compose.yml and change host ports:
# medication-web: "9080:8080"  # Use 9080 instead
```

---

### Issue 8: Keycloak "Invalid Token Issuer" Error

**Symptom**: Keycloak logs show `USER_INFO_REQUEST_ERROR` with "Invalid token issuer"

**Error Message**:
```
WARN [org.keycloak.events] type="USER_INFO_REQUEST_ERROR"
error="invalid_token"
reason="Invalid token issuer. Expected 'http://keycloak:8080/auth/realms/nll-light'"
```

**Root Cause**: Keycloak issuer URL not explicitly configured, causing mismatch

**Check Current Issuer**:
```powershell
$config = Invoke-RestMethod http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration
$config.issuer
# Should be: http://localhost:8082/auth/realms/nll-light
```

**Fix**: Add `KC_HOSTNAME_URL` to docker-compose.yml:

Edit `docker-compose.yml`, add to keycloak environment section:
```yaml
keycloak:
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_DB: dev-file
    KC_HTTP_PORT: 8080
    KC_HOSTNAME_STRICT: false
    KC_HTTP_RELATIVE_PATH: /auth
    KC_HOSTNAME_URL: http://localhost:8082/auth  # ← ADD THIS LINE
```

**Apply Fix**:
```powershell
# Restart Keycloak with new config
docker compose up -d keycloak

# Wait for startup
Start-Sleep -Seconds 10

# Restart medication-web
docker compose restart medication-web

# Verify issuer is now consistent
$config = Invoke-RestMethod http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration
Write-Host "Issuer: $($config.issuer)"
```

**Test the Fix**:
```powershell
# Get token and call userinfo
$body = @{grant_type='password'; client_id='medication-web'; client_secret='web-app-secret'; username='user666'; password='secret'; scope='openid'}
$token = Invoke-RestMethod -Uri "http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/token" -Method Post -Body $body -ContentType 'application/x-www-form-urlencoded'
$userinfo = Invoke-RestMethod -Uri "http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/userinfo" -Headers @{Authorization="Bearer $($token.access_token)"}
Write-Host "✅ User: $($userinfo.preferred_username)"

# Check logs (should be no errors)
docker compose logs keycloak --since 1m | Select-String "invalid_token"
```

See `ISSUER-MISMATCH-FIX.md` for complete technical details.

---

## Quick Health Checks

### 1. All Services Status
```powershell
docker compose ps
```
Expected: All services "Up" (except kong-setup which exits after completion)

---

### 2. Keycloak Health
```powershell
Invoke-RestMethod http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration | Select-Object issuer
```
Expected: issuer = "http://localhost:8082/auth/realms/nll-light"

---

### 3. API Health
```powershell
Invoke-RestMethod http://localhost:8081/api/medications | Measure-Object
```
Expected: Count = 3

---

### 4. Kong Health
```powershell
Invoke-RestMethod http://localhost:8001/status
```
Expected: database.reachable = true

---

### 5. Web Application Health
```powershell
$response = Invoke-WebRequest http://localhost:8080/login
$response.StatusCode
```
Expected: 200

---

### 6. OAuth2 Flow Health
```powershell
$body = @{grant_type='password'; client_id='medication-web'; client_secret='web-app-secret'; username='user666'; password='secret'; scope='openid'}
$token = Invoke-RestMethod -Uri "http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/token" -Method Post -Body $body -ContentType 'application/x-www-form-urlencoded'
if($token.access_token) { "✅ OK" } else { "❌ FAIL" }
```
Expected: ✅ OK

---

## Nuclear Option: Complete Reset

If all else fails, completely reset the environment:

```powershell
# Stop and remove everything
docker compose down -v

# Remove images (forces rebuild)
docker compose down --rmi all

# Clean orphaned containers
docker compose down --remove-orphans

# Rebuild and start fresh
docker compose up --build
```

**Warning**: This will delete all data (H2 database, Keycloak users, etc.)

---

## Log Inspection

### View Logs for Specific Service
```powershell
# Last 50 lines
docker compose logs --tail 50 medication-web

# Follow logs in real-time
docker compose logs -f medication-web

# Search for errors
docker compose logs medication-web | Select-String "ERROR"
```

### View All Logs
```powershell
docker compose logs --tail 100
```

---

## Service-Specific Diagnostics

### Keycloak
```powershell
# Check admin console access
Start-Process http://localhost:8082/auth/admin
# Login: admin / admin

# Verify realm exists
Invoke-RestMethod http://localhost:8082/auth/realms/nll-light
```

### Kong
```powershell
# List services
Invoke-RestMethod http://localhost:8001/services | ConvertTo-Json -Depth 3

# List routes
Invoke-RestMethod http://localhost:8001/routes | ConvertTo-Json -Depth 3

# Test direct connection to API from Kong
docker compose exec kong curl http://medication-api:8080/api/medications
```

### Medication Web
```powershell
# Check environment variables
docker compose exec medication-web printenv | Select-String "API_BASE_URL|SPRING"

# Check OAuth2 configuration
docker compose exec medication-web cat /app/BOOT-INF/classes/application.properties
```

---

## Getting Help

If issues persist:

1. **Collect Diagnostic Info**:
```powershell
# Save all logs
docker compose logs > nll-light-logs.txt

# Save container status
docker compose ps > container-status.txt

# Save configuration
docker compose config > compose-config.txt
```

2. **Check Documentation**:
- Main README: `README.md`
- Web module docs: `medication-web/README.md`
- Verification report: `VERIFICATION-REPORT.md`

3. **Common Resources**:
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/client/
- Keycloak Docs: https://www.keycloak.org/documentation
- Kong Docs: https://docs.konghq.com/

---

## Quick Command Reference

```powershell
# Start all services
docker compose up -d

# Restart specific service
docker compose restart medication-web

# View logs
docker compose logs -f medication-web

# Rebuild after code changes
docker compose build medication-web
docker compose up -d medication-web

# Stop everything
docker compose down

# Full reset
docker compose down -v && docker compose up --build
```
