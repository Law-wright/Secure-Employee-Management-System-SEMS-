INSERT INTO employees (employee_number, first_name, last_name, email, job_title, status, created_at, updated_at)
SELECT 'E0001', 'Ada', 'Admin', 'admin@sems.local', 'Administrator', 'ACTIVE', '2026-11-02T00:00:00Z', '2026-11-02T00:00:00Z'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE employee_number = 'E0001');

INSERT INTO employees (employee_number, first_name, last_name, email, job_title, status, created_at, updated_at)
SELECT 'E0002', 'Max', 'Manager', 'manager@sems.local', 'Manager', 'ACTIVE', '2026-11-02T00:00:00Z', '2026-11-02T00:00:00Z'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE employee_number = 'E0002');

INSERT INTO users (username, password_hash, role, active, employee_id, created_at)
SELECT 'admin', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'ADMIN', 1, employees.id, '2026-11-02T00:00:00Z'
FROM employees
WHERE employees.employee_number = 'E0001'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password_hash, role, active, employee_id, created_at)
SELECT 'manager', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'MANAGER', 1, employees.id, '2026-11-02T00:00:00Z'
FROM employees
WHERE employees.employee_number = 'E0002'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'manager');
