package com.hotel.application.services;

import com.hotel.application.services.impl.EmailServiceImpl;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@hospentra.com");
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
    }

    private String extractHtmlBody(MimeMessage mimeMessage) throws Exception {
        Object content = mimeMessage.getContent();
        if (content instanceof String text) {
            return text;
        }
        Multipart outer = (Multipart) content;
        Multipart related = (Multipart) outer.getBodyPart(0).getContent();
        return related.getBodyPart(0).getContent().toString();
    }

    @Test
    void sendVerificationOtp_escapesGuestNameAndDoesNotLogCleartextOtp(CapturedOutput output) throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationOtp("guest@example.com", "<script>alert('xss')</script>", "948271");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        String html = extractHtmlBody(captor.getValue());
        assertThat(html).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>alert('xss')</script>");
        assertThat(output.getAll()).doesNotContain("948271");
    }

    @Test
    void sendBookingConfirmation_escapesUntrustedTemplateFields() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingConfirmation(
                "guest@example.com",
                "<img src=x onerror=alert(1)>",
                "Suite <b>Royal</b>",
                "2026-12-01",
                "2026-12-05",
                1200.0,
                "HOSP-\"<script>\"-2026"
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        String html = extractHtmlBody(captor.getValue());
        assertThat(html).doesNotContain("<img src=x onerror=alert(1)>");
        assertThat(html).doesNotContain("<b>Royal</b>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;img src=x onerror=alert(1)&gt;");
        assertThat(html).contains("Suite &lt;b&gt;Royal&lt;/b&gt;");
        assertThat(html).contains("HOSP-&quot;&lt;script&gt;&quot;-2026");
    }
}
