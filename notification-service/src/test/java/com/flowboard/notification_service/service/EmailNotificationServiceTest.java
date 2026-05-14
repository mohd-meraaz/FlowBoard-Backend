package com.flowboard.notification_service.service;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationService - Unit Tests")
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailService;

    private final String fromEmail = "notifications@flowboard.com";

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // Inject application.yml properties via Reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:4200");

        // Set up a real MimeMessage backed by a dummy session so MimeMessageHelper works flawlessly
        Session session = Session.getInstance(new Properties());
        mimeMessage = new MimeMessage(session);
    }

    /**
     * Helper method to extract the HTML text body from the MimeMultipart message
     */
    private String extractHtmlBody(MimeMessage message) throws Exception {
        return extractFirstTextContent(message.getContent());
    }

    private String extractFirstTextContent(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }

        if (content instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String extracted = extractFirstTextContent(bodyPart.getContent());
                if (extracted != null) {
                    return extracted;
                }
            }
        }

        return content != null ? content.toString() : null;
    }

    @Test
    @DisplayName("sendNotificationEmail - Absolute URL resolves correctly and includes button")
    void sendNotificationEmail_AbsoluteUrl_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String toEmail = "user@domain.com";
        String title = "Test Absolute";
        String message = "This is an absolute link test.";
        String absoluteUrl = "https://external.com/path";

        emailService.sendNotificationEmail(toEmail, title, message, absoluteUrl);

        verify(mailSender).send(mimeMessage);

        assertThat(mimeMessage.getSubject()).isEqualTo("FlowBoard -- Test Absolute");
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo(fromEmail);
        assertThat(mimeMessage.getRecipients(MimeMessage.RecipientType.TO)[0].toString()).isEqualTo(toEmail);

        String htmlBody = extractHtmlBody(mimeMessage);
        assertThat(htmlBody).contains(message);
        assertThat(htmlBody).contains("href=\"https://external.com/path\"");
        assertThat(htmlBody).contains("View in FlowBoard"); // Button is rendered
    }

    @Test
    @DisplayName("sendNotificationEmail - Relative URL resolves against base URL")
    void sendNotificationEmail_RelativeUrl_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String relativeUrl = "/b/123";

        emailService.sendNotificationEmail("test@test.com", "Title", "Msg", relativeUrl);

        verify(mailSender).send(mimeMessage);

        String htmlBody = extractHtmlBody(mimeMessage);
        // Base URL + Relative path combination verification
        assertThat(htmlBody).contains("href=\"http://localhost:4200/b/123\"");
    }

    @Test
    @DisplayName("sendNotificationEmail - Null or Blank URL omits the action button")
    void sendNotificationEmail_BlankUrl_OmitsButton() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Pass a blank string for deepLinkUrl
        emailService.sendNotificationEmail("test@test.com", "No Link Title", "No link provided.", "   ");

        verify(mailSender).send(mimeMessage);

        String htmlBody = extractHtmlBody(mimeMessage);
        assertThat(htmlBody).contains("No link provided.");
        assertThat(htmlBody).doesNotContain("View in FlowBoard"); // Button should be omitted
        assertThat(htmlBody).doesNotContain("<a href=");
    }

    @Test
    @DisplayName("sendAssignmentEmail - Formats specific message and passes URL")
    void sendAssignmentEmail_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAssignmentEmail("target@test.com", "Project Kickoff", "Alice", "/board/55");

        verify(mailSender).send(mimeMessage);

        assertThat(mimeMessage.getSubject()).isEqualTo("FlowBoard -- You have been assigned a card");
        String htmlBody = extractHtmlBody(mimeMessage);
        assertThat(htmlBody).contains("Alice assigned you to 'Project Kickoff'");
        assertThat(htmlBody).contains("href=\"http://localhost:4200/board/55\"");
    }

    @Test
    @DisplayName("sendOverdueEmail - Formats specific message without an action button")
    void sendOverdueEmail_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOverdueEmail("slackers@test.com", "Quarterly Report", "2024-01-01");

        verify(mailSender).send(mimeMessage);

        assertThat(mimeMessage.getSubject()).isEqualTo("FlowBoard -- Overdue card - action is required");
        String htmlBody = extractHtmlBody(mimeMessage);
        assertThat(htmlBody).contains("The card 'Quarterly Report' was due on 2024-01-01 and has not been marked as done.");
        assertThat(htmlBody).doesNotContain("View in FlowBoard");
    }

    @Test
    @DisplayName("sendNotificationEmail - Handles MessagingException gracefully without throwing")
    void sendNotificationEmail_ThrowsMessagingException_HandledGracefully() throws Exception {
        // Mock a MimeMessage that specifically throws an exception when the Helper tries to modify it
        MimeMessage brokenMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(brokenMessage);

        // This will cause MimeMessageHelper constructor or setup to throw MessagingException
        doThrow(new MessagingException("Simulated SMTP Failure"))
                .when(brokenMessage)
                .setFrom(any(jakarta.mail.Address.class));

        // Act & Assert: The method should internally catch the exception and log an error, NOT crash
        assertDoesNotThrow(() ->
                emailService.sendNotificationEmail("test@test.com", "Title", "Msg", "/link")
        );

        // Verify that send() was never reached because it failed during assembly
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}