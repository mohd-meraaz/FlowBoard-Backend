package com.flowBoard.auth_service.service;

// ...existing imports...
import org.springframework.mail.MailSendException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Test
    void send_methods_should_invoke_mail_sender() {
        JavaMailSender mail = mock(JavaMailSender.class);
        MimeMessage msg = mock(MimeMessage.class);
        when(mail.createMimeMessage()).thenReturn(msg);

        EmailService svc = new EmailService(mail);
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "fromEmail", "noreply@flowboard.test");
        svc.sendVerificationOtp("a@b.com", "123");
        svc.sendForgotPasswordOtp("c@d.com", "321");
        svc.sendAccountStatusEmail("d@e.com", "Demo User", true);
        svc.sendReactivationOtp("f@g.com", "Demo User", "987654");

        verify(mail, times(4)).createMimeMessage();
        verify(mail, times(4)).send(msg);
    }
    @Test
    void send_handlesMessagingException() throws Exception {
        JavaMailSender mail = mock(JavaMailSender.class);
        MimeMessage msg = mock(MimeMessage.class);
        when(mail.createMimeMessage()).thenReturn(msg);

        // Force the mailSender to throw a MailSendException (unchecked MailException)
        doThrow(new MailSendException("SMTP Server down")).when(mail).send(msg);

        EmailService svc = new EmailService(mail);
        org.springframework.test.util.ReflectionTestUtils.setField(svc, "fromEmail", "noreply@flowboard.test");

        // The exception should be caught and logged, not thrown outwards
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            svc.sendVerificationOtp("broken@test.com", "123456");
        });

        verify(mail, times(1)).send(msg);
    }
}

