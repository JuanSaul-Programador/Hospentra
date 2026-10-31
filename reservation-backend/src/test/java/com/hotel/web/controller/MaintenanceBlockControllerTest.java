package com.hotel.web.controller;

import com.hotel.application.services.MaintenanceBlockService;
import com.hotel.domain.model.BlockStatus;
import com.hotel.domain.model.BlockType;
import com.hotel.web.dto.MaintenanceBlockRequest;
import com.hotel.web.dto.MaintenanceBlockResponse;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaintenanceBlockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MaintenanceBlockService maintenanceBlockService;

    @InjectMocks
    private MaintenanceBlockController maintenanceBlockController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(maintenanceBlockController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createBlock_Returns201Created() throws Exception {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(4);
        String requestJson = String.format("""
                {
                    "roomId": 10,
                    "blockType": "OUT_OF_ORDER",
                    "startDate": "%s",
                    "endDate": "%s",
                    "reason": "Fuga en jacuzzi",
                    "contractor": "Plomería S.A."
                }
                """, start, end);

        MaintenanceBlockResponse response = MaintenanceBlockResponse.builder()
                .id(1L)
                .roomId(10L)
                .roomNumber("201")
                .roomTypeName("Royal Suite")
                .blockType(BlockType.OUT_OF_ORDER)
                .startDate(start)
                .endDate(end)
                .reason("Fuga en jacuzzi")
                .contractorName("Plomería S.A.")
                .status(BlockStatus.ACTIVE)
                .createdBy("admin1")
                .createdAt(LocalDateTime.now())
                .durationDays(4)
                .build();

        when(maintenanceBlockService.createBlock(any(MaintenanceBlockRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/maintenance-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomNumber").value("201"))
                .andExpect(jsonPath("$.blockType").value("OUT_OF_ORDER"));
    }

    @Test
    void createBlock_WhenMissingFields_Returns400BadRequest() throws Exception {
        String invalidJson = """
                {
                    "roomId": null,
                    "blockType": null,
                    "startDate": null,
                    "endDate": null,
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/admin/maintenance-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBlocks_Returns200Ok() throws Exception {
        MaintenanceBlockResponse block = MaintenanceBlockResponse.builder()
                .id(1L)
                .roomNumber("201")
                .blockType(BlockType.OUT_OF_ORDER)
                .status(BlockStatus.ACTIVE)
                .build();

        when(maintenanceBlockService.getAllBlocks()).thenReturn(List.of(block));

        mockMvc.perform(get("/api/admin/maintenance-blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("201"));
    }

    @Test
    void getActiveBlocks_Returns200Ok() throws Exception {
        MaintenanceBlockResponse block = MaintenanceBlockResponse.builder()
                .id(2L)
                .roomNumber("301")
                .blockType(BlockType.OUT_OF_SERVICE)
                .status(BlockStatus.ACTIVE)
                .build();

        when(maintenanceBlockService.getActiveBlocks(any(), any())).thenReturn(List.of(block));

        mockMvc.perform(get("/api/admin/maintenance-blocks/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("301"));
    }

    @Test
    void resolveBlock_Returns200Ok() throws Exception {
        MaintenanceBlockResponse resolved = MaintenanceBlockResponse.builder()
                .id(1L)
                .roomNumber("201")
                .status(BlockStatus.RESOLVED)
                .build();

        when(maintenanceBlockService.resolveBlock(1L)).thenReturn(resolved);

        mockMvc.perform(patch("/api/admin/maintenance-blocks/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void cancelBlock_Returns200Ok() throws Exception {
        MaintenanceBlockResponse cancelled = MaintenanceBlockResponse.builder()
                .id(1L)
                .roomNumber("201")
                .status(BlockStatus.CANCELLED)
                .build();

        when(maintenanceBlockService.cancelBlock(1L)).thenReturn(cancelled);

        mockMvc.perform(patch("/api/admin/maintenance-blocks/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
