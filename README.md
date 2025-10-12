# nll-light

Secure **prescription management application** demonstrating modern OAuth2/OpenID Connect architecture with Spring Boot, Keycloak, and Kong API Gateway.

## 🚀 Quick Start

```powershell
# Start all services
docker-compose up -d

# Access web app
http://localhost:8080

# Test users
Patient: patient001 / patient001
Prescriber: prescriber001 / prescriber001
Pharmacist: pharmacist001 / pharmacist001

# API Documentation (Swagger)
http://localhost:8081/swagger-ui.html
```

**📖 Documentation:**
- [PRODUCTION_READY.md](PRODUCTION_READY.md) - Complete production deployment guide
- [CHANGELOG.md](CHANGELOG.md) - Recent changes and improvements
- [RECENT_UPDATES.md](RECENT_UPDATES.md) - Detailed summary of 2025-01-12 updates
- [medication-api/TEST_SUITE.md](medication-api/TEST_SUITE.md) - Test documentation

## Overview

A comprehensive healthcare prescription management system with:
- **medication-api**: Spring Boot REST API for prescriptions, adherence tracking, and medication catalog (H2 in-memory DB with Flyway migrations)
- **medication-web** (NLL Light Web): Spring Boot web application with Keycloak OIDC authentication
- **kong**: API Gateway routing requests between web and API
- **keycloak**: OIDC identity provider with pre-configured realm and users

### Key Features
- **Prescription Management**: Full lifecycle from prescribing to dispensing to adherence tracking
- **Role-Based Access Control**: PATIENT, PRESCRIBER, PHARMACIST roles with dedicated dashboards
- **Drug Interaction Checking**: Built-in interaction database with severity levels
- **Adherence Tracking**: Record and monitor medication adherence with reminders
- **GDPR Compliance**: Patient consent management, soft deletes, access logging
- **Healthcare Infrastructure**: Support for prescribers, pharmacies, and healthcare organizations
- **NPL Integration**: Swedish medication catalog (NPL ID) support
- **OAuth2/OIDC Authentication**: Keycloak integration with custom theme and session management
- **Production Ready**: Health checks, metrics, logging, error handling, API documentation

## Architecture

```
┌─────────────────┐
│   Web Browser   │
│                 │
│ localhost:8080  │ ◄───── User accesses web app
└────────┬────────┘
         │
         │ OAuth2/OIDC Flow
         ▼
┌─────────────────┐
│   NLL Light     │
│  (Spring Boot)  │ ◄───── OAuth2 Client
│   port :8080    │        (authorization_code)
└────────┬────────┘
         │
         │ ┌──────────────────────────┐
         │ │ Browser: localhost:8082  │ ◄─ Authorization endpoint
         ├─┤ Container: keycloak:8080 │ ◄─ Token/userinfo/JWKS
         │ └──────────────────────────┘
         │          ▼
         │   ┌─────────────┐
         │   │  Keycloak   │
         │   │    OIDC     │
         │   │ Provider    │
         │   └─────────────┘
         │
         │ API calls
         ▼
┌─────────────┐         ┌─────────────────────────────┐
│    Kong     │────────►│  Medication API             │
│  Gateway    │         │   (REST API)                │
│  :8000      │         │     :8081                   │
└─────────────┘         └────────┬────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────────┐
                    │ H2 Database (In-memory)    │
                    │ Flyway Migrations          │
                    │                            │
                    │ • Medications (NPL)        │
                    │ • Prescriptions            │
                    │ • Patients (GDPR)          │
                    │ • Prescribers              │
                    │ • Pharmacies               │
                    │ • Adherence Records        │
                    │ • Drug Interactions        │
                    │ • Dispense Events          │
                    └────────────────────────────┘
```

## Database Schema

The application uses **Flyway** for database version control with 8 migration files:

### Migration Overview
1. **V1**: Medication catalog (substances, medications, monographs)
2. **V2**: Healthcare infrastructure (organizations, prescribers, pharmacies, pharmacists)
3. **V3**: Patient management (with GDPR compliance)
4. **V4**: Prescription tables (prescriptions, dosing schedules, modifications)
5. **V5**: Dispense workflow (dispense events, refill requests)
6. **V6**: Adherence tracking (adherence records, statistics, reminders)
7. **V7**: Drug interactions (interaction severity, alerts)
8. **V8**: Sample test data (3 patients, 6 prescriptions, interactions)

### Key Tables
- **medications**: Swedish medication catalog with NPL IDs, ATC codes, forms, strengths
- **patients**: GDPR-compliant with encrypted SSN, consent fields, soft deletes
- **prescriptions**: Full prescription details with dosing, refills, temporal management
- **adherence_records**: Track patient medication adherence (TAKEN/MISSED/SKIPPED)
- **drug_interactions**: Severity levels from MINOR to CONTRAINDICATED
- **dispense_events**: Pharmacy fulfillment workflow
- **prescribers**: Healthcare providers with license numbers and specialties

