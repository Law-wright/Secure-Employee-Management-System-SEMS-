package edu.sems.service;

import edu.sems.domain.AuditAction;
import edu.sems.domain.AuditEventType;
import edu.sems.domain.AuditOutcome;
import edu.sems.domain.LeaveRequest;
import edu.sems.domain.LeaveStatus;
import edu.sems.domain.UserAccount;
import edu.sems.repository.LeaveRequestRepository;
import edu.sems.repository.UserAccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequests;
    private final UserAccountRepository users;
    private final AuditService auditService;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequests,
            UserAccountRepository users,
            AuditService auditService) {
        this.leaveRequests = leaveRequests;
        this.users = users;
        this.auditService = auditService;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public LeaveRequest submit(LocalDate startDate, LocalDate endDate, String reason) {
        validateDates(startDate, endDate);
        String cleanReason = requiredReason(reason);
        UserAccount owner = currentUser();
        LeaveRequest saved = leaveRequests.save(
                new LeaveRequest(owner.getEmployee(), startDate, endDate, cleanReason));
        auditService.recordCurrentUser(
                AuditEventType.LEAVE,
                AuditAction.SUBMIT,
                AuditOutcome.SUCCESS,
                "Submitted leave request " + saved.getId());
        return saved;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<LeaveRequest> findMine() {
        return leaveRequests.findByEmployeeIdOrderByCreatedAtDesc(currentUser().getEmployee().getId());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Transactional(readOnly = true)
    public List<LeaveRequest> findPending() {
        return leaveRequests.findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public LeaveRequest approve(long id) {
        LeaveRequest request = pendingRequest(id);
        UserAccount reviewer = currentUser();
        preventSelfReview(request, reviewer);
        request.approve(reviewer);
        auditService.recordCurrentUser(
                AuditEventType.LEAVE,
                AuditAction.APPROVE,
                AuditOutcome.SUCCESS,
                "Approved leave request " + request.getId());
        return request;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public LeaveRequest reject(long id) {
        LeaveRequest request = pendingRequest(id);
        UserAccount reviewer = currentUser();
        preventSelfReview(request, reviewer);
        request.reject(reviewer);
        auditService.recordCurrentUser(
                AuditEventType.LEAVE,
                AuditAction.REJECT,
                AuditOutcome.SUCCESS,
                "Rejected leave request " + request.getId());
        return request;
    }

    private LeaveRequest pendingRequest(long id) {
        LeaveRequest request = leaveRequests.findById(id)
                .orElseThrow(() -> new LeaveValidationException("leave", "Leave request not found."));
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveValidationException("leave", "Only pending leave requests can be reviewed.");
        }
        return request;
    }

    private static void preventSelfReview(LeaveRequest request, UserAccount reviewer) {
        if (request.getEmployee().getId().equals(reviewer.getEmployee().getId())) {
            throw new LeaveValidationException("leave", "You cannot review your own leave request.");
        }
    }

    private UserAccount currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new LeaveValidationException("user", "A signed-in user is required.");
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new LeaveValidationException("user", "Signed-in account not found."));
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new LeaveValidationException("startDate", "Start date is required.");
        }
        if (endDate == null) {
            throw new LeaveValidationException("endDate", "End date is required.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new LeaveValidationException("startDate", "Start date cannot be before today.");
        }
        if (endDate.isBefore(startDate)) {
            throw new LeaveValidationException("endDate", "End date cannot be before the start date.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > 30) {
            throw new LeaveValidationException("endDate", "Leave requests cannot be longer than 30 days.");
        }
    }

    private static String requiredReason(String reason) {
        String clean = reason == null ? "" : reason.trim();
        if (clean.isEmpty()) {
            throw new LeaveValidationException("reason", "Reason is required.");
        }
        if (clean.length() > 500) {
            throw new LeaveValidationException("reason", "Reason must be 500 characters or fewer.");
        }
        return clean;
    }
}
