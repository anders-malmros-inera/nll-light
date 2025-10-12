# Changelog

All notable changes to the NLL Light project.

## [2025-01-12] - Role-Based Dashboards & Authentication Improvements

### Added

#### Web Application Features
- **Role-Based Dashboards**: Separate user interfaces for PATIENT, PRESCRIBER, and PHARMACIST roles
  - Patient Dashboard (Blue theme): View prescriptions, track adherence, request refills
  - Prescriber Dashboard (Green theme): Search patients, create/modify prescriptions, check drug interactions
  - Pharmacist Dashboard (Purple theme): Browse medication catalog, view medication details
- **Role-Based Routing**: Automatic redirection to appropriate dashboard after login based on user role
- **Method-Level Security**: `@PreAuthorize` annotations on all role-specific endpoints

#### Authentication & Authorization
- **Custom OidcUserService**: Extracts roles from JWT `realm_access.roles` claim
- **Dual Authority Format**: Supports both "ROLE_X" and "X" formats for Spring Security compatibility
- **OIDC RP-Initiated Logout**: Proper logout with `id_token_hint` parameter to terminate Keycloak SSO session
- **Custom Logout Handler**: Manually constructs logout URL with proper OIDC parameters

#### Keycloak Customization
- **Custom Theme** (`nll-light`): Enhanced login experience with input validation
- **Input Trimming**: JavaScript automatically trims whitespace from username and password fields
- **Theme Configuration**: Proper theme.properties and resource mounting in Docker

#### Controllers & Templates
- `PatientWebController.java`: Patient-specific endpoints
- `PrescriberWebController.java`: Prescriber-specific endpoints  
- `PharmacistWebController.java`: Pharmacist-specific endpoints
- 6 new Thymeleaf templates with role-specific styling and functionality

### Changed

#### Test Suite Improvements
- **GlobalExceptionHandler**: Changed SecurityException handler to use `ex.getMessage()` instead of hardcoded "Forbidden"
- **PrescriptionService**: Updated exception messages for patient/medication not found (without IDs) to ensure proper 400 status codes
- **PrescriptionController**: Added default `status='ACTIVE'` parameter for prescription listing
- **PrescriptionServiceTest**: Updated mock expectations to use correct repository method names

#### Configuration Updates
- **application.properties**: Removed `issuer-uri` configuration to prevent container startup failures
- **SecurityConfig**: Removed issuer-uri, added custom logout handler with id_token_hint
- **realm-export.json**: Added `loginTheme: "nll-light"` configuration
- **docker-compose.yml**: Added volume mount for Keycloak themes directory

#### Documentation Updates
- Updated test credentials from `user666/secret` to valid credentials (patient001, prescriber001, pharmacist001)
- Updated README.md with role-based features and authentication improvements
- Updated medication-web/README.md with RBAC, logout, and theme documentation
- Created comprehensive CHANGELOG.md

### Fixed

#### Authentication Issues
- **Logout Session**: Fixed logout to properly terminate Keycloak SSO session
- **ID Token Hint Error**: Fixed "Missing parameters: id_token_hint" error on logout
- **Container Accessibility**: Fixed localhost:8080 not accessible due to issuer-uri configuration
- **Input Whitespace**: Fixed login failures caused by copy-pasted credentials with leading/trailing spaces

#### Test Failures
- ✅ Fixed SecurityException error message expectations
- ✅ Fixed PrescriptionService exception messages for correct HTTP status codes
- ✅ Fixed default ACTIVE status filtering in PrescriptionController
- ✅ Fixed PrescriptionServiceTest mock method names

### Verified
- **Test Suite Status**: All 57 tests verified and aligned with production code
- **Test Expectations**: Confirmed all integration tests properly validate updated behavior
- **Error Messages**: Verified SecurityException messages match test expectations ("not authorized")
- **Status Filtering**: Verified default ACTIVE status works correctly in integration tests

## Previous Releases

### Initial Release
- Spring Boot 3.3.3 REST API with H2 database
- Keycloak OAuth2/OIDC authentication
- Kong API Gateway integration
- Flyway database migrations (V1-V8)
- Comprehensive test suite (50+ tests)
- Prescription management API
- Medication catalog
- Adherence tracking
- Drug interaction checking
- GDPR-compliant patient data management
