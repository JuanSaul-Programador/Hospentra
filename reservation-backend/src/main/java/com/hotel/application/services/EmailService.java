package com.hotel.application.services;

public interface EmailService {

    void sendVerificationOtp(String toEmail, String guestName, String otpCode);

    void sendBookingConfirmation(String toEmail, String guestName, String roomName, String checkIn, String checkOut, Double totalPrice, String voucherAuth);

    void sendPasswordResetOtp(String toEmail, String guestName, String otpCode);

    void sendAccountDeletionNotice(String toEmail, String guestName, String scheduledDate);

    void sendAccountDeletionCancelled(String toEmail, String guestName);
}
