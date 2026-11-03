package edu.sems.security;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.service.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private final AuditService auditService;

    public AuthenticationAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onLoginSuccess(InteractiveAuthenticationSuccessEvent event) {
        auditService.record(
                event.getAuthentication().getName(),
                AuditEventType.AUTHENTICATION,
                AuditAction.LOGIN,
                AuditOutcome.SUCCESS,
                "Signed in");
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        auditService.record(
                username,
                AuditEventType.AUTHENTICATION,
                AuditAction.LOGIN_FAILED,
                AuditOutcome.FAILURE,
                "Sign-in failed");
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
