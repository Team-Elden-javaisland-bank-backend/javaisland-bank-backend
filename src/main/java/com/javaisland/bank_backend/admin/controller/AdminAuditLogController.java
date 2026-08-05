package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.audit.dto.AuditLogDto;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import com.javaisland.bank_backend.common.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<PageResponseDto<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Integer recentDays,
            @PageableDefault(size = 20, sort = "performedAt") Pageable pageable) {

        List<AuditLogDto> fullList;
        if (recentDays != null) {
            fullList = auditLogService.getRecent(recentDays);
        } else if (action != null && from != null && to != null) {
            fullList = auditLogService.getByActionAndDateRange(action, from, to);
        } else if (action != null) {
            fullList = auditLogService.getByAction(action);
        } else if (from != null && to != null) {
            fullList = auditLogService.getByDateRange(from, to);
        } else {
            fullList = auditLogService.getAll();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), fullList.size());
        List<AuditLogDto> paginatedList = fullList.subList(start, end);
        return ResponseEntity.ok(PageResponseDto.from(new PageImpl<>(paginatedList, pageable, fullList.size())));
    }
}
