-- Sample medication data for development/testing
-- Don't specify IDs - let them auto-generate to avoid conflicts with test data
INSERT INTO medications (npl_id, trade_name, generic_name, form, strength, route, atc_code, rx_status, is_available, price, created_at, updated_at) VALUES
  ('NPL001', 'Alimemazin Evolan', 'Alimemazin', 'Tablett', '10 mg', 'Oral', 'R06AD01', 'Rx', true, 125.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('NPL002', 'Elvanse', 'Lisdexamfetamin', 'Kapsel, hård', '30 mg', 'Oral', 'N06BA12', 'Rx', true, 450.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('NPL003', 'Melatonin Orifarm', 'Melatonin', 'Tablett', '3 mg', 'Oral', 'N05CH01', 'OTC', true, 89.90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
