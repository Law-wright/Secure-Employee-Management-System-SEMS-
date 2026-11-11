package edu.sems.employee;

import edu.sems.domain.Employee;
import edu.sems.domain.EmployeeStatus;
import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.AuditEventRepository;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import edu.sems.service.EmployeeService;
import edu.sems.service.EmployeeValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class EmployeeServiceTests {

    private static final String INITIAL_PASSWORD = "employee-pass-1";

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("employee-tests.db");
        registry.add("SEMS_DB_PATH", () -> database.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        auditEvents.deleteAll();
        users.deleteAll();
        employees.deleteAll();
    }

    @Test
    void createEmployee_persistsEmployeeAndUser() {
        Employee employee = createEmployee("E2001", "new_employee", "new.employee@sems.test");

        UserAccount user = users.findByUsername("new_employee").orElseThrow();
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(user.getEmployee().getId()).isEqualTo(employee.getId());
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(user.getPasswordHash()).isNotEqualTo(INITIAL_PASSWORD);
        assertThat(passwordEncoder.matches(INITIAL_PASSWORD, user.getPasswordHash())).isTrue();
    }

    @Test
    void createEmployee_missingName_isRejected() {
        assertThatThrownBy(() -> employeeService.create(
                "E2002", " ", "Person", "person@sems.test", "Analyst",
                "person", INITIAL_PASSWORD, Role.EMPLOYEE))
                .isInstanceOf(EmployeeValidationException.class)
                .hasMessage("First name is required.");

        assertThat(employees.count()).isZero();
        assertThat(users.count()).isZero();
    }

    @Test
    void createEmployee_duplicateEmployeeNumber_isRejected() {
        createEmployee("E2003", "first_user", "first@sems.test");

        assertThatThrownBy(() -> employeeService.create(
                "e2003", "Second", "Person", "second@sems.test", "Analyst",
                "second_user", INITIAL_PASSWORD, Role.EMPLOYEE))
                .isInstanceOf(EmployeeValidationException.class)
                .hasMessage("Employee number is already in use.");

        assertThat(employees.count()).isEqualTo(1);
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void createEmployee_acceptsOrdinaryEmailAddresses() {
        createEmployee("E3001", "email_user_a", "first.last+tag@sub.example.com");
        createEmployee("E3002", "email_user_b", "a@b.co");

        assertThat(employees.count()).isEqualTo(2);
    }

    @Test
    void createEmployee_malformedEmail_isRejected() {
        String[] bad = {"a@b..com", "a@.com", "a@b.", "a b@c.com", "a@@b.com", "a@localhost"};
        int number = 3100;
        for (String email : bad) {
            String employeeNumber = "E" + number++;
            assertThatThrownBy(() -> employeeService.create(
                    employeeNumber, "Email", "Tester", email, "Analyst",
                    "tester_" + employeeNumber, INITIAL_PASSWORD, Role.EMPLOYEE))
                    .as(email)
                    .isInstanceOf(EmployeeValidationException.class)
                    .hasMessage("Enter a valid email address.");
        }
        assertThat(employees.count()).isZero();
    }

    @Test
    void createEmployee_regexBacktrackingEmail_isRejectedQuickly() {
        // 100 characters is the service's own limit; this shape made the old pattern backtrack quadratically.
        String hostile = "a@" + ".".repeat(97) + "@";

        assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                assertThatThrownBy(() -> employeeService.create(
                        "E3200", "Email", "Tester", hostile, "Analyst",
                        "tester_redos", INITIAL_PASSWORD, Role.EMPLOYEE))
                        .isInstanceOf(EmployeeValidationException.class)
                        .hasMessage("Enter a valid email address."));
    }

    @Test
    void updateEmployee_changesJobTitle() {
        Employee employee = createEmployee("E2004", "update_user", "update@sems.test");
        String originalUpdatedAt = employee.getUpdatedAt();

        Employee updated = employeeService.update(
                employee.getId(), "New", "Employee", "update@sems.test", "Senior Analyst");

        assertThat(updated.getJobTitle()).isEqualTo("Senior Analyst");
        assertThat(updated.getUpdatedAt()).isNotEqualTo(originalUpdatedAt);
    }

    @Test
    void deactivateEmployee_disablesLogin() throws Exception {
        Employee employee = createEmployee("E2005", "departing_user", "departing@sems.test");

        employeeService.deactivate(employee.getId());

        assertThat(employees.findById(employee.getId()).orElseThrow().getStatus())
                .isEqualTo(EmployeeStatus.INACTIVE);
        assertThat(users.findByUsername("departing_user").orElseThrow().isActive()).isFalse();
        mockMvc.perform(post("/login")
                        .param("username", "departing_user")
                        .param("password", INITIAL_PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    private Employee createEmployee(String employeeNumber, String username, String email) {
        return employeeService.create(
                employeeNumber,
                "New",
                "Employee",
                email,
                "Analyst",
                username,
                INITIAL_PASSWORD,
                Role.EMPLOYEE);
    }
}