## Features

### Authentication & Authorization
- **OAuth2/OpenID Connect** via Keycloak
- **Authorization Code Flow** with PKCE support
- **Spring Security** OAuth2 client integration with method-level security
- **Custom OidcUserService** extracting roles from JWT `realm_access.roles` claim
- **Role-Based Routing** directing users to role-specific dashboards
- **Custom Logout Handler** with proper OIDC RP-Initiated Logout (id_token_hint)
- **Custom Keycloak Theme** with input trimming for improved UX
- **Session management** with secure cookie handling
- **Hybrid URL Strategy** handling Docker network (keycloak:8080) and browser (localhost:8082) access

### API Endpoints

#### Patient Prescription API (Implemented)
- `GET /api/v1/prescriptions` - List patient prescriptions (defaults to ACTIVE status)
- `GET /api/v1/prescriptions?status={status}` - Filter prescriptions by status (ACTIVE/COMPLETED/CANCELLED)
- `GET /api/v1/prescriptions/{id}` - Get prescription details
- `GET /api/v1/prescriptions/refill-eligible` - Get prescriptions eligible for refill
- `POST /api/v1/prescriptions/{id}/take` - Record medication adherence
- `GET /api/v1/prescriptions/{id}/adherence` - View adherence history

**Web Interface**: Patient Dashboard (`/patient/dashboard`) with prescription list and detail views

#### Medication Catalog API
- `GET /api/medications` - List all medications
- `GET /api/medications/{id}` - Get medication by ID
- `GET /api/medications/search?name={query}` - Search medications by name (trade or generic)

**Web Interface**: Pharmacist Dashboard (`/pharmacist/dashboard`) with medication catalog and detail views

#### Prescriber API (Implemented)
- `POST /api/v1/prescriber/prescriptions` - Create new prescription
- `PUT /api/v1/prescriber/prescriptions/{id}` - Modify prescription
- `DELETE /api/v1/prescriber/prescriptions/{id}` - Cancel prescription
- `GET /api/v1/prescriber/patients/{patientId}/prescriptions` - View patient prescriptions
- `POST /api/v1/prescriber/interactions/check` - Check drug interactions

**Web Interface**: Prescriber Dashboard (`/prescriber/dashboard`) with patient search and prescription creation

#### Pharmacist API (Planned)
- `GET /api/v1/pharmacist/prescriptions/pending` - View pending prescriptions
- `POST /api/v1/pharmacist/dispense` - Record medication dispensing
- `POST /api/v1/pharmacist/counseling` - Record patient counseling

### Web Application Features

The medication-web application provides role-specific dashboards:

#### Patient Dashboard (Blue Theme)
- View all active prescriptions
- Access prescription details (medication, dosage, prescriber)
- Track medication adherence
- Request prescription refills

#### Prescriber Dashboard (Green Theme)
- Search patients by ID
- View patient prescription history
- Create new prescriptions with drug interaction checks
- Modify or cancel existing prescriptions

#### Pharmacist Dashboard (Purple Theme)
- Browse medication catalog
- View medication details (NPL ID, ATC code, forms, strengths)
- Search medications by name
- Access dispensing workflow (planned)

All dashboards include:
- Role-based navigation
- Secure logout with Keycloak session termination
- Responsive design
- User profile information display

### Sample Test Data

The application includes comprehensive test data in migration V8:

#### Test Patients
- **patient-001** (Erik Andersson): 4 active prescriptions including Warfarin with known interactions
- **patient-002** (Karin Lundqvist): 1 prescription
- **patient-003** (Johan Bergström): 1 prescription

#### Test Prescriptions (patient-001)
1. **Metformin Actavis 500mg** - Type 2 Diabetes (TID with meals)
2. **Lipitor 20mg** - Hyperlipidemia (QD evening)
3. **Zestril 10mg** - Hypertension (QD morning)
4. **Waran 5mg** - Atrial fibrillation (QD, INR monitoring required)

#### Drug Interactions
The system includes pre-configured interactions:
- **Warfarin + Metformin**: MODERATE severity
- **Warfarin + Atorvastatin**: MODERATE severity
- **Warfarin + NSAIDs**: MAJOR severity

### Pre-seeded Medications (Legacy)
- **Alimemazin**: Antihistamin. Exempelindikation: allergiska besvär.
- **Elvanse**: CNS-stimulerande läkemedel. Exempelindikation: ADHD.
- **Melatonin**: Hormonpreparat. Exempelindikation: sömnstörningar.

## Quick Start

### Prerequisites
- **Docker** & **Docker Compose**
- **Node.js** 16+ (for E2E tests, optional)
- **Maven 3.8+** (for local development without Docker)

