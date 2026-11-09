package edu.sems.service;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEvent;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.domain.UserAccount;
import edu.sems.repository.AuditEventRepository;
import edu.sems.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_DETAILS_LENGTH = 500;

    private final AuditEventRepository auditEvents;
    private final UserAccountRepository users;

    public AuditService(AuditEventRepository auditEvents, UserAccountRepository users) {
        this.auditEvents = auditEvents;
        this.users = users;
    }

    @Transactional
    public void record(
            String username,
            AuditEventType eventType,
            AuditAction action,
            AuditOutcome outcome,
            String details) {
        String actorUsername = clean(username, MAX_USERNAME_LENGTH);
        Long actorUserId = null;
        if (actorUsername != null) {
            actorUserId = users.findByUsername(actorUsername).map(UserAccount::getId).orElse(null);
        }
        auditEvents.save(new AuditEvent(
                actorUserId,
                actorUsername,
                eventType,
                action,
                outcome,
                clean(details, MAX_DETAILS_LENGTH)));
    }

    @Transactional
    public void recordCurrentUser(
            AuditEventType eventType,
            AuditAction action,
            AuditOutcome outcome,
            String details) {
        record(currentUsername(), eventType, action, outcome, details);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AuditEvent> findAll() {
        return auditEvents.findAllByOrderByOccurredAtDescIdDesc();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AuditEvent> findPage(Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return auditEvents.findAllByOrderByOccurredAtDescIdDesc(newestFirst);
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    private static String clean(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "anonymousUser".equals(trimmed)) {
            return null;
        }
        int semicolon = trimmed.indexOf(';');
        if (semicolon >= 0) {
            trimmed = trimmed.substring(0, semicolon).trim();
        }
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
