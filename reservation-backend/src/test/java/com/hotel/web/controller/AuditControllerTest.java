package com.hotel.web.controller;

import com.hotel.application.services.AuditLogService;
import com.hotel.web.dto.AuditLogResponse;
import com.hotel.web.dto.AuditMetricsResponse;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditController auditController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getLogs_Returns200OkWithPage() throws Exception {
        AuditLogResponse log1 = AuditLogResponse.builder()
                .id(1L)
                .username("admin1")
                .userRole("ROLE_ADMIN")
                .action("CHECK_IN")
                .entityName("Reservation")
                .entityId("HOSP-1-2026")
                .details("Check-in performed")
                .ipAddress("127.0.0.1")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogService.getAuditLogs(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log1), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/audit/logs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CHECK_IN"))
                .andExpect(jsonPath("$.content[0].username").value("admin1"))
                .andExpect(jsonPath("$.content[0].entityId").value("HOSP-1-2026"));
    }

    @Test
    void getActions_Returns200OkWithList() throws Exception {
        when(auditLogService.getAvailableActions())
                .thenReturn(List.of("CHECK_IN", "CHECK_OUT", "CANCEL_RESERVATION", "CREATE_STAFF"));

        mockMvc.perform(get("/api/admin/audit/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("CHECK_IN"))
                .andExpect(jsonPath("$[1]").value("CHECK_OUT"))
                .andExpect(jsonPath("$[2]").value("CANCEL_RESERVATION"));
    }

    @Test
    void getMetrics_Returns200OkWithMetricsDto() throws Exception {
        AuditMetricsResponse metrics = AuditMetricsResponse.builder()
                .totalEvents(250L)
                .todayEvents(18L)
                .failedEvents(2L)
                .distinctOperators(5L)
                .build();

        when(auditLogService.getMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/admin/audit/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(250))
                .andExpect(jsonPath("$.todayEvents").value(18))
                .andExpect(jsonPath("$.failedEvents").value(2));
    }

    @Test
    void exportReservations_Returns200OkWithCsvHeaders() throws Exception {
        byte[] csv = "Código de Reserva,Huésped\nHOSP-1-2026,Carlos".getBytes();
        when(auditLogService.exportReservationsCsv(any(), any())).thenReturn(csv);

        mockMvc.perform(get("/api/admin/audit/export/reservations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(csv));
    }

    @Test
    void exportFinancialJournal_Returns200OkWithCsvHeaders() throws Exception {
        byte[] csv = "Fecha,Tipo,Folio\n2026-10-31,Alojamiento,HOSP-1".getBytes();
        when(auditLogService.exportFinancialJournalCsv(any(), any())).thenReturn(csv);

        mockMvc.perform(get("/api/admin/audit/export/financial-journal"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(csv));
    }
}
