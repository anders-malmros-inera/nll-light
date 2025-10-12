-- Baseline: Existing medication model
-- This migration captures the current medication-focused schema

CREATE TABLE substances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    atc_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name)
);

CREATE TABLE medications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    npl_id VARCHAR(20) UNIQUE,
    trade_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    substance_id BIGINT,
    form VARCHAR(50),
    strength VARCHAR(50),
    route VARCHAR(50),
    atc_code VARCHAR(10),
    rx_status VARCHAR(10),
    is_available BOOLEAN DEFAULT TRUE,
    price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (substance_id) REFERENCES substances(id)
);

CREATE TABLE monographs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication_id BIGINT NOT NULL,
    indication TEXT,
    dosage TEXT,
    contraindications TEXT,
    side_effects TEXT,
    warnings TEXT,
    interactions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medication_id) REFERENCES medications(id),
    UNIQUE(medication_id)
);

-- Indices for medication search
CREATE INDEX idx_medications_trade_name ON medications(trade_name);
CREATE INDEX idx_medications_generic_name ON medications(generic_name);
CREATE INDEX idx_medications_atc_code ON medications(atc_code);
CREATE INDEX idx_medications_npl_id ON medications(npl_id);
CREATE INDEX idx_medications_substance ON medications(substance_id);

CREATE INDEX idx_substances_name ON substances(name);
CREATE INDEX idx_substances_atc ON substances(atc_code);
