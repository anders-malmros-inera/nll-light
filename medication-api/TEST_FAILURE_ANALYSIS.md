# Test Failure Analysis

## Current Situation

According to the user: **"all tests fail"**

## Previous Test Run Results

From the last successful test execution, we had:

### ✅ Passing Tests
- **PrescriptionServiceTest**: 18/18 tests PASSED

### ❌ Failing Tests  
- **RepositoryTest**: 7 tests - ALL ERRORS
- **PatientControllerIntegrationTest**: 9 tests - ALL ERRORS
- **PrescriberControllerIntegrationTest**: 13 tests - ALL ERRORS
- **PrescriptionControllerIntegrationTest**: 7 tests - ALL ERRORS
- **MedicationControllerTest**: 3 tests - ALL ERRORS

**Total**: 39 out of 57 tests failing

## Root Cause: Flyway Circular Dependency

### Error Message
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory'
Caused by: Circular depends-on relationship between 'flyway' and 'entityManagerFactory'
```

### Why This Happens

1. **Main Application (`application.properties`)**:
   ```properties
   spring.flyway.enabled=true
   spring.flyway.baseline-on-migrate=true
   spring.jpa.hibernate.ddl-auto=none
   ```
   - Uses Flyway for database migrations
   - JPA doesn't create schema (ddl-auto=none)

2. **Test Context**:
   - `@DataJpaTest` expects to auto-configure database
   - `@SpringBootTest` loads full application context
   - Both try to use Flyway + JPA simultaneously
   - Creates circular dependency

3. **Spring Boot Auto-Configuration**:
   - Tries to initialize JPA's EntityManagerFactory
   - EntityManagerFactory depends on DataSource
   - Flyway also depends on DataSource
   - If both are enabled, they create circular dependency

## Fixes Applied

### Fix 1: application-test.properties
```properties
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop  
spring.sql.init.mode=embedded
```

### Fix 2: application.properties (in test/resources)
```properties
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=embedded
```

## Why Multiple Property Files?

The project has:
1. `src/test/resources/application.properties` - Base test configuration
2. `src/test/resources/application-test.properties` - Profile-specific config

Tests can load either or both depending on `@ActiveProfiles` annotation.

## Expected Outcome After Fix

With Flyway disabled in tests:
- JPA will create schema from entity classes (`ddl-auto=create-drop`)
- H2 in-memory database will be used
- No circular dependency
- All 57 tests should run

## Potential Remaining Issues

Even after fixing Flyway, tests might still fail due to:

### 1. **Data Setup Issues**
- `data.sql` might not load correctly
- Test data might not match entity relationships
- Foreign key constraints might fail

### 2. **Security Configuration**
- Integration tests need security disabled or mocked
- Current config: `spring.autoconfigure.exclude=...SecurityAutoConfiguration`
- May need `@WithMockUser` or `@AutoConfigureMockMvc`

### 3. **Entity Relationship Issues**
- Cascade settings
- Lazy loading in tests
- Transaction boundaries

### 4. **Mock Configuration**
- Service mocks might not match actual signatures
- Repository mocks might need different behavior

### 5. **Assertion Errors**
- Expected vs actual values
- Date/time precision
- Null handling

## Diagnostic Commands

To verify fix worked:
```powershell
# Run all tests
docker run --rm -v ${PWD}:/src -w /src maven:3.9.9-eclipse-temurin-21 mvn -pl medication-api clean test

# Check results
Get-Content "medication-api\target\surefire-reports\*.txt" | Select-String "Tests run:"

# Get detailed errors from one test class
Get-Content "medication-api\target\surefire-reports\se.inera.nll.nlllight.api.repository.RepositoryTest.txt"
```

## Next Steps

1. ✅ Disable Flyway in both test property files
2. ⏳ Run clean test
3. 🔴 Analyze any remaining failures
4. 🔴 Fix test-specific issues (data, security, assertions)
5. 🔴 Verify all 57 tests pass

## Files Modified

1. `src/test/resources/application-test.properties` - Added `spring.flyway.enabled=false`
2. `src/test/resources/application.properties` - Added `spring.flyway.enabled=false`, changed `sql.init.mode` to `embedded`
3. `src/test/java/.../PatientControllerIntegrationTest.java` - Fixed `setValidUntil → setEndDate` (from earlier fix)

## Configuration Strategy

### For @DataJpaTest (Repository Tests)
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE) // Use configured H2
class RepositoryTest {
  // JPA auto-creates schema from entities
  // No Flyway needed
}
```

### For @SpringBootTest (Integration Tests)
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")  // Uses application-test.properties
class ControllerIntegrationTest {
  // Full Spring context
  // Flyway disabled in test profile
  // JPA creates schema
}
```

### For @WebMvcTest (Controller Unit Tests)
```java
@WebMvcTest(MedicationController.class)
class MedicationControllerTest {
  // Only web layer
  // Services are mocked
  // No database needed
}
```

## Summary

**Root Cause**: Flyway + JPA circular dependency in test context  
**Fix**: Disable Flyway for tests, use JPA DDL auto-creation  
**Status**: Fixes applied, awaiting test run verification  
**Expected**: All tests should now run (may have other failures to address)
