package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.audit.dto.AuditLogDto;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer recentDays) {

        if (recentDays != null) {
            return ResponseEntity.ok(auditLogService.getRecent(recentDays));
        }
        if (action != null && from != null && to != null) {
            return ResponseEntity.ok(auditLogService.getByActionAndDateRange(action, from, to));
        }
        if (action != null) {
            return ResponseEntity.ok(auditLogService.getByAction(action));
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(auditLogService.getByDateRange(from, to));
        }
        return ResponseEntity.ok(auditLogService.getAll());
    }
}
