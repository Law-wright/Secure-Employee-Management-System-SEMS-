package edu.sems.auth;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEvent;
import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.AuditEventRepository;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import edu.sems.support.AccountFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LockoutTests {

    private static final String WRONG_PASSWORD = "wrong-password-1";
    private static final int LIMIT = 5;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("sems-lockout-test.db");
        registry.add("SEMS_DB_PATH", () -> database.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertAccounts() {
        auditEvents.deleteAll();
        users.deleteAll();
        employees.deleteAll();
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E2001", "ada", Role.EMPLOYEE, true);
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E2002", "inactive", Role.EMPLOYEE, false);
    }

    @Test
    void lockedAfterFiveWrongPasswords_evenWithCorrectPassword() throws Exception {
        failTimes("ada", LIMIT);

        UserAccount ada = user("ada");
        assertThat(ada.getFailedAttempts()).isEqualTo(LIMIT);
        assertThat(ada.getLockedUntil()).isNotNull();

        mockMvc.perform(login("ada", AccountFixtures.PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void fewerThanFiveWrongPasswords_doesNotLock() throws Exception {
        failTimes("ada", LIMIT - 1);

        mockMvc.perform(login("ada", AccountFixtures.PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ada"));
        assertThat(user("ada").getLockedUntil()).isNull();
    }

    @Test
    void successfulLogin_resetsTheCounter() throws Exception {
        failTimes("ada", 3);
        mockMvc.perform(login("ada", AccountFixtures.PASSWORD)).andExpect(redirectedUrl("/"));
        assertThat(user("ada").getFailedAttempts()).isZero();

        failTimes("ada", LIMIT - 1);
        mockMvc.perform(login("ada", AccountFixtures.PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ada"));
    }

    @Test
    void lockedAccount_showsTheSameGenericMessage() throws Exception {
        failTimes("ada", LIMIT);
        mockMvc.perform(login("ada", AccountFixtures.PASSWORD)).andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username or password.")))
                .andExpect(content().string(not(containsString("locked"))))
                .andExpect(content().string(not(containsString("Locked"))));
    }

    @Test
    void attemptsDuringALock_doNotExtendIt() throws Exception {
        failTimes("ada", LIMIT);
        String lockedUntil = user("ada").getLockedUntil();

        failTimes("ada", 3);

        UserAccount ada = user("ada");
        assertThat(ada.getLockedUntil()).isEqualTo(lockedUntil);
        assertThat(ada.getFailedAttempts()).isEqualTo(LIMIT);
    }

    @Test
    void expiredLock_allowsSignInAndClearsState() throws Exception {
        expireLock("ada");

        mockMvc.perform(login("ada", AccountFixtures.PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ada"));

        UserAccount ada = user("ada");
        assertThat(ada.getFailedAttempts()).isZero();
        assertThat(ada.getLockedUntil()).isNull();
    }

    @Test
    void wrongPasswordAfterExpiry_startsTheCountAgain() throws Exception {
        expireLock("ada");

        failTimes("ada", 1);

        UserAccount ada = user("ada");
        assertThat(ada.getFailedAttempts()).isEqualTo(1);
        assertThat(ada.getLockedUntil()).isNull();
    }

    @Test
    void unknownUsername_isNotCountedAndLooksTheSame() throws Exception {
        for (int i = 0; i < LIMIT + 2; i++) {
            mockMvc.perform(login("ghost", WRONG_PASSWORD))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }
        assertThat(users.findByUsername("ghost")).isEmpty();
        assertThat(user("ada").getFailedAttempts()).isZero();
    }

    @Test
    void inactiveAccount_isNotCounted() throws Exception {
        failTimes("inactive", LIMIT + 1);

        UserAccount inactive = user("inactive");
        assertThat(inactive.getFailedAttempts()).isZero();
        assertThat(inactive.getLockedUntil()).isNull();
    }

    @Test
    void lockIsAudited_withoutThePassword() throws Exception {
        failTimes("ada", LIMIT);
        mockMvc.perform(login("ada", AccountFixtures.PASSWORD));

        List<AuditEvent> events = auditEvents.findAllByOrderByOccurredAtDescIdDesc();
        assertThat(events).extracting(AuditEvent::getDetails)
                .contains("Account locked after repeated failed sign-ins", "Sign-in refused: account locked");
        assertThat(events).allSatisfy(event -> {
            assertThat(String.valueOf(event.getDetails())).doesNotContain(WRONG_PASSWORD);
            assertThat(String.valueOf(event.getDetails())).doesNotContain(AccountFixtures.PASSWORD);
        });
        assertThat(events).filteredOn(event -> event.getAction() == AuditAction.LOGIN_FAILED)
                .hasSize(LIMIT + 2);
    }

    private void failTimes(String username, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            mockMvc.perform(login(username, WRONG_PASSWORD))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }
    }

    private void expireLock(String username) {
        String past = Instant.now().minus(1, ChronoUnit.MINUTES).toString();
        jdbc.update("update users set failed_attempts = ?, locked_until = ? where username = ?",
                LIMIT, past, username);
    }

    private UserAccount user(String username) {
        return users.findByUsername(username).orElseThrow();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String username, String password) {
        return post("/login")
                .param("username", username)
                .param("password", password)
                .with(csrf());
    }
}
