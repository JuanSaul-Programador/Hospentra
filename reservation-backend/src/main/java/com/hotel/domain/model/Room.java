package com.hotel.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "housekeeper_name", length = 100)
    private String housekeeperName;

    @Column(name = "housekeeping_notes", columnDefinition = "TEXT")
    private String housekeepingNotes;

    @Column(name = "last_cleaned_at")
    private LocalDateTime lastCleanedAt;

    @Column(name = "cleaning_priority", length = 20)
    private String cleaningPriority;
}
