# Medication API Test Documentation

## Overview
This document describes the test suite for the Medication API, including test cases, sequence diagrams, and validation criteria.

## Test Classes

### MedicationControllerTest
Integration tests for the Medication REST API endpoints.

#### Test Environment
- **Framework**: Spring Boot Test with `@SpringBootTest`
- **Web Environment**: Random port with TestRestTemplate
- **Database**: H2 in-memory with test-specific configuration
- **Data**: Pre-seeded via `data.sql` in test resources

## Test Cases

### 1. `list_shouldReturnSeededMedications()`
**Purpose**: Verify that the API returns all seeded medications.

**HTTP Request**:
```
GET /api/medications
```

**Expected Response**:
- Status: 2xx Success
- Body: Non-null List<Medication>
- Size: ≥ 3 medications
- Contains: "Alimemazin", "Elvanse", "Melatonin"

**Sequence Diagram**: [test-list-medications-sequence.puml](test-list-medications-sequence.puml)

---

### 2. `search_shouldFindMelatoninByPartial()`
**Purpose**: Verify that partial name search works correctly.

**HTTP Request**:
```
GET /api/medications/search?name=mel
```

**Expected Response**:
- Status: 2xx Success
- Body: Non-null List<Medication>
- Contains: Medication with name "Melatonin"

**Sequence Diagram**: [test-search-medications-sequence.puml](test-search-medications-sequence.puml)

---

### 3. `get_shouldReturnSingleMedication()`
**Purpose**: Verify that retrieving a single medication by ID works.

**HTTP Request**:
```
GET /api/medications/1
```

**Expected Response**:
- Body: Non-null Medication object
- Name: "Alimemazin"

**Sequence Diagram**: [test-get-medication-sequence.puml](test-get-medication-sequence.puml)

## Test Data
The tests use the following seeded data from `src/test/resources/data.sql`:

```sql
INSERT INTO medication (id, name, description) VALUES
  (1, 'Alimemazin', 'Antihistamin. Exempelindikation: allergiska besvär.'),
  (2, 'Elvanse', 'CNS-stimulerande läkemedel. Exempelindikation: ADHD.'),
  (3, 'Melatonin', 'Hormonpreparat. Exempelindikation: sömnstörningar.');
```

## Running Tests

### Single Module
```bash
mvn -pl medication-api test
```

### All Modules
```bash
mvn test
```

### With Coverage
```bash
mvn -pl medication-api test jacoco:report
```

## Test Configuration
- **Base URL**: `http://localhost:{random_port}`
- **Content Type**: JSON
- **Assertions**: AssertJ assertions
- **HTTP Client**: Spring TestRestTemplate

## Coverage Requirements
- **Line Coverage**: > 80%
- **Branch Coverage**: > 70%
- **Class Coverage**: > 90%

## Maintenance Notes
- Update sequence diagrams when API endpoints change
- Update test data when medication schema changes
- Ensure test isolation (each test should be independent)
- Add new tests for new endpoints following the same pattern