package edu.sems.leave;

import edu.sems.domain.LeaveRequest;
import edu.sems.domain.LeaveStatus;
import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.AuditEventRepository;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.LeaveRequestRepository;
import edu.sems.repository.UserAccountRepository;
import edu.sems.service.LeaveRequestService;
import edu.sems.service.LeaveValidationException;
import edu.sems.support.AccountFixtures;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestServiceTests {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        Path database = tempDir.resolve("leave-tests.db");
        registry.add("SEMS_DB_PATH", () -> database.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Autowired
    private LeaveRequestService leaveService;

    @Autowired
    private LeaveRequestRepository leaveRequests;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        auditEvents.deleteAll();
        leaveRequests.deleteAll();
        users.deleteAll();
        employees.deleteAll();
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void submitLeave_createsPendingRequest() {
        saveUser("E3001", "employee", Role.EMPLOYEE);

        LeaveRequest request = leaveService.submit(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5),
                "Family trip");

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(request.getEmployee().getEmployeeNumber()).isEqualTo("E3001");
        assertThat(request.getReviewer()).isNull();
        assertThat(leaveService.findMine()).extracting(LeaveRequest::getId).containsExactly(request.getId());
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void submitLeave_invalidDates_areRejected() {
        saveUser("E3002", "employee", Role.EMPLOYEE);
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> leaveService.submit(today, today.minusDays(1), "Invalid range"))
                .isInstanceOf(LeaveValidationException.class)
                .hasMessage("End date cannot be before the start date.");
        assertThatThrownBy(() -> leaveService.submit(today.minusDays(1), today, "Past range"))
                .isInstanceOf(LeaveValidationException.class)
                .hasMessage("Start date cannot be before today.");
        assertThatThrownBy(() -> leaveService.submit(today, today.plusDays(30), "Too long"))
                .isInstanceOf(LeaveValidationException.class)
                .hasMessage("Leave requests cannot be longer than 30 days.");
        assertThat(leaveRequests.count()).isZero();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void approveLeave_setsApprovedAndReviewer() {
        UserAccount employee = saveUser("E3003", "employee", Role.EMPLOYEE);
        UserAccount manager = saveUser("E3004", "manager", Role.MANAGER);
        LeaveRequest request = pendingRequest(employee);

        LeaveRequest approved = leaveService.approve(request.getId());

        assertThat(approved.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(approved.getReviewer().getId()).isEqualTo(manager.getId());
        assertThat(approved.getReviewedAt()).isNotBlank();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void rejectLeave_setsRejected() {
        UserAccount employee = saveUser("E3005", "employee", Role.EMPLOYEE);
        saveUser("E3006", "manager", Role.MANAGER);
        LeaveRequest request = pendingRequest(employee);

        LeaveRequest rejected = leaveService.reject(request.getId());

        assertThat(rejected.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void approveLeave_whenAlreadyApproved_isRejected() {
        UserAccount employee = saveUser("E3007", "employee", Role.EMPLOYEE);
        saveUser("E3008", "manager", Role.MANAGER);
        LeaveRequest request = pendingRequest(employee);
        leaveService.approve(request.getId());

        assertThatThrownBy(() -> leaveService.approve(request.getId()))
                .isInstanceOf(LeaveValidationException.class)
                .hasMessage("Only pending leave requests can be reviewed.");
        assertThat(leaveRequests.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employee_cannotApproveLeave() throws Exception {
        UserAccount employee = saveUser("E3009", "employee", Role.EMPLOYEE);
        LeaveRequest request = pendingRequest(employee);

        mockMvc.perform(post("/leave/review/{id}/approve", request.getId()).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));

        mockMvc.perform(post("/access-denied").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You do not have access to that page.")));

        assertThat(leaveRequests.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void manager_cannotReviewOwnLeave() {
        UserAccount manager = saveUser("E3010", "manager", Role.MANAGER);
        LeaveRequest request = pendingRequest(manager);

        assertThatThrownBy(() -> leaveService.approve(request.getId()))
                .isInstanceOf(LeaveValidationException.class)
                .hasMessage("You cannot review your own leave request.");
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    private UserAccount saveUser(String employeeNumber, String username, Role role) {
        return AccountFixtures.saveUser(
                employees, users, passwordEncoder, employeeNumber, username, role, true);
    }

    private LeaveRequest pendingRequest(UserAccount owner) {
        return leaveRequests.save(new LeaveRequest(
                owner.getEmployee(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "Personal leave"));
    }
}
