package com.hotel.web.controller;

import com.hotel.application.services.UserService;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import com.hotel.domain.model.User;
import com.hotel.domain.model.WorkStatus;
import com.hotel.domain.repository.UserRepository;
import com.hotel.infrastructure.security.config.SecurityConfig;
import com.hotel.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.hotel.infrastructure.security.jwt.JwtService;
import com.hotel.web.dto.CreateStaffRequest;
import com.hotel.web.dto.UpdateStaffRequest;
import com.hotel.web.dto.UpdateWorkStatusRequest;
import com.hotel.web.dto.UserResponse;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private String adminToken;
    private String receptionistToken;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .id(1L)
                .username("superadmin")
                .email("admin@hospentra.com")
                .password("encoded")
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        User receptionist = User.builder()
                .id(2L)
                .username("maria")
                .email("maria@hotel.com")
                .password("encoded")
                .role(Role.ROLE_RECEPTIONIST)
                .enabled(true)
                .build();

        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(receptionist));

        adminToken = jwtService.generateToken(admin);
        receptionistToken = jwtService.generateToken(receptionist);
    }

    @Test
    void createStaff_ValidAdminRequest_Returns201Created() throws Exception {
        CreateStaffRequest request = new CreateStaffRequest("carlos.admin", "carlos@hospentra.com", "Pass1234", Role.ROLE_ADMIN, Shift.MORNING);
        UserResponse response = new UserResponse(10L, "carlos.admin", "carlos@hospentra.com", Role.ROLE_ADMIN, true, LocalDateTime.now(), Shift.MORNING, WorkStatus.OFF_DUTY, null);

        when(userService.createStaff(any(CreateStaffRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.username").value("carlos.admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void createStaff_InvalidEmail_Returns400BadRequest() throws Exception {
        CreateStaffRequest request = new CreateStaffRequest("test", "invalid-email", "Pass1234", Role.ROLE_RECEPTIONIST, Shift.MORNING);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStaff_ShortPassword_Returns400BadRequest() throws Exception {
        CreateStaffRequest request = new CreateStaffRequest("test", "valid@hotel.com", "123", Role.ROLE_RECEPTIONIST, Shift.MORNING);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStaff_ValidRequest_Returns200Ok() throws Exception {
        UpdateStaffRequest request = new UpdateStaffRequest("newmail@hospentra.com", Role.ROLE_ADMIN, Shift.NIGHT, "NewSecret123");
        UserResponse response = new UserResponse(5L, "sofia", "newmail@hospentra.com", Role.ROLE_ADMIN, true, LocalDateTime.now(), Shift.NIGHT, WorkStatus.OFF_DUTY, null);

        when(userService.updateStaff(eq(5L), any(UpdateStaffRequest.class), eq("superadmin"))).thenReturn(response);

        mockMvc.perform(put("/api/users/5")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.email").value("newmail@hospentra.com"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.shift").value("NIGHT"));
    }

    @Test
    void updateStaff_InvalidEmail_Returns400BadRequest() throws Exception {
        UpdateStaffRequest request = new UpdateStaffRequest("not-an-email", Role.ROLE_RECEPTIONIST, Shift.MORNING, null);

        mockMvc.perform(put("/api/users/5")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStaff_WhenEmailInUse_Returns409Conflict() throws Exception {
        UpdateStaffRequest request = new UpdateStaffRequest("occupied@hospentra.com", Role.ROLE_RECEPTIONIST, Shift.MORNING, null);

        when(userService.updateStaff(eq(5L), any(UpdateStaffRequest.class), eq("superadmin")))
                .thenThrow(new IllegalArgumentException("Email 'occupied@hospentra.com' is already in use"));

        mockMvc.perform(put("/api/users/5")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email 'occupied@hospentra.com' is already in use"));
    }

    @Test
    void updateStaff_WhenSelfRevokeAdmin_Returns400BadRequest() throws Exception {
        UpdateStaffRequest request = new UpdateStaffRequest("admin@hospentra.com", Role.ROLE_RECEPTIONIST, Shift.MORNING, null);

        when(userService.updateStaff(eq(1L), any(UpdateStaffRequest.class), eq("superadmin")))
                .thenThrow(new IllegalStateException("No puede revocar su propio rol de Administrador"));

        mockMvc.perform(put("/api/users/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No puede revocar su propio rol de Administrador"));
    }

    @Test
    void toggleStaffStatus_Success_Returns200Ok() throws Exception {
        UserResponse response = new UserResponse(3L, "laura", "laura@hotel.com", Role.ROLE_RECEPTIONIST, false, LocalDateTime.now(), Shift.MORNING, WorkStatus.OFF_DUTY, null);

        when(userService.toggleStaffStatus(eq(3L), eq("superadmin"))).thenReturn(response);

        mockMvc.perform(patch("/api/users/3/toggle-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void toggleStaffStatus_WhenSelfDeactivation_Returns400BadRequest() throws Exception {
        when(userService.toggleStaffStatus(eq(1L), eq("superadmin")))
                .thenThrow(new IllegalStateException("No puede desactivar su propia cuenta de administrador en sesión"));

        mockMvc.perform(patch("/api/users/1/toggle-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No puede desactivar su propia cuenta de administrador en sesión"));
    }

    @Test
    void toggleStaffStatus_WhenLastAdmin_Returns400BadRequest() throws Exception {
        when(userService.toggleStaffStatus(eq(2L), eq("superadmin")))
                .thenThrow(new IllegalStateException("No se puede desactivar al único administrador activo del sistema"));

        mockMvc.perform(patch("/api/users/2/toggle-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No se puede desactivar al único administrador activo del sistema"));
    }

    @Test
    void updateWorkStatus_ValidRequest_Returns200Ok() throws Exception {
        UpdateWorkStatusRequest request = new UpdateWorkStatusRequest(WorkStatus.ON_BREAK);
        UserResponse response = new UserResponse(2L, "maria", "maria@hotel.com", Role.ROLE_RECEPTIONIST, true, LocalDateTime.now(), Shift.MORNING, WorkStatus.ON_BREAK, LocalDateTime.now());

        when(userService.updateWorkStatus(eq(2L), eq(WorkStatus.ON_BREAK), eq("maria"))).thenReturn(response);

        mockMvc.perform(patch("/api/users/2/work-status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workStatus").value("ON_BREAK"));
    }

    @Test
    void findAll_Returns200Ok() throws Exception {
        UserResponse u1 = new UserResponse(1L, "admin", "admin@hospentra.com", Role.ROLE_ADMIN, true, null, Shift.MORNING, WorkStatus.ON_DUTY, null);
        when(userService.findAll()).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    void findById_Returns200Ok() throws Exception {
        UserResponse u1 = new UserResponse(2L, "receptionist", "recep@hospentra.com", Role.ROLE_RECEPTIONIST, true, null, Shift.EVENING, WorkStatus.ON_DUTY, null);
        when(userService.findById(2L)).thenReturn(u1);

        mockMvc.perform(get("/api/users/2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.role").value("ROLE_RECEPTIONIST"));
    }
}
