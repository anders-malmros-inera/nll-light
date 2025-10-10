# Docker Rebuild Guide
**Complete setup from scratch**

---

## Quick Start (Full Rebuild)

```powershell
# Stop and remove everything
docker compose down -v

# Rebuild all images
docker compose build --no-cache

# Start all services
docker compose up -d

# Wait for services to start
Start-Sleep -Seconds 15

# Verify all containers are running
docker compose ps
```

---

## Step-by-Step Rebuild Process

### Step 1: Clean Up Everything

```powershell
# Stop all containers and remove volumes
docker compose down -v

# Optional: Remove all images to force complete rebuild
docker compose down --rmi all

# Optional: Remove orphaned containers
docker compose down --remove-orphans
```

**What this does**:
- `-v` = Remove named volumes (clears Keycloak database, etc.)
- `--rmi all` = Remove all images (forces rebuild from scratch)
- `--remove-orphans` = Clean up old containers from previous configurations

---

### Step 2: Rebuild All Images

```powershell
# Build all services with no cache (clean build)
docker compose build --no-cache

# Or build specific services
docker compose build --no-cache medication-api
docker compose build --no-cache medication-web
```

**What this does**:
- Runs Maven builds inside Docker
- Creates fresh Docker images
- `--no-cache` ensures no old layers are reused

---

### Step 3: Start All Services

```powershell
# Start all services in detached mode
docker compose up -d

# Or start with logs visible (useful for debugging)
docker compose up
```

**What this does**:
- Starts Kong, Keycloak, medication-api, medication-web
- `-d` runs in background (detached mode)

---

### Step 4: Verify Everything is Running

```powershell
# Check container status
docker compose ps

# Expected output:
# All containers should show "Up" or "Up (healthy)"

# Check logs for any errors
docker compose logs --tail 50

# Check specific service logs
docker compose logs medication-web
docker compose logs keycloak
```

---

### Step 5: Wait for Services to Start

```powershell
# Wait for services to be ready
Start-Sleep -Seconds 15

# Or monitor logs until ready
docker compose logs -f
# Press Ctrl+C when you see "Started NllLightWebApplication"
```

---

### Step 6: Test the Application

```powershell
# Test Keycloak
$config = Invoke-RestMethod "http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration"
Write-Host "✅ Keycloak Issuer: $($config.issuer)"

# Test API
$medications = Invoke-RestMethod "http://localhost:8081/api/medications"
Write-Host "✅ API returned $($medications.Count) medications"

# Test Kong Gateway
$meds = Invoke-RestMethod "http://localhost:8000/api/medications"
Write-Host "✅ Kong gateway working: $($meds.Count) medications"

# Test Web Application
$response = Invoke-WebRequest "http://localhost:8080/login"
if($response.StatusCode -eq 200) { Write-Host "✅ Web app accessible" }
```

---

## Common Rebuild Scenarios

### Scenario 1: Clean Rebuild (Recommended)

**When to use**: After code changes, configuration updates, or when something isn't working

```powershell
# One command to rule them all
docker compose down -v && docker compose build --no-cache && docker compose up -d

# Wait and verify
Start-Sleep -Seconds 15
docker compose ps
```

---

### Scenario 2: Quick Rebuild (Single Service)

**When to use**: After changing only one module (e.g., medication-web)

```powershell
# Rebuild and restart just medication-web
docker compose build --no-cache medication-web
docker compose up -d medication-web

# Verify
docker compose logs medication-web --tail 20
```

---

### Scenario 3: Rebuild After Configuration Change

**When to use**: After editing application.properties, docker-compose.yml, etc.

```powershell
# Rebuild affected service
docker compose build medication-web

# Recreate containers to pick up environment changes
docker compose up -d --force-recreate medication-web
```

---

### Scenario 4: Nuclear Option (Complete Wipe)

**When to use**: When nothing else works

```powershell
# Stop everything
docker compose down -v --rmi all --remove-orphans

# Optional: Clean Docker system (removes unused images, networks, etc.)
docker system prune -a --volumes

# Rebuild from scratch
docker compose build --no-cache
docker compose up -d

# Wait and verify
Start-Sleep -Seconds 20
docker compose ps
docker compose logs --tail 100
```

**⚠️ WARNING**: This removes ALL Docker resources (not just this project)

---

## Troubleshooting Build Issues

### Issue: Build Fails During Maven Package

**Error**: `Failed to execute goal... BUILD FAILURE`

**Solution**:
```powershell
# Build locally first to check for errors
mvn clean package -DskipTests

# If local build works, rebuild Docker
docker compose build --no-cache
```

---

### Issue: Container Exits Immediately

**Symptom**: `docker compose ps` shows container as "Exited"

**Diagnosis**:
```powershell
# Check why container exited
docker compose logs medication-web
docker compose logs medication-api

# Common causes:
# - Port already in use
# - Configuration error
# - Dependency not available
```

**Solution**:
```powershell
# Check for port conflicts
netstat -ano | findstr :8080
netstat -ano | findstr :8081

# Restart with dependency order
docker compose up -d keycloak kong
Start-Sleep -Seconds 10
docker compose up -d medication-api medication-web
```

---

### Issue: Old Image Still Being Used

**Symptom**: Changes not reflected after rebuild

**Solution**:
```powershell
# Force remove old images
docker compose down --rmi all

# Rebuild without cache
docker compose build --no-cache

# Verify new image was created
docker images | Select-String "medication-web"
# Check the "Created" timestamp
```

