# SEMS Threat Model

Method: STRIDE, with a NIST SP 800-53 Rev. 5 control id only where the code matches that control.
Last reviewed: November 9, 2026

The file in the repository was empty at the start of this review, so earlier row wording could not be recovered. Rows 1, 2, and 8 are the claims that were still visible in this file before it was cleared (generic login error, BCrypt storage, audit rows omit passwords and session ids). Rows 4 and 9 are the object-level and lockout items this review was asked to settle. Rows marked **new** were added from the code. No `[verify]` tags remain.

## Scope

SEMS is a local web application for employee accounts and leave. A browser talks to one Spring Boot process, which reads and writes a SQLite file on the same machine. There is no public registration, email, payroll, attendance, reporting API, or remote deployment in this codebase.

## Assets

- `users.password_hash`: BCrypt hash only. The plaintext password is not stored.
- Employee profile fields: name, email, job title, employee number, active or inactive status.
- Leave requests: dates, reason, status, reviewer.
- `audit_events`: who did what, when, and whether it succeeded. Details are short fixed phrases, ids, or a denied method and path.
- Session cookie `JSESSIONID`.

## Trust boundaries

- Browser to the application on `http://localhost:8080`. The connection is not TLS.
- Application to the SQLite file at `./data/sems.db`, or whatever `SEMS_DB_PATH` points to. Anyone who can read or write that file is outside the application's access checks.
- Roles are enforced in the application (`SecurityConfig` URL rules and `@PreAuthorize` on services). The database file itself has no separate login.

## Roles

`Role` is `ADMIN`, `MANAGER`, or `EMPLOYEE`.

- Administrator: employee create, view, update, and deactivate, and the audit log.
- Manager: leave review (approve or reject). A manager cannot review their own request.
- Employee: own profile, submit leave, and view their own leave. Employee pages require a signed-in user; they are not limited to the `EMPLOYEE` role, so an administrator or manager can open them too.

## Stack

From `pom.xml` and the November 9, 2026 test run:

- Java 25 (`java.version` is 25). The suite ran on JDK 25.0.2.
- Spring Boot 3.5.16 (web, Thymeleaf, Security, validation, data JPA).
- SQLite through `sqlite-jdbc` (the test process loaded 3.49.1.0) and the Hibernate community SQLite dialect.
- Schema from `schema.sql`. `spring.jpa.hibernate.ddl-auto=none`.
- Each connection runs `PRAGMA foreign_keys=ON`.

## Threats and mitigations

