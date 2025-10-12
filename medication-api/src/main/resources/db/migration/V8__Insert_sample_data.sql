-- Sample data for development and testing

-- Insert interaction severities
INSERT INTO interaction_severity (code, name, description, severity_level, color_code) VALUES
('MINOR', 'Minor', 'Minimal clinical significance. Monitor for effects.', 1, '#28a745'),
('MODERATE', 'Moderate', 'May require dosage adjustment or closer monitoring.', 2, '#ffc107'),
('MAJOR', 'Major', 'May result in serious adverse effects. Avoid combination if possible.', 3, '#fd7e14'),
('SEVERE', 'Severe', 'Potentially life-threatening. Combination should be avoided.', 4, '#dc3545'),
('CONTRAINDICATED', 'Contraindicated', 'Absolute contraindication. Do not use together.', 5, '#721c24');

-- Insert sample healthcare organizations
INSERT INTO healthcare_organizations (name, org_number, type, address_line1, postal_code, city, phone) VALUES
('Karolinska Universitetssjukhuset', '252100-0125', 'HOSPITAL', 'Solnavägen 1', '17176', 'Stockholm', '08-517 700 00'),
('Vårdcentralen Solna', '559123-4567', 'PRIMARY_CARE', 'Frösundaleden 20', '16970', 'Solna', '08-123 45 67'),
('Academic Hospital Uppsala', '232100-0133', 'HOSPITAL', 'Akademiska sjukhuset', '75185', 'Uppsala', '018-611 00 00'),
('Vårdcentralen Lund', '559234-5678', 'PRIMARY_CARE', 'Klostergatan 10', '22252', 'Lund', '046-123 45 67');

-- Insert sample prescribers
INSERT INTO prescribers (user_id, license_number, first_name, last_name, specialty, organization_id, email, phone) VALUES
('prescriber1', 'LIC-12345', 'Anna', 'Svensson', 'General Practice', 2, 'anna.svensson@vard.se', '070-123 45 67'),
('prescriber2', 'LIC-23456', 'Erik', 'Johansson', 'Cardiology', 1, 'erik.johansson@karolinska.se', '070-234 56 78'),
('prescriber3', 'LIC-34567', 'Maria', 'Andersson', 'Endocrinology', 1, 'maria.andersson@karolinska.se', '070-345 67 89'),
('prescriber4', 'LIC-45678', 'Lars', 'Nilsson', 'General Practice', 4, 'lars.nilsson@vard.se', '070-456 78 90');

-- Insert sample pharmacies
INSERT INTO pharmacies (name, license_number, chain_name, address_line1, postal_code, city, phone, latitude, longitude, is_24_hours) VALUES
('Apoteket Hjärtat Solna', 'APO-001', 'Apoteket Hjärtat', 'Frösundaleden 12', '16970', 'Solna', '08-123 45 00', 59.3600, 18.0000, FALSE),
('Apoteket Hjärtat City', 'APO-002', 'Apoteket Hjärtat', 'Sergels Torg 1', '11157', 'Stockholm', '08-234 56 00', 59.3326, 18.0649, TRUE),
('Apotek Lejonet Uppsala', 'APO-003', 'Apotek Lejonet', 'Stationsgatan 12', '75340', 'Uppsala', '018-123 45 00', 59.8586, 17.6389, FALSE),
('Lloyds Apotek Lund', 'APO-004', 'Lloyds Apotek', 'Kyrkogatan 5', '22222', 'Lund', '046-234 56 00', 55.7047, 13.1910, FALSE);

-- Insert sample pharmacists
INSERT INTO pharmacists (user_id, license_number, first_name, last_name, pharmacy_id, email, phone) VALUES
('pharmacist1', 'PHA-11111', 'Sofia', 'Karlsson', 1, 'sofia.karlsson@apoteket.se', '070-111 22 33'),
('pharmacist2', 'PHA-22222', 'Johan', 'Lindgren', 2, 'johan.lindgren@apoteket.se', '070-222 33 44'),
('pharmacist3', 'PHA-33333', 'Emma', 'Berg', 3, 'emma.berg@lejonet.se', '070-333 44 55'),
('pharmacist4', 'PHA-44444', 'Anders', 'Olsson', 4, 'anders.olsson@lloyds.se', '070-444 55 66');

