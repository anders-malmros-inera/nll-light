# Test Suite Documentation

## Overview
Comprehensive test suite for the NLL-Light medication prescription system, covering API endpoints, service layer business logic, and data access layer.

## Test Structure

### 1. Integration Tests

#### PatientControllerIntegrationTest
Tests the patient-facing prescription API endpoints using MockMvc and in-memory H2 database.

**Test Cases (9 tests):**
- ✅ `GET /api/patients/{userId}/prescriptions` - List all prescriptions
- ✅ `GET /api/patients/{userId}/prescriptions` - Empty list for new patient
- ✅ `GET /api/patients/{userId}/prescriptions` - 404 for non-existent patient
- ✅ `GET /api/patients/{userId}/prescriptions/{id}` - Get prescription details
- ✅ `GET /api/patients/{userId}/prescriptions/{id}` - 404 for non-existent prescription
- ✅ `GET /api/patients/{userId}/prescriptions/{id}` - 403 when accessing another patient's prescription
- ✅ `GET /api/medications` - List all medications
- ✅ `GET /api/medications/{id}` - Get medication details
- ✅ `GET /api/medications/{id}` - 404 for non-existent medication

#### PrescriberControllerIntegrationTest
Tests the prescriber-facing prescription management API endpoints.

**Test Cases (14 tests):**
- ✅ `POST /api/prescriptions` - Create prescription successfully
- ✅ `POST /api/prescriptions` - Validation error for missing fields
- ✅ `POST /api/prescriptions` - Validation error for invalid quantity
- ✅ `POST /api/prescriptions` - 404 when patient not found
- ✅ `PUT /api/prescriptions/{id}` - Update prescription successfully
- ✅ `PUT /api/prescriptions/{id}` - Fail when updating cancelled prescription
- ✅ `PUT /api/prescriptions/{id}` - 403 for unauthorized prescriber
- ✅ `DELETE /api/prescriptions/{id}` - Cancel prescription successfully
- ✅ `DELETE /api/prescriptions/{id}` - Fail when already cancelled
- ✅ `GET /api/prescriptions` - List prescriber's prescriptions
- ✅ `GET /api/prescriptions` - Empty list for new prescriber
- ✅ `GET /api/prescriptions/{id}` - Get prescription details
- ✅ `GET /api/prescriptions/{id}` - 403 for unauthorized access

### 2. Unit Tests

#### PrescriptionServiceTest
Tests business logic and authorization using Mockito for mocking dependencies.

**Test Cases (15 tests):**
- ✅ Create prescription - Success
- ✅ Create prescription - Patient not found
- ✅ Create prescription - Medication not found
- ✅ Create prescription - Prescriber not found
- ✅ Update prescription - Success
- ✅ Update prescription - Not found
- ✅ Update prescription - Unauthorized
- ✅ Update prescription - Cancelled prescription
- ✅ Cancel prescription - Success
- ✅ Cancel prescription - Already cancelled
- ✅ Get prescriber prescriptions - Success
- ✅ Get prescriber prescriptions - Not found
- ✅ Get prescription by ID - Success
- ✅ Get prescription by ID - Unauthorized
- ✅ Get patient prescriptions - Success
- ✅ Get patient prescription by ID - Success
- ✅ Get patient prescription by ID - Unauthorized

### 3. Repository Tests

#### RepositoryTest
Tests custom JPA queries and data access using @DataJpaTest.

**Test Cases (12 tests):**
- ✅ PatientRepository.findByUserId - Success
- ✅ PatientRepository.findByUserId - Not found
- ✅ PrescriberRepository.findByUserId - Success
- ✅ PrescriberRepository.findByUserId - Not found
- ✅ PrescriptionRepository.findByPatient - Success
- ✅ PrescriptionRepository.findByPatient - Empty list
- ✅ PrescriptionRepository.findByPrescriber - Success
- ✅ PrescriptionRepository.findByPrescriber - Empty list
- ✅ PrescriptionRepository.findByStatus - Success
- ✅ PrescriptionRepository save and retrieve with relationships
- ✅ MedicationRepository.findByAtcCode
- ✅ MedicationRepository.findByActiveIngredient

## Test Configuration

### application-test.properties
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
logging.level.se.inera.nll=DEBUG
```

### TestSecurityConfig
Disables authentication for integration tests to simplify testing without Keycloak.

## Running Tests

### Using Docker (Recommended)
```powershell
docker run --rm -v ${PWD}:/src -w /src maven:3.9.9-eclipse-temurin-21 mvn -pl medication-api test
```

### Using Maven Directly
```bash
mvn -pl medication-api test
```

### Run Specific Test Class
```bash
mvn -pl medication-api test -Dtest=PatientControllerIntegrationTest
```

### Run with Coverage
```bash
mvn -pl medication-api test jacoco:report
```

## Test Coverage Summary

**Total Test Cases: 50+**
- Integration Tests: 23 tests
- Unit Tests: 17 tests
- Repository Tests: 12 tests

**Coverage Areas:**
- ✅ Patient API (all endpoints)
- ✅ Prescriber API (all endpoints)
- ✅ Medication API (all endpoints)
- ✅ Business logic validation
- ✅ Authorization checks
- ✅ Error handling
- ✅ Database queries
- ✅ Entity relationships

## Test Best Practices

1. **@Transactional**: All integration tests use `@Transactional` to rollback database changes after each test
2. **@BeforeEach**: Setup method creates fresh test data for each test
3. **Descriptive Names**: Test methods use descriptive names with @DisplayName
4. **Arrange-Act-Assert**: All tests follow AAA pattern
5. **Isolation**: Each test is independent and can run in any order
6. **Mock vs Real**: Unit tests use mocks, integration tests use real database

## Continuous Integration

These tests are designed to run in CI/CD pipelines:
- Fast execution (in-memory database)
- No external dependencies
- Deterministic results
- Clear failure messages

## Future Enhancements

- [ ] Add Pharmacist API tests when implemented
- [ ] Add security integration tests with Spring Security Test
- [ ] Add performance tests for large datasets
- [ ] Add API contract tests with Spring Cloud Contract
- [ ] Add mutation testing with PIT