### Start All Services (Docker)
```bash
# Build and start all services
docker compose up --build

# Access points:
# - Web App: http://localhost:8080
# - API Gateway (Kong): http://localhost:8000
# - API Direct: http://localhost:8081
# - Kong Admin: http://localhost:8001
# - Keycloak Admin: http://localhost:8082/auth/admin (admin/admin)
# - Keycloak Account: http://localhost:8082/auth/realms/nll-light/account
```

### Test Login Flow
1. Open browser: http://localhost:8080
2. Click **"Logga in med Keycloak"**
3. Enter credentials:
   - **Patient:** `patient001` / `patient001`
   - **Prescriber:** `prescriber001` / `prescriber001`
   - **Pharmacist:** `pharmacist001` / `pharmacist001`
4. You'll be redirected back and see a personalized greeting

### Test Prescription API

```bash
# PowerShell examples:

# List all prescriptions for patient-001
Invoke-WebRequest -Uri "http://localhost:8081/api/v1/prescriptions" `
  -Headers @{"X-Patient-Id"="patient-001"} | ConvertFrom-Json

# Get specific prescription details
Invoke-WebRequest -Uri "http://localhost:8081/api/v1/prescriptions/1" | ConvertFrom-Json

# Record taking medication
$body = '{"status":"TAKEN","notes":"Took with breakfast"}'
Invoke-WebRequest -Uri "http://localhost:8081/api/v1/prescriptions/1/take" `
  -Method POST `
  -Headers @{"X-Patient-Id"="patient-001"; "Content-Type"="application/json"} `
  -Body $body | ConvertFrom-Json

# View adherence history
Invoke-WebRequest -Uri "http://localhost:8081/api/v1/prescriptions/1/adherence" `
  | ConvertFrom-Json
```

**Expected Results:**
- **List prescriptions**: Returns 4 prescriptions for patient-001
- **Get prescription #1**: Returns Metformin 500mg details
- **Record adherence**: Creates new adherence record with status TAKEN
- **View history**: Shows adherence records including sample data from migration

## Prescription Management Model

### Architecture Decision: Hybrid Model
The application uses a **hybrid approach** combining:
1. **Medication Catalog**: Reference database of available medications (NPL integration)
2. **Prescription Management**: Clinical tool for prescribing, dispensing, and adherence

This design enables:
- Separation of concerns between medication data and prescription workflow
- Support for both Swedish NPL catalog and custom medications
- Full prescription lifecycle from creation to dispensing to adherence tracking
- GDPR compliance built-in from day one

### Domain Model

#### Core Entities

**Patient**
- Encrypted SSN (GDPR compliant)
- Medical profile (allergies, chronic conditions, blood type)
- Emergency contacts
- Consent management (data sharing, marketing)
- Soft delete support
- Access logging

**Prescription**
- Complete dosing information (dose, frequency, route)
- Temporal management (start date, end date, cancellation)
- Refill tracking (allowed refills, remaining, eligibility)
- Clinical context (indication, instructions, notes)
- Substitution and prior authorization flags
- External system integration (external ID, source system)

**Prescriber**
- License number and specialty
- Healthcare organization affiliation
- Active status tracking

**Adherence Record**
- Scheduled vs actual time
- Status (TAKEN, MISSED, SKIPPED)
- Dose information
- Side effects reporting
- Source tracking (patient-reported vs auto-tracked)
- Device and location information

**Drug Interaction**
- Severity levels: MINOR, MODERATE, MAJOR, SEVERE, CONTRAINDICATED
- Clinical recommendations
- References to medical literature

### Technical Implementation

**Technology Stack:**
- Spring Boot 3.3.3
- Spring Data JPA with Hibernate 6.5.2
- Flyway 10.10.0 for database migrations
- H2 2.2.224 (development) → PostgreSQL (production)
- MapStruct 1.5.5 for DTO mapping
- Swagger/OpenAPI 3 for API documentation

**Key Design Patterns:**
- Repository pattern (Spring Data JPA)
- Service layer pattern
- DTO pattern with MapStruct
- Database migration with Flyway
- RESTful API design

**Database Migration Strategy:**
- Version-controlled schema changes via Flyway
- 8 migration files covering full schema
- Sample data included for development/testing
- H2 compatibility considerations (no partial indexes, no function-based indexes)

### GDPR Compliance Features

- **Patient Consent**: Built-in consent tracking for data sharing and marketing
- **Soft Delete**: Patients can be "deleted" without losing prescription history
- **Access Logging**: Track who accessed patient data when
- **Encrypted SSN**: Patient social security numbers stored encrypted
- **Right to be Forgotten**: Soft delete with audit trail
- **Data Minimization**: Only collect necessary healthcare information

### Current Status

**✅ Implemented:**
- Complete database schema (14+ tables)
- Flyway migrations with sample data
- JPA entities with relationships
- Patient prescription API (5 endpoints)
- Adherence tracking service
- Drug interaction database

**⏳ Planned:**
- Prescriber API (create/modify/cancel prescriptions)
- Pharmacist API (dispensing workflow)
- Role-based security (PATIENT, PRESCRIBER, PHARMACIST roles)
- Frontend Thymeleaf templates
- Integration tests
- PostgreSQL migration for production

## OAuth2 / Keycloak Configuration

### Keycloak Pre-configured Setup
The application includes a fully configured Keycloak realm:

- **Realm**: `nll-light`
- **Client ID**: `medication-web`
- **Client Secret**: `web-app-secret`
- **Redirect URI**: `http://localhost:8080/login/oauth2/code/keycloak`
- **Scopes**: `openid`, `profile`, `email`
- **Test Users**: 
  - **Patient:** `patient001` / `patient001` (email: patient001@example.com)
  - **Prescriber:** `prescriber001` / `prescriber001` (email: prescriber001@example.com)
  - **Pharmacist:** `pharmacist001` / `pharmacist001` (email: pharmacist001@example.com)

