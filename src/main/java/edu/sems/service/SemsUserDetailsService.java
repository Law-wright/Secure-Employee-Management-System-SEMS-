package edu.sems.service;

import edu.sems.domain.UserAccount;
import edu.sems.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class SemsUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;
    private final Clock clock;

    public SemsUserDetailsService(UserAccountRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));
        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .disabled(!account.isActive())
                .accountLocked(account.isLocked(clock.instant()))
                .build();
    }
}
