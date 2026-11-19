package edu.sems.service;

import java.time.Duration;
import java.time.Instant;

/**
 * What the login page needs to render a remaining-attempts notice or a lockout countdown.
 * Built only from server-side state.
 */
public final class LoginAttemptStatus {

    /** The notice appears once this many attempts, or fewer, remain. */
    public static final int REMAINING_NOTICE_AT = 3;

    private final boolean locked;
    private final int remainingAttempts;
    private final boolean showRemainingAttempts;
    private final String lockedUntil;
    private final String serverNow;
    private final String countdownText;
    private final String remainingMessage;

    private LoginAttemptStatus(
            boolean locked,
            int remainingAttempts,
            boolean showRemainingAttempts,
            String lockedUntil,
            String serverNow,
            String countdownText,
            String remainingMessage) {
        this.locked = locked;
        this.remainingAttempts = remainingAttempts;
        this.showRemainingAttempts = showRemainingAttempts;
        this.lockedUntil = lockedUntil;
        this.serverNow = serverNow;
        this.countdownText = countdownText;
        this.remainingMessage = remainingMessage;
    }

    /** No username is in progress. The page stays quiet even when the limit is 3 or less. */
    public static LoginAttemptStatus idle(int maxAttempts, Instant now) {
        return new LoginAttemptStatus(false, maxAttempts, false, null, now.toString(), "", "");
    }

    public static LoginAttemptStatus counting(int remainingAttempts, Instant now) {
        int remaining = Math.max(0, remainingAttempts);
        boolean show = remaining > 0 && remaining <= REMAINING_NOTICE_AT;
        String message = show ? remainingMessage(remaining) : "";
        return new LoginAttemptStatus(false, remaining, show, null, now.toString(), "", message);
    }

    public static LoginAttemptStatus locked(Instant lockedUntil, Instant now) {
        Duration remaining = Duration.between(now, lockedUntil);
        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }
        return new LoginAttemptStatus(
                true,
                0,
                false,
                lockedUntil.toString(),
                now.toString(),
                formatCountdown(remaining),
                "");
    }

    public boolean isLocked() {
        return locked;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public boolean isShowRemainingAttempts() {
        return showRemainingAttempts;
    }

    public String getLockedUntil() {
        return lockedUntil;
    }

    public String getServerNow() {
        return serverNow;
    }

    public String getCountdownText() {
        return countdownText;
    }

    public String getRemainingMessage() {
        return remainingMessage;
    }

    private static String remainingMessage(int remaining) {
        if (remaining == 1) {
            return "1 login attempt remaining.";
        }
        return remaining + " login attempts remaining.";
    }

    static String formatCountdown(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return unit(hours, "hour") + " " + unit(minutes, "minute") + " " + unit(seconds, "second");
        }
        return unit(minutes, "minute") + " " + unit(seconds, "second");
    }

    private static String unit(long count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }
}
