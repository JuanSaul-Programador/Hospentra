package com.hotel.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long reservationId;
    private Long roomId;
    private String roomNumber;
    private String roomTypeName;
    private String roomTypeNameEn;
    private Long userId;
    private String guestName;
    private Integer rating;
    private Integer cleanlinessRating;
    private Integer serviceRating;
    private Integer comfortRating;
    private Integer locationRating;
    private String title;
    private String comment;
    private String managementResponse;
    private LocalDateTime managementRespondedAt;
    private LocalDateTime createdAt;
    private boolean verifiedStay;
}
