# Prescription Model Proposal
**From Medication Catalog to Prescription Management System**

**Date**: October 11, 2025  
**Status**: Design Proposal

---

## Table of Contents

1. [Conceptual Shift](#conceptual-shift)
2. [Domain Model Comparison](#domain-model-comparison)
3. [Database Schema](#database-schema)
4. [API Changes](#api-changes)
5. [Use Cases & Features](#use-cases--features)
6. [Migration Path](#migration-path)
7. [Implementation Roadmap](#implementation-roadmap)

---

## Conceptual Shift

### Current Model: Medication Catalog
**Focus**: Static medication information  
**Users**: General public, healthcare professionals (reference)  
**Primary Use Case**: "What is this medication? What are its properties?"

```
User → Search Medication → View Information
```

### Proposed Model: Prescription Management
**Focus**: Dynamic prescription lifecycle and patient-specific medication management  
**Users**: Patients, prescribers (doctors/nurses), pharmacists, caregivers  
**Primary Use Cases**:
- "What medications am I currently taking?"
- "When should I take my next dose?"
- "Can I refill this prescription?"
- "Are there any interactions between my medications?"
- "What prescriptions need renewal?"

```
Patient → View My Prescriptions → Manage Medications → Track Adherence
Doctor → Create Prescription → Monitor Patient → Adjust Treatment
Pharmacist → Dispense Medication → Track Inventory → Counsel Patient
```

### Key Differences

| Aspect | Medication Model | Prescription Model |
|--------|------------------|-------------------|
| **Data Focus** | Product catalog | Patient records |
| **Primary Entity** | Medication (static) | Prescription (dynamic) |
| **Relationships** | Medication ↔ Monograph | Patient ↔ Prescription ↔ Medication |
| **Time Dimension** | Timeless reference | Time-bound (start/end dates, refills) |
| **User Context** | Anonymous browsing | Authenticated patient-specific |
| **Privacy** | Public information | Protected health information (PHI) |
| **Compliance** | Medical device regs | GDPR + healthcare data protection |
| **Complexity** | Low-medium | High |

---

## Domain Model Comparison

### Current: Medication-Centric Model

```
┌─────────────┐
│  Medication │ (Product catalog)
├─────────────┤
│ id          │
│ name        │
│ description │
└─────────────┘
```

### Proposed: Prescription-Centric Model

```
┌──────────┐         ┌──────────────┐         ┌─────────────┐
│  Patient │────────→│ Prescription │────────→│  Medication │
└──────────┘    1:N  └──────────────┘    N:1  └─────────────┘
     │                      │                        │
     │                      │                        │
     ↓                      ↓                        ↓
┌──────────┐         ┌──────────────┐         ┌─────────────┐
│ Profile  │         │  Dispense    │         │  Monograph  │
└──────────┘         │  Event       │         └─────────────┘
                     └──────────────┘
                            │
                            ↓
                     ┌──────────────┐
                     │ Adherence    │
                     │ Record       │
                     └──────────────┘
```

### Core Entities

#### 1. Patient
```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    private String id;  // Swedish personnummer (encrypted/hashed)
    
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    
    @OneToMany(mappedBy = "patient")
    private List<Prescription> prescriptions;
    
    @Embedded
    private ContactInformation contactInfo;
    
    @Embedded
    private MedicalProfile medicalProfile;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 2. Prescription (Core Entity)
```java
@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescriber_id")
    private Prescriber prescriber;
    
    // Prescription details
    private String prescriptionNumber;  // Unique identifier (e.g., from e-prescription system)
    
    @Enumerated(EnumType.STRING)
    private PrescriptionStatus status;  // ACTIVE, COMPLETED, CANCELLED, EXPIRED
    
    // Clinical information
    @Embedded
    private Dosing dosing;
    
    private String indication;  // Reason for prescription
    private String instructions;  // Patient instructions (e.g., "Take with food")
    
    // Temporal
    private LocalDate prescribedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Refills
    private Integer refillsAllowed;
    private Integer refillsRemaining;
    private LocalDate lastRefillDate;
    
    // Quantity
    private Integer quantityPrescribed;
    private String quantityUnit;
    private Integer daysSupply;
    
    // Flags
    private Boolean isPRN;  // Pro re nata (as needed)
    private Boolean isSubstitutionAllowed;
    private Boolean requiresPriorAuthorization;
    
    // Relationships
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private List<DispenseEvent> dispenseEvents;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private List<AdherenceRecord> adherenceRecords;
    
    // Audit
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 3. Dosing (Embedded)
```java
@Embeddable
public class Dosing {
    private Double dose;
    private String doseUnit;  // mg, mL, tablets, etc.
    
    private String frequency;  // "BID" (twice daily), "QD" (once daily), "TID" (three times daily)
    private String frequencyDescription;  // "Take 2 tablets every 12 hours"
    
    @ElementCollection
    @CollectionTable(name = "dosing_schedule")
    private List<DosingTime> schedule;  // Specific times: [08:00, 20:00]
    
    private String route;  // oral, IV, topical, etc.
    
    private Double maxDailyDose;
    private String maxDailyDoseUnit;
}
```

#### 4. DispenseEvent
```java
@Entity
@Table(name = "dispense_events")
public class DispenseEvent {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacist_id")
    private Pharmacist pharmacist;
    
    private LocalDateTime dispensedAt;
    
    private Integer quantityDispensed;
    private String lotNumber;
    private LocalDate expirationDate;
    
    private BigDecimal patientCost;
    private BigDecimal reimbursementAmount;
    
    private String notes;  // Pharmacist counseling notes
}
```

#### 5. AdherenceRecord
```java
@Entity
@Table(name = "adherence_records")
public class AdherenceRecord {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;
    
    private LocalDateTime scheduledTime;
    private LocalDateTime actualTime;
    
    @Enumerated(EnumType.STRING)
    private AdherenceStatus status;  // TAKEN, MISSED, SKIPPED, PARTIAL
    
    private String notes;  // Patient notes about why missed, etc.
    
    @Enumerated(EnumType.STRING)
    private RecordSource source;  // PATIENT_REPORTED, AUTO_TRACKED, DEVICE
}
```

#### 6. Prescriber
```java
@Entity
@Table(name = "prescribers")
public class Prescriber {
    @Id
    private Long id;
    
    private String licenseNumber;  // Swedish: Läkarlegitimation
    private String firstName;
    private String lastName;
    private String specialty;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private HealthcareOrganization organization;
    
    @Embedded
    private ContactInformation contactInfo;
}
```

#### 7. Medication (Simplified from current model)
```java
@Entity
@Table(name = "medications")
public class Medication {
    @Id
    private Long id;
    
    private String nplId;  // National Product ID
    private String tradeName;
    private String genericName;
    
    @ManyToOne
    @JoinColumn(name = "substance_id")
    private Substance activeSubstance;
    
    private String form;  // tablet, capsule, solution
    private String strength;
    private String route;
    
    private String atcCode;
    
    @Enumerated(EnumType.STRING)
    private RxStatus rxStatus;  // RX, OTC
    
    @OneToOne(mappedBy = "medication", cascade = CascadeType.ALL)
    private Monograph monograph;
    
    // Market info
    private Boolean isAvailable;
    private BigDecimal price;
}
```

---

## Database Schema

### Core Tables

```sql
-- Patients (Protected Health Information)
CREATE TABLE patients (
    id VARCHAR(64) PRIMARY KEY,  -- Hashed personnummer
    encrypted_ssn BYTEA NOT NULL UNIQUE,  -- Encrypted Swedish SSN
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10),
    email VARCHAR(255),
    phone VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    postal_code VARCHAR(10),
    city VARCHAR(100),
    
    -- Medical profile
    allergies TEXT[],
    chronic_conditions TEXT[],
    weight_kg DECIMAL(5,2),
    height_cm INT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100),
    
    -- Soft delete for GDPR
    deleted_at TIMESTAMP,
    
    CONSTRAINT chk_gender CHECK (gender IN ('M', 'F', 'O', 'U'))
);

-- Prescriptions (Core entity)
CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL REFERENCES patients(id),
    medication_id BIGINT NOT NULL REFERENCES medications(id),
    prescriber_id BIGINT REFERENCES prescribers(id),
    
    prescription_number VARCHAR(50) UNIQUE NOT NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Dosing
    dose DECIMAL(10,2),
    dose_unit VARCHAR(20),
    frequency VARCHAR(20),
    frequency_description TEXT,
    route VARCHAR(50),
    max_daily_dose DECIMAL(10,2),
    
    -- Clinical
    indication TEXT,
    instructions TEXT,
    
    -- Temporal
    prescribed_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    
    -- Refills
    refills_allowed INT DEFAULT 0,
    refills_remaining INT DEFAULT 0,
    last_refill_date DATE,
    
    -- Quantity
    quantity_prescribed INT,
    quantity_unit VARCHAR(20),
    days_supply INT,
    
    -- Flags
    is_prn BOOLEAN DEFAULT FALSE,
    is_substitution_allowed BOOLEAN DEFAULT TRUE,
    requires_prior_auth BOOLEAN DEFAULT FALSE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100),
    
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'SUSPENDED')),
    CONSTRAINT chk_dates CHECK (start_date <= COALESCE(end_date, start_date)),
    CONSTRAINT chk_refills CHECK (refills_remaining <= refills_allowed)
);

-- Dosing schedule (one-to-many)
CREATE TABLE dosing_schedule (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    time_of_day TIME NOT NULL,
    dose DECIMAL(10,2),
    instructions TEXT,
    ordinal INT DEFAULT 1,
    
    UNIQUE(prescription_id, ordinal)
);

-- Dispense events (pharmacy fulfillment)
CREATE TABLE dispense_events (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    pharmacy_id BIGINT REFERENCES pharmacies(id),
    pharmacist_id BIGINT REFERENCES pharmacists(id),
    
    dispensed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    quantity_dispensed INT NOT NULL,
    lot_number VARCHAR(50),
    expiration_date DATE,
    
    patient_cost DECIMAL(10,2),
    reimbursement_amount DECIMAL(10,2),
    
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- Adherence tracking
CREATE TABLE adherence_records (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    
    scheduled_time TIMESTAMP NOT NULL,
    actual_time TIMESTAMP,
    
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    source VARCHAR(20) DEFAULT 'PATIENT_REPORTED',
    
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT chk_adherence_status CHECK (status IN ('TAKEN', 'MISSED', 'SKIPPED', 'PARTIAL')),
    CONSTRAINT chk_source CHECK (source IN ('PATIENT_REPORTED', 'AUTO_TRACKED', 'DEVICE'))
);

-- Prescribers
CREATE TABLE prescribers (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE,  -- Links to Keycloak user
    license_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    specialty VARCHAR(100),
    organization_id BIGINT REFERENCES healthcare_organizations(id),
    email VARCHAR(255),
    phone VARCHAR(20),
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Pharmacies
CREATE TABLE pharmacies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    license_number VARCHAR(50) UNIQUE,
    address_line1 VARCHAR(255),
    postal_code VARCHAR(10),
    city VARCHAR(100),
    phone VARCHAR(20),
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- Pharmacists
CREATE TABLE pharmacists (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    pharmacy_id BIGINT REFERENCES pharmacies(id),
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- Healthcare organizations (clinics, hospitals)
CREATE TABLE healthcare_organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    org_number VARCHAR(20) UNIQUE,  -- Swedish organisationsnummer
    type VARCHAR(50),  -- CLINIC, HOSPITAL, PRIMARY_CARE
    address_line1 VARCHAR(255),
    postal_code VARCHAR(10),
    city VARCHAR(100),
    phone VARCHAR(20),
    
    created_at TIMESTAMP DEFAULT NOW()
);

-- Medications (simplified - keep current model)
CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,
    npl_id VARCHAR(20) UNIQUE,
    trade_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    substance_id BIGINT REFERENCES substances(id),
    form VARCHAR(50),
    strength VARCHAR(50),
    route VARCHAR(50),
    atc_code VARCHAR(10),
    rx_status VARCHAR(10),
    is_available BOOLEAN DEFAULT TRUE,
    price DECIMAL(10,2),
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### Indices for Performance

```sql
-- Patient lookups
CREATE INDEX idx_patients_encrypted_ssn ON patients(encrypted_ssn);
CREATE INDEX idx_patients_name ON patients(last_name, first_name);

-- Prescription queries
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_medication ON prescriptions(medication_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status);
CREATE INDEX idx_prescriptions_dates ON prescriptions(start_date, end_date);
CREATE INDEX idx_prescriptions_number ON prescriptions(prescription_number);
CREATE INDEX idx_prescriptions_prescriber ON prescriptions(prescriber_id);

-- Active prescriptions (common query)
CREATE INDEX idx_prescriptions_active ON prescriptions(patient_id, status) 
    WHERE status = 'ACTIVE';

-- Dispense events
CREATE INDEX idx_dispense_prescription ON dispense_events(prescription_id);
CREATE INDEX idx_dispense_pharmacy ON dispense_events(pharmacy_id);
CREATE INDEX idx_dispense_date ON dispense_events(dispensed_at);

-- Adherence
CREATE INDEX idx_adherence_prescription ON adherence_records(prescription_id);
CREATE INDEX idx_adherence_scheduled ON adherence_records(scheduled_time);
CREATE INDEX idx_adherence_status ON adherence_records(status);

-- Prescribers
CREATE INDEX idx_prescribers_license ON prescribers(license_number);
CREATE INDEX idx_prescribers_org ON prescribers(organization_id);
```

---

## API Changes

### Current API (Medication-focused)
```
GET    /api/v1/medications           # List medications
GET    /api/v1/medications/{id}      # Get medication details
GET    /api/v1/medications/search    # Search medications
```

### Proposed API (Prescription-focused)

#### Patient API
```
# My Prescriptions
GET    /api/v1/prescriptions                          # List my active prescriptions
GET    /api/v1/prescriptions/{id}                     # Get prescription details
GET    /api/v1/prescriptions/history                  # All prescriptions (active + past)
GET    /api/v1/prescriptions/due                      # Medications due now/soon

# Adherence tracking
POST   /api/v1/prescriptions/{id}/take                # Record taking medication
GET    /api/v1/prescriptions/{id}/adherence           # Get adherence history
GET    /api/v1/prescriptions/{id}/adherence/stats     # Adherence statistics

# Refills
POST   /api/v1/prescriptions/{id}/refill-request      # Request refill
GET    /api/v1/prescriptions/refill-eligible          # List refillable prescriptions

# Interactions
POST   /api/v1/prescriptions/check-interactions       # Check my current meds for interactions
GET    /api/v1/prescriptions/{id}/interactions        # Interactions for specific prescription

# Medication information (still accessible)
GET    /api/v1/medications/{id}                       # Medication reference info
GET    /api/v1/medications/search                     # Search medication catalog
```

#### Prescriber API
```
# Prescribe
POST   /api/v1/prescriber/prescriptions               # Create new prescription
PUT    /api/v1/prescriber/prescriptions/{id}          # Modify prescription
DELETE /api/v1/prescriber/prescriptions/{id}          # Cancel prescription

# Patient management
GET    /api/v1/prescriber/patients                    # My patients
GET    /api/v1/prescriber/patients/{id}/prescriptions # Patient's prescription history
GET    /api/v1/prescriber/patients/{id}/adherence     # Patient adherence overview

# Clinical decision support
POST   /api/v1/prescriber/interaction-check           # Check before prescribing
POST   /api/v1/prescriber/dose-check                  # Validate dose based on patient
GET    /api/v1/prescriber/alternatives                # Therapeutic alternatives
```

#### Pharmacist API
```
# Dispensing
POST   /api/v1/pharmacy/dispense                      # Record dispensing event
GET    /api/v1/pharmacy/prescriptions/pending         # Prescriptions ready to fill
GET    /api/v1/pharmacy/prescriptions/{id}            # Prescription details

# Patient counseling
GET    /api/v1/pharmacy/patients/{id}/medications     # Patient's current medications
POST   /api/v1/pharmacy/counseling/{prescriptionId}   # Record counseling session

# Inventory
GET    /api/v1/pharmacy/inventory/{medicationId}      # Stock levels
POST   /api/v1/pharmacy/inventory/order               # Order medication
```

#### Admin API
```
# Analytics
GET    /api/v1/admin/analytics/prescriptions          # Prescription trends
GET    /api/v1/admin/analytics/adherence              # Adherence statistics
GET    /api/v1/admin/analytics/top-medications        # Most prescribed

# Audit
GET    /api/v1/admin/audit-log                        # Access logs (GDPR compliance)
GET    /api/v1/admin/audit-log/patient/{id}           # Who accessed patient data
```

### Example API Implementations

#### Get My Active Prescriptions
```java
@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<PrescriptionDTO>> getMyPrescriptions(
            @RequestParam(required = false) String status,
            Authentication auth) {
        
        String patientId = extractPatientId(auth);
        
        List<Prescription> prescriptions = prescriptionService
                .getPatientPrescriptions(patientId, status);
        
        List<PrescriptionDTO> dtos = prescriptions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/due")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DoseDueDTO>> getMedicationsDue(Authentication auth) {
        String patientId = extractPatientId(auth);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusHours(2);
        
        List<DoseDue> doses = prescriptionService
                .getDosesDue(patientId, now, soon);
        
        return ResponseEntity.ok(doses.stream()
                .map(this::toDoseDTO)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/take")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AdherenceRecordDTO> recordMedicationTaken(
            @PathVariable Long id,
            @RequestBody TakeMedicationRequest request,
            Authentication auth) {
        
        String patientId = extractPatientId(auth);
        
        AdherenceRecord record = prescriptionService.recordAdherence(
                id, 
                patientId,
                LocalDateTime.now(),
                AdherenceStatus.TAKEN,
                request.getNotes()
        );
        
        return ResponseEntity.ok(toAdherenceDTO(record));
    }
}
```

#### Create Prescription (Prescriber)
```java
@RestController
@RequestMapping("/api/v1/prescriber/prescriptions")
@PreAuthorize("hasRole('PRESCRIBER')")
public class PrescriberController {

    @PostMapping
    public ResponseEntity<PrescriptionDTO> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest request,
            Authentication auth) {
        
        Long prescriberId = extractPrescriberId(auth);
        
        // Validate patient exists and prescriber has access
        validateAccess(prescriberId, request.getPatientId());
        
        // Check for interactions with existing medications
        InteractionCheckResult interactions = interactionService
                .checkWithCurrentMedications(
                        request.getPatientId(), 
                        request.getMedicationId()
                );
        
        if (interactions.hasMajorInteractions()) {
            // Require explicit override
            if (!request.isOverrideInteractions()) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new InteractionWarning(interactions));
            }
        }
        
        Prescription prescription = prescriptionService.createPrescription(
                PrescriptionBuilder.builder()
                        .patientId(request.getPatientId())
                        .prescriberId(prescriberId)
                        .medicationId(request.getMedicationId())
                        .dosing(request.getDosing())
                        .indication(request.getIndication())
                        .instructions(request.getInstructions())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .refillsAllowed(request.getRefillsAllowed())
                        .quantityPrescribed(request.getQuantityPrescribed())
                        .daysSupply(request.getDaysSupply())
                        .build()
        );
        
        // Audit log
        auditService.logPrescriptionCreated(prescription, prescriberId);
        
        // Notify patient (optional)
        notificationService.notifyNewPrescription(prescription);
        
        return ResponseEntity
                .created(URI.create("/api/v1/prescriptions/" + prescription.getId()))
                .body(toDTO(prescription));
    }
}
```

---

## Use Cases & Features

### Patient Use Cases

#### 1. Medication Schedule View
```
┌─────────────────────────────────────────────┐
│ Today's Medications          Friday, Oct 11 │
├─────────────────────────────────────────────┤
│                                              │
│ 🔔 Due Now (08:00)                          │
│   ☐ Metformin 500mg - 2 tablets             │
│       With breakfast                         │
│   ☐ Lisinopril 10mg - 1 tablet              │
│                                              │
│ ⏰ Upcoming                                  │
│   12:00 - Metformin 500mg (2 tablets)       │
│   20:00 - Metformin 500mg (2 tablets)       │
│                                              │
│ ✅ Completed Today                           │
│   06:00 - Levothyroxine 50mcg ✓            │
│                                              │
├─────────────────────────────────────────────┤
│ [View All Prescriptions]  [Refill Requests] │
└─────────────────────────────────────────────┘
```

#### 2. Prescription Details
```
Prescription #RX-2025-001234

Metformin Oral Tablet 500mg
Prescribed by: Dr. Anna Svensson, Vårdcentralen Solna
Date: 2025-09-15
Status: ● Active

DOSING INSTRUCTIONS
  Take 2 tablets by mouth 3 times daily with meals
  
  Morning (08:00):   2 tablets
  Midday (12:00):    2 tablets
  Evening (20:00):   2 tablets

INDICATION
  Type 2 Diabetes Mellitus

PATIENT INSTRUCTIONS
  • Take with food to reduce stomach upset
  • Do not crush or chew extended-release tablets
  • Contact doctor if experiencing persistent nausea

SUPPLY & REFILLS
  Quantity: 180 tablets
  Days Supply: 30 days
  Refills: 5 remaining
  Last Refilled: 2025-10-01
  Next Refill Available: 2025-10-22

⚠️ INTERACTION WARNING
  This medication may interact with your current prescription 
  for Warfarin. Monitor blood sugar closely.
  
[Request Refill]  [Set Reminder]  [Contact Prescriber]
```

#### 3. Adherence Tracking
```
Weekly Adherence Report

Metformin 500mg
━━━━━━━━━━━━━━━━━━ 95% (20/21 doses)

Mon  ✓ ✓ ✓
Tue  ✓ ✓ ✓
Wed  ✓ ✗ ✓  ← Missed midday dose
Thu  ✓ ✓ ✓
Fri  ✓ ✓ ✓
Sat  ✓ ✓ ✓
Sun  ✓ ✓ ✓

Monthly Trend: ↗ 92% → 95%

💡 TIP: Set a phone alarm for your midday dose
```

#### 4. Refill Management
```
Refill Requests

Ready to Refill
  ☐ Metformin 500mg
    • Last filled: Oct 1
    • Days remaining: 11
    • 5 refills left
    
  ☐ Lisinopril 10mg
    • Last filled: Sep 28
    • Days remaining: 8
    • 3 refills left

⚠️ Action Needed
  ⚠ Atorvastatin 20mg
    • No refills remaining
    • Contact Dr. Svensson for renewal
    
[Select All] [Request Selected Refills]
[Choose Pharmacy: Apoteket Hjärtat, Solna]
```

### Prescriber Use Cases

#### 1. Patient Medication Overview
```
Patient: Erik Andersson (19800512-XXXX)

Current Active Prescriptions (4)
┌──────────────────────────────────────────────────┐
│ Metformin 500mg                 Adherence: 95%   │
│   1000mg TID with meals                          │
│   Prescribed: 2025-09-15 by Dr. Svensson        │
│                                                  │
│ Lisinopril 10mg                 Adherence: 98%   │
│   10mg QD in morning                             │
│   Prescribed: 2025-08-01 by Dr. Svensson        │
│                                                  │
│ Atorvastatin 20mg               Adherence: 89%   │
│   20mg QHS (at bedtime)                          │
│   Prescribed: 2025-07-20 by Dr. Karlsson        │
│                                                  │
│ Warfarin 5mg                    Adherence: 100%  │
│   5mg QD, INR monitoring required                │
│   Prescribed: 2024-12-10 by Dr. Lundgren        │
│   ⚠️ 2 major interactions                        │
└──────────────────────────────────────────────────┘

Recent Lab Values
  HbA1c: 6.8% (target <7.0%) ✓
  INR: 2.3 (target 2.0-3.0) ✓
  LDL: 2.1 mmol/L (target <2.6) ✓

[Prescribe New]  [View Full History]  [Export Summary]
```

#### 2. New Prescription Workflow
```
New Prescription for Erik Andersson

Step 1: Select Medication
  Search: [Metformin_______________] 🔍
  
  Results:
  • Metformin Oral Tablet 500mg (Actavis)
  • Metformin Oral Tablet 850mg (Actavis)
  • Metformin XR Tablet 500mg (Teva)
  
Step 2: Dosing
  Dose: [2] tablets
  Frequency: [TID ▼] (3 times daily)
  Route: [Oral ▼]
  
  Schedule:
    Morning:  [08:00] - 2 tablets
    Midday:   [12:00] - 2 tablets
    Evening:  [20:00] - 2 tablets

Step 3: Clinical Info
  Indication: [Type 2 Diabetes Mellitus]
  Instructions: [Take with meals to reduce GI upset]
  
Step 4: Supply
  Quantity: [180] tablets
  Days Supply: [30] days
  Refills: [11] (1 year supply)

⚠️ INTERACTION CHECK
  ✓ No major interactions detected
  
  ℹ️ Minor interaction with Warfarin:
     Metformin may affect blood glucose; monitor INR

☑️ Substitution allowed
☐ Prior authorization required

[Cancel]  [Save as Draft]  [Prescribe]
```

### Pharmacist Use Cases

#### 1. Dispensing Queue
```
Prescriptions Ready to Fill

Priority (2)
  RX-001234 - Erik Andersson
    Metformin 500mg, #180
    Due: Oct 12
    [Fill Now]
  
  RX-001235 - Maria Johansson
    Lisinopril 10mg, #90
    Due: Oct 11 ⚠️ OVERDUE
    [Fill Now]

Today's Pickups (8)
  ... list continues ...
  
Counseling Required (3)
  RX-001240 - New medication
  RX-001241 - High-risk drug
  RX-001242 - Patient request
```

#### 2. Dispensing Workflow
```
Dispense: RX-001234

Patient: Erik Andersson
Medication: Metformin 500mg Oral Tablet
Quantity Prescribed: 180 tablets
Refills Remaining: 5

✓ Verify patient identity
✓ Check prescription validity
✓ Scan medication barcode: [________]

Lot Number: [ABC123456]
Expiration: [2027-08-15]

Patient Cost: 95 SEK
Reimbursement: 105 SEK

☑️ Counseling completed
☑️ Patient informed of side effects
☑️ Patient advised to take with meals

Notes: [Patient tolerating well, no issues reported]

[Cancel]  [Dispense & Print Label]
```

---

## Migration Path

### Phase 1: Dual Model (Months 1-2)
**Goal**: Run both models in parallel

1. **Add new prescription tables** alongside existing medication tables
2. **Create prescription API** at `/api/v1/prescriptions`
3. **Keep existing medication API** at `/api/v1/medications` (read-only reference)
4. **Update frontend** to show "My Prescriptions" for authenticated users
5. **Medication search** still available for reference

**Database**:
```sql
-- Keep existing
medications (existing table unchanged)
monographs (existing table unchanged)

-- Add new
patients
prescriptions
prescribers
dispense_events
adherence_records
```

### Phase 2: Feature Parity (Months 3-4)
**Goal**: Prescription model has all features

1. **Adherence tracking** UI and API
2. **Refill management** workflow
3. **Prescriber portal** for creating prescriptions
4. **Pharmacist portal** for dispensing
5. **Interaction checking** based on patient's active prescriptions
6. **Notifications** (email/SMS) for medication reminders

### Phase 3: Data Migration (Month 5)
**Goal**: Migrate existing medication data to prescriptions (if applicable)

If you have test/demo medication data:
```sql
-- Example: Create sample prescriptions for testing
INSERT INTO prescriptions (
    patient_id, medication_id, prescriber_id,
    prescription_number, status,
    dose, dose_unit, frequency,
    prescribed_date, start_date,
    refills_allowed, refills_remaining,
    quantity_prescribed, days_supply
)
SELECT 
    'test-patient-1',  -- Test patient
    m.id,
    1,  -- Test prescriber
    'RX-TEST-' || m.id,
    'ACTIVE',
    500, 'mg', 'TID',
    CURRENT_DATE - INTERVAL '30 days',
    CURRENT_DATE - INTERVAL '30 days',
    11, 11,
    180, 30
FROM medications m
WHERE m.id IN (1, 2, 3);  -- Your existing medications
```

### Phase 4: Deprecation (Month 6)
**Goal**: Remove old model

1. **Mark medication CRUD API as deprecated** (keep read-only reference)
2. **Remove medication create/update/delete** endpoints
3. **Update documentation** to focus on prescriptions
4. **Archive old data** (if any)

---

## Implementation Roadmap

### Week 1-2: Database Schema
- [ ] Design complete schema with all tables
- [ ] Create Flyway migrations
- [ ] Add sample data for testing
- [ ] Set up patient data encryption/hashing

### Week 3-4: Core Entities
- [ ] Create JPA entities (Patient, Prescription, etc.)
- [ ] Implement repositories
- [ ] Write unit tests for domain logic
- [ ] Set up audit logging

### Week 5-6: Patient API
- [ ] Implement PrescriptionController (GET prescriptions)
- [ ] Adherence tracking endpoints
- [ ] Refill request endpoints
- [ ] Integration tests

### Week 7-8: Prescriber API
- [ ] Create prescription endpoint
- [ ] Modify/cancel prescription endpoints
- [ ] Clinical decision support (interactions, dose checking)
- [ ] Patient overview endpoints

### Week 9-10: Pharmacist API
- [ ] Dispensing workflow
- [ ] Prescription queue management
- [ ] Counseling documentation

### Week 11-12: Frontend
- [ ] Patient portal: My Prescriptions view
- [ ] Medication schedule/calendar
- [ ] Adherence tracking UI
- [ ] Refill management UI

### Week 13-14: Advanced Features
- [ ] Notifications (reminders, refill alerts)
- [ ] Analytics dashboard
- [ ] Reporting (adherence reports, prescription trends)

### Week 15-16: Security & Compliance
- [ ] GDPR compliance features (data export, erasure)
- [ ] Audit logging for PHI access
- [ ] Security testing
- [ ] Access control testing

---

## Key Benefits of Prescription Model

### For Patients
- ✅ Personalized medication schedule
- ✅ Adherence tracking and reminders
- ✅ Easy refill management
- ✅ Interaction checking across all medications
- ✅ Access to prescription history

### For Prescribers
- ✅ Patient medication overview
- ✅ Clinical decision support
- ✅ Electronic prescribing workflow
- ✅ Adherence monitoring
- ✅ Better patient outcomes

### For Pharmacists
- ✅ Efficient dispensing workflow
- ✅ Patient counseling documentation
- ✅ Inventory management integration
- ✅ Quality assurance

### For Healthcare System
- ✅ Better medication adherence
- ✅ Reduced medication errors
- ✅ Improved patient safety
- ✅ Data for quality improvement
- ✅ Cost savings (fewer complications)

---

## Comparison Summary

| Feature | Medication Model | Prescription Model |
|---------|-----------------|-------------------|
| **Complexity** | Low | High |
| **User Auth** | Optional | Required |
| **Data Privacy** | Public | PHI (Protected) |
| **Primary Users** | General public | Patients, prescribers, pharmacists |
| **Key Entities** | Medication, Monograph | Patient, Prescription, Dispense |
| **Time Dimension** | Static | Dynamic (lifecycle) |
| **Clinical Value** | Reference | Actionable |
| **Compliance** | General medical | GDPR + healthcare regulations |
| **Dev Effort** | 2-3 months | 6-9 months |
| **Business Model** | Information service | Clinical tool / SaaS |

---

## Recommendation

**Hybrid Approach**: Keep both models but make prescriptions primary

```
Medications (Reference)
  └─► Used by prescriptions
  └─► Public search/browse
  └─► Drug monographs

Prescriptions (Core)
  └─► Patient-specific
  └─► Clinical workflow
  └─► Real-world usage
```

This allows:
- **Medications** remain as a reference catalog (like FASS)
- **Prescriptions** become the primary clinical tool
- Both can coexist and complement each other

---

**Next Steps**:
1. Review this proposal and decide on scope
2. Prioritize features (MVP vs. full system)
3. Choose migration path (greenfield vs. incremental)
4. Update ROADMAP.md with prescription model timelines

**Document Status**: Proposal for review  
**Date**: October 11, 2025