-- Insert sample substances
INSERT INTO substances (name, description, atc_code) VALUES
('Metformin', 'Biguanide antidiabetic agent', 'A10BA02'),
('Atorvastatin', 'HMG-CoA reductase inhibitor (statin)', 'C10AA05'),
('Lisinopril', 'ACE inhibitor', 'C09AA03'),
('Warfarin', 'Vitamin K antagonist anticoagulant', 'B01AA03'),
('Levothyroxine', 'Thyroid hormone', 'H03AA01'),
('Omeprazole', 'Proton pump inhibitor', 'A02BC01'),
('Amoxicillin', 'Beta-lactam antibiotic', 'J01CA04'),
('Paracetamol', 'Analgesic and antipyretic', 'N02BE01'),
('Ibuprofen', 'NSAID', 'M01AE01'),
('Simvastatin', 'HMG-CoA reductase inhibitor (statin)', 'C10AA01');

-- Insert sample medications
INSERT INTO medications (npl_id, trade_name, generic_name, substance_id, form, strength, route, atc_code, rx_status, is_available, price) VALUES
('NPL-001', 'Metformin Actavis', 'Metformin', 1, 'Tablet', '500mg', 'Oral', 'A10BA02', 'RX', TRUE, 95.50),
('NPL-002', 'Metformin Actavis', 'Metformin', 1, 'Tablet', '850mg', 'Oral', 'A10BA02', 'RX', TRUE, 112.00),
('NPL-003', 'Lipitor', 'Atorvastatin', 2, 'Tablet', '20mg', 'Oral', 'C10AA05', 'RX', TRUE, 145.00),
('NPL-004', 'Lipitor', 'Atorvastatin', 2, 'Tablet', '40mg', 'Oral', 'C10AA05', 'RX', TRUE, 195.00),
('NPL-005', 'Zestril', 'Lisinopril', 3, 'Tablet', '10mg', 'Oral', 'C09AA03', 'RX', TRUE, 78.00),
('NPL-006', 'Zestril', 'Lisinopril', 3, 'Tablet', '20mg', 'Oral', 'C09AA03', 'RX', TRUE, 98.00),
('NPL-007', 'Waran', 'Warfarin', 4, 'Tablet', '2.5mg', 'Oral', 'B01AA03', 'RX', TRUE, 65.00),
('NPL-008', 'Waran', 'Warfarin', 4, 'Tablet', '5mg', 'Oral', 'B01AA03', 'RX', TRUE, 72.00),
('NPL-009', 'Euthyrox', 'Levothyroxine', 5, 'Tablet', '50mcg', 'Oral', 'H03AA01', 'RX', TRUE, 58.00),
('NPL-010', 'Omeprazol Teva', 'Omeprazole', 6, 'Capsule', '20mg', 'Oral', 'A02BC01', 'RX', TRUE, 85.00),
('NPL-011', 'Alvedon', 'Paracetamol', 8, 'Tablet', '500mg', 'Oral', 'N02BE01', 'OTC', TRUE, 45.00),
('NPL-012', 'Ipren', 'Ibuprofen', 9, 'Tablet', '400mg', 'Oral', 'M01AE01', 'OTC', TRUE, 52.00);

-- Insert sample monographs
INSERT INTO monographs (medication_id, indication, dosage, contraindications, side_effects, warnings, interactions) VALUES
(1, 'Type 2 diabetes mellitus', 'Initial: 500mg twice daily or 850mg once daily. Maximum: 2550mg/day in divided doses', 'Severe renal impairment, acute metabolic acidosis, diabetic ketoacidosis', 'Nausea, diarrhea, abdominal pain, loss of appetite, metallic taste', 'May cause lactic acidosis (rare but serious). Monitor renal function.', 'Increased risk of hypoglycemia with insulin or sulfonylureas. May affect warfarin metabolism.'),
(3, 'Hypercholesterolemia, prevention of cardiovascular disease', '10-80mg once daily in the evening', 'Active liver disease, pregnancy, breastfeeding', 'Muscle pain, headache, nausea, constipation, elevated liver enzymes', 'Monitor liver function. Report unexplained muscle pain immediately (risk of rhabdomyolysis).', 'Increased risk of myopathy with gemfibrozil, cyclosporine. Grapefruit juice may increase levels.'),
(5, 'Hypertension, heart failure, post-myocardial infarction', '10-40mg once daily', 'Angioedema history with ACE inhibitors, pregnancy', 'Dry cough, dizziness, headache, hyperkalemia', 'May cause first-dose hypotension. Monitor potassium and renal function.', 'NSAIDs may reduce effectiveness. Potassium supplements increase hyperkalemia risk.'),
(8, 'Thromboembolic disorders, atrial fibrillation', 'Individualized based on INR target (usually 2-3)', 'Active bleeding, severe hypertension, pregnancy (especially 1st trimester)', 'Bleeding, bruising, nausea', 'Requires regular INR monitoring. Numerous drug and food interactions.', 'Interacts with many medications affecting metabolism. Vitamin K antagonizes effect.'),
(9, 'Hypothyroidism', '50-200mcg once daily on empty stomach', 'Uncorrected adrenal insufficiency, acute MI', 'Palpitations, tremor, headache, insomnia (usually indicates over-replacement)', 'Take on empty stomach. Do not switch brands without consulting physician.', 'Reduced absorption with calcium, iron, antacids. May increase warfarin effect.');

