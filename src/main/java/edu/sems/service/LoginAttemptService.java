package edu.sems.service;

import edu.sems.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Counts wrong-password sign-ins per account and starts a temporary lock after too many in a row.
 * The lock ends on its own, so an attacker cannot keep a real user out for long.
 */
@Service
public class LoginAttemptService {

    private final UserAccountRepository users;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(
            UserAccountRepository users,
            Clock clock,
            @Value("${sems.lockout.max-attempts:5}") int maxAttempts,
            @Value("${sems.lockout.duration-minutes:15}") long lockMinutes) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("sems.lockout.max-attempts must be at least 1");
        }
        if (lockMinutes < 1) {
            throw new IllegalArgumentException("sems.lockout.duration-minutes must be at least 1");
        }
        this.users = users;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    /**
     * Records one wrong password for an existing, active account.
     * Unknown usernames and inactive accounts are not counted.
     *
     * @return true when this failure locked the account
     */
    @Transactional
    public boolean recordFailure(String username) {
        return users.findByUsername(username)
                .filter(account -> account.isActive())
                .map(account -> account.recordFailedAttempt(maxAttempts, lockDuration, clock.instant()))
                .orElse(false);
    }

    /** Clears the counter after a successful sign-in. */
    @Transactional
    public void recordSuccess(String username) {
        users.findByUsername(username)
                .filter(account -> account.hasLockoutState())
                .ifPresent(account -> account.clearFailedAttempts());
    }
}
