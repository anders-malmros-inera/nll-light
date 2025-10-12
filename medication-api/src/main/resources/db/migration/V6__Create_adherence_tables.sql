-- Adherence tracking

CREATE TABLE adherence_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    
    scheduled_time TIMESTAMP NOT NULL,
    actual_time TIMESTAMP,
    
    status VARCHAR(20) NOT NULL,
    
    -- Details
    dose_taken DECIMAL(10,2),
    dose_unit VARCHAR(20),
    
    -- Context
    notes TEXT,
    side_effects_reported TEXT,
    
    -- Tracking method
    source VARCHAR(20) DEFAULT 'PATIENT_REPORTED',
    device_id VARCHAR(100),
    
    -- Location (optional, for context)
    location_type VARCHAR(50),
    
    -- Reminder information
    reminder_sent_at TIMESTAMP,
    reminder_acknowledged BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    
    CONSTRAINT chk_adherence_status CHECK (status IN ('TAKEN', 'MISSED', 'SKIPPED', 'PARTIAL', 'DELAYED', 'EARLY')),
    CONSTRAINT chk_source CHECK (source IN ('PATIENT_REPORTED', 'AUTO_TRACKED', 'DEVICE', 'CAREGIVER', 'INFERRED')),
    CONSTRAINT chk_location_type CHECK (location_type IS NULL OR location_type IN ('HOME', 'WORK', 'TRAVEL', 'OTHER'))
);

-- Adherence statistics (materialized view / aggregate table)
CREATE TABLE adherence_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL UNIQUE,
    patient_id VARCHAR(64) NOT NULL,
    
    -- Period
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    -- Counts
    total_scheduled INT DEFAULT 0,
    total_taken INT DEFAULT 0,
    total_missed INT DEFAULT 0,
    total_skipped INT DEFAULT 0,
    
    -- Percentages
    adherence_rate DECIMAL(5,2),
    
    -- Streaks
    current_streak_days INT DEFAULT 0,
    longest_streak_days INT DEFAULT 0,
    
    -- Last update
    last_calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- Medication reminders
CREATE TABLE medication_reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    patient_id VARCHAR(64) NOT NULL,
    
    reminder_time TIME NOT NULL,
    days_of_week VARCHAR(100),
    
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Notification preferences
    notification_method VARCHAR(50) NOT NULL,
    notification_advance_minutes INT DEFAULT 0,
    
    -- Contact
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    
    -- Snooze
    snooze_enabled BOOLEAN DEFAULT TRUE,
    snooze_duration_minutes INT DEFAULT 10,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    
    CONSTRAINT chk_notification_method CHECK (notification_method IN ('EMAIL', 'SMS', 'PUSH', 'APP', 'NONE'))
);

-- Indices
CREATE INDEX idx_adherence_prescription ON adherence_records(prescription_id);
CREATE INDEX idx_adherence_patient ON adherence_records(patient_id);
CREATE INDEX idx_adherence_scheduled ON adherence_records(scheduled_time);
CREATE INDEX idx_adherence_status ON adherence_records(status);
CREATE INDEX idx_adherence_source ON adherence_records(source);
-- H2 doesn't support function-based indexes - DATE(scheduled_time) would work in PostgreSQL
-- CREATE INDEX idx_adherence_date ON adherence_records(DATE(scheduled_time));

CREATE INDEX idx_adherence_stats_prescription ON adherence_statistics(prescription_id);
CREATE INDEX idx_adherence_stats_patient ON adherence_statistics(patient_id);
CREATE INDEX idx_adherence_stats_period ON adherence_statistics(period_start, period_end);

CREATE INDEX idx_reminders_prescription ON medication_reminders(prescription_id);
CREATE INDEX idx_reminders_patient ON medication_reminders(patient_id);
CREATE INDEX idx_reminders_active ON medication_reminders(is_active);
CREATE INDEX idx_reminders_time ON medication_reminders(reminder_time);
