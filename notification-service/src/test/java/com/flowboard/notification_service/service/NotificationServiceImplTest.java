package com.flowboard.notification_service.service;

import com.flowboard.notification_service.dto.NotificationResponse;
import com.flowboard.notification_service.dto.SendBulkNotificationRequest;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.exception.CustomException;
import com.flowboard.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl - Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private EmailNotificationService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private Notification testNotification;
    private final Long notificationId = 100L;
    private final Long recipientId = 1L;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(notificationId)
                .recipientId(recipientId)
                .actorId(2L)
                .type(NotificationType.ASSIGNMENT)
                .title("Test Notification")
                .message("This is a test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Standard save mock to return the passed entity
        lenient().when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── Sending Notifications ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Send & SendBulk Operations")
    class SendTests {

        @Test
        @DisplayName("Send notification without email")
        void send_NoEmail_Success() {
            SendNotificationRequest req = new SendNotificationRequest();
            req.setRecipientId(recipientId);
            req.setTitle("Hello");
            req.setSendEmail(false);

            NotificationResponse res = notificationService.send(req);

            assertThat(res.getTitle()).isEqualTo("Hello");
            verify(repository).save(any(Notification.class));
            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Send notification with valid email triggers EmailService")
        void send_WithEmail_Success() {
            SendNotificationRequest req = new SendNotificationRequest();
            req.setRecipientId(recipientId);
            req.setTitle("Important");
            req.setSendEmail(true);
            req.setRecipientEmail("user@domain.com");

            notificationService.send(req);

            verify(repository).save(any(Notification.class));
            verify(emailService).sendNotificationEmail(
                    eq("user@domain.com"), eq("Important"), any(), any()
            );
        }

        @Test
        @DisplayName("Send notification skips email if email is blank")
        void send_WithBlankEmail_SkipsEmailService() {
            SendNotificationRequest req = new SendNotificationRequest();
            req.setSendEmail(true);
            req.setRecipientEmail("   ");

            notificationService.send(req);

            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Send bulk notification saves all items")
        void sendBulk_Success() {
            SendBulkNotificationRequest req = new SendBulkNotificationRequest();
            req.setRecipientIds(Arrays.asList(1L, 2L, 3L));
            req.setTitle("Bulk Message");

            List<NotificationResponse> responses = notificationService.sendBulk(req);

            assertThat(responses).hasSize(3);
            verify(repository).saveAll(anyList());
        }
    }

    // ── Pre-packaged Notify Methods ─────────────────────────────────────────────

    @Nested
    @DisplayName("Pre-Packaged System Notifications")
    class SystemNotifyTests {

        @Test
        @DisplayName("notifyAssignment sends email")
        void notifyAssignment_Success() {
            notificationService.notifyAssignment(recipientId, 2L, 10L, "Fix Bug", "dev@domain.com");

            verify(repository).save(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.ASSIGNMENT);
            verify(emailService).sendNotificationEmail(eq("dev@domain.com"), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("notifyOverdue sends email")
        void notifyOverdue_Success() {
            notificationService.notifyOverdue(recipientId, 10L, "Task 1", "2023-01-01", "dev@domain.com");

            verify(repository).save(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.OVERDUE);
            verify(emailService).sendNotificationEmail(eq("dev@domain.com"), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("notifyMention does NOT send email")
        void notifyMention_Success() {
            notificationService.notifyMention(recipientId, 2L, 10L, "Task");
            verify(repository).save(any(Notification.class));
            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("notifyDueDateApproaching does NOT send email")
        void notifyDueDateApproaching_Success() {
            notificationService.notifyDueDateApproaching(recipientId, 10L, "Task", "2 days");
            verify(repository).save(any(Notification.class));
            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("notifyCardMovedToDone does NOT send email")
        void notifyCardMovedToDone_Success() {
            notificationService.notifyCardMovedToDone(recipientId, 2L, 10L, "Task");
            verify(repository).save(any(Notification.class));
            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("notifyCommentReply does NOT send email")
        void notifyCommentReply_Success() {
            notificationService.notifyCommentReply(recipientId, 2L, 10L, "Task");
            verify(repository).save(any(Notification.class));
            verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString(), anyString());
        }
    }

    // ── Retrieval Operations ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Retrieval Operations")
    class RetrievalTests {

        @Test
        @DisplayName("getByRecipient maps correctly")
        void getByRecipient_Success() {
            when(repository.findByRecipientIdOrderByCreatedAtDesc(recipientId))
                    .thenReturn(Collections.singletonList(testNotification));

            List<NotificationResponse> res = notificationService.getByRecipient(recipientId);
            assertThat(res).hasSize(1);
        }

        @Test
        @DisplayName("getUnreadByRecipient maps correctly")
        void getUnreadByRecipient_Success() {
            when(repository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientId))
                    .thenReturn(Collections.singletonList(testNotification));

            List<NotificationResponse> res = notificationService.getUnreadByRecipient(recipientId);
            assertThat(res).hasSize(1);
        }

        @Test
        @DisplayName("getByRecipientAndType maps correctly")
        void getByRecipientAndType_Success() {
            when(repository.findByRecipientIdAndTypeOrderByCreatedAtDesc(recipientId, NotificationType.ASSIGNMENT))
                    .thenReturn(Collections.singletonList(testNotification));

            List<NotificationResponse> res = notificationService.getByRecipientAndType(recipientId, NotificationType.ASSIGNMENT);
            assertThat(res).hasSize(1);
        }

        @Test
        @DisplayName("getAll fetches full list")
        void getAll_Success() {
            when(repository.findAll()).thenReturn(Collections.singletonList(testNotification));
            List<NotificationResponse> res = notificationService.getAll();
            assertThat(res).hasSize(1);
        }

        @Test
        @DisplayName("getUnreadCount returns correct count")
        void getUnreadCount_Success() {
            when(repository.countByRecipientIdAndIsReadFalse(recipientId)).thenReturn(5L);
            long count = notificationService.getUnreadCount(recipientId);
            assertThat(count).isEqualTo(5L);
        }
    }

    // ── Read & Cleanup Operations ───────────────────────────────────────────────

    @Nested
    @DisplayName("Read Status & Deletion")
    class ReadAndCleanupTests {

        @Test
        @DisplayName("markAsRead - Success sets flag and timestamp")
        void markAsRead_Success() {
            when(repository.existsByIdAndRecipientId(notificationId, recipientId)).thenReturn(true);
            when(repository.findById(notificationId)).thenReturn(Optional.of(testNotification));

            NotificationResponse res = notificationService.markAsRead(notificationId, recipientId);

            assertThat(res.isRead()).isTrue();
            assertThat(testNotification.getReadAt()).isNotNull();
            verify(repository).save(testNotification);
        }

        @Test
        @DisplayName("markAsRead - Throws when access denied")
        void markAsRead_AccessDenied_ThrowsException() {
            when(repository.existsByIdAndRecipientId(notificationId, recipientId)).thenReturn(false);

            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, recipientId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("access denied");
        }

        @Test
        @DisplayName("markAllAsRead - Calls repository method")
        void markAllAsRead_Success() {
            notificationService.markAllAsRead(recipientId);
            verify(repository).markAllAsRead(recipientId);
        }

        @Test
        @DisplayName("deleteNotification - Success")
        void deleteNotification_Success() {
            when(repository.existsByIdAndRecipientId(notificationId, recipientId)).thenReturn(true);
            notificationService.deleteNotification(notificationId, recipientId);
            verify(repository).deleteById(notificationId);
        }

        @Test
        @DisplayName("deleteNotification - Throws when access denied")
        void deleteNotification_AccessDenied_ThrowsException() {
            when(repository.existsByIdAndRecipientId(notificationId, recipientId)).thenReturn(false);

            assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, recipientId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("access denied");
        }

        @Test
        @DisplayName("deleteReadNotifications - Calls repository method")
        void deleteReadNotifications_Success() {
            notificationService.deleteReadNotifications(recipientId);
            verify(repository).deleteReadByRecipientId(recipientId);
        }
    }
}