package com.hotel.application.services.impl;

import com.hotel.application.services.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@hospentra.com}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Override
    @Async
    public void sendVerificationOtp(String toEmail, String guestName, String otpCode) {
        log.info("==================================================");
        log.info("HOSPENTRA OTP DISPATCH -> Target: [{}], Code: [{}]", toEmail, otpCode);
        log.info("==================================================");

        if (!mailEnabled) {
            log.info("Mail dispatch disabled by configuration for {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Hospentra Sanctuary Retreat");
            helper.setTo(toEmail);
            helper.setSubject("Código de Verificación de Cuenta — Hospentra Sanctuary");

            String html = buildOtpHtml(guestName, otpCode);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Verification OTP email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to dispatch verification email to {}: {}. OTP is still valid via console log.", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendBookingConfirmation(String toEmail, String guestName, String roomName, String checkIn, String checkOut, Double totalPrice, String voucherAuth) {
        log.info("Booking confirmation triggered for {} ({})", toEmail, voucherAuth);

        if (!mailEnabled) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Hospentra Sanctuary Retreat");
            helper.setTo(toEmail);
            helper.setSubject("Confirmación Oficial de Reserva — Hospentra [" + voucherAuth + "]");

            String html = buildBookingHtml(guestName, roomName, checkIn, checkOut, totalPrice, voucherAuth);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Booking confirmation email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpHtml(String guestName, String otpCode) {
        String name = (guestName != null && !guestName.isBlank()) ? guestName : "Huésped Distinguido";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background-color:#f8fafc;font-family:\"Helvetica Neue\",Helvetica,Arial,sans-serif;color:#0f172a;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f8fafc;padding:40px 16px;'>"
                + "<tr><td align='center'>"
                + "<table width='100%' style='max-width:560px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 4px 16px rgba(0,0,0,0.04);'>"
                + "<tr><td style='background:#0f172a;padding:32px;text-align:center;'>"
                + "<h1 style='margin:0;color:#c59b27;font-size:24px;letter-spacing:2px;font-weight:600;font-family:Georgia,serif;'>HOSPENTRA</h1>"
                + "<p style='margin:4px 0 0;color:#94a3b8;font-size:12px;letter-spacing:1px;text-transform:uppercase;'>Boutique Sanctuary & Private Retreats</p>"
                + "</td></tr>"
                + "<tr><td style='padding:36px 32px;'>"
                + "<h2 style='margin:0 0 16px;font-size:20px;color:#0f172a;font-weight:600;'>Verificación de Seguridad</h2>"
                + "<p style='font-size:15px;line-height:1.6;color:#475569;margin:0 0 20px;'>Estimado/a <strong>" + name + "</strong>,</p>"
                + "<p style='font-size:15px;line-height:1.6;color:#475569;margin:0 0 24px;'>Gracias por iniciar su registro en Hospentra. Para garantizar la privacidad de sus itinerarios, confirmar su correo y proteger su cuenta, ingrese el siguiente código confidencial en la aplicación:</p>"
                + "<div style='text-align:center;margin:28px 0;'>"
                + "<div style='display:inline-block;background:#f1f5f9;border:2px dashed #c59b27;border-radius:10px;padding:16px 32px;'>"
                + "<span style='font-size:32px;font-weight:700;letter-spacing:10px;color:#0f172a;font-family:Courier,monospace;'>" + otpCode + "</span>"
                + "</div>"
                + "</div>"
                + "<p style='font-size:13px;color:#94a3b8;text-align:center;margin:0 0 28px;'>Este código de seguridad es intransferible y expirará en <strong>15 minutos</strong>.</p>"
                + "<hr style='border:none;border-top:1px solid #e2e8f0;margin:24px 0;'/>"
                + "<p style='font-size:12px;color:#94a3b8;line-height:1.5;margin:0;'>Si usted no solicitó este registro en Hospentra Sanctuary, puede ignorar este mensaje de forma segura.</p>"
                + "</td></tr>"
                + "<tr><td style='background:#f8fafc;padding:20px 32px;text-align:center;border-top:1px solid #e2e8f0;'>"
                + "<p style='margin:0;font-size:12px;color:#94a3b8;'>© 2026 Hospentra Sanctuary Retreats. Todos los derechos reservados.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }

    private String buildBookingHtml(String guestName, String roomName, String checkIn, String checkOut, Double totalPrice, String voucherAuth) {
        String name = (guestName != null && !guestName.isBlank()) ? guestName : "Huésped Distinguido";
        String price = totalPrice != null ? String.format("%.2f", totalPrice) : "0.00";
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background-color:#f8fafc;font-family:\"Helvetica Neue\",Helvetica,Arial,sans-serif;color:#0f172a;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f8fafc;padding:40px 16px;'>"
                + "<tr><td align='center'>"
                + "<table width='100%' style='max-width:580px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 4px 16px rgba(0,0,0,0.04);'>"
                + "<tr><td style='background:#0f172a;padding:32px;text-align:center;'>"
                + "<h1 style='margin:0;color:#c59b27;font-size:24px;letter-spacing:2px;font-weight:600;font-family:Georgia,serif;'>HOSPENTRA</h1>"
                + "<p style='margin:4px 0 0;color:#94a3b8;font-size:12px;letter-spacing:1px;text-transform:uppercase;'>Itinerario & Voucher Oficial de Estancia</p>"
                + "</td></tr>"
                + "<tr><td style='padding:36px 32px;'>"
                + "<h2 style='margin:0 0 16px;font-size:20px;color:#0f172a;font-weight:600;'>Reserva Confirmada</h2>"
                + "<p style='font-size:15px;line-height:1.6;color:#475569;margin:0 0 20px;'>Estimado/a <strong>" + name + "</strong>,</p>"
                + "<p style='font-size:15px;line-height:1.6;color:#475569;margin:0 0 24px;'>Nos complace confirmar su reservación en Hospentra Sanctuary. Sus preparativos de autor y amenidades exclusivas ya se encuentran programados para su llegada:</p>"
                + "<div style='background:#f8fafc;border-radius:8px;padding:20px;border:1px solid #e2e8f0;margin-bottom:24px;'>"
                + "<table width='100%' cellpadding='6' cellspacing='0' style='font-size:14px;color:#334155;'>"
                + "<tr><td><strong>Suite:</strong></td><td align='right'>" + roomName + "</td></tr>"
                + "<tr><td><strong>Check-In:</strong></td><td align='right'>" + checkIn + " (15:00 hrs)</td></tr>"
                + "<tr><td><strong>Check-Out:</strong></td><td align='right'>" + checkOut + " (12:00 hrs)</td></tr>"
                + "<tr><td><strong>Monto Total:</strong></td><td align='right' style='color:#0f172a;font-weight:700;'>$" + price + " USD</td></tr>"
                + "<tr><td><strong>Código de Autorización:</strong></td><td align='right' style='color:#c59b27;font-weight:700;font-family:monospace;'>" + voucherAuth + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<p style='font-size:14px;color:#475569;line-height:1.6;margin:0 0 20px;'>Puede consultar y descargar su <strong>Folio Digital Oficial</strong> en cualquier momento desde su panel de reservas en nuestra plataforma.</p>"
                + "</td></tr>"
                + "<tr><td style='background:#f8fafc;padding:20px 32px;text-align:center;border-top:1px solid #e2e8f0;'>"
                + "<p style='margin:0;font-size:12px;color:#94a3b8;'>Hospentra Concierge 24/7 • Riviera Maya Sanctuary</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }
}
