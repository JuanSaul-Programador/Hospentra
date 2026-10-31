package com.hotel.application.scheduler;

import com.hotel.domain.model.Guest;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountPurgeSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestRepository guestRepository;

    private AccountPurgeScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AccountPurgeScheduler(userRepository, guestRepository);
    }

    @Test
    void purgeExpiredAccounts_WhenExpiredAccountsExist_PurgesUserAndAnonymizesGuest() {
        Guest guest = Guest.builder()
                .id(99L)
                .firstName("Carlos")
                .lastName("Gomez")
                .email("carlos@gmail.com")
                .phone("999123456")
                .build();

        User expiredUser = User.builder()
                .id(50L)
                .username("carlos.g")
                .scheduledDeletionDate(LocalDateTime.now().minusDays(1))
                .guest(guest)
                .build();

        when(userRepository.findByScheduledDeletionDateLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredUser));

        int purgedCount = scheduler.purgeExpiredAccounts();

        assertThat(purgedCount).isEqualTo(1);
        assertThat(guest.getFirstName()).isEqualTo("Huésped");
        assertThat(guest.getLastName()).contains("Anonimizado #G-99");
        assertThat(guest.getEmail()).contains("anonymized_99@deleted.hospentra.com");
        assertThat(guest.getPhone()).isNull();

        verify(guestRepository).save(guest);
        verify(userRepository).delete(expiredUser);
    }

    @Test
    void purgeExpiredAccounts_WhenNoExpiredAccounts_DoesNothing() {
        when(userRepository.findByScheduledDeletionDateLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int purgedCount = scheduler.purgeExpiredAccounts();

        assertThat(purgedCount).isEqualTo(0);
        verify(userRepository, never()).delete(any());
    }
}
