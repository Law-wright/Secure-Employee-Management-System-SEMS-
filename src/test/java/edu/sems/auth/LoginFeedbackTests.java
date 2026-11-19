package edu.sems.auth;

import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.AuditEventRepository;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import edu.sems.service.LoginAttemptService;
import edu.sems.support.AccountFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginFeedbackTests {

    private static final String WRONG_PASSWORD = "wrong-password-1";
    private static final int LIMIT = 5;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("sems-login-feedback-test.db");
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

    @Autowired
    private LoginAttemptService loginAttempts;

    @BeforeEach
    void insertAccounts() {
        auditEvents.deleteAll();
        users.deleteAll();
        employees.deleteAll();
        loginAttempts.clearTransientAttempts();
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E3001", "ada", Role.EMPLOYEE, true);
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E3002", "inactive", Role.EMPLOYEE, false);
    }

    @Test
    void counterStaysHiddenAboveThreeRemainingAttempts() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("attempts remaining"))))
                .andExpect(content().string(not(containsString("lockout-notice"))));

        MockHttpSession session = failTimes("ada", 1);

        mockMvc.perform(get("/login").param("error", "").session(session))
                .andExpect(content().string(containsString("Invalid username or password.")))
                .andExpect(content().string(not(containsString("attempts remaining"))))
                .andExpect(content().string(not(containsString("id=\"attempts-remaining\""))));
        statusRequest(session)
                .andExpect(jsonPath("$.remainingAttempts").value(4))
                .andExpect(jsonPath("$.showRemainingAttempts").value(false))
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    void counterShownAndDecrementsAtThreeTwoAndOne() throws Exception {
        failTimes("ada", 1);

        MockHttpSession three = failTimes("ada", 1);
        assertShown(three, 3);

        MockHttpSession two = failTimes("ada", 1);
        assertShown(two, 2);

        MockHttpSession one = failTimes("ada", 1);
        assertShown(one, 1);
    }

    @Test
    void lockoutShowsMessageAndCountdown() throws Exception {
        MockHttpSession session = failTimes("ada", LIMIT);
        String lockedUntil = user("ada").getLockedUntil();

        mockMvc.perform(get("/login").param("error", "").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This account has been locked out.")))
                .andExpect(content().string(containsString("Time remaining:")))
                .andExpect(content().string(containsString("id=\"lockout-countdown\"")))
                .andExpect(content().string(containsString(lockedUntil)))
                .andExpect(content().string(containsString("/js/login-feedback.js")))
                .andExpect(content().string(not(containsString("Invalid username or password."))))
                .andExpect(content().string(not(containsString("attempts remaining"))));

        statusRequest(session)
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockedUntil").value(lockedUntil))
                .andExpect(jsonPath("$.showRemainingAttempts").value(false))
                .andExpect(jsonPath("$.countdownText", containsString("minute")));
    }

    @Test
    void lockoutNoticeClearsAfterExpiry() throws Exception {
        MockHttpSession session = failTimes("ada", LIMIT);
        expireLock("ada");

        mockMvc.perform(get("/login").param("error", "").session(session))
                .andExpect(content().string(not(containsString("id=\"lockout-notice\""))))
                .andExpect(content().string(not(containsString("This account has been locked out."))))
                .andExpect(content().string(not(containsString("attempts remaining"))));
        statusRequest(session)
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.showRemainingAttempts").value(false))
                .andExpect(jsonPath("$.lockedUntil").value(nullValue()));

        mockMvc.perform(login("ada", AccountFixtures.PASSWORD).session(session))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ada"));
    }

    @Test
    void unknownUsername_matchesTheCounterAndLockout() throws Exception {
        MockHttpSession hidden = failTimes("ghost", 1);
        mockMvc.perform(get("/login").param("error", "").session(hidden))
                .andExpect(content().string(not(containsString("attempts remaining"))));

        MockHttpSession shown = failTimes("ghost", 1);
        assertShown(shown, 3);

        MockHttpSession locked = failTimes("ghost", 3);
        mockMvc.perform(get("/login").param("error", "").session(locked))
                .andExpect(content().string(containsString("This account has been locked out.")))
                .andExpect(content().string(containsString("Time remaining:")));
        statusRequest(locked)
                .andExpect(jsonPath("$.locked").value(true));

        assertThat(users.findByUsername("ghost")).isEmpty();
        assertThat(user("ada").getFailedAttempts()).isZero();
    }

    private void assertShown(MockHttpSession session, int remaining) throws Exception {
        String message = remaining == 1
                ? "1 login attempt remaining."
                : remaining + " login attempts remaining.";
        mockMvc.perform(get("/login").param("error", "").session(session))
                .andExpect(content().string(containsString(message)))
                .andExpect(content().string(not(containsString("This account has been locked out."))));
        statusRequest(session)
                .andExpect(jsonPath("$.remainingAttempts").value(remaining))
                .andExpect(jsonPath("$.showRemainingAttempts").value(true))
                .andExpect(jsonPath("$.locked").value(false));
    }

    private org.springframework.test.web.servlet.ResultActions statusRequest(MockHttpSession session) throws Exception {
        return mockMvc.perform(get("/login/status")
                .session(session)
                .accept(MediaType.APPLICATION_JSON));
    }

    private MockHttpSession failTimes(String username, int times) throws Exception {
        MvcResult result = null;
        for (int i = 0; i < times; i++) {
            result = mockMvc.perform(login(username, WRONG_PASSWORD))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"))
                    .andReturn();
        }
        return (MockHttpSession) result.getRequest().getSession(false);
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
