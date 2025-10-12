# Role-Based Web Interface Implementation Summary

## Overview
Implemented separate web interfaces for three user roles: PATIENT, PRESCRIBER, and PHARMACIST.

## Implementation Details

### Security Configuration (`SecurityConfig.java`)
- **Method Security**: Enabled with `@EnableMethodSecurity(prePostEnabled = true)`
- **Role Extraction**: Custom `OidcUserService` extracts roles from JWT `realm_access.roles` claim
- **Authority Mapping**: Adds both `ROLE_X` and `X` formats for Spring Security compatibility
- **Authentication**: OAuth2/OIDC integration with Keycloak

### Controllers

#### 1. PatientWebController (`/patient`)
- **Access**: `@PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'PATIENT')")`
- **Endpoints**:
  - `GET /patient/dashboard` - View patient's prescriptions
  - `GET /patient/prescriptions/{id}` - View prescription details
- **API Integration**: Calls `/api/v1/prescriptions` with `X-Patient-Id` header
- **Template**: Blue theme (#2c3e50, #3498db)

#### 2. PrescriberWebController (`/prescriber`)
- **Access**: `@PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")`
- **Endpoints**:
  - `GET /prescriber/dashboard` - Main dashboard with medications catalog
  - `GET /prescriber/prescriptions/new` - Create prescription form
  - `POST /prescriber/prescriptions` - Submit new prescription
- **API Integration**: 
  - Calls `/api/medications` for medication list
  - Calls `/api/v1/prescriber/prescriptions` with `X-Prescriber-Id` header
- **Template**: Green theme (#27ae60)

#### 3. PharmacistWebController (`/pharmacist`)
- **Access**: `@PreAuthorize("hasAnyAuthority('ROLE_PHARMACIST', 'PHARMACIST')")`
- **Endpoints**:
  - `GET /pharmacist/dashboard` - Medication catalog view
  - `GET /pharmacist/medications/{id}` - Medication details
- **API Integration**: Calls `/api/medications` and `/api/medications/{id}`
- **Template**: Purple theme (#8e44ad)

### WebController Routing
- **Root URL (`/`)**: Detects user role and redirects to appropriate dashboard
- **Role Detection**: Checks OAuth2User authorities for PATIENT/PRESCRIBER/PHARMACIST
- **Fallback**: If no role detected, renders generic index template

### Templates

#### Patient Templates
1. **patient-dashboard.html**
   - Displays patient's prescriptions in grid layout
   - Color-coded status badges (ACTIVE, COMPLETED, CANCELLED)
   - Shows: medication name, prescription #, prescriber, dates, refills
   - Links to prescription detail view

2. **patient-prescription-detail.html**
   - Detailed prescription information
   - Sections: Prescription Info, Medication, Prescriber, Refills
   - Read-only view (patients cannot edit)
   - Back navigation to dashboard

#### Prescriber Templates
1. **prescriber-dashboard.html**
   - Action cards: Create Prescription, Medication Catalog, Patient Records
   - Medications grid showing all available medications
   - Links to prescription creation form

2. **prescriber-create-prescription.html**
   - Comprehensive prescription creation form
   - Fields: Patient ID, Medication (dropdown), Dosage, Quantity, Days Supply, Frequency, Instructions, Refills
   - Client-side validation
   - Form submission to POST endpoint

#### Pharmacist Templates
1. **pharmacist-dashboard.html**
   - Full medication catalog with search functionality
   - Real-time search filtering (by name, form, ATC code)
   - Availability badges
   - Shows: trade name, generic name, form, strength, ATC code, price, NPL ID
   - Links to medication detail view

2. **pharmacist-medication-detail.html**
   - Comprehensive medication information
   - Sections: Basic Info, Classification, Commercial Info, System Info
   - Displays all medication properties including codes and IDs

## User Credentials

### Test Users (in Keycloak)
1. **Patient**: 
   - Username: `patient001`
   - Password: `patient001`
   - Attribute: `patient-id=["patient-001"]`
   - Role: `PATIENT`

2. **Prescriber**:
   - Username: `prescriber001`
   - Password: `prescriber001`
   - Attribute: `prescriber-id=["prescriber-001"]`
   - Role: `PRESCRIBER`

3. **Pharmacist**:
   - Username: `pharmacist001`
   - Password: `pharmacist001`
   - Attribute: `pharmacist-id=["pharmacist-001"]`
   - Role: `PHARMACIST`

## Testing Instructions

### 1. Test Patient Access
```
1. Navigate to http://localhost:8080
2. Login with: patient001 / patient001
3. Should redirect to /patient/dashboard
4. Verify: Blue theme, list of prescriptions
5. Click "View Details" on a prescription
6. Verify: Detailed prescription view with all information
```

### 2. Test Prescriber Access
```
1. Logout from current session
2. Login with: prescriber001 / prescriber001
3. Should redirect to /prescriber/dashboard
4. Verify: Green theme, action cards, medication catalog
5. Click "Create New Prescription"
6. Verify: Prescription form with all fields
7. Fill form and submit (or cancel)
```

### 3. Test Pharmacist Access
```
1. Logout from current session
2. Login with: pharmacist001 / pharmacist001
3. Should redirect to /pharmacist/dashboard
4. Verify: Purple theme, medication catalog with search
5. Use search box to filter medications
6. Click "View Details" on a medication
7. Verify: Comprehensive medication information
```

### 4. Test Access Control
```
1. Login as patient001
2. Manually navigate to http://localhost:8080/prescriber/dashboard
3. Expected: 403 Forbidden or redirect to patient dashboard
4. Repeat for other role combinations
```

## Key Features

### Role-Based Access Control
- ✅ Separate dashboards per user role
- ✅ Method-level security with @PreAuthorize
- ✅ Role extraction from JWT token
- ✅ Automatic routing from root URL

### Custom Attribute Extraction
- ✅ Extracts patient-id, prescriber-id, pharmacist-id from JWT
- ✅ Handles List<String> attribute type
- ✅ Fallback values for missing attributes
- ✅ Passed as headers to API calls

### User Experience
- ✅ Distinct color themes per role (Blue/Green/Purple)
- ✅ Role-appropriate functionality
- ✅ Responsive grid layouts
- ✅ Search/filter capabilities
- ✅ Clear navigation and logout

### API Integration
- ✅ RestClient for HTTP calls
- ✅ Role-specific API endpoints
- ✅ Custom headers (X-Patient-Id, X-Prescriber-Id)
- ✅ Error handling with user-friendly messages

## Files Modified/Created

### Modified Files
1. `medication-web/src/main/java/se/inera/nll/nlllight/web/SecurityConfig.java`
2. `medication-web/src/main/java/se/inera/nll/nlllight/web/WebController.java`

### Created Files
3. `medication-web/src/main/java/se/inera/nll/nlllight/web/PatientWebController.java`
4. `medication-web/src/main/java/se/inera/nll/nlllight/web/PrescriberWebController.java`
5. `medication-web/src/main/java/se/inera/nll/nlllight/web/PharmacistWebController.java`
6. `medication-web/src/main/resources/templates/patient-dashboard.html`
7. `medication-web/src/main/resources/templates/patient-prescription-detail.html`
8. `medication-web/src/main/resources/templates/prescriber-dashboard.html`
9. `medication-web/src/main/resources/templates/prescriber-create-prescription.html`
10. `medication-web/src/main/resources/templates/pharmacist-dashboard.html`
11. `medication-web/src/main/resources/templates/pharmacist-medication-detail.html`

## Next Steps

### Immediate
1. ✅ All templates created
2. ✅ All controllers implemented
3. ✅ Security configuration complete
4. ⏳ Manual testing with all three user types

### Future Enhancements
- Add pagination for large lists
- Implement advanced search and filtering
- Add prescription history for pharmacists
- Patient records management for prescribers
- Real-time notifications
- Print/export functionality
- Prescription refill workflow
- Medication interaction checks

## Architecture Benefits

1. **Separation of Concerns**: Each role has its own controller and views
2. **Security**: Method-level access control prevents unauthorized access
3. **Maintainability**: Clear structure makes it easy to add/modify role-specific features
4. **Scalability**: Easy to add new roles or endpoints
5. **User Experience**: Role-appropriate interfaces improve usability
6. **API Integration**: Clean separation between web layer and API layer

## Known Limitations

1. **Browser Session**: Need to clear session when switching users
2. **No Role Switching**: Users must logout to switch roles
3. **Single Role**: Users can only have one primary role
4. **Static Routing**: Root URL routing is simple (no complex role hierarchies)

## Troubleshooting

### Issue: Cannot access role-specific dashboard
**Solution**: 
1. Check browser console for errors
2. Verify user has correct role in Keycloak
3. Clear browser cookies/session
4. Check medication-web logs: `docker compose logs medication-web`

### Issue: 403 Forbidden when accessing dashboard
**Solution**:
1. Verify @EnableMethodSecurity is present
2. Check role extraction in oidcUserService()
3. Ensure user has realm role (not client role)
4. Verify authorities contain both "ROLE_X" and "X" formats

### Issue: Attributes not extracted (patient-id, etc.)
**Solution**:
1. Check Keycloak user attributes are set
2. Verify attribute type is List<String>
3. Check logs for attribute extraction errors
4. Ensure attributes are included in ID token

