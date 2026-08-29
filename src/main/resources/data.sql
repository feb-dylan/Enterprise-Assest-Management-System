-- 1. Insert Categories (Using unique IDs 1, 2, and 3)
INSERT INTO categories (id, name, description, created_at, updated_at)
VALUES
    (1, 'Laptops', 'Company laptops including Lenovo, ASUS, and Dell models', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Desktop PCs', 'Office desktop towers and workstation PCs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Network & Peripherals', 'Routers, switches, and external accessories', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Insert Laptops (Linked to Category ID 1)
INSERT INTO assets (asset_code, name, serial_number, status, category_id, created_at, updated_at)
VALUES
    ('AST-LAP-001', 'Lenovo ThinkPad X1 Carbon', 'LNV-TP-2026-001', 'AVAILABLE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AST-LAP-002', 'ASUS ROG Zephyrus G14', 'ASUS-ROG-2026-002', 'AVAILABLE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AST-LAP-003', 'Dell XPS 15', 'DELL-XPS-2026-003', 'ASSIGNED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Insert Desktop PCs (Linked to Category ID 2)
INSERT INTO assets (asset_code, name, serial_number, status, category_id, created_at, updated_at)
VALUES
    ('AST-PC-001', 'Dell OptiPlex 7090 Tower', 'DELL-OPT-2026-010', 'AVAILABLE', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AST-PC-002', 'HP EliteDesk 800 G6', 'HP-ELT-2026-011', 'ASSIGNED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AST-PC-003', 'Lenovo ThinkCentre M70q', 'LNV-TC-2026-012', 'AVAILABLE', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. Insert Network & Peripherals (Linked to Category ID 3)
INSERT INTO assets (asset_code, name, serial_number, status, category_id, created_at, updated_at)
VALUES
    ('AST-NET-001', 'Cisco Meraki Wi-Fi 6 Router', 'CSCO-RTR-9901', 'AVAILABLE', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AST-NET-002', 'TP-Link 24-Port Gigabit Switch', 'TPL-SW-8802', 'AVAILABLE', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('AST-NET-003', 'Logitech MX Master 3S Mouse', 'LOGI-MS-7703', 'ASSIGNED', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);