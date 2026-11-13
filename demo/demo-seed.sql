-- Demo data for a fictional Technology Department.
-- Fresh database only: load src/main/resources/schema.sql first, then this file.
-- Shared password for every login: sems-demo-1
-- Hash copied from src/main/resources/data-dev.sql. Do not replace it.
-- There is no hire_date column. created_at is the hire timestamp (UTC).
-- There is no department column. The team is implied by job_title.
-- Leave request ids below are the ids assigned on a fresh database.

PRAGMA foreign_keys = ON;

BEGIN;

INSERT INTO employees (employee_number, first_name, last_name, email, job_title, status, created_at, updated_at)
VALUES
    ('E0001', 'Ada', 'Admin', 'admin@sems.local', 'Administrator', 'ACTIVE', '2026-11-02T00:00:00Z', '2026-11-02T00:00:00Z'),
    ('E0002', 'Max', 'Manager', 'manager@sems.local', 'Manager', 'ACTIVE', '2026-11-02T00:00:00Z', '2026-11-02T00:00:00Z'),
    ('E0003', 'Priya', 'Shah', 'priya.shah@sems.local', 'Director of Technology', 'ACTIVE', '2020-03-16T15:00:00Z', '2020-03-16T15:00:00Z'),
    ('E0004', 'Jordan', 'Hale', 'jordan.hale@sems.local', 'Senior Platform Engineer', 'ACTIVE', '2021-06-07T14:10:00Z', '2021-06-07T14:10:00Z'),
    ('E0005', 'Mei', 'Chen', 'mei.chen@sems.local', 'Platform Engineer', 'ACTIVE', '2022-01-18T14:00:00Z', '2022-01-18T14:00:00Z'),
    ('E0006', 'Omar', 'Farouk', 'omar.farouk@sems.local', 'Platform Site Reliability Engineer', 'ACTIVE', '2023-04-03T15:40:00Z', '2023-04-03T15:40:00Z'),
    ('E0007', 'Elena', 'Vasquez', 'elena.vasquez@sems.local', 'Security Engineer', 'ACTIVE', '2021-09-13T16:45:00Z', '2021-09-13T16:45:00Z'),
    ('E0008', 'Noah', 'Brooks', 'noah.brooks@sems.local', 'Security Analyst', 'ACTIVE', '2024-02-26T16:05:00Z', '2024-02-26T16:05:00Z'),
    ('E0009', 'Samira', 'Okonkwo', 'samira.okonkwo@sems.local', 'Help Desk Lead', 'ACTIVE', '2020-11-09T15:30:00Z', '2026-10-29T15:14:44Z'),
    ('E0010', 'Luis', 'Ortega', 'luis.ortega@sems.local', 'Help Desk Specialist', 'ACTIVE', '2023-08-21T14:25:00Z', '2023-08-21T14:25:00Z'),
    ('E0011', 'Hannah', 'Kim', 'hannah.kim@sems.local', 'Data Analyst', 'ACTIVE', '2022-05-02T13:15:00Z', '2022-05-02T13:15:00Z'),
    ('E0012', 'Caleb', 'Wright', 'caleb.wright@sems.local', 'Analytics Engineer', 'ACTIVE', '2024-07-15T15:55:00Z', '2024-07-15T15:55:00Z'),
    ('E0013', 'Aisha', 'Rahman', 'aisha.rahman@sems.local', 'QA Engineer', 'ACTIVE', '2021-12-01T15:20:00Z', '2021-12-01T15:20:00Z'),
    ('E0014', 'Benito', 'Alvarez', 'benito.alvarez@sems.local', 'QA Analyst', 'ACTIVE', '2026-11-02T16:48:33Z', '2026-11-02T16:48:33Z'),
    ('E0015', 'Riley', 'Nguyen', 'riley.nguyen@sems.local', 'Junior Platform Engineer', 'ACTIVE', '2025-06-09T15:10:00Z', '2025-06-09T15:10:00Z'),
    ('E0016', 'Casey', 'Quinn', 'casey.quinn@sems.local', 'Help Desk Technician', 'ACTIVE', '2026-02-02T15:00:00Z', '2026-02-02T15:00:00Z');

