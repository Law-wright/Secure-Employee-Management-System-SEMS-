package edu.sems.service;

import edu.sems.domain.UserAccount;
import edu.sems.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SemsUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public SemsUserDetailsService(UserAccountRepository users) {
        this.users = users;
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
                .build();
    }
}