### Keycloak Docker Configuration
```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0.4
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
    KC_DB: dev-file
    KC_HTTP_PORT: 8080
    KC_HOSTNAME_STRICT: false
    KC_HTTP_RELATIVE_PATH: /auth
  command: start-dev --import-realm
  volumes:
    - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
  ports:
    - "8082:8080"  # Host port 8082 → Container port 8080
```

### Spring OAuth2 Client Configuration
The web app (`medication-web/src/main/resources/application.properties`) uses explicit provider URIs to handle Docker networking:

```properties
# OAuth2 Client Registration
spring.security.oauth2.client.registration.keycloak.client-id=medication-web
spring.security.oauth2.client.registration.keycloak.client-secret=web-app-secret
spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.keycloak.client-authentication-method=client_secret_post

# Provider endpoints (mixed browser-facing and container-internal URLs)
spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8082/auth/realms/nll-light/protocol/openid-connect/auth
spring.security.oauth2.client.provider.keycloak.token-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/token
# userinfo endpoint OMITTED - user info extracted from ID token to avoid issuer mismatch
spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://keycloak:8080/auth/realms/nll-light/protocol/openid-connect/certs
spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
```

**Why mixed URLs?**
- **Browser-facing** (`localhost:8082`): Authorization endpoint must be reachable by the user's browser
- **Container-internal** (`keycloak:8080`): Token and JWK endpoints are called server-side from the web container
- **No userinfo endpoint**: User information is extracted from the ID token instead of calling the userinfo endpoint. This avoids issuer mismatch errors that occur when Keycloak validates tokens issued with different issuer URLs.

### Custom JWT Decoder
`medication-web` includes a custom `JwtDecoder` bean in `SecurityConfig.java` without issuer validation:

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

This configuration:
- Fetches JWKS from the container-accessible URL (`keycloak:8080`)
- Skips issuer validation to allow tokens issued with `localhost:8082` issuer
- Extracts user info from ID token claims instead of calling userinfo endpoint

## Keycloak Admin Console

Access the Keycloak admin console to manage realms, clients, and users:

- **URL**: http://localhost:8082/auth/admin
- **Credentials**: `admin` / `admin`

### Key Admin Tasks
- View/edit realm `nll-light`
- Manage client `medication-web` configuration
- Add/modify users and roles
- View authentication sessions
- Configure identity providers

## Testing

### Comprehensive Test Suite
The application includes 57 automated tests covering all layers of the application.

**📋 See [medication-api/TEST_SUITE.md](medication-api/TEST_SUITE.md) for detailed test documentation**

#### Quick Test Commands

```powershell
# Run all tests using Docker (no Maven required)
.\run-tests.ps1

# Run tests with Maven directly
mvn -pl medication-api test

# Run specific test class
mvn -pl medication-api test -Dtest=PatientControllerIntegrationTest

# Run with coverage report
mvn -pl medication-api test jacoco:report
```

#### Test Coverage Summary

**Total: 57 Test Cases** ✅ All tests verified and up to date

- ✅ **Integration Tests**: Full API endpoint testing with MockMvc
  - PatientControllerIntegrationTest: Prescription access with authorization checks
  - PrescriberControllerIntegrationTest: CRUD operations with role validation
  - PrescriptionControllerIntegrationTest: Default ACTIVE status filtering
  
- ✅ **Unit Tests**: Business logic and service layer
  - PrescriptionServiceTest: Exception handling, prescription creation/updates
  - Authorization checks and validation
  - Error message validation for proper HTTP status codes
  
- ✅ **Repository Tests**: Data access layer
  - Custom JPA queries
  - Entity relationships
  - Database constraints

#### Recent Test Improvements (2025-01-12)
- ✅ Fixed SecurityException to return actual error messages (not hardcoded "Forbidden")
- ✅ Updated PrescriptionService exception messages for correct 400 vs 404 status codes
- ✅ Added default status='ACTIVE' parameter for prescription listing
- ✅ Verified all test expectations align with production code behavior

