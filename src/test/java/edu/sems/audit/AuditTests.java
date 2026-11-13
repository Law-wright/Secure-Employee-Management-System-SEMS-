package edu.sems.audit;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEvent;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.AuditEventRepository;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import edu.sems.service.AuditService;
import edu.sems.service.EmployeeService;
import edu.sems.support.AccountFixtures;
import edu.sems.web.AuditTimestamps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("audit-tests.db");
        registry.add("SEMS_DB_PATH", () -> database.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        auditEvents.deleteAll();
        users.deleteAll();
        employees.deleteAll();
    }

    @Test
    void successfulLogin_writesAuditWithoutPassword() throws Exception {
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E5001", "ada", Role.EMPLOYEE, true);

        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", "ada")
                        .param("password", AccountFixtures.PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String sessionId = result.getRequest().getSession(false).getId();
        String passwordHash = users.findByUsername("ada").orElseThrow().getPasswordHash();
        List<AuditEvent> logins = auditEvents.findAll().stream()
                .filter(event -> event.getAction() == AuditAction.LOGIN && event.getOutcome() == AuditOutcome.SUCCESS)
                .toList();

        assertThat(logins).hasSize(1);
        AuditEvent login = logins.get(0);
        assertThat(login.getEventType()).isEqualTo(AuditEventType.AUTHENTICATION);
        assertThat(login.getActorUsername()).isEqualTo("ada");
        assertThat(login.getDetails()).isEqualTo("Signed in");
        assertThat(rowText(login))
                .doesNotContain(AccountFixtures.PASSWORD)
                .doesNotContain(passwordHash);
        assertThat(login.getDetails()).doesNotContain(sessionId);
        assertThat(login.getActorUsername()).doesNotContain(sessionId);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void employeeUpdate_writesAudit() {
        UserAccount admin = AccountFixtures.saveUser(
                employees, users, passwordEncoder, "E5002", "admin", Role.ADMIN, true);
        var employee = employeeService.create(
                "E5003",
                "New",
                "Employee",
                "new.employee@sems.test",
                "Analyst",
                "new_employee",
                AccountFixtures.PASSWORD,
                Role.EMPLOYEE);

        employeeService.update(employee.getId(), "New", "Employee", "new.employee@sems.test", "Senior Analyst");

        List<AuditEvent> updates = auditEvents.findAll().stream()
                .filter(event -> event.getEventType() == AuditEventType.EMPLOYEE && event.getAction() == AuditAction.UPDATE)
                .toList();
        assertThat(updates).hasSize(1);
        assertThat(updates.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(updates.get(0).getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(auditEvents.findAll())
                .allSatisfy(event -> assertThat(rowText(event)).doesNotContain(AccountFixtures.PASSWORD));
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void accessDenied_writesAudit() throws Exception {
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E5004", "employee", Role.EMPLOYEE, true);

        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden());

        List<AuditEvent> denied = auditEvents.findAll().stream()
                .filter(event -> event.getAction() == AuditAction.ACCESS_DENIED && event.getOutcome() == AuditOutcome.FAILURE)
                .toList();
        assertThat(denied).hasSize(1);
        assertThat(denied.get(0).getEventType()).isEqualTo(AuditEventType.AUTHORIZATION);
        assertThat(denied.get(0).getActorUsername()).isEqualTo("employee");
        assertThat(rowText(denied.get(0))).doesNotContain(AccountFixtures.PASSWORD);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listedEvents_areNewestFirst_whenInsertedOutOfOrder() throws Exception {
        insertEvent("2026-07-15T17:00:00Z", "newest");
        insertEvent("2026-01-15T18:00:00Z", "oldest");
        insertEvent("2026-01-15T18:00:00Z", "tie");

        List<AuditEvent> ordered = auditService.findAll();

        assertThat(ordered).extracting(AuditEvent::getDetails).containsExactly("newest", "tie", "oldest");
        assertThat(ordered).extracting(AuditEvent::getOccurredAt).containsExactly(
                "2026-07-15T17:00:00Z",
                "2026-01-15T18:00:00Z",
                "2026-01-15T18:00:00Z");
        assertThat(ordered.get(1).getId()).isGreaterThan(ordered.get(2).getId());
        assertThat(ordered.get(0).getId()).isLessThan(ordered.get(2).getId());

        var firstPage = auditService.findPage(PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id")));
        var secondPage = auditService.findPage(PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "occurredAt")));
        assertThat(firstPage.getContent()).extracting(AuditEvent::getDetails).containsExactly("newest", "tie");
        assertThat(secondPage.getContent()).extracting(AuditEvent::getDetails).containsExactly("oldest");

        String page = mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int oldest = page.indexOf(AuditTimestamps.format("2026-01-15T18:00:00Z"));
        int newest = page.indexOf(AuditTimestamps.format("2026-07-15T17:00:00Z"));
        assertThat(newest).isGreaterThanOrEqualTo(0);
        assertThat(oldest).isGreaterThan(newest);
        assertThat(page).doesNotContain("2026-01-15T18:00:00Z");
        assertThat(page).doesNotContain("2026-07-15T17:00:00Z");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void xssUsernameIsEscapedInAuditLog() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "<script>alert(1)</script>")
                        .param("password", "x")
                        .with(csrf()));

        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotViewAuditLog() throws Exception {
        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
    }

    private void insertEvent(String occurredAt, String details) {
        jdbc.update(
                """
                INSERT INTO audit_events (occurred_at, event_type, action, outcome, details)
                VALUES (?, 'AUTHENTICATION', 'LOGIN', 'SUCCESS', ?)
                """,
                occurredAt,
                details);
    }

    private static String rowText(AuditEvent event) {
        return String.join(" ",
                String.valueOf(event.getActorUserId()),
                String.valueOf(event.getActorUsername()),
                String.valueOf(event.getEventType()),
                String.valueOf(event.getAction()),
                String.valueOf(event.getOutcome()),
                String.valueOf(event.getDetails()),
                String.valueOf(event.getOccurredAt()));
    }
}