-- Insert sample drug interactions
INSERT INTO drug_interactions (substance_id_a, substance_id_b, severity_id, interaction_type, description, clinical_effects, mechanism, management) VALUES
(4, 1, 2, 'PHARMACOKINETIC', 'Warfarin-Metformin interaction', 'Metformin may enhance anticoagulant effect of warfarin', 'Metformin may alter gut flora affecting vitamin K production', 'Monitor INR more frequently when starting or stopping metformin'),
(4, 2, 3, 'PHARMACODYNAMIC', 'Warfarin-Atorvastatin interaction', 'Atorvastatin may enhance anticoagulant effect of warfarin', 'Atorvastatin inhibits CYP2C9 metabolism of warfarin', 'Monitor INR closely. Consider dose adjustment of warfarin.'),
(4, 9, 2, 'PHARMACODYNAMIC', 'Warfarin-Ibuprofen interaction', 'NSAIDs increase bleeding risk with warfarin', 'NSAIDs inhibit platelet function and may cause gastric erosions', 'Avoid combination if possible. Use paracetamol instead. If necessary, monitor INR closely.'),
(2, 10, 2, 'PHARMACOKINETIC', 'Atorvastatin-Simvastatin duplication', 'Both are statins - duplication of therapy', 'Same mechanism of action', 'Do not use together - choose one statin'),
(3, 9, 2, 'PHARMACODYNAMIC', 'Lisinopril-Ibuprofen interaction', 'NSAIDs may reduce antihypertensive effect of ACE inhibitors', 'NSAIDs cause sodium retention and reduce renal prostaglandin synthesis', 'Monitor blood pressure. Consider alternative analgesic.');

-- Insert test patients (these would normally come from user registration)
INSERT INTO patients (id, user_id, encrypted_ssn, first_name, last_name, date_of_birth, gender, email, phone, address_line1, postal_code, city, allergies, chronic_conditions, weight_kg, height_cm, preferred_pharmacy_id) VALUES
('patient-001', 'patient1', 'ENCRYPTED_19800512-XXXX', 'Erik', 'Andersson', '1980-05-12', 'M', 'erik.andersson@example.com', '070-111 11 11', 'Storgatan 10', '11122', 'Stockholm', 'Penicillin', 'Type 2 Diabetes, Hypertension, Hyperlipidemia', 85.5, 178, 1),
('patient-002', 'patient2', 'ENCRYPTED_19750823-XXXX', 'Karin', 'Lundqvist', '1975-08-23', 'F', 'karin.lundqvist@example.com', '070-222 22 22', 'Vasagatan 5', '11120', 'Stockholm', NULL, 'Hypothyroidism', 68.0, 165, 2),
('patient-003', 'patient3', 'ENCRYPTED_19920315-XXXX', 'Johan', 'Bergström', '1992-03-15', 'M', 'johan.bergstrom@example.com', '070-333 33 33', 'Kungsgatan 25', '75321', 'Uppsala', 'Sulfa drugs', NULL, 75.0, 182, 3);

-- Insert sample prescriptions
INSERT INTO prescriptions (patient_id, medication_id, prescriber_id, prescription_number, status, dose, dose_unit, frequency, frequency_description, route, prescribed_date, start_date, end_date, refills_allowed, refills_remaining, quantity_prescribed, quantity_unit, days_supply, indication, instructions, is_prn, is_substitution_allowed) VALUES
('patient-001', 1, 1, 'RX-2025-001001', 'ACTIVE', 1000, 'mg', 'TID', 'Three times daily with meals', 'Oral', '2024-09-15', '2024-09-15', '2025-09-15', 11, 11, 180, 'tablets', 30, 'Type 2 Diabetes Mellitus', 'Take 2 tablets (500mg each) three times daily with meals. Take with food to reduce stomach upset.', FALSE, TRUE),
('patient-001', 3, 1, 'RX-2025-001002', 'ACTIVE', 20, 'mg', 'QD', 'Once daily in the evening', 'Oral', '2024-07-20', '2024-07-20', '2025-07-20', 11, 10, 90, 'tablets', 90, 'Hyperlipidemia', 'Take 1 tablet every evening. Report any unexplained muscle pain immediately.', FALSE, TRUE),
('patient-001', 5, 2, 'RX-2025-001003', 'ACTIVE', 10, 'mg', 'QD', 'Once daily in the morning', 'Oral', '2024-08-01', '2024-08-01', '2025-08-01', 11, 11, 90, 'tablets', 90, 'Hypertension', 'Take 1 tablet every morning. Monitor blood pressure regularly.', FALSE, TRUE),
('patient-001', 8, 2, 'RX-2025-001004', 'ACTIVE', 5, 'mg', 'QD', 'Once daily at the same time', 'Oral', '2024-12-10', '2024-12-10', '2025-12-10', 11, 9, 90, 'tablets', 90, 'Atrial fibrillation (anticoagulation)', 'Take 1 tablet daily at the same time. INR monitoring required. Avoid major dietary changes in vitamin K intake.', FALSE, FALSE),
('patient-002', 9, 3, 'RX-2025-002001', 'ACTIVE', 50, 'mcg', 'QD', 'Once daily on empty stomach', 'Oral', '2024-06-01', '2024-06-01', '2025-06-01', 11, 8, 100, 'tablets', 100, 'Hypothyroidism', 'Take 1 tablet every morning 30 minutes before breakfast. Do not take with calcium or iron supplements.', FALSE, FALSE),
('patient-003', 10, 4, 'RX-2025-003001', 'ACTIVE', 20, 'mg', 'QD', 'Once daily before breakfast', 'Oral', '2024-10-01', '2024-10-01', '2025-04-01', 2, 2, 90, 'capsules', 90, 'Gastroesophageal reflux disease', 'Take 1 capsule every morning before breakfast. Swallow whole, do not chew.', FALSE, TRUE);

