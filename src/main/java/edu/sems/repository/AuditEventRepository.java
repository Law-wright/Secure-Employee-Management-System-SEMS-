package edu.sems.repository;

import edu.sems.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("select event from AuditEvent event order by event.occurredAt desc, event.id desc")
    List<AuditEvent> findAllByOrderByOccurredAtDescIdDesc();

    @Query(
            value = "select event from AuditEvent event order by event.occurredAt desc, event.id desc",
            countQuery = "select count(event) from AuditEvent event")
    Page<AuditEvent> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);
}
