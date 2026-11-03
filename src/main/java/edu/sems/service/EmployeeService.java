package edu.sems.service;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.domain.Employee;
import edu.sems.domain.EmployeeStatus;
import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class EmployeeService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,50}");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final EmployeeRepository employees;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public EmployeeService(
            EmployeeRepository employees,
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.employees = employees;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employees.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Employee findById(long id) {
        return employees.findById(id)
                .orElseThrow(() -> new EmployeeValidationException("employee", "Employee not found."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Employee create(
            String employeeNumber,
            String firstName,
            String lastName,
            String email,
            String jobTitle,
            String username,
            String password,
            Role role) {
        String cleanNumber = required(employeeNumber, "employeeNumber", "Employee number", 50);
        String cleanFirstName = required(firstName, "firstName", "First name", 50);
        String cleanLastName = required(lastName, "lastName", "Last name", 50);
        String cleanEmail = validEmail(email);
        String cleanJobTitle = required(jobTitle, "jobTitle", "Job title", 80);
        String cleanUsername = validUsername(username);
        validPassword(password);
        if (role == null) {
            throw new EmployeeValidationException("role", "Role is required.");
        }
        if (employees.existsByEmployeeNumberIgnoreCase(cleanNumber)) {
            throw new EmployeeValidationException("employeeNumber", "Employee number is already in use.");
        }
        if (employees.existsByEmailIgnoreCase(cleanEmail)) {
            throw new EmployeeValidationException("email", "Email is already in use.");
        }
        if (users.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new EmployeeValidationException("username", "Username is already in use.");
        }

        Employee employee = employees.save(
                new Employee(cleanNumber, cleanFirstName, cleanLastName, cleanEmail, cleanJobTitle));
        users.save(new UserAccount(
                cleanUsername,
                passwordEncoder.encode(password),
                role,
                true,
                employee));
        auditService.recordCurrentUser(
                AuditEventType.EMPLOYEE,
                AuditAction.CREATE,
                AuditOutcome.SUCCESS,
                "Created employee " + employee.getEmployeeNumber());
        return employee;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Employee update(long id, String firstName, String lastName, String email, String jobTitle) {
        Employee employee = findById(id);
        String cleanFirstName = required(firstName, "firstName", "First name", 50);
        String cleanLastName = required(lastName, "lastName", "Last name", 50);
        String cleanEmail = validEmail(email);
        String cleanJobTitle = required(jobTitle, "jobTitle", "Job title", 80);
        if (employees.existsByEmailIgnoreCaseAndIdNot(cleanEmail, id)) {
            throw new EmployeeValidationException("email", "Email is already in use.");
        }
        employee.update(cleanFirstName, cleanLastName, cleanEmail, cleanJobTitle);
        auditService.recordCurrentUser(
                AuditEventType.EMPLOYEE,
                AuditAction.UPDATE,
                AuditOutcome.SUCCESS,
                "Updated employee " + employee.getEmployeeNumber());
        return employee;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deactivate(long id) {
        Employee employee = findById(id);
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            return;
        }
        UserAccount user = users.findByEmployeeId(id)
                .orElseThrow(() -> new EmployeeValidationException("employee", "Employee account not found."));
        employee.deactivate();
        user.deactivate();
        auditService.recordCurrentUser(
                AuditEventType.EMPLOYEE,
                AuditAction.DEACTIVATE,
                AuditOutcome.SUCCESS,
                "Deactivated employee " + employee.getEmployeeNumber());
    }

    private static String required(String value, String field, String label, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw new EmployeeValidationException(field, label + " is required.");
        }
        if (clean.length() > maxLength) {
            throw new EmployeeValidationException(field, label + " must be " + maxLength + " characters or fewer.");
        }
        return clean;
    }

    private static String validEmail(String email) {
        String clean = required(email, "email", "Email", 100).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(clean).matches()) {
            throw new EmployeeValidationException("email", "Enter a valid email address.");
        }
        return clean;
    }

    private static String validUsername(String username) {
        String clean = username == null ? "" : username.trim();
        if (!USERNAME.matcher(clean).matches()) {
            throw new EmployeeValidationException(
                    "username",
                    "Username must be 3–50 letters, numbers, or underscores.");
        }
        return clean;
    }

    private static void validPassword(String password) {
        if (password == null
                || password.length() < 10
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new EmployeeValidationException(
                    "password",
                    "Password must be at least 10 characters and include a letter and a number.");
        }
    }
}