INSERT INTO users (username, password_hash, role, active, employee_id, created_at, failed_attempts, locked_until)
VALUES
    ('admin', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'ADMIN', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0001'), '2026-11-02T00:00:00Z', 0, NULL),
    ('manager', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'MANAGER', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0002'), '2026-11-02T00:00:00Z', 0, NULL),
    ('pshah', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'MANAGER', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0003'), '2020-03-16T15:30:00Z', 0, NULL),
    ('jhale', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0004'), '2021-06-07T14:20:00Z', 0, NULL),
    ('mchen', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0005'), '2022-01-18T14:15:00Z', 0, NULL),
    ('evasquez', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0007'), '2021-09-13T16:55:00Z', 0, NULL),
    ('sokonkwo', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0009'), '2020-11-09T15:45:00Z', 0, NULL),
    ('hkim', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0011'), '2022-05-02T13:30:00Z', 0, NULL),
    ('arahman', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0013'), '2021-12-01T15:35:00Z', 0, NULL),
    ('demo.lockout', '$2a$10$B5JLloocgxPFnqIqtvxMOuqwMaAcTmX870LqAyhU7qSAsvydrY9xe', 'EMPLOYEE', 1,
        (SELECT id FROM employees WHERE employee_number = 'E0016'), '2026-02-02T15:30:00Z', 0, NULL);

-- Pending rows have no reviewer. Approved and rejected rows name one (schema CHECK).
INSERT INTO leave_requests (
    employee_id, start_date, end_date, reason, status, reviewer_user_id, reviewed_at, created_at)
VALUES
    ((SELECT id FROM employees WHERE employee_number = 'E0005'),
        '2026-11-24', '2026-11-26',
        'Travel for a family gathering over Thanksgiving.',
        'PENDING', NULL, NULL, '2026-11-10T16:02:11Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0007'),
        '2026-11-18', '2026-11-19',
        'Dental appointment and a day to recover.',
        'PENDING', NULL, NULL, '2026-11-11T13:22:40Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0011'),
        '2026-12-22', '2026-12-31',
        'Year-end leave with family.',
        'PENDING', NULL, NULL, '2026-11-08T17:30:15Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0003'),
        '2027-01-04', '2027-01-08',
        'Planned personal trip. Another manager needs to review this.',
        'PENDING', NULL, NULL, '2026-11-09T15:11:08Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0010'),
        '2026-10-12', '2026-10-16',
        'Out of town for a sibling wedding.',
        'APPROVED',
        (SELECT id FROM users WHERE username = 'manager'),
        '2026-10-01T16:30:00Z', '2026-09-28T14:00:00Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0009'),
        '2026-11-06', '2026-11-06',
        'School conference for my child.',
        'APPROVED',
        (SELECT id FROM users WHERE username = 'manager'),
        '2026-11-03T14:18:09Z', '2026-11-02T18:12:00Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0012'),
        '2026-12-01', '2026-12-02',
        'Moving to a new apartment.',
        'APPROVED',
        (SELECT id FROM users WHERE username = 'pshah'),
        '2026-11-05T18:19:04Z', '2026-11-04T15:40:00Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0006'),
        '2026-11-27', '2026-11-28',
        'Long weekend after the Thanksgiving on-call rotation.',
        'REJECTED',
        (SELECT id FROM users WHERE username = 'manager'),
        '2026-11-11T15:16:44Z', '2026-11-10T14:05:00Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0008'),
        '2026-09-14', '2026-09-18',
        'Conference travel that overlapped a security audit.',
        'REJECTED',
        (SELECT id FROM users WHERE username = 'pshah'),
        '2026-09-08T17:12:00Z', '2026-09-02T15:00:00Z'),
    ((SELECT id FROM employees WHERE employee_number = 'E0013'),
        '2026-08-10', '2026-08-14',
        'Leave during the release regression window.',
        'REJECTED',
        (SELECT id FROM users WHERE username = 'manager'),
        '2026-08-04T16:45:00Z', '2026-08-01T14:20:00Z');

INSERT INTO audit_events (occurred_at, actor_user_id, actor_username, event_type, action, outcome, details)
VALUES
    ('2026-10-29T15:06:11Z', (SELECT id FROM users WHERE username = 'admin'), 'admin',
        'AUTHENTICATION', 'LOGIN', 'SUCCESS', 'Signed in'),
    ('2026-10-29T15:14:44Z', (SELECT id FROM users WHERE username = 'admin'), 'admin',
        'EMPLOYEE', 'UPDATE', 'SUCCESS', 'Updated employee E0009'),
    ('2026-10-29T15:21:02Z', (SELECT id FROM users WHERE username = 'admin'), 'admin',
        'AUTHENTICATION', 'LOGOUT', 'SUCCESS', 'Signed out'),
    ('2026-11-02T16:40:18Z', (SELECT id FROM users WHERE username = 'admin'), 'admin',
        'AUTHENTICATION', 'LOGIN', 'SUCCESS', 'Signed in'),
    ('2026-11-02T16:48:33Z', (SELECT id FROM users WHERE username = 'admin'), 'admin',
        'EMPLOYEE', 'CREATE', 'SUCCESS', 'Created employee E0014'),
    ('2026-11-03T14:05:33Z', (SELECT id FROM users WHERE username = 'manager'), 'manager',
        'AUTHENTICATION', 'LOGIN', 'SUCCESS', 'Signed in'),
    ('2026-11-03T14:18:09Z', (SELECT id FROM users WHERE username = 'manager'), 'manager',
        'LEAVE', 'APPROVE', 'SUCCESS', 'Approved leave request 6'),
    ('2026-11-03T14:26:40Z', (SELECT id FROM users WHERE username = 'manager'), 'manager',
        'AUTHENTICATION', 'LOGOUT', 'SUCCESS', 'Signed out'),
    ('2026-11-05T18:12:27Z', (SELECT id FROM users WHERE username = 'pshah'), 'pshah',
        'AUTHENTICATION', 'LOGIN', 'SUCCESS', 'Signed in'),
    ('2026-11-05T18:19:04Z', (SELECT id FROM users WHERE username = 'pshah'), 'pshah',
        'LEAVE', 'APPROVE', 'SUCCESS', 'Approved leave request 7'),
    ('2026-11-06T13:44:02Z', (SELECT id FROM users WHERE username = 'jhale'), 'jhale',
        'AUTHENTICATION', 'LOGIN_FAILED', 'FAILURE', 'Sign-in failed'),
    ('2026-11-08T17:30:15Z', (SELECT id FROM users WHERE username = 'hkim'), 'hkim',
        'LEAVE', 'SUBMIT', 'SUCCESS', 'Submitted leave request 3'),
    ('2026-11-10T19:41:20Z', (SELECT id FROM users WHERE username = 'jhale'), 'jhale',
        'AUTHORIZATION', 'ACCESS_DENIED', 'FAILURE', 'Denied GET /audit'),
    ('2026-11-11T15:16:44Z', (SELECT id FROM users WHERE username = 'manager'), 'manager',
        'LEAVE', 'REJECT', 'SUCCESS', 'Rejected leave request 8');

COMMIT;