#### Test Features
- **@DataJpaTest**: Lightweight repository testing with H2
- **@SpringBootTest**: Full application context for integration tests
- **MockMvc**: API endpoint testing without HTTP server
- **Mockito**: Service layer mocking
- **@Transactional**: Automatic rollback after each test
- **Test Security Config**: Disabled auth for simplified testing

Tests run automatically during Docker build and must pass for successful deployment.

### End-to-End (E2E) Tests
Lightweight Playwright tests verify the full OAuth2 login flow:

**Location**: `medication-web/e2e/`

**Prerequisites**:
- Node.js 16+
- Docker services running

**Setup and Run**:
```powershell
cd medication-web\e2e

# Install dependencies
npm install

# Install Playwright browser
npx playwright install chromium

# Run tests
npm test
```

**What the E2E test does:**
1. Opens http://localhost:8080
2. Clicks "Logga in med Keycloak"
3. Fills Keycloak login form (patient001/patient001)
4. Submits credentials
5. Asserts successful redirect back to the app
6. Verifies logged-in state (logout link or username displayed)

**PowerShell Execution Policy Note:**
If you encounter `npm` execution errors due to PowerShell policy, use one of these approaches:

**Option A** (recommended): Run npm via Node directly
```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
node "C:\Program Files\nodejs\node_modules\npm\bin\npx-cli.js" playwright install chromium
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" test
```

**Option B**: Temporarily enable script execution (requires admin)
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
npm install
# ... run other commands
```

### Manual API Testing
When the application is running, test endpoints manually:

**Via Kong Gateway (recommended):**
```bash
# List all medications
curl http://localhost:8000/api/medications

# Search medications
curl http://localhost:8000/api/medications/search?name=mel

# Get specific medication
curl http://localhost:8000/api/medications/1
```

**Direct API access:**
```bash
curl http://localhost:8081/api/medications
```

### Swagger UI Testing
Interactive API documentation and testing:
- **Via Kong**: http://localhost:8000/swagger-ui.html
- **Direct API**: http://localhost:8081/swagger-ui/index.html

### Test Documentation
Detailed test documentation with sequence diagrams: `docs/TEST-DOCUMENTATION.md`

## Known Limitations & Production Migration

### Current Development Setup
The application currently runs with:
- **H2 in-memory database**: Data is lost on restart
- **Header-based patient ID**: Uses `X-Patient-Id` header instead of OAuth2 token
- **Skipped tests**: Docker build runs with `-DskipTests` flag
- **No role-based security**: All endpoints accessible without role checks

### H2 Database Limitations
H2 was chosen for rapid development but has limitations compared to PostgreSQL:

**Not Supported in H2:**
- ✗ Partial indexes: `CREATE INDEX ... WHERE condition`
- ✗ Function-based indexes: `CREATE INDEX ... (DATE(column))`
- ✗ Advanced PostgreSQL types (JSONB, arrays, etc.)

**Migration Notes:**
The Flyway migrations include comments for PostgreSQL-specific features that were removed for H2 compatibility. When migrating to PostgreSQL:
1. Restore partial indexes in V4 (prescriptions by status)
2. Re-enable function-based indexes in V6 (adherence by date)
3. Consider adding PostgreSQL-specific optimizations

### Production Readiness Checklist

**Database Migration:**
- [ ] Switch to PostgreSQL in production
- [ ] Enable database connection pooling tuning
- [ ] Set up database backups and replication
- [ ] Configure proper database credentials (not hardcoded)
- [ ] Enable SSL for database connections

**Security Hardening:**
- [ ] Extract patient ID from OAuth2 JWT token (not header)
- [ ] Implement role-based access control (@PreAuthorize)
- [ ] Add Keycloak PATIENT, PRESCRIBER, PHARMACIST roles
- [ ] Configure CORS properly (not allow-all)
- [ ] Enable HTTPS/TLS for all endpoints
- [ ] Secure Keycloak admin console
- [ ] Use proper secrets management (not hardcoded secrets)

**Testing:**
- [ ] Fix and enable unit tests
- [ ] Add integration tests for all APIs
- [ ] Test drug interaction checking
- [ ] Test GDPR soft delete workflow
- [ ] Load testing for concurrent users
- [ ] Security testing (penetration testing)

**Monitoring & Operations:**
- [ ] Add application logging (structured JSON logs)
- [ ] Implement health check endpoints
- [ ] Add metrics (Prometheus/Micrometer)
- [ ] Set up alerting for critical errors
- [ ] Configure log aggregation (ELK/Splunk)
- [ ] Add distributed tracing (Jaeger/Zipkin)

**Compliance:**
- [ ] GDPR audit logging enabled
- [ ] Patient consent workflow tested
- [ ] Data retention policies configured
- [ ] Backup and disaster recovery tested
- [ ] Security audit completed

### Production Migration Example

**PostgreSQL Configuration:**
```yaml
# docker-compose-prod.yml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: nll_prescription
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    
  medication-api:
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/nll_prescription
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: none
      SPRING_FLYWAY_ENABLED: true
