-- Core prescription tables

CREATE TABLE prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    medication_id BIGINT NOT NULL,
    prescriber_id BIGINT,
    
    prescription_number VARCHAR(50) UNIQUE NOT NULL,
    
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Dosing information
    dose DECIMAL(10,2),
    dose_unit VARCHAR(20),
    frequency VARCHAR(20),
    frequency_description TEXT,
    route VARCHAR(50),
    max_daily_dose DECIMAL(10,2),
    max_daily_dose_unit VARCHAR(20),
    
    -- Clinical information
    indication TEXT,
    instructions TEXT,
    clinical_notes TEXT,
    
    -- Temporal
    prescribed_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    
    -- Refills
    refills_allowed INT DEFAULT 0,
    refills_remaining INT DEFAULT 0,
    last_refill_date DATE,
    next_refill_eligible_date DATE,
    
    -- Quantity
    quantity_prescribed INT,
    quantity_unit VARCHAR(20),
    days_supply INT,
    
    -- Flags
    is_prn BOOLEAN DEFAULT FALSE,
    is_substitution_allowed BOOLEAN DEFAULT TRUE,
    requires_prior_auth BOOLEAN DEFAULT FALSE,
    prior_auth_number VARCHAR(50),
    is_controlled_substance BOOLEAN DEFAULT FALSE,
    
    -- External references
    external_prescription_id VARCHAR(100),
    external_system VARCHAR(50),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    -- Cancellation
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(100),
    cancellation_reason TEXT,
    
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (medication_id) REFERENCES medications(id),
    FOREIGN KEY (prescriber_id) REFERENCES prescribers(id),
    
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'SUSPENDED', 'PENDING')),
    CONSTRAINT chk_dates CHECK (start_date <= COALESCE(end_date, start_date)),
    CONSTRAINT chk_refills CHECK (refills_remaining <= refills_allowed),
    CONSTRAINT chk_quantity CHECK (quantity_prescribed > 0),
    CONSTRAINT chk_days_supply CHECK (days_supply > 0)
);

-- Dosing schedule (one-to-many)
CREATE TABLE dosing_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    time_of_day TIME NOT NULL,
    dose DECIMAL(10,2),
    dose_unit VARCHAR(20),
    instructions TEXT,
    ordinal INT DEFAULT 1,
    day_of_week VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    UNIQUE(prescription_id, ordinal),
    CONSTRAINT chk_day_of_week CHECK (day_of_week IS NULL OR day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'DAILY'))
);

-- Prescription modifications history
CREATE TABLE prescription_modifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    modified_by VARCHAR(100) NOT NULL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modification_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    CONSTRAINT chk_modification_type CHECK (modification_type IN ('DOSE_CHANGE', 'STATUS_CHANGE', 'REFILL_CHANGE', 'CANCELLATION', 'EXTENSION', 'OTHER'))
);

-- Indices for prescription queries
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_medication ON prescriptions(medication_id);
CREATE INDEX idx_prescriptions_prescriber ON prescriptions(prescriber_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status);
CREATE INDEX idx_prescriptions_dates ON prescriptions(start_date, end_date);
CREATE INDEX idx_prescriptions_number ON prescriptions(prescription_number);
CREATE INDEX idx_prescriptions_prescribed_date ON prescriptions(prescribed_date);
CREATE INDEX idx_prescriptions_external ON prescriptions(external_prescription_id);

-- Active prescriptions (most common query) - H2 doesn't support WHERE clause in CREATE INDEX
CREATE INDEX idx_prescriptions_active ON prescriptions(patient_id, status);

-- Refills
CREATE INDEX idx_prescriptions_refill_eligible ON prescriptions(patient_id, next_refill_eligible_date, refills_remaining);

-- Dosing schedule
CREATE INDEX idx_dosing_schedule_prescription ON dosing_schedule(prescription_id);
CREATE INDEX idx_dosing_schedule_time ON dosing_schedule(time_of_day);

-- Modifications
CREATE INDEX idx_prescription_mods_prescription ON prescription_modifications(prescription_id);
CREATE INDEX idx_prescription_mods_timestamp ON prescription_modifications(modified_at);
