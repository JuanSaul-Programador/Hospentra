package com.hotel.application.services;

import com.hotel.application.services.impl.AuditLogServiceImpl;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.AuditLogRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.web.dto.AuditLogResponse;
import com.hotel.web.dto.AuditMetricsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomOrderRepository roomOrderRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logAction_withExplicitParams_shouldSaveAuditLog() {
        auditLogService.logAction("admin1", "ROLE_ADMIN", "CREATE_STAFF", "User", "recep1", "Created receptionist", "192.168.1.10", "SUCCESS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin1");
        assertThat(saved.getUserRole()).isEqualTo("ROLE_ADMIN");
        assertThat(saved.getAction()).isEqualTo("CREATE_STAFF");
        assertThat(saved.getEntityName()).isEqualTo("User");
        assertThat(saved.getEntityId()).isEqualTo("recep1");
        assertThat(saved.getDetails()).isEqualTo("Created receptionist");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void logAction_withSecurityContext_shouldExtractUserAndRole() {
        var auth = new UsernamePasswordAuthenticationToken("manager", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        auditLogService.logAction("CHECK_IN", "Reservation", "HOSP-1-2026", "Check-in performed", "SUCCESS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("manager");
        assertThat(saved.getUserRole()).isEqualTo("ROLE_ADMIN");
        assertThat(saved.getAction()).isEqualTo("CHECK_IN");
        assertThat(saved.getEntityName()).isEqualTo("Reservation");
        assertThat(saved.getEntityId()).isEqualTo("HOSP-1-2026");
    }

    @Test
    void logAction_whenRepositoryFails_shouldNotThrowException() {
        doThrow(new RuntimeException("DB down")).when(auditLogRepository).save(any(AuditLog.class));

        auditLogService.logAction("system", "SYSTEM", "TEST", "Entity", "1", "Details", "127.0.0.1", "SUCCESS");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getAuditLogs_shouldReturnMappedPage() {
        AuditLog log1 = AuditLog.builder()
                .id(1L)
                .username("admin1")
                .userRole("ROLE_ADMIN")
                .action("CHECK_IN")
                .entityName("Reservation")
                .entityId("HOSP-1-2026")
                .details("Check-in suite 101")
                .ipAddress("127.0.0.1")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log1), PageRequest.of(0, 10), 1);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.getAuditLogs("CHECK_IN", "admin1", LocalDate.now(), LocalDate.now(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("CHECK_IN");
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("admin1");
        assertThat(result.getContent().get(0).getEntityId()).isEqualTo("HOSP-1-2026");
    }

    @Test
    void getAvailableActions_shouldReturnDistinctList() {
        when(auditLogRepository.findDistinctActions()).thenReturn(List.of("CHECK_IN", "CHECK_OUT", "CREATE_STAFF"));

        List<String> actions = auditLogService.getAvailableActions();

        assertThat(actions).containsExactly("CHECK_IN", "CHECK_OUT", "CREATE_STAFF");
    }

    @Test
    void getMetrics_shouldCalculateMetrics() {
        when(auditLogRepository.count()).thenReturn(150L);
        when(auditLogRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(25L);
        when(auditLogRepository.countByStatus("FAILURE")).thenReturn(3L);
        when(auditLogRepository.findDistinctActions()).thenReturn(List.of("A", "B", "C", "D"));

        AuditMetricsResponse metrics = auditLogService.getMetrics();

        assertThat(metrics.getTotalEvents()).isEqualTo(150L);
        assertThat(metrics.getTodayEvents()).isEqualTo(25L);
        assertThat(metrics.getFailedEvents()).isEqualTo(3L);
        assertThat(metrics.getDistinctOperators()).isEqualTo(4L);
    }

    @Test
    void exportReservationsCsv_shouldGenerateValidCsvWithUtf8Bom() {
        Guest guest = Guest.builder().firstName("Carlos").lastName("Gomez").email("carlos@gmail.com").phone("+1 555-0101").build();
        RoomType roomType = RoomType.builder().name("Penthouse Imperial").pricePerNight(new BigDecimal("450.00")).build();
        Room room = Room.builder().roomNumber("401").roomType(roomType).build();

        Reservation reservation = Reservation.builder()
                .id(10L)
                .guest(guest)
                .room(room)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .totalPrice(new BigDecimal("1350.00"))
                .status(ReservationStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        byte[] csvBytes = auditLogService.exportReservationsCsv(null, null);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csvContent).startsWith("\uFEFFsep=;");
        assertThat(csvContent).contains("Código de Reserva;Huésped;Email de Contacto;Teléfono;Habitación;Categoría;Fecha de Llegada;Fecha de Salida;Noches;Método de Pago;Estado de Pago;Tarifa Noche (USD);Total Hospedaje (USD);Estado de Reserva");
        assertThat(csvContent).contains("HOSP-10-2026");
        assertThat(csvContent).contains("Carlos Gomez");
        assertThat(csvContent).contains("Suite 401");
        assertThat(csvContent).contains("Penthouse Imperial");
        assertThat(csvContent).contains("1350.00");
    }

    @Test
    void exportFinancialJournalCsv_shouldIncludeBothReservationsAndOrders() {
        Guest guest = Guest.builder().firstName("Elena").lastName("Rostova").email("elena@gmail.com").build();
        Room room = Room.builder().roomNumber("302").build();
        Reservation reservation = Reservation.builder()
                .id(20L)
                .guest(guest)
                .room(room)
                .checkInDate(LocalDate.now())
                .totalPrice(new BigDecimal("600.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        RoomOrder order = RoomOrder.builder()
                .id(5L)
                .roomNumber("302")
                .therapistName("Sofia Martinez")
                .totalAmount(new BigDecimal("120.00"))
                .orderedAt(LocalDateTime.now())
                .status(OrderStatus.DELIVERED)
                .build();

        when(reservationRepository.findAll()).thenReturn(List.of(reservation));
        when(roomOrderRepository.findAllByOrderByOrderedAtDesc()).thenReturn(List.of(order));

        byte[] csvBytes = auditLogService.exportFinancialJournalCsv(null, null);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csvContent).startsWith("\uFEFFsep=;");
        assertThat(csvContent).contains("Fecha;Tipo de Registro;Folio / Referencia;Huésped / Suite;Concepto;Canal de Pago;Total (USD);Estado Contable");
        assertThat(csvContent).contains("Alojamiento");
        assertThat(csvContent).contains("HOSP-20-2026");
        assertThat(csvContent).contains("Elena Rostova");
        assertThat(csvContent).contains("Servicio Extra");
        assertThat(csvContent).contains("CMD-5");
        assertThat(csvContent).contains("Wellness & Spa Concierge");
        assertThat(csvContent).contains("120.00");
    }

    @Test
    void exportReservationsCsv_shouldNeutralizeCsvFormulaInjectionPrefixes() {
        Guest maliciousGuest = Guest.builder()
                .firstName("=CMD|'/C calc'!A0")
                .lastName("+SUM(1,1)")
                .email("@evil.com")
                .phone("-51999999999")
                .build();
        RoomType roomType = RoomType.builder().name("\tFormula").pricePerNight(new BigDecimal("100.00")).build();
        Room room = Room.builder().roomNumber("101").roomType(roomType).build();
        Reservation reservation = Reservation.builder()
                .id(99L)
                .guest(maliciousGuest)
                .room(room)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(1))
                .totalPrice(new BigDecimal("100.00"))
                .status(ReservationStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        String csvContent = new String(auditLogService.exportReservationsCsv(null, null), StandardCharsets.UTF_8);

        assertThat(csvContent).contains("'=CMD|'/C calc'!A0 +SUM(1,1)");
        assertThat(csvContent).contains("'@evil.com");
        assertThat(csvContent).contains("'-51999999999");
        assertThat(csvContent).contains("'\tFormula");
    }

    @Test
    void logAction_shouldUseRemoteAddrAndIgnoreForgedForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.20.30.40");
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        auditLogService.logAction("LOGIN", "User", "user1", "User login", "SUCCESS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("10.20.30.40");
    }
}
