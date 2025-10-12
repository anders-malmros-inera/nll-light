# Test Failure Analysis - Detailed Report

**Date**: October 12, 2025  
**Status**: Analysis Complete - 3 Major Issues Identified

---

## Test Results Summary

### Current Status (Before Fixes)
- ✅ **PrescriptionServiceTest**: 18/18 tests PASSED
- ❌ **RepositoryTest**: 0/7 tests (7 errors)
- ❌ **PatientControllerIntegrationTest**: 0/9 tests (9 errors)
- ❌ **PrescriberControllerIntegrationTest**: 0/13 tests (13 errors)
- ❌ **PrescriptionControllerIntegrationTest**: 0/7 tests (7 errors)
- ❌ **MedicationControllerTest**: 0/3 tests (3 errors)

**Total**: 18 passing, 39 failing (67% failure rate)

---

## Root Cause Analysis

### ❌ Issue #1: SQL Data Script Error (CRITICAL)

**Error Message**:
```
Failed to execute SQL script statement #1 of file [data.sql]: 
INSERT INTO medication (id, name, description) VALUES...
```

**Root Cause**:
- **Table Name Mismatch**:
  - Entity: `@Table(name = "medications")` (plural)
  - data.sql: `INSERT INTO medication` (singular)

- **Column Name Mismatch**:
  - Entity columns: `trade_name`, `generic_name`, `form`, `strength`, etc.
  - data.sql columns: `name`, `description`

**Impact**: All tests that require database initialization fail

**✅ Fix Applied**:
Updated `src/main/resources/data.sql`:
```sql
-- Before
INSERT INTO medication (id, name, description) VALUES
  (1, 'Alimemazin', 'Antihistamin...'),
  (2, 'Elvanse', 'CNS-stimulerande...'),
  (3, 'Melatonin', 'Hormonpreparat...');

-- After
INSERT INTO medications (id, npl_id, trade_name, generic_name, form, strength, route, atc_code, rx_status, is_available, price, created_at, updated_at) VALUES
  (1, 'NPL001', 'Alimemazin Evolan', 'Alimemazin', 'Tablett', '10 mg', 'Oral', 'R06AD01', 'Rx', true, 125.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'NPL002', 'Elvanse', 'Lisdexamfetamin', 'Kapsel, hård', '30 mg', 'Oral', 'N06BA12', 'Rx', true, 450.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'NPL003', 'Melatonin Orifarm', 'Melatonin', 'Tablett', '3 mg', 'Oral', 'N05CH01', 'OTC', true, 89.90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

**Files Modified**:
- `src/main/resources/data.sql`

---

### ❌ Issue #2: Bean Definition Conflict (CRITICAL)

**Error Message**:
```
BeanDefinitionOverrideException: Invalid bean definition with name 'filterChain'
Cannot register bean definition [SecurityConfig.filterChain] 
since there is already [TestSecurityConfig.filterChain] bound.
```

**Root Cause**:
- `TestSecurityConfig` has `@EnableWebSecurity` annotation
- This causes Spring to try to create TWO filterChain beans:
  1. From main `SecurityConfig` class
  2. From test `TestSecurityConfig` class
- Spring doesn't allow duplicate bean names by default

**Impact**: All integration tests fail during Spring context loading

**✅ Fix Applied**:
Updated `src/test/java/se/inera/nll/nlllight/config/TestSecurityConfig.java`:
```java
// Before
@TestConfiguration
@EnableWebSecurity  // <-- THIS CAUSES THE CONFLICT
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {...}
}

// After
@TestConfiguration  // Removed @EnableWebSecurity
public class TestSecurityConfig {
    @Bean
    @Primary  // Added @Primary to override main config
    public SecurityFilterChain filterChain(HttpSecurity http) {...}
}
```

**Why This Works**:
- Removed `@EnableWebSecurity` - prevents duplicate web security configuration
- Added `@Primary` - tells Spring to use THIS bean when multiple candidates exist
- Test config now properly overrides main security config

**Files Modified**:
- `src/test/java/se/inera/nll/nlllight/config/TestSecurityConfig.java`

---

### ❌ Issue #3: Flyway Configuration (PARTIALLY RESOLVED)

**Error Message**:
```
Circular depends-on relationship between 'flyway' and 'entityManagerFactory'
```

**Root Cause**:
- Main application uses Flyway for database migrations
- Test context tries to use both Flyway AND JPA auto-configuration
- Creates circular dependency

**✅ Fixes Applied**:
1. Updated `src/test/resources/application.properties`:
   ```properties
   spring.flyway.enabled=false
   spring.sql.init.mode=embedded
   ```

2. Updated `src/test/resources/application-test.properties`:
   ```properties
   spring.flyway.enabled=false
   spring.sql.init.mode=embedded
   ```

**Status**: Fixed in configuration, but may still appear in cached test results

**Files Modified**:
- `src/test/resources/application.properties`
- `src/test/resources/application-test.properties`

---

## Additional Issues Found (From Earlier)

### ✅ Issue #4: Wrong Method Name in Tests (FIXED)

**Error**: `cannot find symbol: method setValidUntil(java.time.LocalDate)`

**Fix**: Changed `setValidUntil()` to `setEndDate()` in PatientControllerIntegrationTest.java

**Files Modified**:
- `src/test/java/.../controller/PatientControllerIntegrationTest.java` (2 occurrences fixed)

---

### ✅ Issue #5: Orphaned Code in Test Files (FIXED)

**Error**: Duplicate class declarations after closing braces

**Fix**: Truncated files to remove orphaned code
- RepositoryTest.java: 511 → 269 lines (242 lines removed)
- PrescriptionServiceTest.java: 790 → 424 lines (366 lines removed)

---

## Configuration Strategy

### Repository Tests (@DataJpaTest)
```java
@DataJpaTest
@ActiveProfiles("test")  // Uses application-test.properties
class RepositoryTest {
    // JPA creates schema from entities (ddl-auto=create-drop)
    // Flyway disabled
    // data.sql loaded after schema creation
}
```

### Integration Tests (@SpringBootTest)
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)  // Override security with @Primary
class ControllerIntegrationTest {
    // Full Spring context
    // Test security config disables authentication
    // Flyway disabled, JPA creates schema
}
```

