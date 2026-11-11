package edu.sems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @OneToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private String lockedUntil;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, Role role, boolean active, Employee employee) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.employee = employee;
        this.createdAt = Instant.now().toString();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void deactivate() {
        this.active = false;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public String getLockedUntil() {
        return lockedUntil;
    }

    /** True while the temporary sign-in lock is in force at the given moment. */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(Instant.parse(lockedUntil));
    }

    /**
     * Counts one failed sign-in. An expired lock is cleared first, so the count starts again at one.
     *
     * @return true when this failure started a new lock
     */
    public boolean recordFailedAttempt(int maxAttempts, Duration lockDuration, Instant now) {
        if (lockedUntil != null && !isLocked(now)) {
            failedAttempts = 0;
            lockedUntil = null;
        }
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration).toString();
            return true;
        }
        return false;
    }

    public boolean hasLockoutState() {
        return failedAttempts > 0 || lockedUntil != null;
    }

    public void clearFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }
}