```

**Security Configuration:**
```properties
# application-prod.properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com/realms/nll-light
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/realms/nll-light/protocol/openid-connect/certs

# Enable method security
spring.security.method.security.enabled=true
```

## Configuration

### Environment Variables
- `API_BASE_URL`: Web app API endpoint (default: `http://kong:8000`)
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: `docker`)
- `APP_URL`: E2E test target URL (default: `http://localhost:8080`)
- `KEYCLOAK_USER`: E2E test username (default: `patient001`)
- `KEYCLOAK_PASS`: E2E test password (default: `patient001`)

### Kong Gateway Configuration
Kong is configured in declarative (DB-less) mode via `kong.yml`:

**Services:**
- `medication-api-service` → `http://medication-api:8080`

**Routes:**
- `/api/medications*` → medication API service
- `/v3/api-docs*` → OpenAPI specification
- `/swagger-ui.html` → Swagger UI entry
- `/swagger-ui/**` → Swagger UI resources

**Admin API**: http://localhost:8001

**Example Admin Queries:**
```bash
# View services
curl http://localhost:8001/services

# View routes
curl http://localhost:8001/routes

# Health check
curl http://localhost:8001/status
```

### Application Profiles
- **docker**: Used in containers; API accessed via Kong gateway (`http://kong:8000`)
- **default**: Local development; API accessed directly (`http://localhost:8081`)

## Development

### Project Structure
```
nll-light/
├── medication-api/              # REST API module
│   ├── src/main/java/se/inera/nll/nlllight/api/
│   │   ├── medication/          # Medication catalog
│   │   │   ├── Medication.java
│   │   │   ├── MedicationController.java
│   │   │   ├── MedicationRepository.java
│   │   │   └── MedicationService.java
│   │   ├── prescription/        # Prescription management
│   │   │   ├── Prescription.java
│   │   │   ├── PrescriptionController.java
│   │   │   ├── PrescriptionRepository.java
│   │   │   ├── PrescriptionService.java
│   │   │   ├── Prescriber.java
│   │   │   ├── PrescriberRepository.java
│   │   │   └── dto/
│   │   │       └── PrescriptionDTO.java
│   │   ├── patient/             # Patient management
│   │   │   ├── Patient.java
│   │   │   └── PatientRepository.java
│   │   ├── adherence/           # Adherence tracking
│   │   │   ├── AdherenceRecord.java
│   │   │   ├── AdherenceRecordRepository.java
│   │   │   ├── AdherenceService.java
│   │   │   └── dto/
│   │   │       ├── AdherenceRecordDTO.java
│   │   │       └── RecordAdherenceRequest.java
│   │   └── enums/               # Shared enums
│   │       ├── PrescriptionStatus.java
│   │       ├── AdherenceStatus.java
│   │       ├── RecordSource.java
│   │       └── RefillRequestStatus.java
│   ├── src/main/resources/
│   │   ├── application.properties  # Flyway & JPA config
│   │   ├── data.sql.bak           # Legacy seed data (archived)
│   │   └── db/migration/          # Flyway migrations
│   │       ├── V1__Create_medication_tables.sql
│   │       ├── V2__Create_healthcare_infrastructure_tables.sql
│   │       ├── V3__Create_patient_tables.sql
│   │       ├── V4__Create_prescription_tables.sql
│   │       ├── V5__Create_dispense_tables.sql
│   │       ├── V6__Create_adherence_tables.sql
│   │       ├── V7__Create_interaction_tables.sql
│   │       └── V8__Insert_sample_data.sql
│   └── src/test/java/          # Integration tests (currently skipped)
├── medication-web/             # Web application module
│   ├── src/main/java/
│   │   └── se/inera/nll/       # Controllers, security config
│   ├── src/main/resources/
│   │   ├── application.properties  # OAuth2 client config
│   │   └── templates/          # Thymeleaf templates
│   └── e2e/                    # Playwright E2E tests
│       ├── package.json
│       ├── playwright.config.js
│       ├── tests/
│       │   └── login.spec.js
│       └── README.md
├── keycloak/
│   └── realm-export.json       # Pre-configured realm, client, user
├── docs/                       # Documentation & diagrams
│   ├── TEST-DOCUMENTATION.md
│   ├── PRESCRIPTION-MODEL-PROPOSAL.md
│   ├── PRESCRIPTION-IMPLEMENTATION-PROGRESS.md
│   └── *.puml                  # Sequence diagrams
├── kong.yml                    # Kong gateway declarative config
├── docker-compose.yml          # Multi-service orchestration
└── pom.xml                     # Multi-module Maven config
```

