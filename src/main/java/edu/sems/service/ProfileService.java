package edu.sems.service;

import edu.sems.domain.Employee;
import edu.sems.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProfileService {

    private final UserAccountRepository users;

    public ProfileService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findEmployeeByUsername(String username) {
        return users.findByUsername(username).map(account -> account.getEmployee());
    }
}
