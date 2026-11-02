package edu.sems.support;

import edu.sems.domain.Employee;
import edu.sems.domain.Role;
import edu.sems.domain.UserAccount;
import edu.sems.repository.EmployeeRepository;
import edu.sems.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class AccountFixtures {

    public static final String PASSWORD = "test-password-1";

    private AccountFixtures() {
    }

    public static UserAccount saveUser(
            EmployeeRepository employees,
            UserAccountRepository users,
            PasswordEncoder encoder,
            String employeeNumber,
            String username,
            Role role,
            boolean active) {
        Employee employee = employees.save(new Employee(
                employeeNumber,
                "Test",
                "User",
                username + "@sems.test",
                "Tester"));
        return users.save(new UserAccount(username, encoder.encode(PASSWORD), role, active, employee));
    }
}
