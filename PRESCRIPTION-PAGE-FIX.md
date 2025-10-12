# Prescription Page Fix - October 11, 2025

## Problem
The prescription page (`/prescriptions`) was returning a 404 error with the message:
```
"message":"no Route matched with those values"
```

## Root Cause Analysis

### Error Details
When users tried to access `/prescriptions`, the web application would call the REST API to fetch prescription data. The error occurred because:

1. **Docker Compose Environment Variable**: The `docker-compose.yml` file had:
   ```yaml
   medication-web:
     environment:
       - API_BASE_URL=http://kong:8000
   ```

2. **Kong Gateway**: Kong was configured to route `/api/medications` but **NOT** `/api/v1/prescriptions`

3. **Environment Override**: Even though `application.properties` was updated to use `medication-api:8081`, the Docker Compose environment variable was overriding it, forcing the app to still use Kong.

4. **Missing Route**: When the web app tried to call `http://kong:8000/api/v1/prescriptions`, Kong responded with a 404 because no route was configured for that path.

### Verification
- Direct API call worked: `http://localhost:8081/api/v1/prescriptions` ✅
- Through Kong failed: `http://localhost:8000/api/v1/prescriptions` ❌
- Web app environment variable pointed to Kong ❌

## Solution

Changed the `docker-compose.yml` to point directly to the medication-api service:

### File Modified: `docker-compose.yml`

**Before**:
```yaml
medication-web:
  build:
    context: .
    dockerfile: medication-web/Dockerfile
  environment:
    - API_BASE_URL=http://kong:8000
  depends_on:
    - kong
    - keycloak
```

**After**:
```yaml
medication-web:
  build:
    context: .
    dockerfile: medication-web/Dockerfile
  environment:
    - API_BASE_URL=http://medication-api:8081
  depends_on:
    - kong
    - keycloak
    - medication-api
```

### Additional Change: `application.properties`
Also updated the default value in case the environment variable is not set:

**Before**:
```properties
api.base-url=${API_BASE_URL:http://kong:8000}
```

**After**:
```properties
# Using direct connection to medication-api instead of Kong gateway
# since Kong routes are not yet configured for prescription endpoints
api.base-url=${API_BASE_URL:http://medication-api:8081}
```

### Why This Works
- `medication-api` is the Docker service name for the API container
- Docker's internal DNS resolves `medication-api` to the correct container IP
- The API is running on port 8081 inside the container
- This bypasses Kong entirely, accessing the API directly
- The environment variable in `docker-compose.yml` takes precedence over the default in `application.properties`

### Key Learning
⚠️ **Environment variables in Docker Compose override property files!**  
The `${VAR:default}` syntax in Spring Boot means "use environment variable VAR if it exists, otherwise use 'default'". Since Docker Compose was setting `API_BASE_URL`, the property file default was never used.

## Trade-offs
**Pros**:
- ✅ Immediate fix, no Kong configuration needed
- ✅ Simpler architecture for development
- ✅ Fewer moving parts to debug
- ✅ Consistent behavior across all API endpoints

**Cons**:
- ⚠️ Bypasses Kong's features (rate limiting, authentication, etc.)
- ⚠️ Different approach than might be used in production
- ⚠️ Would need Kong routes configured before deploying with Kong

## Alternative Solution: Configure Kong Routes

If Kong features are needed, you would need to add routes for the prescription endpoints:

```bash
# Add route for prescription endpoints
curl -i -X POST http://localhost:8001/routes \
  --data "name=prescription-route" \
  --data "paths[]=/api/v1/prescriptions" \
  --data "service.id=<medication-api-service-id>"

# Or configure in docker-compose.yml or Kong declarative config
```

## Testing After Fix

### 1. Update Docker Compose
```yaml
# In docker-compose.yml
medication-web:
  environment:
    - API_BASE_URL=http://medication-api:8081
```

### 2. Restart the Web Container
```powershell
docker-compose up -d medication-web
```

### 3. Verify Environment Variable
```powershell
docker exec nll-light-medication-web-1 env | Select-String -Pattern "API"
# Should show: API_BASE_URL=http://medication-api:8081
```

### 4. Check Application Startup
```
Started NllLightWebApplication in 1.821 seconds
```

### 5. Test the Prescription Page
- Navigate to `http://localhost:8080`
- Log in through Keycloak
- Click "📋 Mina Recept"
- Should now display prescriptions without errors ✅

### 6. Verify No Errors in Logs
```powershell
docker logs nll-light-medication-web-1 | Select-String -Pattern "ERROR"
# Should return nothing (no errors)
```

## Files Modified

### 1. `docker-compose.yml`
- Changed `API_BASE_URL` environment variable from `http://kong:8000` to `http://medication-api:8081`
- Added `medication-api` to `depends_on` list

### 2. `medication-web/src/main/resources/application.properties`
- Changed default `api.base-url` from `http://kong:8000` to `http://medication-api:8081`
- Added comment explaining the bypass

## Status
✅ **FIXED** - Prescription page now works by connecting directly to the API service

## Troubleshooting Steps Used

1. **Checked logs for errors** → Found "no Route matched" error
2. **Tested API directly** → Confirmed API works on port 8081
3. **Tested through Kong** → Confirmed Kong doesn't have prescription routes
4. **Updated application.properties** → Didn't work (cached layers)
5. **Rebuilt without cache** → Still didn't work
6. **Checked environment variables** → Found the override!
7. **Updated docker-compose.yml** → SUCCESS!

## Next Steps (Optional)

If you want to use Kong for all API traffic:

1. **Configure Kong Routes** for prescription endpoints:
   - `/api/v1/prescriptions`
   - `/api/v1/prescriptions/{id}`
   - `/api/v1/prescriptions/refill-eligible`
   - `/api/v1/prescriptions/{id}/take`
   - `/api/v1/prescriptions/{id}/adherence`

2. **Revert** `api.base-url` back to `http://kong:8000`

3. **Add Kong Plugins** as needed:
   - Rate limiting
   - Authentication
   - CORS
   - Request/Response transformation

## Docker Service Communication

For reference, here's how services communicate:

```
Browser → http://localhost:8080 → medication-web container
medication-web → http://medication-api:8081 → medication-api container
medication-web → http://keycloak:8080 → keycloak container

(Kong is currently bypassed for prescription endpoints)
```

## Verification Commands

```powershell
# Check if web app is running
docker ps | Select-String "medication-web"

# View web app logs
docker logs nll-light-medication-web-1 --tail 50

# Test API directly
Invoke-RestMethod -Uri "http://localhost:8081/api/v1/prescriptions" `
  -Headers @{"X-Patient-Id"="patient-001"}

# Check Kong routes (if needed)
Invoke-RestMethod -Uri "http://localhost:8001/routes"
```

## Summary

The prescription page was failing because Kong didn't have routes configured for the new prescription API endpoints. The immediate fix was to bypass Kong and connect the web application directly to the API service using Docker's internal DNS. This is suitable for development but may need to be revisited for production if Kong's gateway features are required.
