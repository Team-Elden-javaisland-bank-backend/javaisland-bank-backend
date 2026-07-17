package com.javaisland.bank_backend.audit.repository;

import com.javaisland.bank_backend.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActionOrderByPerformedAtDesc(String action);

    List<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByActionAndPerformedAtBetweenOrderByPerformedAtDesc(String action, LocalDateTime from, LocalDateTime to);

    @Query("SELECT a FROM AuditLog a WHERE a.performedAt >= :from ORDER BY a.performedAt DESC")
    List<AuditLog> findRecentLogs(@Param("from") LocalDateTime from);
}
