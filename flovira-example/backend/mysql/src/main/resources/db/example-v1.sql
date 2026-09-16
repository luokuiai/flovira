CREATE TABLE IF NOT EXISTS example_user (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    organization_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS example_purchase_request (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    applicant_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    department VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO example_user (id, name, organization_id, role_id) VALUES
    ('alice', 'Alice Applicant', 'engineering', 'employee'),
    ('manager', 'Morgan Manager', 'engineering', 'manager'),
    ('finance', 'Frank Finance', 'finance', 'finance');

INSERT IGNORE INTO example_purchase_request (id, title, applicant_id, amount, department, status)
VALUES ('purchase-001', 'Developer laptops', 'alice', 24000.00, 'Engineering', 'DRAFT');