| # | Threat (STRIDE) | Example attack | Mitigation | NIST |
|---|---|---|---|---|
| 1 | Spoofing | Attacker guesses usernames from different login errors | Unknown user, wrong password, and inactive account all show "Invalid username or password." The login template does not echo the submitted password. Covered by `AuthenticationTests`. | IA-6 |
| 2 | Spoofing | Stolen password database is cracked offline | `BCryptPasswordEncoder` stores a salted BCrypt hash. `PasswordEncoderTest` checks that the hash matches the original password, rejects a different one, and does not contain the plaintext. New passwords must be at least 10 characters and include a letter and a number. | IA-5 |
| 3 | Tampering | **new.** A forged browser request creates or changes an employee or leave record | `SecurityConfig` does not disable CSRF. Spring Security's CSRF protection stays on. State-changing tests send a CSRF token. | SI-10 |
| 4 | Elevation of privilege | Employee reads or changes another person's record by id | `/profile` loads only `authentication.getName()`. "My leave" queries only that account's employee id. `/employees/**` requires `ADMIN` in `SecurityConfig` and on `EmployeeService`, so an employee has no employee-by-id page. Leave review requires `MANAGER`. `preventSelfReview` blocks a manager from approving or rejecting their own request. Any manager may decide any other pending request; there is no department or owner check on that queue. | AC-3 |
| 5 | Elevation of privilege | **new.** A signed-in employee opens an administrator or manager page | URL rules require `ADMIN` for `/employees/**` and `/audit/**`, and `MANAGER` for `/leave/review/**`. The same roles are repeated with `@PreAuthorize` on the employee, leave-review, and audit read methods. A denial is written to the audit log and forwarded to `/access-denied`. Covered by `RbacTests` and `LeaveRequestServiceTests`. | AC-6 |
| 6 | Spoofing | **new.** A deactivated account keeps signing in | `SemsUserDetailsService` sets the Spring Security user disabled when `users.active` is 0. Deactivate sets both the employee and the user inactive. The login page still shows the generic error. Covered by `AuthenticationTests.inactiveAccount_isRejected` and `EmployeeServiceTests.deactivateEmployee_disablesLogin`. | AC-2 |
| 7 | Repudiation | **new.** A user denies a sign-in, employee change, leave decision, or blocked request | Login success, login failure, and logout are recorded by `AuthenticationAuditListener`. Employee create, update, and deactivate, and leave submit, approve, and reject, call `AuditService`. `AuditAccessDeniedHandler` records the denied method and path. `/audit` is administrator-only. The list query is `order by occurredAt desc, id desc`. | AU-2 |
| 8 | Information disclosure | Secrets leak through the audit log | Listeners write fixed phrases such as "Signed in" or "Sign-in failed". They do not write the password, the password hash, or the session id. `AuditService` drops an anonymous name and cuts details at 500 characters. `AuditTests` checks that a successful login row does not contain the password or the hash. | AU-3 |
| 9 | Denial of service | Repeated password guesses lock out a real user, or go unthrottled | Not implemented. There is no attempt counter, lockout, or rate limiter under `src/main`. A failed sign-in is only written to the audit log as "Sign-in failed". | AC-7 not met |
| 10 | Information disclosure | **new.** Someone on the network reads the session or the form posts | The server listens on HTTP port 8080. Nothing in the configuration enables TLS. The session cookie is HttpOnly and SameSite=Lax. The Secure flag is not set. | SC-8 not met |
| 11 | Tampering | **new.** Someone with access to the computer edits `data/sems.db` | The database is a normal SQLite file. The application does not encrypt it or hash-chain audit rows. Foreign keys are turned on, which blocks orphaned rows, not file edits. | SC-28 not met |
| 12 | Information disclosure | **new.** An error response includes a stack trace or the rejected password | `server.error.include-stacktrace=never`. The login error page does not print the submitted password (`AuthenticationTests`). | SI-11 |
| 13 | Tampering | **new.** Stored name, title, or leave reason is rendered as HTML | Audit, employee, leave, and profile templates print values with `th:text`. There is no `th:utext` under `src/main`. | SI-10 |

## Known gaps

- No login rate limit or account lockout (row 9). Failed attempts are audited and not throttled.
- No object-level ownership beyond the signed-in user's profile and own leave list (row 4). Every manager can decide every other employee's pending request. There are no departments.
- No TLS, and the session cookie is not marked Secure (row 10). This matches a localhost demo and does not protect a shared network.
- The SQLite file is not encrypted, and audit rows are not tamper-evident (row 11). An administrator can read the audit log in the app; that does not stop someone who can open the file.
- `EmployeeService.deactivate` does not stop an administrator from deactivating their own account.
- The `dev` profile seeds `admin` and `manager` from `data-dev.sql`. The README documents the shared demo password. Tests do not use those accounts.
- There is no password-change or password-reset flow. An administrator sets the initial password at employee creation.
- Windows has not been run for this review. The commands are in the README and were not executed here.

These were checked and are not open gaps: generic login errors, BCrypt storage, inactive-account rejection, role checks on employee, audit, and leave-review URLs, self-review prevention, CSRF left enabled, and audit rows that omit the password and session id.

## Verification

`./mvnw test` on November 9, 2026, JDK 25.0.2: Tests run: 28, Failures: 0, Errors: 0, Skipped: 0.
