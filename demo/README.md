# Demo database

`demo/reset-demo.sh` rebuilds a local SQLite database for rehearsal. It does not change the schema or the application.

Stop SEMS first. The script refuses to run if something is listening on port 8080.

From the repository root:

```zsh
./demo/reset-demo.sh
```

If `data/sems.db` already exists, the script copies it to `data/sems-before-demo-reset-<timestamp>.db` and does not overwrite that backup. It then creates a new `data/sems.db` from `src/main/resources/schema.sql` and `demo/demo-seed.sql`, and prints row counts for employees, users, leave requests, and audit events.

`data/` is git-ignored. Commit the files in `demo/`, not the database.

Every seeded login uses the password `sems-demo-1`. The hash is the one already in `src/main/resources/data-dev.sql`. Lockout columns start at `failed_attempts = 0` and `locked_until` NULL, including `demo.lockout`.

| Username | Role | Person | Use |
| --- | --- | --- | --- |
| `admin` | ADMIN | Ada Admin | Employees and the audit log |
| `manager` | MANAGER | Max Manager | Leave review |
| `pshah` | MANAGER | Priya Shah, Director of Technology | Second manager. Her own January leave is pending, so she cannot approve it herself |
| `jhale` | EMPLOYEE | Jordan Hale | Employee sign-in |
| `mchen` | EMPLOYEE | Mei Chen | Pending Thanksgiving leave |
| `evasquez` | EMPLOYEE | Elena Vasquez | Pending dental leave |
| `sokonkwo` | EMPLOYEE | Samira Okonkwo | Approved leave |
| `hkim` | EMPLOYEE | Hannah Kim | Pending year-end leave |
| `arahman` | EMPLOYEE | Aisha Rahman | Rejected leave |
| `demo.lockout` | EMPLOYEE | Casey Quinn | Throwaway account for the 5-failure lockout. Do not use it for the rest of the demo |

Six people have no login: Omar Farouk, Noah Brooks, Luis Ortega, Caleb Wright, Benito Alvarez, and Riley Nguyen.

There is no hire-date or department column. Hire timing is `created_at` (UTC). Teams are only in the job title: Platform Engineering, Security, Help Desk, Data and Analytics, QA, and the department head.
