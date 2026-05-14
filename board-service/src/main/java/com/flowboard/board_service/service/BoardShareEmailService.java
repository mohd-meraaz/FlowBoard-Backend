package com.flowboard.board_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardShareEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Async
    public void sendBoardInviteEmail(String toEmail,
                                     String inviterName,
                                     String boardName,
                                     String boardLink,
                                     boolean publicBoard) {
        String safeInviter = inviterName == null || inviterName.isBlank() ? "A teammate" : inviterName;
        String title = publicBoard ? "A public board was shared with you" : "A private board was shared with you";
        String message = publicBoard
                ? safeInviter + " shared the public board '" + boardName + "' with you."
                : safeInviter + " invited you to collaborate on the private board '" + boardName + "'. Sign in with this email to access it.";

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(toEmail);
            mail.setSubject("FlowBoard -- " + title);
            mail.setText(buildEmailBody(title, message, boardLink));

            mailSender.send(mail);
        } catch (Exception ex) {
            log.error("Failed to send board invite email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildEmailBody(String title, String message, String boardLink) {
        String url = resolveUrl(boardLink);
        return """
                FlowBoard

                %s

                %s

                Open board: %s
                """.formatted(title, message, url);
    }

    private String resolveUrl(String value) {
        if (value == null || value.isBlank()) {
            return frontendBaseUrl;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        String path = value.startsWith("/") ? value : "/" + value;
        return base + path;
    }
}
