# Testplan för NLL-Light Medication API

## Testsvitens omfattning

### 1. Repository Tests (DataJpaTest)
**Fil**: `RepositoryTest.java`
**Fokus**: Testa custom JPA queries och dataåtkomst

#### PrescriptionRepository Tests
- ✅ `shouldFindPrescriptionsByPatientId()` - findByPatientId()
- ✅ `shouldFindPrescriptionsByPatientIdAndStatus()` - findByPatientIdAndStatus()
- ✅ `shouldFindActivePrescriptionsByPatient()` - findActivePrescriptionsByPatient()
- ✅ `shouldFindByPrescriptionNumber()` - findByPrescriptionNumber()
- ✅ `shouldFindRefillEligiblePrescriptions()` - findRefillEligiblePrescriptions()
- ✅ `shouldFindByPrescriberId()` - findByPrescriberId()
- ✅ `shouldReturnEmptyWhenNoMatch()` - edge case

#### MedicationRepository Tests (optional)
- findById(), findByNplId() etc.

### 2. Service Tests (Mockito Unit Tests)
**Fil**: `PrescriptionServiceTest.java`
**Fokus**: Business logic, validering, fel-hantering

#### createPrescription Tests
- ✅ `shouldCreatePrescriptionSuccessfully()`
- ✅ `shouldThrowExceptionWhenPatientNotFound()`
- ✅ `shouldThrowExceptionWhenMedicationNotFound()`
- ✅ `shouldThrowExceptionWhenPrescriberNotFound()`
- ✅ `shouldSetDefaultValuesCorrectly()`
- ✅ `shouldGenerateUniquePrescriptionNumber()`

#### updatePrescription Tests
- ✅ `shouldUpdatePrescriptionSuccessfully()`
- ✅ `shouldThrowExceptionWhenPrescriptionNotFound()`
- ✅ `shouldThrowExceptionWhenUnauthorizedPrescriber()`
- ✅ `shouldOnlyUpdateProvidedFields()` - null-fält ska ignoreras

#### cancelPrescription Tests
- ✅ `shouldCancelPrescriptionSuccessfully()`
- ✅ `shouldThrowExceptionWhenAlreadyCancelled()`
- ✅ `shouldRecordCancellationDetails()`

#### Query Tests
- ✅ `shouldGetPatientPrescriptionsWithStatus()`
- ✅ `shouldGetPatientPrescriptionsWithoutStatus()` - uses findActivePrescriptionsByPatient
- ✅ `shouldGetRefillEligiblePrescriptions()`
- ✅ `shouldConvertToDTOCorrectly()` - private toDTO method

### 3. Controller Integration Tests (SpringBootTest + MockMvc)
**Fil 1**: `PrescriptionControllerIntegrationTest.java` (patient-facing)
**Fil 2**: `PrescriberControllerIntegrationTest.java`

#### PrescriptionController Tests (Patient API)
**GET /api/v1/prescriptions**
- ✅ `shouldReturnAllActivePrescriptionsForPatient()`
- ✅ `shouldFilterPrescriptionsByStatus()`
- ✅ `shouldReturnEmptyListWhenNoPrescrip tions()`

**GET /api/v1/prescriptions/{id}**
- ✅ `shouldReturnPrescriptionDetails()`
- ✅ `shouldReturn404WhenPrescriptionNotFound()`

**GET /api/v1/prescriptions/refill-eligible**
- ✅ `shouldReturnRefillEligiblePrescriptions()`
- ✅ `shouldReturnEmptyWhenNoRefillEligible()`

#### PrescriberController Tests
**POST /api/v1/prescriber/prescriptions**
- ✅ `shouldCreatePrescriptionSuccessfully()`
- ✅ `shouldReturn400WhenInvalidRequest()` - validation
- ✅ `shouldReturn400WhenMedicationNotFound()`
- ✅ `shouldReturn400WhenPatientNotFound()`

**PUT /api/v1/prescriber/prescriptions/{id}**
- ✅ `shouldUpdatePrescriptionSuccessfully()`
- ✅ `shouldReturn400WhenValidationFails()`
- ✅ `shouldReturn404WhenPrescriptionNotFound()`
- ✅ `shouldReturn403WhenUnauthorizedPrescriber()`

**DELETE /api/v1/prescriber/prescriptions/{id}**
- ✅ `shouldCancelPrescriptionSuccessfully()`
- ✅ `shouldReturn404WhenPrescriptionNotFound()`

**GET /api/v1/prescriber/prescriptions**
- ✅ `shouldReturnAllPrescriberPrescriptions()`
- ✅ `shouldFilterByPatientId()`

## Total antal tester: ~40 tests

## Test Data Setup Strategy

### Entity Creation Helpers
```java
private Patient createTestPatient(String userId) {
    Patient patient = new Patient();
    patient.setId("patient-" + UUID.randomUUID());
    patient.setUserId(userId);
    patient.setEncryptedSsn("encrypted-ssn-" + userId);
    patient.setFirstName("Test");
    patient.setLastName("Patient");
    patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
    return patientRepository.save(patient);
}

private Medication createTestMedication(String tradeName) {
    Medication medication = new Medication();
    medication.setNplId("NPL-" + UUID.randomUUID());
    medication.setTradeName(tradeName);
    medication.setGenericName("Generic " + tradeName);
    medication.setForm("tablet");
    medication.setStrength("100mg");
    medication.setAtcCode("N02BE01");
    medication.setIsAvailable(true);
    return medicationRepository.save(medication);
}

private Prescriber createTestPrescriber(String userId) {
    Prescriber prescriber = new Prescriber();
    prescriber.setUserId(userId);
    prescriber.setFirstName("Dr");
    prescriber.setLastName("Smith");
    prescriber.setSpecialty("General Medicine");
    prescriber.setLicenseNumber("LIC-" + userId);
    return prescriberRepository.save(prescriber);
}

private CreatePrescriptionRequest createValidPrescriptionRequest(
        String patientId, Long medicationId) {
    CreatePrescriptionRequest request = new CreatePrescriptionRequest();
    request.setPatientId(patientId);
    request.setMedicationId(medicationId);
    request.setDose(new BigDecimal("100.00"));
    request.setDoseUnit("mg");
    request.setFrequency("BID");
    request.setFrequencyDescription("Twice daily");
    request.setRoute("PO");
    request.setStartDate(LocalDate.now());
    request.setQuantityPrescribed(60);
    request.setQuantityUnit("tablets");
    request.setDaysSupply(30);
    request.setRefillsAllowed(3);
    request.setIndication("Pain management");
    request.setInstructions("Take with food");
    return request;
}
```

## Test Configuration

### application-test.properties
```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
logging.level.se.inera.nll=DEBUG
```

### TestSecurityConfig
```java
@TestConfiguration
public class TestSecurityConfig {
    @Bean
    @Primary
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .build();
    }
}
```

## Coverage Summary
- **Repository Layer**: 7 tests - Täcker alla custom queries
- **Service Layer**: ~20 tests - Täcker all business logic, validering, error handling
- **Controller Layer**: ~13 tests - Täcker alla endpoints, HTTP status codes, validation

## Kör tester
```bash
# Via Docker
docker run --rm -v ${PWD}:/src -w /src maven:3.9.9-eclipse-temurin-21 mvn -pl medication-api test

# Lokal Maven
mvn -pl medication-api test

# PowerShell script
./run-tests.ps1
```
