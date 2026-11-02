CREATE TABLE IF NOT EXISTS employees (
    id INTEGER PRIMARY KEY,
    employee_number TEXT NOT NULL UNIQUE,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    job_title TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE')),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    employee_id INTEGER NOT NULL UNIQUE,
    created_at TEXT NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS leave_requests (
    id INTEGER PRIMARY KEY,
    employee_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewer_user_id INTEGER,
    reviewed_at TEXT,
    created_at TEXT NOT NULL,
    CHECK (end_date >= start_date),
    CHECK (status = 'PENDING' OR reviewer_user_id IS NOT NULL),
    FOREIGN KEY (employee_id) REFERENCES employees (id),
    FOREIGN KEY (reviewer_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS audit_events (
    id INTEGER PRIMARY KEY,
    occurred_at TEXT NOT NULL,
    actor_user_id INTEGER,
    actor_username TEXT,
    event_type TEXT NOT NULL CHECK (event_type IN ('AUTHENTICATION', 'AUTHORIZATION', 'EMPLOYEE', 'LEAVE')),
    action TEXT NOT NULL CHECK (action IN (
        'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'CREATE', 'UPDATE', 'DEACTIVATE',
        'SUBMIT', 'APPROVE', 'REJECT', 'ACCESS_DENIED'
    )),
    outcome TEXT NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    details TEXT,
    FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_leave_requests_employee_id ON leave_requests (employee_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_status ON leave_requests (status);
CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_at ON audit_events (occurred_at);
