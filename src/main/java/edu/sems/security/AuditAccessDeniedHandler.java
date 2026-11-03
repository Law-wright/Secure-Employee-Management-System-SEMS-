package edu.sems.security;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditService auditService;
    private final AccessDeniedHandler delegate;

    public AuditAccessDeniedHandler(AuditService auditService) {
        this.auditService = auditService;
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/access-denied");
        this.delegate = handler;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        auditService.record(
                username,
                AuditEventType.AUTHORIZATION,
                AuditAction.ACCESS_DENIED,
                AuditOutcome.FAILURE,
                "Denied " + request.getMethod() + " " + requestPath(request));
        delegate.handle(request, response, accessDeniedException);
    }

    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int semicolon = uri.indexOf(';');
        if (semicolon >= 0) {
            uri = uri.substring(0, semicolon);
        }
        return uri;
    }
}
