-- Patient tables with privacy protection

CREATE TABLE patients (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE,
    encrypted_ssn VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10),
    
    -- Contact information
    email VARCHAR(255),
    phone VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    postal_code VARCHAR(10),
    city VARCHAR(100),
    country VARCHAR(50) DEFAULT 'Sweden',
    
    -- Emergency contact
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(50),
    
    -- Medical profile
    allergies TEXT,
    chronic_conditions TEXT,
    weight_kg DECIMAL(5,2),
    height_cm INT,
    blood_type VARCHAR(5),
    
    -- Preferences
    preferred_language VARCHAR(10) DEFAULT 'sv',
    preferred_pharmacy_id BIGINT,
    
    -- Privacy & consent
    consent_data_sharing BOOLEAN DEFAULT FALSE,
    consent_marketing BOOLEAN DEFAULT FALSE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    -- Soft delete for GDPR compliance
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    FOREIGN KEY (preferred_pharmacy_id) REFERENCES pharmacies(id),
    CONSTRAINT chk_gender CHECK (gender IN ('M', 'F', 'O', 'U'))
);

-- Audit log for patient data access (GDPR requirement)
CREATE TABLE patient_access_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    accessed_by VARCHAR(255) NOT NULL,
    access_role VARCHAR(50) NOT NULL,
    access_purpose VARCHAR(100),
    access_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT chk_access_type CHECK (access_type IN ('VIEW', 'UPDATE', 'CREATE', 'DELETE', 'EXPORT'))
);

-- Indices
CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_patients_name ON patients(last_name, first_name);
CREATE INDEX idx_patients_dob ON patients(date_of_birth);
CREATE INDEX idx_patients_deleted ON patients(deleted_at);

CREATE INDEX idx_access_log_patient ON patient_access_log(patient_id);
CREATE INDEX idx_access_log_accessed_by ON patient_access_log(accessed_by);
CREATE INDEX idx_access_log_timestamp ON patient_access_log(accessed_at);
