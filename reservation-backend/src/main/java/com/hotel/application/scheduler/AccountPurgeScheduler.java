package com.hotel.application.scheduler;

import com.hotel.domain.model.Guest;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountPurgeScheduler {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;

    @Scheduled(cron = "${app.account.purge-cron:0 0 3 * * ?}")
    @Transactional
    public int purgeExpiredAccounts() {
        LocalDateTime now = LocalDateTime.now();
        List<User> expiredUsers = userRepository.findByScheduledDeletionDateLessThanEqual(now);

        if (expiredUsers.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (User user : expiredUsers) {
            log.info("Purging expired account ID: {} (Username: {}) scheduled for deletion at: {}",
                    user.getId(), user.getUsername(), user.getScheduledDeletionDate());

            Guest guest = user.getGuest();
            if (guest != null) {
                guest.setFirstName("Huésped");
                guest.setLastName("Anonimizado #G-" + guest.getId());
                guest.setEmail("anonymized_" + guest.getId() + "@deleted.hospentra.com");
                guest.setPhone(null);
                guestRepository.save(guest);
            }

            userRepository.delete(user);
            count++;
        }

        log.info("Total purged expired accounts: {}", count);
        return count;
    }
}
