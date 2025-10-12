-- Drug interaction data

CREATE TABLE interaction_severity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    severity_level INT NOT NULL,
    color_code VARCHAR(7),
    CONSTRAINT chk_severity_level CHECK (severity_level BETWEEN 1 AND 5)
);

CREATE TABLE drug_interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    substance_id_a BIGINT NOT NULL,
    substance_id_b BIGINT NOT NULL,
    severity_id BIGINT NOT NULL,
    
    interaction_type VARCHAR(50),
    description TEXT NOT NULL,
    clinical_effects TEXT,
    mechanism TEXT,
    management TEXT,
    
    -- References
    reference_source VARCHAR(255),
    reference_url TEXT,
    evidence_level VARCHAR(50),
    
    -- Metadata
    is_active BOOLEAN DEFAULT TRUE,
    reviewed_date DATE,
    reviewed_by VARCHAR(100),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (substance_id_a) REFERENCES substances(id),
    FOREIGN KEY (substance_id_b) REFERENCES substances(id),
    FOREIGN KEY (severity_id) REFERENCES interaction_severity(id),
    
    UNIQUE(substance_id_a, substance_id_b),
    CONSTRAINT chk_different_substances CHECK (substance_id_a <> substance_id_b),
    CONSTRAINT chk_interaction_type CHECK (interaction_type IN ('PHARMACOKINETIC', 'PHARMACODYNAMIC', 'SYNERGISTIC', 'ANTAGONISTIC', 'OTHER'))
);

-- Detected interaction alerts
CREATE TABLE interaction_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    prescription_id_a BIGINT NOT NULL,
    prescription_id_b BIGINT NOT NULL,
    interaction_id BIGINT NOT NULL,
    
    alert_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- Override
    is_overridden BOOLEAN DEFAULT FALSE,
    overridden_by VARCHAR(100),
    overridden_at TIMESTAMP,
    override_reason TEXT,
    override_justification TEXT,
    
    -- Acknowledged
    is_acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP,
    
    -- Resolution
    resolved_at TIMESTAMP,
    resolution_action VARCHAR(100),
    
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (prescription_id_a) REFERENCES prescriptions(id),
    FOREIGN KEY (prescription_id_b) REFERENCES prescriptions(id),
    FOREIGN KEY (interaction_id) REFERENCES drug_interactions(id),
    
    CONSTRAINT chk_alert_status CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'OVERRIDDEN', 'RESOLVED'))
);

-- Indices
CREATE INDEX idx_interactions_substance_a ON drug_interactions(substance_id_a);
CREATE INDEX idx_interactions_substance_b ON drug_interactions(substance_id_b);
CREATE INDEX idx_interactions_severity ON drug_interactions(severity_id);
CREATE INDEX idx_interactions_active ON drug_interactions(is_active);

CREATE INDEX idx_alerts_patient ON interaction_alerts(patient_id);
CREATE INDEX idx_alerts_prescription_a ON interaction_alerts(prescription_id_a);
CREATE INDEX idx_alerts_prescription_b ON interaction_alerts(prescription_id_b);
CREATE INDEX idx_alerts_interaction ON interaction_alerts(interaction_id);
CREATE INDEX idx_alerts_status ON interaction_alerts(status);
CREATE INDEX idx_alerts_date ON interaction_alerts(alert_date);
