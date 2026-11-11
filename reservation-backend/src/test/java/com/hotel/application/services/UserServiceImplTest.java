package com.hotel.application.services;

import com.hotel.application.services.impl.UserServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import com.hotel.domain.model.User;
import com.hotel.domain.model.WorkStatus;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.CreateStaffRequest;
import com.hotel.web.dto.UpdateStaffRequest;
import com.hotel.web.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void createStaff_WhenRoleIsAdmin_ShouldCreateSuccessfully() {
        CreateStaffRequest request = new CreateStaffRequest("new.admin", "admin@hospentra.com", "pass123", Role.ROLE_ADMIN, Shift.MORNING);
        when(userRepository.existsByUsername("new.admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@hospentra.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");

        User savedUser = User.builder()
                .id(10L)
                .username("new.admin")
                .email("admin@hospentra.com")
                .password("encodedPass")
                .role(Role.ROLE_ADMIN)
                .shift(Shift.MORNING)
                .enabled(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createStaff(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.role()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void createStaff_WhenRoleCustomer_ThrowsIllegalArgumentException() {
        CreateStaffRequest request = new CreateStaffRequest("cust", "cust@hotel.com", "pass123", Role.ROLE_CUSTOMER, Shift.MORNING);

        assertThatThrownBy(() -> userService.createStaff(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Staff accounts cannot have the CUSTOMER role");
    }

    @Test
    void createStaff_WhenUsernameTaken_ThrowsIllegalArgumentException() {
        CreateStaffRequest request = new CreateStaffRequest("existing", "new@hotel.com", "pass123", Role.ROLE_RECEPTIONIST, Shift.MORNING);
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> userService.createStaff(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void createStaff_WhenEmailInUse_ThrowsIllegalArgumentException() {
        CreateStaffRequest request = new CreateStaffRequest("newuser", "existing@hotel.com", "pass123", Role.ROLE_RECEPTIONIST, Shift.MORNING);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@hotel.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createStaff(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void updateStaff_Success() {
        User existingUser = User.builder()
                .id(2L)
                .username("maria.recep")
                .email("old@hotel.com")
                .role(Role.ROLE_RECEPTIONIST)
                .shift(Shift.MORNING)
                .enabled(true)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("new@hotel.com", 2L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStaffRequest request = new UpdateStaffRequest("new@hotel.com", Role.ROLE_ADMIN, Shift.NIGHT, null);
        UserResponse response = userService.updateStaff(2L, request, "superadmin");

        assertThat(response.email()).isEqualTo("new@hotel.com");
        assertThat(response.role()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(response.shift()).isEqualTo(Shift.NIGHT);
    }

    @Test
    void updateStaff_WithNewValidPassword_EncodesPassword() {
        User existingUser = User.builder()
                .id(2L)
                .username("maria.recep")
                .email("maria@hotel.com")
                .password("oldEncoded")
                .role(Role.ROLE_RECEPTIONIST)
                .shift(Shift.MORNING)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("maria@hotel.com", 2L)).thenReturn(false);
        when(passwordEncoder.encode("newSecretPass")).thenReturn("newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStaffRequest request = new UpdateStaffRequest("maria@hotel.com", Role.ROLE_RECEPTIONIST, Shift.EVENING, "newSecretPass");
        UserResponse response = userService.updateStaff(2L, request, "superadmin");

        assertThat(response).isNotNull();
        assertThat(existingUser.getPassword()).isEqualTo("newEncoded");
    }

    @Test
    void updateStaff_WithPasswordShorterThan6_ThrowsIllegalArgumentException() {
        User existingUser = User.builder().id(2L).username("test").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("test@hotel.com", 2L)).thenReturn(false);

        UpdateStaffRequest request = new UpdateStaffRequest("test@hotel.com", Role.ROLE_RECEPTIONIST, Shift.MORNING, "123");

        assertThatThrownBy(() -> userService.updateStaff(2L, request, "superadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La contraseña debe tener al menos 6 caracteres");
    }

    @Test
    void updateStaff_WhenEmailInUseByOther_ThrowsIllegalArgumentException() {
        User existingUser = User.builder().id(2L).username("test").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmailAndIdNot("duplicate@hotel.com", 2L)).thenReturn(true);

        UpdateStaffRequest request = new UpdateStaffRequest("duplicate@hotel.com", Role.ROLE_RECEPTIONIST, Shift.MORNING, null);

        assertThatThrownBy(() -> userService.updateStaff(2L, request, "superadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void updateStaff_WhenRoleCustomer_ThrowsException() {
        User existingUser = User.builder().id(2L).username("test").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));

        UpdateStaffRequest request = new UpdateStaffRequest("test@hotel.com", Role.ROLE_CUSTOMER, Shift.MORNING, null);

        assertThatThrownBy(() -> userService.updateStaff(2L, request, "superadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Staff accounts cannot have the CUSTOMER role");
    }

    @Test
    void updateStaff_WhenSelfRevokeAdmin_ThrowsException() {
        User adminUser = User.builder()
                .id(1L)
                .username("superadmin")
                .email("admin@hotel.com")
                .role(Role.ROLE_ADMIN)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmailAndIdNot("admin@hotel.com", 1L)).thenReturn(false);

        UpdateStaffRequest request = new UpdateStaffRequest("admin@hotel.com", Role.ROLE_RECEPTIONIST, Shift.MORNING, null);

        assertThatThrownBy(() -> userService.updateStaff(1L, request, "superadmin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No puede revocar su propio rol de Administrador");
    }

    @Test
    void toggleStaffStatus_WhenSelfDeactivation_ThrowsException() {
        User adminUser = User.builder()
                .id(1L)
                .username("superadmin")
                .enabled(true)
                .role(Role.ROLE_ADMIN)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.toggleStaffStatus(1L, "superadmin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No puede desactivar su propia cuenta de administrador en sesión");
    }

    @Test
    void toggleStaffStatus_WhenLastActiveAdmin_ThrowsException() {
        User otherAdmin = User.builder()
                .id(2L)
                .username("otheradmin")
                .enabled(true)
                .role(Role.ROLE_ADMIN)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRoleAndEnabledTrue(Role.ROLE_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.toggleStaffStatus(2L, "superadmin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede desactivar al único administrador activo del sistema");
    }

    @Test
    void toggleStaffStatus_DeactivateStaff_Success() {
        User staff = User.builder()
                .id(3L)
                .username("recepcionista")
                .enabled(true)
                .role(Role.ROLE_RECEPTIONIST)
                .build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.toggleStaffStatus(3L, "superadmin");

        assertThat(response.enabled()).isFalse();
    }

    @Test
    void toggleStaffStatus_ReactivateInactiveAdmin_Success() {
        User inactiveAdmin = User.builder()
                .id(4L)
                .username("antiguo.admin")
                .enabled(false)
                .role(Role.ROLE_ADMIN)
                .build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(inactiveAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.toggleStaffStatus(4L, "superadmin");

        assertThat(response.enabled()).isTrue();
    }

    @Test
    void updateWorkStatus_WhenOnBreak_SetsBreakStartedAt() {
        User user = User.builder().id(5L).username("camarista").role(Role.ROLE_RECEPTIONIST).workStatus(WorkStatus.ON_DUTY).build();
        when(userRepository.findByUsername("camarista")).thenReturn(Optional.of(user));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateWorkStatus(5L, WorkStatus.ON_BREAK, "camarista");

        assertThat(response.workStatus()).isEqualTo(WorkStatus.ON_BREAK);
        assertThat(user.getBreakStartedAt()).isNotNull();
    }

    @Test
    void updateWorkStatus_WhenOnDuty_ClearsBreakStartedAt() {
        User user = User.builder().id(5L).username("camarista").role(Role.ROLE_RECEPTIONIST).workStatus(WorkStatus.ON_BREAK).build();
        when(userRepository.findByUsername("camarista")).thenReturn(Optional.of(user));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateWorkStatus(5L, WorkStatus.ON_DUTY, "camarista");

        assertThat(response.workStatus()).isEqualTo(WorkStatus.ON_DUTY);
        assertThat(user.getBreakStartedAt()).isNull();
    }

    @Test
    void updateWorkStatus_WhenNonAdminTargetsAnotherUser_ThrowsAccessDeniedException() {
        User currentUser = User.builder().id(5L).username("camarista").role(Role.ROLE_RECEPTIONIST).build();
        when(userRepository.findByUsername("camarista")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> userService.updateWorkStatus(9L, WorkStatus.ON_BREAK, "camarista"))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateWorkStatus_WhenAdminTargetsAnotherUser_Succeeds() {
        User adminUser = User.builder().id(1L).username("admin").role(Role.ROLE_ADMIN).build();
        User targetUser = User.builder().id(9L).username("cocinero").role(Role.ROLE_KITCHEN).workStatus(WorkStatus.OFF_DUTY).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(9L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateWorkStatus(9L, WorkStatus.ON_DUTY, "admin");

        assertThat(response.workStatus()).isEqualTo(WorkStatus.ON_DUTY);
    }

    @Test
    void updateWorkStatus_WhenUsernameMissing_ThrowsAccessDeniedException() {
        assertThatThrownBy(() -> userService.updateWorkStatus(5L, WorkStatus.ON_BREAK, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findById_WhenFound_ReturnsResponse() {
        User user = User.builder().id(6L).username("chef").role(Role.ROLE_KITCHEN).build();
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(6L);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("chef");
    }

    @Test
    void findById_WhenNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_ReturnsAllStaff() {
        User user1 = User.builder().id(1L).username("u1").role(Role.ROLE_ADMIN).build();
        User user2 = User.builder().id(2L).username("u2").role(Role.ROLE_RECEPTIONIST).build();
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> list = userService.findAll();

        assertThat(list).hasSize(2);
    }
}
