package edu.sems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private String startDate;

    @Column(name = "end_date", nullable = false)
    private String endDate;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewer_user_id")
    private UserAccount reviewer;

    @Column(name = "reviewed_at")
    private String reviewedAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    protected LeaveRequest() {
    }

    public LeaveRequest(Employee employee, LocalDate startDate, LocalDate endDate, String reason) {
        this.employee = employee;
        this.startDate = startDate.toString();
        this.endDate = endDate.toString();
        this.reason = reason;
        this.status = LeaveStatus.PENDING;
        this.createdAt = Instant.now().toString();
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getStartDate() {
        return LocalDate.parse(startDate);
    }

    public LocalDate getEndDate() {
        return LocalDate.parse(endDate);
    }

    public String getReason() {
        return reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public UserAccount getReviewer() {
        return reviewer;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void approve(UserAccount reviewer) {
        review(LeaveStatus.APPROVED, reviewer);
    }

    public void reject(UserAccount reviewer) {
        review(LeaveStatus.REJECTED, reviewer);
    }

    private void review(LeaveStatus newStatus, UserAccount reviewer) {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be reviewed.");
        }
        this.status = newStatus;
        this.reviewer = reviewer;
        this.reviewedAt = Instant.now().toString();
    }
}
