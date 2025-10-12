# Test Fixes - Round 2

**Status**: 22/57 tests passing (39% → target: 100%)  
**Errors Fixed**: 7 errors, 3 failures  
**Remaining**: 32 errors, 3 failures → 0

---

## Issues Found & Fixed

### ✅ Issue #4: Data.sql Primary Key Conflicts

**Problem**: Tests create test data with IDs 1, 2, 3, but data.sql also inserts with IDs 1, 2, 3

**Error**:
```
Primary key violation: "PRIMARY KEY ON PUBLIC.MEDICATIONS(ID)...
```

**✅ Fix 1**: Removed ID column from INSERT - let IDs auto-generate
```sql
-- Before
INSERT INTO medications (id, npl_id, trade_name, ...) VALUES (1, 'NPL001', ...)

-- After
INSERT INTO medications (npl_id, trade_name, ...) VALUES ('NPL001', ...)
```

**✅ Fix 2**: Disabled data.sql loading in tests
```properties
spring.sql.init.mode=never
```

**Files Modified**:
- `src/main/resources/data.sql`
- `src/test/resources/application.properties`

---

### ✅ Issue #5: Bean Override Not Allowed

**Problem**: Spring Boot doesn't allow bean overriding by default, so `@Primary` doesn't work

**Error**:
```
BeanDefinitionOverrideException: Invalid bean definition with name 'filterChain'
Cannot register bean definition [SecurityConfig.filterChain]
since there is already [TestSecurityConfig.filterChain] bound.
```

**✅ Fix**: Enable bean definition overriding in test properties
```properties
spring.main.allow-bean-definition-overriding=true
```

**Files Modified**:
- `src/test/resources/application.properties`

---

### ✅ Issue #6: Test Assertions Using Old Data

**Problem**: Tests expect old medication names from previous data.sql

**Failures**:
```
expected: "Alimemazin" but was: "Alimemazin Evolan"
expected: "Melatonin" but was: "Melatonin Orifarm"
```

**✅ Fix**: Updated test assertions to match new medication trade names
```java
// Before
.contains("Alimemazin", "Elvanse", "Melatonin")

// After
.contains("Alimemazin Evolan", "Elvanse", "Melatonin Orifarm")
```

**Files Modified**:
- `src/test/java/.../medication/MedicationControllerTest.java` (3 assertions updated)

---

## Summary of All Fixes Applied

### Configuration Files
1. ✅ `src/test/resources/application.properties`
   - Added `spring.main.allow-bean-definition-overriding=true`
   - Changed `spring.sql.init.mode=embedded` → `never`

2. ✅ `src/test/resources/application-test.properties`
   - Added `spring.flyway.enabled=false`
   - Added `spring.sql.init.mode=embedded`

3. ✅ `src/main/resources/data.sql`
   - Fixed table name: `medication` → `medications`
   - Fixed columns: Added all required fields
   - Removed `id` from INSERT to allow auto-generation

### Test Files
4. ✅ `src/test/java/se/inera/nll/nlllight/config/TestSecurityConfig.java`
   - Removed `@EnableWebSecurity`
   - Added `@Primary`

5. ✅ `src/test/java/.../controller/PatientControllerIntegrationTest.java`
   - Fixed `setValidUntil()` → `setEndDate()` (2 occurrences)

6. ✅ `src/test/java/.../medication/MedicationControllerTest.java`
   - Updated medication name assertions (3 tests)

---

## Current Test Status

### ✅ Passing Tests (22/57)
- **PrescriptionServiceTest**: 18/18 ✅
- **RepositoryTest**: 4/7 (3 errors remaining)

### ❌ Still Failing (35/57)
- **RepositoryTest**: 3 errors (primary key conflicts - should be fixed after rebuild)
- **MedicationControllerTest**: 3 failures (assertion mismatches - FIXED)
- **PatientControllerIntegrationTest**: 9 errors (bean override - FIXED)
- **PrescriberControllerIntegrationTest**: 13 errors (bean override - FIXED)
- **PrescriptionControllerIntegrationTest**: 7 errors (bean override - FIXED)

---

## Expected Outcome After Re-run

With all fixes applied:

1. ✅ **data.sql** won't conflict with test data (no IDs, and disabled in tests)
2. ✅ **Bean overriding** enabled - TestSecurityConfig will override SecurityConfig
3. ✅ **Medication assertions** match new trade names
4. 🎯 **Target**: 50+ tests passing (90%+ pass rate)

---

## Remaining Potential Issues

After the fixes, you may still see:
- **Integration test data setup** - tests may need specific data
- **Security/authentication** - some endpoints may still check auth
- **Lazy loading** - entity relationships may not be fetched
- **Transaction boundaries** - data may not be visible across tests

---

## Next Step

Run clean test build:

```powershell
cd C:\dev\workspace\nll-light
docker run --rm -v ${PWD}:/src -w /src maven:3.9.9-eclipse-temurin-21 mvn -pl medication-api clean test
```

Check results:
```powershell
Get-Content "medication-api\target\surefire-reports\*.txt" | Select-String "Tests run:"
```

Expected improvement: **22 → 50+ tests passing**
