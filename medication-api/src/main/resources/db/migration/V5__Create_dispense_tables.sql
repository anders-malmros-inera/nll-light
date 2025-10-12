-- Dispense events (pharmacy fulfillment)

CREATE TABLE dispense_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    pharmacy_id BIGINT,
    pharmacist_id BIGINT,
    
    dispensed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Dispensed medication details
    medication_id BIGINT NOT NULL,
    quantity_dispensed INT NOT NULL,
    dose_form VARCHAR(50),
    strength VARCHAR(50),
    
    -- Product tracking
    lot_number VARCHAR(50),
    ndc_code VARCHAR(20),
    expiration_date DATE,
    manufacturer VARCHAR(255),
    
    -- Financial
    patient_cost DECIMAL(10,2),
    insurance_cost DECIMAL(10,2),
    reimbursement_amount DECIMAL(10,2),
    copay_amount DECIMAL(10,2),
    
    -- Substitution
    is_substituted BOOLEAN DEFAULT FALSE,
    original_medication_id BIGINT,
    substitution_reason VARCHAR(100),
    
    -- Counseling
    counseling_provided BOOLEAN DEFAULT FALSE,
    counseling_notes TEXT,
    patient_signature_obtained BOOLEAN DEFAULT FALSE,
    
    -- Verification
    verification_method VARCHAR(50),
    verification_id VARCHAR(100),
    
    -- Notes
    dispense_notes TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(id),
    FOREIGN KEY (pharmacist_id) REFERENCES pharmacists(id),
    FOREIGN KEY (medication_id) REFERENCES medications(id),
    FOREIGN KEY (original_medication_id) REFERENCES medications(id),
    
    CONSTRAINT chk_quantity_positive CHECK (quantity_dispensed > 0),
    CONSTRAINT chk_verification_method CHECK (verification_method IN ('PHOTO_ID', 'PERSONNUMMER', 'ELECTRONIC', 'OTHER'))
);

-- Refill requests
CREATE TABLE refill_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    pharmacy_id BIGINT,
    
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    requested_quantity INT,
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Processing
    processed_at TIMESTAMP,
    processed_by VARCHAR(100),
    processing_notes TEXT,
    
    -- Denial
    denial_reason TEXT,
    
    -- Preferred pickup
    preferred_pickup_date DATE,
    preferred_pickup_time TIME,
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(id),
    
    CONSTRAINT chk_refill_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'PROCESSING', 'READY', 'PICKED_UP', 'CANCELLED'))
);

-- Indices
CREATE INDEX idx_dispense_prescription ON dispense_events(prescription_id);
CREATE INDEX idx_dispense_pharmacy ON dispense_events(pharmacy_id);
CREATE INDEX idx_dispense_pharmacist ON dispense_events(pharmacist_id);
CREATE INDEX idx_dispense_date ON dispense_events(dispensed_at);
CREATE INDEX idx_dispense_medication ON dispense_events(medication_id);
CREATE INDEX idx_dispense_lot ON dispense_events(lot_number);

CREATE INDEX idx_refill_requests_prescription ON refill_requests(prescription_id);
CREATE INDEX idx_refill_requests_patient ON refill_requests(patient_id);
CREATE INDEX idx_refill_requests_pharmacy ON refill_requests(pharmacy_id);
CREATE INDEX idx_refill_requests_status ON refill_requests(status);
CREATE INDEX idx_refill_requests_date ON refill_requests(request_date);