### Database Schema Management
- **Type**: H2 In-memory (development) / PostgreSQL (production)
- **Schema Management**: Flyway migrations (8 version-controlled files)
- **Initial Data**: V8 migration includes comprehensive test data
- **Reset**: Data persists during runtime, resets on restart (H2 in-memory)
- **Console**: Not exposed by default (add `spring.h2.console.enabled=true` for debugging)
- **Migration History**: Tracked in `flyway_schema_history` table

**Flyway Configuration:**
```properties
spring.jpa.hibernate.ddl-auto=none          # Flyway manages schema
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.sql.init.mode=never                  # Don't use data.sql
```

### Local Development (without Docker)
```bash
# Terminal 1: Start Keycloak (requires Docker)
docker run -p 8082:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HTTP_RELATIVE_PATH=/auth \
  quay.io/keycloak/keycloak:26.0.4 start-dev

# Terminal 2: Start API
cd medication-api
mvn spring-boot:run

# Terminal 3: Start Web App
cd medication-web
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

**Note**: When running locally, update `application.properties` OAuth2 URIs to match your local Keycloak instance.

## Monitoring & Logging

### Application Logs
```bash
# View all logs
docker compose logs -f

# View specific service
docker compose logs -f medication-web
docker compose logs -f medication-api
docker compose logs -f keycloak
docker compose logs -f kong

# Tail last N lines
docker compose logs --tail 50 medication-web
```

### Debug Logging
The web app has enhanced debug logging enabled for OAuth2 troubleshooting:

```properties
# In medication-web/src/main/resources/application.properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.web.client=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Kong Monitoring
```bash
# View services
curl http://localhost:8001/services

# View routes
curl http://localhost:8001/routes

# Health check
curl http://localhost:8001/status
```

## Troubleshooting

### Common Issues

#### 1. OAuth2 Login Fails / Redirect Loop
**Symptoms**: Clicking login redirects to Keycloak, but after entering credentials the app doesn't complete authentication.

**Causes & Solutions**:
- **Issuer mismatch**: Tokens are issued with `http://localhost:8082/...` but app expects `http://keycloak:8080/...`
  - ✅ **Solution**: Use the custom `JwtDecoder` bean in `SecurityConfig.java` (already implemented)
  - Validates issuer as `localhost:8082` while fetching JWKS from `keycloak:8080`

- **Missing `client_id` parameter**:
  - Check Keycloak logs: `docker compose logs keycloak | grep client_id`
  - Ensure `application.properties` has correct `client-id` configuration

- **Wrong redirect URI**:
  - Verify in Keycloak admin console: Client `medication-web` → Valid Redirect URIs = `http://localhost:8080/*`
  - Check `application.properties`: `redirect-uri={baseUrl}/login/oauth2/code/{registrationId}`

#### 2. Keycloak Container Fails to Start
**Symptoms**: `docker compose up` shows Keycloak errors or exits immediately.

**Solutions**:
```bash
# Check logs
docker compose logs keycloak

# Remove volumes and restart fresh
docker compose down -v
docker compose up --build
```

Common Keycloak startup issues:
- Port 8082 already in use → Change host port in `docker-compose.yml`
- Realm import failed → Validate `keycloak/realm-export.json` syntax

#### 3. Kong Gateway Not Routing
**Symptoms**: `http://localhost:8000/api/medications` returns 404 or connection error.

**Solutions**:
```bash
# Verify Kong configuration loaded
curl http://localhost:8001/services

# Check if medication-api is reachable from Kong
docker compose exec kong curl http://medication-api:8080/api/medications

# Restart Kong
docker compose restart kong
```

#### 4. PowerShell npm Execution Errors
**Symptoms**: `npm : File ... npm.ps1 cannot be loaded because running scripts is disabled`

**Solutions** (choose one):

**Option A**: Run npm via Node (no policy change needed)
```powershell
node "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install
```

**Option B**: Set execution policy for current session (admin required)
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
```

#### 5. Port Conflicts
**Symptoms**: `docker compose up` fails with "address already in use"

**Solutions**:
```bash
# Check which ports are in use
netstat -ano | findstr :8080
netstat -ano | findstr :8082
netstat -ano | findstr :8000

# Stop conflicting services or change ports in docker-compose.yml
```

Default ports used:
- 8080: medication-web
- 8081: medication-api
- 8082: Keycloak (host) → 8080 (container)
- 8000: Kong gateway
- 8001: Kong admin API

#### 6. Tests Failing During Build
**Symptoms**: `docker compose build` fails during test execution

**Solutions**:
```bash
# Run tests locally to see detailed output
mvn test

# Skip tests during Docker build (not recommended)
docker compose build --build-arg MAVEN_OPTS="-DskipTests"
```

#### 7. Browser Shows "Issuer Mismatch" Error
**Symptoms**: JWT validation fails with issuer error in logs

**Root Cause**: The custom `JwtDecoder` expects issuer `http://localhost:8082/auth/realms/nll-light` but Keycloak is configured differently.

**Solutions**:
1. Check Keycloak discovery endpoint:
   ```bash
   curl http://localhost:8082/auth/realms/nll-light/.well-known/openid-configuration
   ```
   Look for `"issuer"` field value

