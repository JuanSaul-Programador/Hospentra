package com.hotel.application.services;

import com.hotel.application.mapper.RoomTypeMapper;
import com.hotel.application.services.impl.RoomTypeServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.RoomTypeRepository;
import com.hotel.web.dto.RoomTypeRequest;
import com.hotel.web.dto.RoomTypeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    private RoomTypeMapper roomTypeMapper;
    private RoomTypeServiceImpl roomTypeService;

    private RoomType sampleRoomType;

    @BeforeEach
    void setUp() {
        roomTypeMapper = new RoomTypeMapper();
        roomTypeService = new RoomTypeServiceImpl(roomTypeRepository, roomTypeMapper);

        sampleRoomType = RoomType.builder()
                .id(1L)
                .name("Suite Presidencial")
                .nameEn("Presidential Suite")
                .description("Vista panorámica y jacuzzi")
                .descriptionEn("Panoramic view and jacuzzi")
                .capacity(4)
                .pricePerNight(new BigDecimal("450.00"))
                .build();
    }

    @Test
    void findAllReturnsAllRoomTypesWithBilingualFields() {
        when(roomTypeRepository.findAll()).thenReturn(List.of(sampleRoomType));

        List<RoomTypeResponse> result = roomTypeService.findAll();

        assertThat(result).hasSize(1);
        RoomTypeResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Suite Presidencial");
        assertThat(response.nameEn()).isEqualTo("Presidential Suite");
        assertThat(response.description()).isEqualTo("Vista panorámica y jacuzzi");
        assertThat(response.descriptionEn()).isEqualTo("Panoramic view and jacuzzi");
        assertThat(response.capacity()).isEqualTo(4);
        assertThat(response.pricePerNight()).isEqualByComparingTo("450.00");
    }

    @Test
    void findByIdSuccessWhenExists() {
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(sampleRoomType));

        RoomTypeResponse response = roomTypeService.findById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nameEn()).isEqualTo("Presidential Suite");
    }

    @Test
    void findByIdThrowsExceptionWhenNotFound() {
        when(roomTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomTypeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RoomType not found with id: 99");
    }

    @Test
    void createSuccessWithBilingualFields() {
        RoomTypeRequest request = new RoomTypeRequest(
                "Royal Suite",
                "Royal Suite En",
                "Terraza privada",
                "Private terrace",
                2,
                new BigDecimal("600.00")
        );

        when(roomTypeRepository.existsByName("Royal Suite")).thenReturn(false);
        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(invocation -> {
            RoomType entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        RoomTypeResponse response = roomTypeService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Royal Suite");
        assertThat(response.nameEn()).isEqualTo("Royal Suite En");
        assertThat(response.description()).isEqualTo("Terraza privada");
        assertThat(response.descriptionEn()).isEqualTo("Private terrace");
        assertThat(response.capacity()).isEqualTo(2);
        assertThat(response.pricePerNight()).isEqualByComparingTo("600.00");
        verify(roomTypeRepository).save(any(RoomType.class));
    }

    @Test
    void createThrowsExceptionWhenNameAlreadyExists() {
        RoomTypeRequest request = new RoomTypeRequest(
                "Suite Presidencial",
                "Presidential Suite",
                "Desc",
                "Desc En",
                2,
                new BigDecimal("300.00")
        );

        when(roomTypeRepository.existsByName("Suite Presidencial")).thenReturn(true);

        assertThatThrownBy(() -> roomTypeService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    void updateSuccessUpdatesBilingualFieldsAndPricing() {
        RoomTypeRequest updateRequest = new RoomTypeRequest(
                "Suite Presidencial Deluxe",
                "Presidential Suite Deluxe",
                "Nueva descripción",
                "New description",
                5,
                new BigDecimal("550.00")
        );

        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(sampleRoomType));
        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomTypeResponse response = roomTypeService.update(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Suite Presidencial Deluxe");
        assertThat(response.nameEn()).isEqualTo("Presidential Suite Deluxe");
        assertThat(response.description()).isEqualTo("Nueva descripción");
        assertThat(response.descriptionEn()).isEqualTo("New description");
        assertThat(response.capacity()).isEqualTo(5);
        assertThat(response.pricePerNight()).isEqualByComparingTo("550.00");
    }

    @Test
    void updateThrowsExceptionWhenNotFound() {
        RoomTypeRequest updateRequest = new RoomTypeRequest(
                "Inexistente",
                "Nonexistent",
                "Desc",
                "Desc En",
                2,
                new BigDecimal("200.00")
        );

        when(roomTypeRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomTypeService.update(88L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RoomType not found with id: 88");

        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    void deleteSuccessWhenExists() {
        when(roomTypeRepository.existsById(1L)).thenReturn(true);

        roomTypeService.delete(1L);

        verify(roomTypeRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteThrowsExceptionWhenNotFound() {
        when(roomTypeRepository.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() -> roomTypeService.delete(77L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RoomType not found with id: 77");

        verify(roomTypeRepository, never()).deleteById(anyLong());
    }
}
