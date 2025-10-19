-- Add quantity_dispensed column to prescriptions table
ALTER TABLE prescriptions ADD COLUMN quantity_dispensed INT NOT NULL DEFAULT 0;