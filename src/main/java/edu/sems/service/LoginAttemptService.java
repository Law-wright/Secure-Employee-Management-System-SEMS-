package edu.sems.service;

import edu.sems.domain.UserAccount;
import edu.sems.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts wrong-password sign-ins per account and starts a temporary lock after too many in a row.
 * The lock ends on its own, so an attacker cannot keep a real user out for long.
 * Usernames that are not an active account are counted only in memory, with the same limit and
 * duration, so the login page cannot be used to tell whether an account exists.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TRANSIENT_KEYS = 10_000;
    private static final int MAX_KEY_LENGTH = 200;

    private final UserAccountRepository users;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;
    private final ConcurrentHashMap<String, TransientAttempt> transientAttempts = new ConcurrentHashMap<>();

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
     * Unknown usernames and inactive accounts are counted only in memory.
     *
     * @return true when this failure locked an existing active account
     */
    @Transactional
    public boolean recordFailure(String username) {
        String key = normalizeUsername(username);
        if (key == null) {
            return false;
        }
        Optional<UserAccount> account = trackedAccount(key);
        if (account.isPresent()) {
            return account.get().recordFailedAttempt(maxAttempts, lockDuration, clock.instant());
        }
        transientAttempt(key).record(maxAttempts, lockDuration, clock.instant());
        evictTransient(clock.instant());
        return false;
    }

    /** Clears the counter after a successful sign-in. */
    @Transactional
    public void recordSuccess(String username) {
        String key = normalizeUsername(username);
        if (key == null) {
            return;
        }
        trackedAccount(key)
                .filter(UserAccount::hasLockoutState)
                .ifPresent(UserAccount::clearFailedAttempts);
        transientAttempts.remove(key);
    }

    /**
     * Remaining attempts and lockout deadline for the username currently being tried.
     * A blank username yields an idle status so the page does not invent a counter.
     */
    @Transactional(readOnly = true)
    public LoginAttemptStatus status(String username) {
        Instant now = clock.instant();
        String key = normalizeUsername(username);
        if (key == null) {
            return LoginAttemptStatus.idle(maxAttempts, now);
        }
        Optional<UserAccount> account = trackedAccount(key);
        if (account.isPresent()) {
            return statusFor(account.get(), now);
        }
        TransientAttempt attempt = transientAttempts.get(key);
        if (attempt == null) {
            return LoginAttemptStatus.counting(maxAttempts, now);
        }
        synchronized (attempt) {
            return attempt.status(maxAttempts, now);
        }
    }

    /**
     * The session stores this value. Short names are unchanged so they still match the account table.
     * Very long names are hashed so they cannot bloat the session or the in-memory map.
     */
    public String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        if (username.length() <= MAX_KEY_LENGTH) {
            return username;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(username.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            return username.substring(0, MAX_KEY_LENGTH);
        }
    }

    /** Drops in-memory counts. Active-account rows are untouched. */
    public void clearTransientAttempts() {
        transientAttempts.clear();
    }

    private Optional<UserAccount> trackedAccount(String username) {
        return users.findByUsername(username).filter(UserAccount::isActive);
    }

    private LoginAttemptStatus statusFor(UserAccount account, Instant now) {
        if (account.isLocked(now)) {
            return LoginAttemptStatus.locked(Instant.parse(account.getLockedUntil()), now);
        }
        int used = account.getLockedUntil() != null ? 0 : account.getFailedAttempts();
        return LoginAttemptStatus.counting(Math.max(0, maxAttempts - used), now);
    }

    private TransientAttempt transientAttempt(String key) {
        TransientAttempt created = new TransientAttempt();
        TransientAttempt existing = transientAttempts.putIfAbsent(key, created);
        return existing == null ? created : existing;
    }

    private void evictTransient(Instant now) {
        if (transientAttempts.size() <= MAX_TRANSIENT_KEYS) {
            return;
        }
        transientAttempts.entrySet().removeIf(entry -> !entry.getValue().isLocked(now));
        if (transientAttempts.size() > MAX_TRANSIENT_KEYS) {
            transientAttempts.clear();
        }
    }

    private static final class TransientAttempt {
        private int failedAttempts;
        private Instant lockedUntil;

        private boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        private void record(int maxAttempts, Duration lockDuration, Instant now) {
            synchronized (this) {
                if (isLocked(now)) {
                    return;
                }
                if (lockedUntil != null) {
                    failedAttempts = 0;
                    lockedUntil = null;
                }
                failedAttempts++;
                if (failedAttempts >= maxAttempts) {
                    lockedUntil = now.plus(lockDuration);
                }
            }
        }

        private LoginAttemptStatus status(int maxAttempts, Instant now) {
            if (isLocked(now)) {
                return LoginAttemptStatus.locked(lockedUntil, now);
            }
            int used = lockedUntil != null ? 0 : failedAttempts;
            return LoginAttemptStatus.counting(Math.max(0, maxAttempts - used), now);
        }
    }
}