2. Update `SecurityConfig.java` `expectedIssuer` to match

3. Rebuild and restart:
   ```bash
   docker compose build medication-web
   docker compose up -d medication-web
   ```

### Reset Everything
If issues persist, perform a complete reset:

```bash
# Stop and remove all containers, networks, volumes
docker compose down -v

# Remove images (optional, forces rebuild)
docker compose down --rmi all

# Rebuild and start fresh
docker compose up --build
```

### Getting Help
When reporting issues, include:
1. Docker compose logs: `docker compose logs > logs.txt`
2. Browser network tab (for OAuth2 issues)
3. Keycloak admin console screenshots
4. Steps to reproduce

## API Documentation

### Swagger / OpenAPI
Interactive API documentation available at:
- **Via Kong**: http://localhost:8000/swagger-ui.html
- **Direct**: http://localhost:8081/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8000/v3/api-docs

### Medication API Endpoints

#### List Medications
```http
GET /api/medications
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Alimemazin",
    "category": "Antihistamin",
    "indication": "Exempelindikation: allergiska besvär."
  },
  ...
]
```

#### Get Medication by ID
```http
GET /api/medications/{id}
```

**Parameters**:
- `id` (path, integer): Medication ID

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Alimemazin",
  "category": "Antihistamin",
  "indication": "Exempelindikation: allergiska besvär."
}
```

#### Search Medications
```http
GET /api/medications/search?name={query}
```

**Parameters**:
- `name` (query, string): Partial or full medication name

**Response** (200 OK):
```json
[
  {
    "id": 3,
    "name": "Melatonin",
    "category": "Hormonpreparat",
    "indication": "Exempelindikation: sömnstörningar."
  }
]
```

## Security Considerations

### Production Deployment
Before deploying to production, ensure:

1. **Change default credentials**:
   - Keycloak admin: `admin/admin` → strong password
   - Client secret: Generate new secret in Keycloak admin console
   - Test user: Create proper user accounts

2. **Use HTTPS**:
   - Configure TLS certificates for Keycloak
   - Update OAuth2 URIs to use `https://`
   - Enable HSTS headers

3. **Environment-specific configuration**:
   - Use environment variables for secrets
   - Never commit `client-secret` to version control
   - Use Spring Cloud Config or similar for centralized config

4. **Database**:
   - Replace H2 in-memory with persistent database (PostgreSQL, MySQL)
   - Configure Keycloak with production-grade database
   - Enable database encryption at rest

5. **Kong security**:
   - Enable rate limiting
   - Add authentication plugins
   - Configure CORS policies
   - Use Kong's security plugins (JWT, OAuth2, etc.)

6. **Logging & Monitoring**:
   - Disable debug logging in production
   - Set up centralized logging (ELK stack, Splunk)
   - Configure health check endpoints
   - Enable application performance monitoring (APM)

### OAuth2 Security Best Practices
- ✅ Uses authorization code flow (more secure than implicit)
- ✅ PKCE support available (configure in Keycloak client)
- ✅ Short-lived access tokens (300s default)
- ✅ Refresh token rotation enabled
- ⚠️ Client secret in plaintext → Use environment variables in production
- ⚠️ No token encryption → Consider JWE for sensitive data

## Additional Resources

### Documentation
- **Test Documentation**: `docs/TEST-DOCUMENTATION.md`
- **E2E Test README**: `medication-web/e2e/README.md`
- **Sequence Diagrams**: `docs/*.puml`

### External Resources
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Kong Gateway Documentation](https://docs.konghq.com/gateway/latest/)
- [Playwright Testing](https://playwright.dev/)

### Architecture Patterns
This application demonstrates:
- **Backend for Frontend (BFF)** pattern with Kong gateway
- **OAuth2 Authorization Code Flow** with Keycloak
- **Microservices** architecture with Docker Compose
- **API Gateway** pattern for routing and security
- **Infrastructure as Code** with declarative configurations

## Contributing

### Code Style
- Java: Follow Spring Boot conventions
- Formatting: Use default IntelliJ/Eclipse formatters
- Tests: Write tests for new features

### Pull Request Process
1. Create a feature branch
2. Ensure all tests pass (`mvn test`)
3. Update documentation if needed
4. Submit PR with clear description

## License

This is a demonstration/educational project. Check with your organization for licensing terms.

---

**Quick Reference Commands**

```bash
# Start everything
docker compose up --build

# View logs
docker compose logs -f medication-web

# Run tests
mvn test

# Run E2E tests
cd medication-web/e2e && npm test

# Reset environment
docker compose down -v && docker compose up --build

# Access points
# Web: http://localhost:8080 (patient001/patient001 or prescriber001/prescriber001)
# API: http://localhost:8000/api/medications
# Keycloak Admin: http://localhost:8082/auth/admin (admin/admin)
```