---

### Issue: Volume Data Persisting

**Symptom**: Old data still present (e.g., old Keycloak users)

**Solution**:
```powershell
# Remove volumes
docker compose down -v

# List volumes to verify removal
docker volume ls | Select-String "nll-light"

# Start fresh
docker compose up -d
```

---

## Build Time Optimization

### First Build (Slow - ~2-5 minutes)

```powershell
# Initial build downloads dependencies
docker compose build
```

**What happens**:
- Downloads Maven dependencies
- Compiles Java code
- Runs tests
- Creates Docker layers

---

### Subsequent Builds (Faster - ~30 seconds)

```powershell
# Uses cached layers when possible
docker compose build medication-web
```

**What happens**:
- Reuses base image layers
- Only rebuilds changed layers
- Much faster

---

### Skip Tests for Faster Builds

**Not recommended for production, but useful for development**:

Edit Dockerfiles to add `-DskipTests`:

```dockerfile
# In medication-web/Dockerfile
RUN mvn -q -pl medication-web -am -DskipTests package
```

Then rebuild:
```powershell
docker compose build medication-web
```

---

## Monitoring Build Progress

### View Build Logs

```powershell
# Build with output (not detached)
docker compose build medication-web

# Watch progress:
# - Downloading dependencies
# - Compiling
# - Running tests
# - Creating image
```

---

### Check Build Size

```powershell
# View image sizes
docker images | Select-String "nll-light"

# Typical sizes:
# medication-web: ~400-500 MB
# medication-api: ~400-500 MB
```

---

## Complete Rebuild Checklist

Use this checklist for a guaranteed clean rebuild:

- [ ] Stop all containers: `docker compose down -v`
- [ ] Remove old images: `docker compose down --rmi all`
- [ ] Check no containers running: `docker ps`
- [ ] Build with no cache: `docker compose build --no-cache`
- [ ] Start services: `docker compose up -d`
- [ ] Wait for startup: `Start-Sleep -Seconds 15`
- [ ] Check status: `docker compose ps` (all should be "Up")
- [ ] Test Keycloak: Visit http://localhost:8082/auth/admin
- [ ] Test API: `Invoke-RestMethod http://localhost:8081/api/medications`
- [ ] Test Web: Visit http://localhost:8080
- [ ] Check logs: `docker compose logs --tail 50`
- [ ] Test login: Login with user666/secret

---

## Useful Commands Reference

```powershell
# View running containers
docker compose ps

# View all containers (including stopped)
docker compose ps -a

# View logs (all services)
docker compose logs

# View logs (specific service)
docker compose logs medication-web

# Follow logs in real-time
docker compose logs -f medication-web

# Restart a service
docker compose restart medication-web

# Stop all services
docker compose down

# Stop and remove volumes
docker compose down -v

# Build without cache
docker compose build --no-cache

# Build specific service
docker compose build medication-web

# Start all services
docker compose up -d

# Start specific service
docker compose up -d medication-web

# View resource usage
docker stats

# Enter a container shell
docker compose exec medication-web bash

# View container environment variables
docker compose exec medication-web printenv
```

---

## Development Workflow

### After Code Changes

```powershell
# 1. Edit code in your IDE
# 2. Rebuild the changed service
docker compose build medication-web

# 3. Restart the service
docker compose up -d medication-web

# 4. Check logs
docker compose logs -f medication-web

# 5. Test your changes
# Visit http://localhost:8080
```

---

### After Configuration Changes

**application.properties, docker-compose.yml, etc.**

```powershell
# Rebuild
docker compose build medication-web

# Force recreate to pick up environment changes
docker compose up -d --force-recreate medication-web

# Verify
docker compose logs medication-web --tail 20
```

---

## Access Points After Rebuild

Once rebuild is complete, services are available at:

| Service | URL | Credentials |
|---------|-----|-------------|
| Web App | http://localhost:8080 | user666/secret |
| API (Direct) | http://localhost:8081/api/medications | - |
| API (Kong) | http://localhost:8000/api/medications | - |
| Swagger UI | http://localhost:8000/swagger-ui.html | - |
| Keycloak Admin | http://localhost:8082/auth/admin | admin/admin |
| Kong Admin | http://localhost:8001 | - |

---

## Expected Build Output

### Successful Build

```
[+] Building 18.5s (21/21) FINISHED
 ✔ Container nll-light-kong-1            Started
 ✔ Container nll-light-keycloak-1        Started
 ✔ Container nll-light-medication-api-1  Started
 ✔ Container nll-light-medication-web-1  Started
```

### Successful Status

```
NAME                             STATUS
nll-light-medication-web-1       Up 2 minutes
nll-light-medication-api-1       Up 2 minutes
nll-light-keycloak-1             Up 2 minutes
nll-light-kong-1                 Up 2 minutes (healthy)
```

---

## Quick Commands

### Complete Fresh Start
```powershell
docker compose down -v && docker compose build --no-cache && docker compose up -d && Start-Sleep -Seconds 15 && docker compose ps
```

### Rebuild Single Service
```powershell
docker compose build --no-cache medication-web && docker compose up -d medication-web && docker compose logs -f medication-web
```

### Check Everything is Working
```powershell
docker compose ps; Invoke-RestMethod http://localhost:8081/api/medications | Measure-Object; Invoke-WebRequest http://localhost:8080/login | Select-Object StatusCode
```

---

**Need help?** See `TROUBLESHOOTING.md` for specific issues.
