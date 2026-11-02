package edu.sems.auth;

import edu.sems.domain.Role;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import edu.sems.support.AccountFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("sems-test.db");
        registry.add("SEMS_DB_PATH", () -> database.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void insertAccounts() {
        users.deleteAll();
        employees.deleteAll();
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E1001", "ada", Role.EMPLOYEE, true);
        AccountFixtures.saveUser(employees, users, passwordEncoder, "E1002", "inactive", Role.EMPLOYEE, false);
    }

    @Test
    void validLogin_redirectsToHome() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "ada")
                        .param("password", AccountFixtures.PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ada"));
    }

    @Test
    void invalidLogin_showsGenericError() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "ada")
                        .param("password", "wrong-password-1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username or password.")))
                .andExpect(content().string(not(containsString("wrong-password-1"))));
    }

    @Test
    void inactiveAccount_isRejected() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "inactive")
                        .param("password", AccountFixtures.PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Invalid username or password.")))
                .andExpect(content().string(not(containsString("disabled"))))
                .andExpect(content().string(not(containsString("Inactive"))));
    }
}
