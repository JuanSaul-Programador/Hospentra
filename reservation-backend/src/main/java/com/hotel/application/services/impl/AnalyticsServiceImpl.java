package com.hotel.application.services.impl;

import com.hotel.application.services.AnalyticsService;
import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.OrderStatus;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Room;
import com.hotel.domain.model.RoomOrder;
import com.hotel.domain.model.RoomOrderItem;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.web.dto.HotelAnalyticsResponse;
import com.hotel.web.dto.MonthlyTrendDto;
import com.hotel.web.dto.SuitePerformanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ReservationRepository reservationRepository;
    private final RoomOrderRepository roomOrderRepository;
    private final RoomRepository roomRepository;

    @Override
    public HotelAnalyticsResponse getExecutiveAnalytics() {
        List<Room> allRooms = roomRepository.findAll();
        List<Reservation> validReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .toList();

        List<RoomOrder> validOrders = roomOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        double roomRevenue = validReservations.stream()
                .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                .sum();

        double extrasRevenue = validOrders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                .sum();

        double gastronomyRevenue = 0.0;
        double barRevenue = 0.0;
        double spaRevenue = 0.0;

        for (RoomOrder order : validOrders) {
            if (order.getItems() != null) {
                for (RoomOrderItem item : order.getItems()) {
                    double itemTotal = item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0.0;
                    ExtraCategory cat = (item.getExtraService() != null) ? item.getExtraService().getCategory() : null;
                    if (cat == ExtraCategory.GASTRONOMY) {
                        gastronomyRevenue += itemTotal;
                    } else if (cat == ExtraCategory.BAR_WINE) {
                        barRevenue += itemTotal;
                    } else if (cat == ExtraCategory.WELLNESS_SPA) {
                        spaRevenue += itemTotal;
                    } else {
                        gastronomyRevenue += itemTotal;
                    }
                }
            }
        }

        long totalNightsSold = validReservations.stream()
                .mapToLong(r -> {
                    if (r.getCheckInDate() != null && r.getCheckOutDate() != null) {
                        long diff = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
                        return diff > 0 ? diff : 1L;
                    }
                    return 1L;
                })
                .sum();

        int totalRoomCount = Math.max(1, allRooms.size());
        long availableCapacity = (long) totalRoomCount * 30L;
        double occupancyRate = Math.min(100.0, Math.round((totalNightsSold * 100.0 / availableCapacity) * 10.0) / 10.0);
        double adr = totalNightsSold > 0 ? Math.round((roomRevenue / totalNightsSold) * 100.0) / 100.0 : 0.0;
        double revpar = Math.round((roomRevenue / availableCapacity) * 100.0) / 100.0;
        double totalRevenue = Math.round((roomRevenue + extrasRevenue) * 100.0) / 100.0;

        List<SuitePerformanceDto> suitePerformances = new ArrayList<>();
        for (Room room : allRooms) {
            List<Reservation> roomResList = validReservations.stream()
                    .filter(r -> r.getRoom() != null && r.getRoom().getId().equals(room.getId()))
                    .toList();

            long nights = roomResList.stream()
                    .mapToLong(r -> {
                        long d = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
                        return d > 0 ? d : 1L;
                    })
                    .sum();

            double rev = roomResList.stream()
                    .mapToDouble(r -> r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0)
                    .sum();

            double roomOcc = Math.min(100.0, Math.round((nights * 100.0 / 30.0) * 10.0) / 10.0);
            double roomAdr = nights > 0 ? Math.round((rev / nights) * 100.0) / 100.0 : 0.0;

            String typeName = (room.getRoomType() != null) ? room.getRoomType().getName() : "Suite Boutique";

            suitePerformances.add(SuitePerformanceDto.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomTypeName(typeName)
                    .nightsOccupied(nights)
                    .revenue(Math.round(rev * 100.0) / 100.0)
                    .occupancyRate(roomOcc)
                    .adr(roomAdr)
                    .build());
        }

        suitePerformances.sort(Comparator.comparingDouble(SuitePerformanceDto::getRevenue).reversed());

        Map<String, MonthlyTrendDto> trendsMap = new LinkedHashMap<>();
        String[] defaultMonths = {"Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre"};
        for (String m : defaultMonths) {
            trendsMap.put(m, MonthlyTrendDto.builder()
                    .month(m)
                    .roomRevenue(0.0)
                    .extrasRevenue(0.0)
                    .totalRevenue(0.0)
                    .occupancyRate(0.0)
                    .build());
        }

        for (Reservation r : validReservations) {
            if (r.getCheckInDate() != null) {
                String monthName = r.getCheckInDate().getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                MonthlyTrendDto trend = trendsMap.computeIfAbsent(monthName, k -> MonthlyTrendDto.builder()
                        .month(k)
                        .roomRevenue(0.0)
                        .extrasRevenue(0.0)
                        .totalRevenue(0.0)
                        .occupancyRate(0.0)
                        .build());
                double rPrice = r.getTotalPrice() != null ? r.getTotalPrice().doubleValue() : 0.0;
                trend.setRoomRevenue(Math.round((trend.getRoomRevenue() + rPrice) * 100.0) / 100.0);
                trend.setTotalRevenue(Math.round((trend.getTotalRevenue() + rPrice) * 100.0) / 100.0);
            }
        }

        for (RoomOrder o : validOrders) {
            if (o.getOrderedAt() != null) {
                String monthName = o.getOrderedAt().getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                MonthlyTrendDto trend = trendsMap.computeIfAbsent(monthName, k -> MonthlyTrendDto.builder()
                        .month(k)
                        .roomRevenue(0.0)
                        .extrasRevenue(0.0)
                        .totalRevenue(0.0)
                        .occupancyRate(0.0)
                        .build());
                double oPrice = o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0;
                trend.setExtrasRevenue(Math.round((trend.getExtrasRevenue() + oPrice) * 100.0) / 100.0);
                trend.setTotalRevenue(Math.round((trend.getTotalRevenue() + oPrice) * 100.0) / 100.0);
            }
        }

        List<MonthlyTrendDto> monthlyTrends = new ArrayList<>(trendsMap.values());

        return HotelAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .roomRevenue(Math.round(roomRevenue * 100.0) / 100.0)
                .extrasRevenue(Math.round(extrasRevenue * 100.0) / 100.0)
                .gastronomyRevenue(Math.round(gastronomyRevenue * 100.0) / 100.0)
                .barRevenue(Math.round(barRevenue * 100.0) / 100.0)
                .spaRevenue(Math.round(spaRevenue * 100.0) / 100.0)
                .totalReservations(validReservations.size())
                .totalNightsSold(totalNightsSold)
                .occupancyRate(occupancyRate)
                .adr(adr)
                .revpar(revpar)
                .suitePerformances(suitePerformances)
                .monthlyTrends(monthlyTrends)
                .build();
    }
}
