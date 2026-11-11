# SEMS Threat Model

Method: STRIDE, with a NIST SP 800-53 Rev. 5 control id only where the code matches that control.
Last reviewed: November 11, 2026 (dependency findings added after OWASP Dependency-Check)

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

- Java 25 (`java.version` is 25). The suite ran on JDK 25.0.5.
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
| 9 | Denial of service | Repeated password guesses go unthrottled, or are used to lock out a real user | `LoginAttemptService` counts wrong passwords per active account. After 5 in a row (`sems.lockout.max-attempts`) the account is locked for 15 minutes (`sems.lockout.duration-minutes`), and the lock ends on its own. A correct password is refused during a lock, attempts during a lock are not counted and do not extend it, and a successful sign-in or an expired lock resets the count. Unknown usernames and inactive accounts are not counted. The login page shows the same generic message in every case. The lock and each refused attempt are written to the audit log. `LockoutTests` covers this. | AC-7 |
| 10 | Information disclosure | **new.** Someone on the network reads the session or the form posts | The server listens on HTTP port 8080. Nothing in the configuration enables TLS. The session cookie is HttpOnly and SameSite=Lax. The Secure flag is not set. | SC-8 not met |
| 11 | Tampering | **new.** Someone with access to the computer edits `data/sems.db` | The database is a normal SQLite file. The application does not encrypt it or hash-chain audit rows. Foreign keys are turned on, which blocks orphaned rows, not file edits. | SC-28 not met |
| 12 | Information disclosure | **new.** An error response includes a stack trace or the rejected password | `server.error.include-stacktrace=never`. The login error page does not print the submitted password (`AuthenticationTests`). | SI-11 |
| 13 | Tampering | **new.** Stored name, title, or leave reason is rendered as HTML | Audit, employee, leave, and profile templates print values with `th:text`. There is no `th:utext` under `src/main`. | SI-10 |

## Dependency findings (OWASP Dependency-Check, November 11, 2026)

Spring Boot 3.5.16 reached end of its free support window on June 30, 2026, so some managed dependencies no longer get free patch releases. A first scan reported 61 CVEs. Version overrides in `pom.xml` for Tomcat (10.1.60), Jackson (2.21.7), Log4j (2.25.5) and sqlite-jdbc (3.53.2.1) cut that to 34. Overriding Spring Framework to 6.2.20 and Spring Security to 6.5.12 was tried and could not be resolved, so those versions are not available to this build.

"Not used" means a search of `src/main` found no use of the affected feature. Each row was checked against the code, not only against the CVE title. SEMS is a local application, so no row is rated above low residual risk, but this is a judgment about this deployment and not a statement that the CVEs are harmless.

| Component | CVEs | Affected feature | Used by SEMS? | Decision |
| --- | --- | --- | --- | --- |
| spring-core 6.2.19 | 14 CVEs (5 critical, 5 high, 3 medium, 1 low) | WebFlux, RSocket, Jetty 12 reactive adapter, XSLT views, FreeMarker, functional endpoints, server-sent events, WebSocket, multipart parts, Aalto XML, UrlHandlerFilter, UrlFileNameViewController, Content-Disposition from untrusted input, user-supplied SpEL | Not used. SEMS uses Spring MVC, Thymeleaf, and Tomcat only. | Accepted. Not applicable. |
| spring-core 6.2.19 | CVE-2026-59282 (high) | Data binding applies user-supplied property paths, causing denial of service | Partly. Forms bind through `@ModelAttribute` to fixed form classes. | Accepted. Impact is availability only, on a local app. |
| spring-core 6.2.19 | CVE-2026-59281 (medium) | XSS when rendering `Errors.getFieldErrors()` with no arguments | Not used. Templates use `th:errors`, and no code calls `getFieldError`. | Accepted. Not applicable. |
| spring-core 6.2.19 | CVE-2026-59283 (critical) | SpEL `SimpleEvaluationContext` guard bypass with the SpEL compiler | Not used. No SpEL is evaluated from user input. | Accepted. Not applicable. |
| spring-security-core 6.5.11 | CVE-2026-59270 (critical), 41707, 47841, 47842 | Embedded LDAP server, DPoP proofs, WebAuthn, `AesBytesEncryptor` | Not used. | Accepted. Not applicable. |
| spring-security-core 6.5.11 | CVE-2026-59276 (medium) | Security-sensitive values compared without constant time | Possibly, inside the framework. Exploiting it needs many timed requests. | Accepted. Low residual risk on a local app. |
| spring-data-jpa 3.5.13 | CVE-2026-47834 (medium) | Sort validation bypass when sort parameters come from untrusted input | Not exposed. Queries use fixed sort orders, and no controller passes a user-supplied sort. `findPage(Pageable)` exists but is called only from tests. | Accepted. Do not pass user-supplied sort parameters to it. |
| sqlite-jdbc 3.53.2.1 | 11 CVEs, fixed in SQLite 3.54.0 or later | FTS5, FTS3/4, R-Tree and session extensions, stack exhaustion from CTE nesting, command-line shell | The schema has no virtual tables. Queries are static JPQL with bound parameters. An attacker would need to supply SQL or a crafted database file. The shell issue does not affect a JDBC driver. | Accepted. Upgrade when a driver bundling SQLite 3.54.0 or later is available. |

Long-term fix: migrate to a supported Spring Boot line (4.x), which moves Spring Framework, Spring Security and Spring Data to patched releases. This is a larger change because the Hibernate SQLite dialect and the Thymeleaf integration must be rechecked.

## Known gaps

- The application runs on Spring Boot 3.5, which is past its free support window. 34 dependency CVEs remain open and are assessed in the dependency findings above. A migration to a supported line is the long-term fix.
- Lockout is per account, not per address (row 9). Someone who knows a username can keep that account locked by repeating failed sign-ins every 15 minutes. There is no administrator unlock.
- No object-level ownership beyond the signed-in user's profile and own leave list (row 4). Every manager can decide every other employee's pending request. There are no departments.
- No TLS, and the session cookie is not marked Secure (row 10). This matches a localhost demo and does not protect a shared network.
- The SQLite file is not encrypted, and audit rows are not tamper-evident (row 11). An administrator can read the audit log in the app; that does not stop someone who can open the file.
- `EmployeeService.deactivate` does not stop an administrator from deactivating their own account.
- The `dev` profile seeds `admin` and `manager` from `data-dev.sql`. The README documents the shared demo password. Tests do not use those accounts.
- There is no password-change or password-reset flow. An administrator sets the initial password at employee creation.
- Windows has not been run for this review. The commands are in the README and were not executed here.

These were checked and are not open gaps: generic login errors, BCrypt storage, inactive-account rejection, role checks on employee, audit, and leave-review URLs, self-review prevention, CSRF left enabled, and audit rows that omit the password and session id.

## Verification

`./mvnw clean test` on November 10, 2026, JDK 25.0.5: Tests run: 38, Failures: 0, Errors: 0, Skipped: 0.
