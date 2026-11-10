package edu.sems.security;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.service.AuditService;
import edu.sems.service.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private final AuditService auditService;
    private final LoginAttemptService loginAttempts;

    public AuthenticationAuditListener(AuditService auditService, LoginAttemptService loginAttempts) {
        this.auditService = auditService;
        this.loginAttempts = loginAttempts;
    }

    @EventListener
    public void onLoginSuccess(InteractiveAuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        loginAttempts.recordSuccess(username);
        auditService.record(
                username,
                AuditEventType.AUTHENTICATION,
                AuditAction.LOGIN,
                AuditOutcome.SUCCESS,
                "Signed in");
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        boolean refusedByLock = event instanceof AuthenticationFailureLockedEvent;
        auditService.record(
                username,
                AuditEventType.AUTHENTICATION,
                AuditAction.LOGIN_FAILED,
                AuditOutcome.FAILURE,
                refusedByLock ? "Sign-in refused: account locked" : "Sign-in failed");
        if (event instanceof AuthenticationFailureBadCredentialsEvent
                && username != null
                && loginAttempts.recordFailure(username)) {
            auditService.record(
                    username,
                    AuditEventType.AUTHENTICATION,
                    AuditAction.LOGIN_FAILED,
                    AuditOutcome.FAILURE,
                    "Account locked after repeated failed sign-ins");
        }
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null) {
            return;
        }
        auditService.record(
                authentication.getName(),
                AuditEventType.AUTHENTICATION,
                AuditAction.LOGOUT,
                AuditOutcome.SUCCESS,
                "Signed out");
    }
}
