package com.flowboard.board_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardShareEmailService - Unit Tests")
class BoardShareEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private BoardShareEmailService emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private final String fromEmail = "noreply@flowboard.com";
    private final String baseUrl = "http://localhost:4200";

    @BeforeEach
    void setUp() {
        // Inject values normally provided by application.yml
        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", baseUrl);
    }

    @Test
    @DisplayName("Send public board invite with relative link and valid inviter")
    void sendBoardInviteEmail_PublicBoard_ValidInviter_RelativeLink() {
        // Arrange
        String toEmail = "target@domain.com";
        String inviterName = "Alice";
        String boardName = "Marketing Campaign";
        String relativeLink = "/b/public-123";

        // Act
        emailService.sendBoardInviteEmail(toEmail, inviterName, boardName, relativeLink, true);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getFrom()).isEqualTo(fromEmail);
        assertThat(sentMessage.getTo()).containsExactly(toEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("FlowBoard -- A public board was shared with you");

        // Verify text generation and URL resolution
        String text = sentMessage.getText();
        assertThat(text).contains("Alice shared the public board 'Marketing Campaign' with you.");
        assertThat(text).contains("Open board: http://localhost:4200/b/public-123");
    }

    @Test
    @DisplayName("Send private board invite with absolute link and null inviter fallback")
    void sendBoardInviteEmail_PrivateBoard_NullInviter_AbsoluteLink() {
        // Arrange
        String toEmail = "user@domain.com";
        String absoluteLink = "https://customdomain.com/board/999";

        // Act (Pass null for inviterName to trigger "A teammate" fallback)
        emailService.sendBoardInviteEmail(toEmail, null, "Secret Project", absoluteLink, false);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getSubject()).isEqualTo("FlowBoard -- A private board was shared with you");

        // Verify text fallback and absolute URL bypass
        String text = sentMessage.getText();
        assertThat(text).contains("A teammate invited you to collaborate on the private board 'Secret Project'.");
        assertThat(text).contains("Open board: https://customdomain.com/board/999");
    }

    @Test
    @DisplayName("Send invite with blank link resolves strictly to base URL")
    void sendBoardInviteEmail_BlankLink_ResolvesToBaseUrl() {
        // Arrange
        String toEmail = "user@domain.com";

        // Act (Pass empty string for link and blank string for inviterName)
        emailService.sendBoardInviteEmail(toEmail, "   ", "My Board", "", true);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        String text = sentMessage.getText();
        assertThat(text).contains("A teammate shared"); // Tests the .isBlank() fallback check
        assertThat(text).contains("Open board: http://localhost:4200"); // Base URL fallback
    }

    @Test
    @DisplayName("Throw when MailSender fails")
    void sendBoardInviteEmail_MailException_ThrowsCustomException() {
        // Arrange
        doThrow(new RuntimeException("SMTP Server Down")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertThatThrownBy(() ->
                emailService.sendBoardInviteEmail("test@test.com", "Bob", "Board", "/link", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send board invite email");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
