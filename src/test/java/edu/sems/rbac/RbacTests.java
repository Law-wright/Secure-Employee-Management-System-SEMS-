package edu.sems.rbac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("sems-test.db");
        registry.add("SEMS_DB_PATH", () -> database.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canOpenEmployeeCreate() throws Exception {
        mockMvc.perform(get("/employees/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New employee")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manager_canOpenLeaveReview() throws Exception {
        mockMvc.perform(get("/leave/review"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Leave review")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_canOpenOwnProfile() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Profile")));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_cannotOpenAuditLog() throws Exception {
        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));

        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You do not have access to that page.")));
    }
}
