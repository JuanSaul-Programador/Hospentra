package com.hotel.application.services;

import com.hotel.application.services.impl.AnalyticsServiceImpl;
import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.ExtraService;
import com.hotel.domain.model.OrderStatus;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Room;
import com.hotel.domain.model.RoomOrder;
import com.hotel.domain.model.RoomOrderItem;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.web.dto.HotelAnalyticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomOrderRepository roomOrderRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private Room testRoom;
    private RoomType testRoomType;

    @BeforeEach
    void setUp() {
        testRoomType = RoomType.builder()
                .id(1L)
                .name("Penthouse Imperial")
                .build();

        testRoom = Room.builder()
                .id(101L)
                .roomNumber("101")
                .roomType(testRoomType)
                .build();
    }

    @Test
    void getExecutiveAnalytics_CalculatesCorrectly() {
        Reservation res = Reservation.builder()
                .id(1L)
                .room(testRoom)
                .checkInDate(LocalDate.now().minusDays(3))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.COMPLETED)
                .totalPrice(new BigDecimal("1500.00"))
                .build();

        ExtraService gastroService = ExtraService.builder()
                .id(1L)
                .category(ExtraCategory.GASTRONOMY)
                .build();

        RoomOrderItem item = RoomOrderItem.builder()
                .id(1L)
                .extraService(gastroService)
                .subtotal(new BigDecimal("120.00"))
                .build();

        RoomOrder order = RoomOrder.builder()
                .id(1L)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("120.00"))
                .orderedAt(LocalDateTime.now())
                .items(List.of(item))
                .build();

        when(roomRepository.findAll()).thenReturn(List.of(testRoom));
        when(reservationRepository.findAll()).thenReturn(List.of(res));
        when(roomOrderRepository.findAll()).thenReturn(List.of(order));

        HotelAnalyticsResponse response = analyticsService.getExecutiveAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getRoomRevenue()).isEqualTo(1500.00);
        assertThat(response.getExtrasRevenue()).isEqualTo(120.00);
        assertThat(response.getTotalRevenue()).isEqualTo(1620.00);
        assertThat(response.getGastronomyRevenue()).isEqualTo(120.00);
        assertThat(response.getTotalReservations()).isEqualTo(1);
        assertThat(response.getTotalNightsSold()).isEqualTo(3);
        assertThat(response.getAdr()).isEqualTo(500.00);
        assertThat(response.getSuitePerformances()).isNotEmpty();
        assertThat(response.getSuitePerformances().get(0).getRoomNumber()).isEqualTo("101");
        assertThat(response.getMonthlyTrends()).isNotEmpty();
    }

    @Test
    void getExecutiveAnalytics_WhenEmptyRepositories_ReturnsZeroMetricsWithoutExceptions() {
        when(roomRepository.findAll()).thenReturn(List.of());
        when(reservationRepository.findAll()).thenReturn(List.of());
        when(roomOrderRepository.findAll()).thenReturn(List.of());

        HotelAnalyticsResponse response = analyticsService.getExecutiveAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualTo(0.0);
        assertThat(response.getRoomRevenue()).isEqualTo(0.0);
        assertThat(response.getExtrasRevenue()).isEqualTo(0.0);
        assertThat(response.getAdr()).isEqualTo(0.0);
        assertThat(response.getRevpar()).isEqualTo(0.0);
        assertThat(response.getOccupancyRate()).isEqualTo(0.0);
        assertThat(response.getTotalReservations()).isEqualTo(0);
        assertThat(response.getTotalNightsSold()).isEqualTo(0L);
        assertThat(response.getSuitePerformances()).isEmpty();
        assertThat(response.getMonthlyTrends()).isNotEmpty();
    }

    @Test
    void getExecutiveAnalytics_ExcludesCancelledReservationsAndOrders() {
        Reservation validRes = Reservation.builder()
                .id(1L)
                .room(testRoom)
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(new BigDecimal("800.00"))
                .build();

        Reservation cancelledRes = Reservation.builder()
                .id(2L)
                .room(testRoom)
                .checkInDate(LocalDate.now().minusDays(5))
                .checkOutDate(LocalDate.now().minusDays(3))
                .status(ReservationStatus.CANCELLED)
                .totalPrice(new BigDecimal("2000.00"))
                .build();

        RoomOrder validOrder = RoomOrder.builder()
                .id(1L)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("50.00"))
                .orderedAt(LocalDateTime.now())
                .build();

        RoomOrder cancelledOrder = RoomOrder.builder()
                .id(2L)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("300.00"))
                .orderedAt(LocalDateTime.now())
                .build();

        when(roomRepository.findAll()).thenReturn(List.of(testRoom));
        when(reservationRepository.findAll()).thenReturn(List.of(validRes, cancelledRes));
        when(roomOrderRepository.findAll()).thenReturn(List.of(validOrder, cancelledOrder));

        HotelAnalyticsResponse response = analyticsService.getExecutiveAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getRoomRevenue()).isEqualTo(800.00);
        assertThat(response.getExtrasRevenue()).isEqualTo(50.00);
        assertThat(response.getTotalRevenue()).isEqualTo(850.00);
        assertThat(response.getTotalReservations()).isEqualTo(1);
    }

    @Test
    void getExecutiveAnalytics_CalculatesDifferentCategoriesBreakdownCorrectly() {
        ExtraService barService = ExtraService.builder()
                .id(10L)
                .category(ExtraCategory.BAR_WINE)
                .build();

        ExtraService spaService = ExtraService.builder()
                .id(11L)
                .category(ExtraCategory.WELLNESS_SPA)
                .build();

        RoomOrderItem barItem = RoomOrderItem.builder()
                .id(1L)
                .extraService(barService)
                .subtotal(new BigDecimal("75.00"))
                .build();

        RoomOrderItem spaItem = RoomOrderItem.builder()
                .id(2L)
                .extraService(spaService)
                .subtotal(new BigDecimal("220.00"))
                .build();

        RoomOrder order = RoomOrder.builder()
                .id(1L)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("295.00"))
                .orderedAt(LocalDateTime.now())
                .items(List.of(barItem, spaItem))
                .build();

        when(roomRepository.findAll()).thenReturn(List.of(testRoom));
        when(reservationRepository.findAll()).thenReturn(List.of());
        when(roomOrderRepository.findAll()).thenReturn(List.of(order));

        HotelAnalyticsResponse response = analyticsService.getExecutiveAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getBarRevenue()).isEqualTo(75.00);
        assertThat(response.getSpaRevenue()).isEqualTo(220.00);
        assertThat(response.getExtrasRevenue()).isEqualTo(295.00);
    }

    @Test
    void getExecutiveAnalytics_SortsSuitesByRevenueDescending() {
        Room room2 = Room.builder()
                .id(102L)
                .roomNumber("102")
                .roomType(testRoomType)
                .build();

        Reservation resRoom1 = Reservation.builder()
                .id(1L)
                .room(testRoom)
                .checkInDate(LocalDate.now().minusDays(1))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.COMPLETED)
                .totalPrice(new BigDecimal("300.00"))
                .build();

        Reservation resRoom2 = Reservation.builder()
                .id(2L)
                .room(room2)
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.COMPLETED)
                .totalPrice(new BigDecimal("1200.00"))
                .build();

        when(roomRepository.findAll()).thenReturn(List.of(testRoom, room2));
        when(reservationRepository.findAll()).thenReturn(List.of(resRoom1, resRoom2));
        when(roomOrderRepository.findAll()).thenReturn(List.of());

        HotelAnalyticsResponse response = analyticsService.getExecutiveAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getSuitePerformances()).hasSize(2);
        assertThat(response.getSuitePerformances().get(0).getRoomNumber()).isEqualTo("102");
        assertThat(response.getSuitePerformances().get(0).getRevenue()).isEqualTo(1200.00);
        assertThat(response.getSuitePerformances().get(1).getRoomNumber()).isEqualTo("101");
        assertThat(response.getSuitePerformances().get(1).getRevenue()).isEqualTo(300.00);
    }
}
