# Faktisk API-struktur för NLL-Light Medication System

## Datamodell (Entities)

### Prescription Entity
- **ID**: Long (auto-generated)
- **Relationer**:
  - patient: Patient (ManyToOne, LAZY)
  - medication: Medication (ManyToOne, LAZY)
  - prescriber: Prescriber (ManyToOne, LAZY)
- **Fält**:
  - prescriptionNumber: String (unique)
  - status: PrescriptionStatus enum (ACTIVE, COMPLETED, CANCELLED, EXPIRED, SUSPENDED, PENDING)
  - **Dosering**: dose (BigDecimal), doseUnit, frequency, frequencyDescription, route, maxDailyDose, maxDailyDoseUnit
  - **Klinisk info**: indication, instructions, clinicalNotes
  - **Tidsspann**: prescribedDate, startDate, endDate
  - **Refills**: refillsAllowed, refillsRemaining, lastRefillDate, nextRefillEligibleDate
  - **Kvantitet**: quantityPrescribed, quantityUnit, daysSupply
  - **Flaggor**: isPRN, isSubstitutionAllowed, requiresPriorAuthorization, isControlledSubstance
  - **Audit**: createdAt, updatedAt, createdBy, cancelledAt, cancelledBy, cancellationReason

### Patient Entity
- **ID**: String (64 char)
- **Fält**:
  - userId: String (unique)
  - encryptedSsn: String (NOT personalNumber!)
  - firstName, lastName
  - dateOfBirth: LocalDate
  - gender, email, phone
  - address (addressLine1, addressLine2, postalCode, city, country)
  - emergencyContact (name, phone, relationship)
  - medicalProfile (allergies, chronicConditions, weightKg, heightCm, bloodType)
  - preferences (preferredLanguage, preferredPharmacyId)
  - consent (consentDataSharing, consentMarketing)

### Medication Entity
- **ID**: Long (auto-generated)
- **Fält**:
  - nplId: String (unique)
  - tradeName: String
  - genericName: String (NOT activeIngredient!)
  - substanceId: Long
  - form, strength, route
  - atcCode, rxStatus
  - isAvailable: Boolean
  - price: BigDecimal
- **Transient metoder**:
  - getName(): returnerar tradeName eller genericName
  - getDescription(): returnerar form + strength

### Prescriber Entity
- **Fält**: userId, firstName, lastName, specialty, licenseNumber, etc.

## PrescriptionStatus Enum
```java
ACTIVE, COMPLETED, CANCELLED, EXPIRED, SUSPENDED, PENDING
```
**OBS**: DISPENSED finns INTE!

## PrescriptionRepository
Faktiska query-metoder:
- `findByPatientIdAndStatus(String patientId, PrescriptionStatus status): List<Prescription>`
- `findByPatientId(String patientId): List<Prescription>`
- `findByPrescriptionNumber(String prescriptionNumber): Optional<Prescription>`
- `findRefillEligiblePrescriptions(String patientId, LocalDate date): List<Prescription>`
- `findActivePrescriptionsByPatient(String patientId): List<Prescription>`
- `findByPrescriberId(String prescriberUserId): List<Prescription>`
- `findByPrescriberIdAndPatientId(String prescriberUserId, String patientId): List<Prescription>`

**Metoder som INTE finns**:
- ~~findByPatient(Patient patient)~~
- ~~findByPrescriber(Prescriber prescriber)~~
- ~~findByStatus(PrescriptionStatus status)~~

## PrescriptionService
Faktiska metoder:

### Läsoperationer
```java
List<PrescriptionDTO> getPatientPrescriptions(String patientId, String status)
PrescriptionDTO getPrescriptionById(Long id)
List<PrescriptionDTO> getRefillEligiblePrescriptions(String patientId)
```

### Skrivoperationer
```java
PrescriptionDTO createPrescription(CreatePrescriptionRequest request, String prescriberUserId)
PrescriptionDTO updatePrescription(Long id, UpdatePrescriptionRequest request, String prescriberUserId)
void cancelPrescription(Long id, String reason, String userId)
```

## DTO-strukturer

### CreatePrescriptionRequest
**Required fält**:
- patientId: String
- medicationId: Long
- dose: BigDecimal
- doseUnit: String
- frequency: String
- route: String
- startDate: LocalDate
- quantityPrescribed: Integer

**Optional fält**:
- frequencyDescription, indication, instructions
- endDate, quantityUnit, daysSupply, refillsAllowed
- isPRN (default false), isSubstitutionAllowed (default true)

### UpdatePrescriptionRequest
**All fält optional utom**:
- modificationReason: String (required!)

**Fält som kan uppdateras**:
- dose, doseUnit, frequency, frequencyDescription
- route, indication, instructions
- endDate, refillsAllowed, isSubstitutionAllowed

### PrescriptionDTO
Omfattande DTO med alla relevanta fält från Prescription + sammanslagen data från Medication och Prescriber.

## Skillnader mot ursprungliga tester

| Testad funktionalitet | Förväntad | Faktisk |
|----------------------|-----------|---------|
| Medication.setActiveIngredient() | Finns | **FINNS INTE** - använd setGenericName() |
| Patient.setPersonalNumber() | Finns | **FINNS INTE** - använd setEncryptedSsn() |
| Prescription dosage/quantity | Enkla String/int | **KOMPLEX** - BigDecimal dose, doseUnit, quantityPrescribed |
| PrescriptionStatus.DISPENSED | Finns | **FINNS INTE** |
| Service method signatures | Individuella parametrar | **DTO-objekt** |
| Repository queries | findByPatient(), findByPrescriber() | **findByPatientId(), findByPrescriberId()** |

## Controllers

### PrescriptionController (Patient-facing)
**Base path**: `/api/v1/prescriptions`
- `GET /` - Get my prescriptions (optional ?status parameter)
- `GET /{id}` - Get prescription details
- `GET /refill-eligible` - Get refill-eligible prescriptions
- `POST /{id}/take` - Record medication taken (adherence)
- `GET /{id}/adherence` - Get adherence history

**Header**: `X-Patient-Id` (TODO: ska komma från OAuth2 token)

### PrescriberController
**Base path**: `/api/v1/prescriber`
- `POST /prescriptions` - Create prescription
- `PUT /prescriptions/{id}` - Update prescription
- `DELETE /prescriptions/{id}` - Cancel prescription
- `GET /prescriptions` - Get my prescriptions (optional ?patientId parameter)
- `GET /prescriptions/{id}` - Get prescription details

**Header**: `X-Prescriber-Id` (TODO: ska komma från OAuth2 token)

### MedicationController
*(Ej granskad i denna analys)*

## Nästa steg för tester
1. Skriv tester baserade på faktiska DTO-strukturer
2. Använd faktiska repository-metoder
3. Testa med korrekt Prescription-struktur (dose, frequency etc.)
4. Använd rätt entity-fält (encryptedSsn, genericName, userId etc.)
5. Testa båda controllers: PrescriptionController (patient) och PrescriberController
