package com.javaisland.bank_backend.audit.repository;

import com.javaisland.bank_backend.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActionOrderByPerformedAtDesc(String action);

    List<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(OffsetDateTime from, OffsetDateTime to);

    List<AuditLog> findByActionAndPerformedAtBetweenOrderByPerformedAtDesc(String action, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT a FROM AuditLog a WHERE a.performedAt >= :from ORDER BY a.performedAt DESC")
    List<AuditLog> findRecentLogs(@Param("from") OffsetDateTime from);
}