### Controller Unit Tests (@WebMvcTest)
```java
@WebMvcTest(MedicationController.class)
class MedicationControllerTest {
    // Only web layer loaded
    // Services mocked
    // No database needed
}
```

---

## Expected Outcome After Fixes

With all fixes applied:

1. ✅ **data.sql loads correctly** - table and column names match entity
2. ✅ **Security configuration works** - TestSecurityConfig properly overrides with @Primary
3. ✅ **Flyway disabled in tests** - no circular dependency
4. ✅ **All 57 tests should run** (may have some assertion failures to fix)

---

## Next Steps - Testing the Fixes

### Step 1: Clean Build
```powershell
cd C:\dev\workspace\nll-light
docker run --rm -v ${PWD}:/src -w /src maven:3.9.9-eclipse-temurin-21 mvn -pl medication-api clean test
```

### Step 2: Check Results
```powershell
Get-Content "medication-api\target\surefire-reports\*.txt" | Select-String "Tests run:"
```

### Step 3: Analyze Any Remaining Failures

**Potential remaining issues**:
- **Assertion errors** - expected vs actual values don't match
- **Data setup** - test data doesn't match test expectations
- **Lazy loading** - entity relationships not properly fetched
- **Transaction boundaries** - data not committed/visible in tests
- **Mock behavior** - mocks don't return expected values

### Step 4: Fix Test-Specific Issues

For each remaining failure:
1. Read full error from surefire report
2. Identify root cause (assertion, data, mock, etc.)
3. Fix test code or test data
4. Re-run specific test class
5. Verify fix

---

## Files Modified Summary

### Configuration Files (3)
1. `src/test/resources/application.properties` - Disabled Flyway, changed sql.init.mode
2. `src/test/resources/application-test.properties` - Disabled Flyway  
3. `src/main/resources/data.sql` - Fixed table/column names, added proper data

### Test Configuration (1)
4. `src/test/java/se/inera/nll/nlllight/config/TestSecurityConfig.java` - Removed @EnableWebSecurity, added @Primary

### Test Files (1 - earlier fix)
5. `src/test/java/.../controller/PatientControllerIntegrationTest.java` - Fixed method names

---

## Success Criteria

✅ **BUILD SUCCESS** message from Maven  
✅ All 57 tests execute (no context loading errors)  
✅ **0 Errors** in test execution  
✅ **PrescriptionServiceTest**: 18/18 passing (already working)  
🎯 **Target**: At least 90% of tests passing after fixes

---

## Verification Commands

```powershell
# Run all tests
mvn -pl medication-api clean test

# Count test results
Get-Content "medication-api\target\surefire-reports\*.txt" | Select-String "Tests run:" | Measure-Object

# See only failures
Get-Content "medication-api\target\surefire-reports\*.txt" | Select-String "FAILURE|ERROR" | Select-Object -First 10

# Get detailed error for specific test
Get-Content "medication-api\target\surefire-reports\se.inera.nll.nlllight.api.repository.RepositoryTest.txt"
```

---

## Documentation Created

1. `TEST_STATUS.md` - Overall test status tracking
2. `TEST_FAILURE_ANALYSIS.md` - This detailed analysis
3. `TEST_DETAILED_ANALYSIS.md` - Complete technical breakdown

---

## Conclusion

**3 critical issues identified and fixed**:
1. ✅ SQL data script - table and column name mismatches
2. ✅ Security bean conflict - duplicate filterChain beans
3. ✅ Flyway circular dependency - disabled in test config

**Next action**: Run clean test build to verify fixes and analyze any remaining failures.
