# Secure Employee Management System (SEMS)

SEMS is a security-focused learning project: it implements access control, authentication hardening, and audit logging, and documents its own threat model. It is a local Java web application for managing employee accounts and leave. The project runs from one codebase on macOS and Windows. Sign-in, role checks, administrator employee management, employee leave submission with manager decisions, and an administrator audit log are in place.

The working directory for every command below is the project root, the folder that contains `pom.xml`.

## Current status

- macOS: `./mvnw clean test` passed on November 11, 2026 with Temurin 25.0.5 (`Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`), including `LockoutTests`. The 16-step browser demonstration recorded here was completed on November 3, 2026 against a fresh database.
- Do not commit `data/sems.db`, anything else under `data/`, or `target/`. Those are local runtime and build files.
- Windows: unverified. A Windows 10 or 11 machine still needs to run the commands in the Windows section.

## Prerequisites

- JDK 25 (Eclipse Temurin 25 is the setup used here)
- Git
- Cursor or IntelliJ IDEA
- No separate Maven install. The Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.16 on the first build.

Java source, `schema.sql`, and configuration are the same on both operating systems. Do not add Windows-only or macOS-only application code.

## Windows setup

These steps are written for Windows 10 and 11 using PowerShell. They have not been run yet.

1. Install Eclipse Temurin 25 (Windows x64) from [Adoptium](https://adoptium.net/).
2. Set `JAVA_HOME` to the Temurin 25 directory, for example `C:\Program Files\Eclipse Adoptium\jdk-25.0.2-hotspot`. Add `%JAVA_HOME%\bin` to `Path`.
3. Open a new PowerShell window and check Java:

   ```powershell
   java -version
   echo $env:JAVA_HOME
   ```

   `java -version` should report version 25.

4. Install [Git for Windows](https://git-scm.com/download/win) and confirm `git --version` works in PowerShell.
5. Clone the repository and move into the project root:

   ```powershell
   git clone <repository-url>
   cd Secure-Employee-Management-System-SEMS-
   ```

6. Open that folder in Cursor, or in IntelliJ IDEA and import it as a Maven project. Set the project SDK to Java 25.
7. No environment variables are required. The database file defaults to `data\sems.db` under the project root. To use another file for the current PowerShell window only:

   ```powershell
   $env:SEMS_DB_PATH = "D:\sems-data\sems.db"
   ```

8. From the project root:

   ```powershell
   .\mvnw.cmd -version
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

9. Open `http://localhost:8080/login` in a browser.
10. Tests are `.\mvnw.cmd clean test`. The application does not need to be running.

## macOS setup

These steps cover Apple Silicon and Intel Macs. The shell is zsh. They were run on this machine with Temurin 25.

1. Install Temurin 25 with `brew install --cask temurin@25`, or install the macOS package from [Adoptium](https://adoptium.net/) that matches the chip.
2. Point `JAVA_HOME` at JDK 25. Add this to `~/.zshrc` if `java -version` does not already report 25:

   ```zsh
   export JAVA_HOME=$(/usr/libexec/java_home -v 25)
   export PATH="$JAVA_HOME/bin:$PATH"
   ```

   Open a new terminal and check:

   ```zsh
   java -version
   echo "$JAVA_HOME"
   ```

3. Install Git with Xcode Command Line Tools, or with `brew install git`. Confirm `git --version`.
4. Clone the repository and move into the project root:

   ```zsh
   git clone <repository-url>
   cd Secure-Employee-Management-System-SEMS-
   ```

5. If Git did not keep the executable bit on the wrapper, run `chmod +x mvnw` once.
6. Open the folder in Cursor or IntelliJ IDEA. Set the project SDK to Java 25.
7. No environment variables are required. The database file defaults to `data/sems.db` under the project root. To use another file for the current terminal only:

   ```zsh
   export SEMS_DB_PATH="$HOME/sems-data/sems.db"
   ```

8. From the project root:

   ```zsh
   ./mvnw -version
   ./mvnw clean test
   ./mvnw spring-boot:run
   ```

9. Open `http://localhost:8080/login` in a browser.
10. Tests are `./mvnw clean test`. The application does not need to be running.

## Local demo accounts

`./mvnw spring-boot:run` loads the `dev` profile and creates two accounts if they are not already in the local database. Both use the same local demo password: `sems-demo-1`.

| Username | Role |
| --- | --- |
| `admin` | Administrator |
| `manager` | Manager |

This password is for the local demo database only. Do not reuse it for any personal account, and do not replace it in Git with a password you use anywhere else. The database stores a BCrypt hash in `src/main/resources/data-dev.sql`, not the password itself. Tests do not use these accounts. They create their own users in a temporary database.

A browser request to `http://localhost:8080` redirects to `/login`. A wrong password shows "Invalid username or password." An inactive account shows that same message. After 5 wrong passwords in a row an account is locked for 15 minutes, and the page still shows that same message. After sign-in, the home page shows the username. Log out with the button in the header. An account that opens an administrator page without that role sees "You do not have access to that page." The audit log at `/audit` is administrator-only. It lists sign-in, employee changes, leave decisions, and denied requests, newest first. Times are shown in Central time. Passwords and session ids are not written there.

## Configuration

Settings live in `src/main/resources/application.properties`.

- Server port: `8080`
- Database URL: `jdbc:sqlite:${SEMS_DB_PATH:./data/sems.db}`
- The application creates the database parent directory on startup. Run Maven from the project root so `./data/sems.db` is created inside the project.
- Schema comes from `src/main/resources/schema.sql`. Hibernate does not create or alter tables.
- SQLite foreign keys are turned on for each connection with `PRAGMA foreign_keys=ON`.
- Local database files matching `data/*.db` are gitignored. Each developer has a private database.
- Sign-in lockout: `sems.lockout.max-attempts=5` and `sems.lockout.duration-minutes=15`.
- A database created before lockout was added lacks two `users` columns. Stop the app and run this once from the project root, or delete your local `sems.db`:

  ```zsh
  sqlite3 data/sems.db "ALTER TABLE users ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;"
  sqlite3 data/sems.db "ALTER TABLE users ADD COLUMN locked_until TEXT;"
  ```

  Running a line twice fails with "duplicate column name", which is harmless.
- If you change `schema.sql` during development, stop the app and delete your local `sems.db`, then start again. `CREATE TABLE IF NOT EXISTS` will not modify a table that already exists.
- `./mvnw spring-boot:run` turns on the `dev` profile, which loads `data-dev.sql`. Automated tests do not load that file.
- Do not commit `data/sems.db`, `target/`, or a `.env` file. Do not put a personal password in `data-dev.sql` or this README.

## Tests

Tests use a temporary SQLite file, not `data/sems.db`. They cover database startup and foreign-key enforcement, password hashing, valid and invalid sign-in, inactive accounts, role rules, employee creation and validation, updates, deactivation, leave submission and date rules, manager decisions, repeated decisions, self-review prevention, forbidden employee approval, sign-in lockout (lock after five wrong passwords, expiry, reset, and unknown or inactive accounts not counted), and the audit log. The audit tests check a successful sign-in, an employee update, a denied administrator page, newest-first ordering, and that the recorded row does not contain the password. Separate tests check that an audit time renders as Central Standard Time in winter and Central Daylight Time in summer. The Surefire summary is saved in `docs/testing/surefire-summary.txt`.

## Security testing

`./mvnw clean test` is the security test run. It checks the controls recorded in `THREAT_MODEL.md`: generic login errors, BCrypt password storage, inactive accounts, lockout after five failed sign-ins, role checks for the employee, audit, and leave-review URLs, self-review prevention, and audit rows that omit the password and session id. `EmployeeServiceTests` also rejects a backtracking-shaped email address within one second. Dependency versions for Tomcat, Jackson, Log4j, and SQLite JDBC are pinned in `pom.xml`. An OWASP Dependency-Check scan is separate from `./mvnw clean test` and uses a local NVD API key that is not committed.

## Layout

- `src/main/java/edu/sems/` — application code
- `src/main/resources/schema.sql` — tables `employees`, `users`, `leave_requests`, and `audit_events`
- `src/test/java/edu/sems/` — JUnit 5 tests
- `docs/diagrams/` — domain class diagram, application-layer class diagram, and runtime architecture
- `docs/testing/` — recorded Surefire test summary
- `THREAT_MODEL.md` — STRIDE threat model for this application
- `data/` — created at startup, not committed

The remaining work is presentation rehearsal.
