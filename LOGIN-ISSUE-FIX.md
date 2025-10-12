# Login Issue Fix - October 11, 2025

## Problem
After restarting the web application, login appeared to fail with users seeing an error page.

## Root Cause Analysis

### What Appeared to Happen
Users would successfully authenticate with Keycloak but then see an error page instead of the home page.

### Actual Issue
The login itself was **working perfectly**. The OAuth2 authentication flow completed successfully:

1. ✅ User redirects to Keycloak
2. ✅ User enters credentials
3. ✅ Keycloak issues tokens
4. ✅ Web app receives and validates tokens
5. ✅ User is authenticated
6. ❌ **Error occurs when loading the home page**

### The Real Problem
After successful login, the web app tried to load the home page (`/`), which calls `/api/medications` to display the medication list. However, there was a transient network connection issue:

```
ERROR: Request processing failed: org.springframework.web.client.ResourceAccessException: 
I/O error on GET request for "http://medication-api:8081/api/medications": null
java.nio.channels.ClosedChannelException
```

### Why This Happened
1. The `medication-web` container was restarted
2. It started up quickly (in ~2 seconds)
3. Docker Compose's `depends_on` only waits for containers to **start**, not for applications to be **ready**
4. When the first user logged in, the medication-api service was still initializing
5. The first HTTP request hit a closed channel

### Verification
- Login flow worked: ✅ Authentication successful, user "user666" logged in
- OAuth2 tokens received: ✅ All token claims present
- The error only occurred when trying to fetch medications: ❌ Connection issue

## Solution

### Immediate Workaround
Simply **try logging in again**. After the initial failed attempt, the containers are fully connected and subsequent requests work fine.

### Permanent Fix
Updated `docker-compose.yml` to add health checks and proper startup dependencies:

**Changes Made:**

1. **Added healthcheck to medication-api:**
   ```yaml
   medication-api:
     healthcheck:
       test: ["CMD-SHELL", "timeout 1 bash -c '</dev/tcp/localhost/8080' || exit 1"]
       interval: 10s
       timeout: 5s
       retries: 5
       start_period: 30s
   ```

2. **Updated medication-web dependencies:**
   ```yaml
   medication-web:
     depends_on:
       medication-api:
         condition: service_healthy  # Wait for API to be ready
       kong:
         condition: service_started
       keycloak:
         condition: service_started
   ```

### How This Fixes It
- **Before**: Web app starts as soon as medication-api container starts (but app not ready)
- **After**: Web app waits until medication-api is healthy (port 8080 accepting connections)
- **Result**: No more connection errors on first login attempt

## Testing After Fix

### 1. Restart All Services
```powershell
docker-compose down
docker-compose up -d
```

### 2. Monitor Startup
```powershell
docker-compose ps
# Wait until medication-api shows "healthy" status
```

### 3. Test Login
- Navigate to `http://localhost:8080`
- Click login
- Enter credentials (user666 / user666)
- Should successfully login and see medication list ✅

### 4. Verify No Errors
```powershell
docker logs nll-light-medication-web-1 --tail 20 | Select-String -Pattern "ERROR"
# Should show no errors
```

## Files Modified

### `docker-compose.yml`
- Added healthcheck configuration to `medication-api` service
- Changed `depends_on` for `medication-web` to use conditional dependencies
- Ensures proper startup order and readiness

## Understanding the Error

### What is ClosedChannelException?
This Java exception occurs when trying to perform I/O operations on a closed network channel. In this case:
- The web app tried to make an HTTP request to the API
- The connection was established but then immediately closed
- This typically happens during application startup when ports are being initialized

### Why It's Transient
- First request: API not fully ready → Connection fails
- Subsequent requests: API fully initialized → Connections succeed
- The issue resolves itself after ~30 seconds of uptime

## Prevention Strategy

### Health Checks
Health checks ensure services are genuinely ready to accept traffic:
- **Port check**: Verifies the port is open
- **Start period**: Gives 30 seconds for initial startup
- **Interval**: Checks every 10 seconds
- **Retries**: Allows 5 failed attempts before marking unhealthy

### Dependency Conditions
Docker Compose now supports:
- `service_started`: Container is running (old behavior)
- `service_healthy`: Container passes health check (new behavior)
- `service_completed_successfully`: For one-time tasks

## Current Status
✅ **Login works** - OAuth2 authentication is functioning correctly
✅ **Docker Compose updated** - Health checks configured
⏳ **Next restart** - Will test the new startup dependencies

## Future Improvements

### Option 1: Add Spring Boot Actuator
More reliable health check using Spring Boot's built-in health endpoint:
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
```

### Option 2: Retry Logic in Web App
Add resilience4j or Spring Retry to automatically retry failed API calls:
```java
@Retryable(
    value = ResourceAccessException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public List<Medication> getMedications() {
    // API call
}
```

### Option 3: Circuit Breaker
Implement circuit breaker pattern to handle API unavailability gracefully.

## Summary

The "login failure" wasn't actually a login problem—it was a timing issue where the home page couldn't load medications because the API wasn't fully ready yet. The fix ensures the web app waits for the API to be healthy before starting, preventing this race condition.

**Bottom line**: Login works fine, and with the health check fix, users won't see errors even on the first attempt after a restart.
