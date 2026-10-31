package com.hotel.application.services.impl;

import com.hotel.application.services.AuditLogService;
import com.hotel.domain.model.AuditLog;
import com.hotel.domain.model.OrderStatus;
import com.hotel.domain.model.PaymentMethod;
import com.hotel.domain.model.PaymentStatus;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.RoomOrder;
import com.hotel.domain.repository.AuditLogRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.web.dto.AuditLogResponse;
import com.hotel.web.dto.AuditMetricsResponse;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ReservationRepository reservationRepository;
    private final RoomOrderRepository roomOrderRepository;

    @Override
    @Transactional
    public void logAction(String action, String entityName, String entityId, String details, String status) {
        String username = "system";
        String userRole = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
            if (!auth.getAuthorities().isEmpty()) {
                userRole = auth.getAuthorities().iterator().next().getAuthority();
            }
        }

        String ipAddress = "127.0.0.1";
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                ipAddress = forwarded.split(",")[0].trim();
            } else {
                ipAddress = request.getRemoteAddr();
            }
        }

        logAction(username, userRole, action, entityName, entityId, details, ipAddress, status);
    }

    @Override
    @Transactional
    public void logAction(String username, String userRole, String action, String entityName, String entityId, String details, String ipAddress, String status) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username != null ? username : "system")
                    .userRole(userRole != null ? userRole : "SYSTEM")
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                    .status(status != null ? status : "SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist audit log for action: {}", action, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String action, String username, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }

            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableActions() {
        return auditLogRepository.findDistinctActions();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditMetricsResponse getMetrics() {
        long totalEvents = auditLogRepository.count();
        long todayEvents = auditLogRepository.countByCreatedAtGreaterThanEqual(LocalDate.now().atStartOfDay());
        long failedEvents = auditLogRepository.countByStatus("FAILURE");
        long distinctOperators = auditLogRepository.findDistinctActions().size();

        return AuditMetricsResponse.builder()
                .totalEvents(totalEvents)
                .todayEvents(todayEvents)
                .failedEvents(failedEvents)
                .distinctOperators(distinctOperators)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReservationsCsv(LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = reservationRepository.findAll();

        if (startDate != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getCheckInDate() != null && !r.getCheckInDate().isBefore(startDate))
                    .toList();
        }

        if (endDate != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getCheckOutDate() != null && !r.getCheckOutDate().isAfter(endDate))
                    .toList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("sep=;\r\n");
        sb.append("Código de Reserva;Huésped;Email de Contacto;Teléfono;Habitación;Categoría;Fecha de Llegada;Fecha de Salida;Noches;Método de Pago;Estado de Pago;Tarifa Noche (USD);Total Hospedaje (USD);Estado de Reserva\r\n");

        for (Reservation r : reservations) {
            String code = "HOSP-" + r.getId() + "-2026";
            String guestName = r.getGuest() != null ? r.getGuest().getFirstName() + " " + r.getGuest().getLastName() : "N/A";
            String guestEmail = r.getGuest() != null ? r.getGuest().getEmail() : "N/A";
            String guestPhone = r.getGuest() != null && r.getGuest().getPhone() != null ? r.getGuest().getPhone() : "";
            String roomNumber = r.getRoom() != null ? "Suite " + r.getRoom().getRoomNumber() : "N/A";
            String roomTypeName = r.getRoom() != null && r.getRoom().getRoomType() != null ? r.getRoom().getRoomType().getName() : "N/A";
            long nights = (r.getCheckInDate() != null && r.getCheckOutDate() != null)
                    ? Math.max(1, ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate()))
                    : 1;

            sb.append(escapeCsv(code)).append(';')
                    .append(escapeCsv(guestName)).append(';')
                    .append(escapeCsv(guestEmail)).append(';')
                    .append(escapeCsv(guestPhone)).append(';')
                    .append(escapeCsv(roomNumber)).append(';')
                    .append(escapeCsv(roomTypeName)).append(';')
                    .append(r.getCheckInDate() != null ? r.getCheckInDate().toString() : "").append(';')
                    .append(r.getCheckOutDate() != null ? r.getCheckOutDate().toString() : "").append(';')
                    .append(nights).append(';')
                    .append(escapeCsv(formatPaymentMethod(r.getPaymentMethod()))).append(';')
                    .append(escapeCsv(formatPaymentStatus(r.getPaymentStatus()))).append(';')
                    .append(r.getRoom() != null && r.getRoom().getRoomType() != null ? r.getRoom().getRoomType().getPricePerNight() : "0.00").append(';')
                    .append(r.getTotalPrice() != null ? r.getTotalPrice().toString() : "0.00").append(';')
                    .append(escapeCsv(formatReservationStatus(r.getStatus())))
                    .append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportFinancialJournalCsv(LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("sep=;\r\n");
        sb.append("Fecha;Tipo de Registro;Folio / Referencia;Huésped / Suite;Concepto;Canal de Pago;Total (USD);Estado Contable\r\n");

        List<Reservation> reservations = reservationRepository.findAll();
        for (Reservation r : reservations) {
            LocalDate date = r.getCheckInDate() != null ? r.getCheckInDate() : LocalDate.now();
            if (startDate != null && date.isBefore(startDate)) continue;
            if (endDate != null && date.isAfter(endDate)) continue;

            String code = "HOSP-" + r.getId() + "-2026";
            String guestName = r.getGuest() != null ? r.getGuest().getFirstName() + " " + r.getGuest().getLastName() : "N/A";
            String roomInfo = r.getRoom() != null ? "Suite " + r.getRoom().getRoomNumber() : "Suite General";

            sb.append(date).append(';')
                    .append("Alojamiento").append(';')
                    .append(escapeCsv(code)).append(';')
                    .append(escapeCsv(guestName + " (" + roomInfo + ")")).append(';')
                    .append(escapeCsv("Estancia Hospentra Sanctuary")).append(';')
                    .append(escapeCsv(formatPaymentMethod(r.getPaymentMethod()))).append(';')
                    .append(r.getTotalPrice() != null ? r.getTotalPrice().toString() : "0.00").append(';')
                    .append(escapeCsv(formatPaymentStatus(r.getPaymentStatus())))
                    .append("\r\n");
        }

        List<RoomOrder> orders = roomOrderRepository.findAllByOrderByOrderedAtDesc();
        for (RoomOrder o : orders) {
            LocalDate date = o.getOrderedAt() != null ? o.getOrderedAt().toLocalDate() : LocalDate.now();
            if (startDate != null && date.isBefore(startDate)) continue;
            if (endDate != null && date.isAfter(endDate)) continue;

            String roomInfo = o.getRoomNumber() != null ? "Suite " + o.getRoomNumber() : "En Suite";
            String concept = o.getTherapistName() != null ? "Wellness & Spa Concierge" : "Gastronomía Room Service";

            sb.append(date).append(';')
                    .append("Servicio Extra").append(';')
                    .append(escapeCsv("CMD-" + o.getId())).append(';')
                    .append(escapeCsv(roomInfo)).append(';')
                    .append(escapeCsv(concept)).append(';')
                    .append("Carga a Folio de Suite").append(';')
                    .append(o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0.00").append(';')
                    .append(escapeCsv(formatOrderStatus(o.getStatus())))
                    .append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null) return "No Especificado";
        return switch (method) {
            case PAY_AT_CHECKIN -> "Pago en Recepción (Front Desk)";
            case CREDIT_CARD -> "Tarjeta de Crédito / Débito";
        };
    }

    private String formatPaymentStatus(PaymentStatus status) {
        if (status == null) return "No Especificado";
        return switch (status) {
            case PAID -> "Liquidado / Pagado";
            case PENDING -> "Pendiente de Cobro";
            case REFUNDED -> "Reembolsado";
            case PARTIALLY_REFUNDED -> "Reembolso Parcial";
        };
    }

    private String formatReservationStatus(ReservationStatus status) {
        if (status == null) return "No Especificado";
        return switch (status) {
            case CHECKED_IN -> "En Casa (Check-In)";
            case CONFIRMED -> "Confirmada (Por Ingresar)";
            case COMPLETED -> "Estancia Completada";
            case PENDING -> "Pendiente de Pago";
            case CANCELLED -> "Cancelada";
        };
    }

    private String formatOrderStatus(OrderStatus status) {
        if (status == null) return "No Especificado";
        return switch (status) {
            case RECEIVED -> "Recibida en Cocina";
            case PREPARING -> "En Preparación";
            case READY_FOR_DELIVERY -> "En Ruta a Suite";
            case DELIVERED -> "Entregado en Suite";
            case CANCELLED -> "Cancelado";
        };
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .userRole(log.getUserRole())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
