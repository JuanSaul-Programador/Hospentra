package com.hotel.application.services;

import com.hotel.web.dto.AuditLogResponse;
import com.hotel.web.dto.AuditMetricsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AuditLogService {

    void logAction(String action, String entityName, String entityId, String details, String status);

    void logAction(String username, String userRole, String action, String entityName, String entityId, String details, String ipAddress, String status);

    Page<AuditLogResponse> getAuditLogs(String action, String username, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<String> getAvailableActions();

    AuditMetricsResponse getMetrics();

    byte[] exportReservationsCsv(LocalDate startDate, LocalDate endDate);

    byte[] exportFinancialJournalCsv(LocalDate startDate, LocalDate endDate);
}