-- Insert dosing schedules for some prescriptions
INSERT INTO dosing_schedule (prescription_id, time_of_day, dose, dose_unit, instructions, ordinal, day_of_week) VALUES
(1, '08:00:00', 1000, 'mg', 'Take with breakfast', 1, 'DAILY'),
(1, '12:00:00', 1000, 'mg', 'Take with lunch', 2, 'DAILY'),
(1, '20:00:00', 1000, 'mg', 'Take with dinner', 3, 'DAILY'),
(2, '20:00:00', 20, 'mg', 'Take in the evening', 1, 'DAILY'),
(3, '08:00:00', 10, 'mg', 'Take in the morning', 1, 'DAILY'),
(4, '20:00:00', 5, 'mg', 'Take at the same time each day', 1, 'DAILY'),
(5, '07:00:00', 50, 'mcg', 'Take on empty stomach', 1, 'DAILY'),
(6, '08:00:00', 20, 'mg', 'Take before breakfast', 1, 'DAILY');

-- Insert some sample adherence records
INSERT INTO adherence_records (prescription_id, patient_id, scheduled_time, actual_time, status, source) VALUES
(1, 'patient-001', '2025-10-10 08:00:00', '2025-10-10 08:05:00', 'TAKEN', 'PATIENT_REPORTED'),
(1, 'patient-001', '2025-10-10 12:00:00', NULL, 'MISSED', 'PATIENT_REPORTED'),
(1, 'patient-001', '2025-10-10 20:00:00', '2025-10-10 20:10:00', 'TAKEN', 'PATIENT_REPORTED'),
(2, 'patient-001', '2025-10-10 20:00:00', '2025-10-10 20:15:00', 'TAKEN', 'PATIENT_REPORTED'),
(3, 'patient-001', '2025-10-10 08:00:00', '2025-10-10 08:00:00', 'TAKEN', 'PATIENT_REPORTED'),
(5, 'patient-002', '2025-10-10 07:00:00', '2025-10-10 07:05:00', 'TAKEN', 'PATIENT_REPORTED');

-- Insert sample dispense events
INSERT INTO dispense_events (prescription_id, pharmacy_id, pharmacist_id, medication_id, quantity_dispensed, lot_number, expiration_date, patient_cost, reimbursement_amount, counseling_provided, verification_method) VALUES
(1, 1, 1, 1, 180, 'LOT-ABC123', '2027-08-15', 95.50, 105.00, TRUE, 'PERSONNUMMER'),
(2, 1, 1, 3, 90, 'LOT-DEF456', '2027-06-20', 145.00, 155.00, TRUE, 'PERSONNUMMER'),
(5, 2, 2, 9, 100, 'LOT-GHI789', '2027-12-10', 58.00, 68.00, TRUE, 'PERSONNUMMER');

-- Insert interaction alerts for patient-001 (has multiple medications)
INSERT INTO interaction_alerts (patient_id, prescription_id_a, prescription_id_b, interaction_id, status, is_acknowledged, acknowledged_by, acknowledged_at) VALUES
('patient-001', 4, 1, 1, 'ACKNOWLEDGED', TRUE, 'prescriber2', '2024-12-11 10:00:00'),
('patient-001', 4, 2, 2, 'ACTIVE', FALSE, NULL, NULL);
