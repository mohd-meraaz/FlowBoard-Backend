package com.flowboard.notification_service.controller;

import com.flowboard.notification_service.dto.NotificationResponse;
import com.flowboard.notification_service.dto.SendBulkNotificationRequest;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.exception.CustomException;
import com.flowboard.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController - Unit Tests")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private final Long userId = 1L;
    private final Long notificationId = 100L;

    @Test
    @DisplayName("resolveUserId throws exception when header is missing")
    void resolveUserId_MissingHeader_ThrowsException() {
        assertThatThrownBy(() -> notificationController.getMyNotifications(null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    @DisplayName("send - Returns 201 Created")
    void send_Success() {
        SendNotificationRequest request = mock(SendNotificationRequest.class);
        NotificationResponse response = mock(NotificationResponse.class);
        when(notificationService.send(request)).thenReturn(response);

        ResponseEntity<NotificationResponse> result = notificationController.send(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("sendBulk - Returns 201 Created")
    void sendBulk_Success() {
        SendBulkNotificationRequest request = mock(SendBulkNotificationRequest.class);
        List<NotificationResponse> responses = Collections.singletonList(mock(NotificationResponse.class));
        when(notificationService.sendBulk(request)).thenReturn(responses);

        ResponseEntity<List<NotificationResponse>> result = notificationController.sendBulk(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("notifyAssignment - Returns 200 OK")
    void notifyAssignment_Success() {
        ResponseEntity<String> result = notificationController.notifyAssignment(1L, 2L, 3L, "Task", "test@domain.com");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Assignment notification sent");
        verify(notificationService).notifyAssignment(1L, 2L, 3L, "Task", "test@domain.com");
    }

    @Test
    @DisplayName("notifyDueDateBody - Parses Map correctly and Returns 200 OK")
    void notifyDueDateBody_Success() {
        Map<String, Object> req = Map.of(
                "recipientId", 1L,
                "cardId", 100L,
                "cardTitle", "Title",
                "timeLeft", "2 days"
        );

        ResponseEntity<String> result = notificationController.notifyDueDateBody(req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Due date notification sent");
        verify(notificationService).notifyDueDateApproaching(1L, 100L, "Title", "2 days");
    }

    @Test
    @DisplayName("notifyOverdueBody - Parses Map correctly with email and Returns 200 OK")
    void notifyOverdueBody_WithEmail_Success() {
        Map<String, Object> req = new HashMap<>();
        req.put("recipientId", 1L);
        req.put("cardId", 100L);
        req.put("cardTitle", "Title");
        req.put("dueDate", "2023-01-01");
        req.put("recipientEmail", "test@domain.com");

        ResponseEntity<String> result = notificationController.notifyOverdueBody(req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Overdue notification sent");
        verify(notificationService).notifyOverdue(1L, 100L, "Title", "2023-01-01", "test@domain.com");
    }

    @Test
    @DisplayName("notifyOverdueBody - Parses Map correctly without email and Returns 200 OK")
    void notifyOverdueBody_WithoutEmail_Success() {
        Map<String, Object> req = new HashMap<>();
        req.put("recipientId", 1L);
        req.put("cardId", 100L);
        req.put("cardTitle", "Title");
        req.put("dueDate", "2023-01-01");
        // Omitting recipientEmail

        ResponseEntity<String> result = notificationController.notifyOverdueBody(req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).notifyOverdue(1L, 100L, "Title", "2023-01-01", null);
    }

    @Test
    @DisplayName("notifyMention - Returns 200 OK")
    void notifyMention_Success() {
        ResponseEntity<String> result = notificationController.notifyMention(1L, 2L, 3L, "Task");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).notifyMention(1L, 2L, 3L, "Task");
    }

    @Test
    @DisplayName("notifyDueDate - Returns 200 OK")
    void notifyDueDate_Success() {
        ResponseEntity<String> result = notificationController.notifyDueDate(1L, 3L, "Task", "1 hour");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).notifyDueDateApproaching(1L, 3L, "Task", "1 hour");
    }

    @Test
    @DisplayName("notifyDone - Returns 200 OK")
    void notifyDone_Success() {
        ResponseEntity<String> result = notificationController.notifyDone(1L, 2L, 3L, "Task");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).notifyCardMovedToDone(1L, 2L, 3L, "Task");
    }

    @Test
    @DisplayName("notifyReply - Returns 200 OK")
    void notifyReply_Success() {
        ResponseEntity<String> result = notificationController.notifyReply(1L, 2L, 3L, "Task");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).notifyCommentReply(1L, 2L, 3L, "Task");
    }

    @Test
    @DisplayName("notifyOverdue - Returns 200 OK")
    void notifyOverdue_Success() {
        ResponseEntity<String> result = notificationController.notifyOverdue(1L, 3L, "Task", "2023-01-01", "test@test.com");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).notifyOverdue(1L, 3L, "Task", "2023-01-01", "test@test.com");
    }

    @Test
    @DisplayName("getMyNotifications - Returns 200 OK")
    void getMyNotifications_Success() {
        List<NotificationResponse> responses = Collections.singletonList(mock(NotificationResponse.class));
        when(notificationService.getByRecipient(userId)).thenReturn(responses);

        ResponseEntity<List<NotificationResponse>> result = notificationController.getMyNotifications(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getUnread - Returns 200 OK")
    void getUnread_Success() {
        List<NotificationResponse> responses = Collections.singletonList(mock(NotificationResponse.class));
        when(notificationService.getUnreadByRecipient(userId)).thenReturn(responses);

        ResponseEntity<List<NotificationResponse>> result = notificationController.getUnread(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getUnreadCount - Returns 200 OK")
    void getUnreadCount_Success() {
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        ResponseEntity<Long> result = notificationController.getUnreadCount(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getByType - Returns 200 OK")
    void getByType_Success() {
        List<NotificationResponse> responses = Collections.singletonList(mock(NotificationResponse.class));
        when(notificationService.getByRecipientAndType(userId, NotificationType.ASSIGNMENT)).thenReturn(responses);

        ResponseEntity<List<NotificationResponse>> result = notificationController.getByType(NotificationType.ASSIGNMENT, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getAll - Returns 200 OK when role is PLATFORM_ADMIN")
    void getAll_AdminRole_Success() {
        List<NotificationResponse> responses = Collections.singletonList(mock(NotificationResponse.class));
        when(notificationService.getAll()).thenReturn(responses);

        ResponseEntity<List<NotificationResponse>> result = notificationController.getAll("PLATFORM_ADMIN");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getAll - Throws CustomException when role is not PLATFORM_ADMIN")
    void getAll_InvalidRole_ThrowsException() {
        assertThatThrownBy(() -> notificationController.getAll("MEMBER"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Admin access required")
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(notificationService, never()).getAll();
    }

    @Test
    @DisplayName("markAsRead - Returns 200 OK")
    void markAsRead_Success() {
        NotificationResponse response = mock(NotificationResponse.class);
        when(notificationService.markAsRead(notificationId, userId)).thenReturn(response);

        ResponseEntity<NotificationResponse> result = notificationController.markAsRead(notificationId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("markAllAsRead - Returns 200 OK")
    void markAllAsRead_Success() {
        ResponseEntity<String> result = notificationController.markAllAsRead(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("All notifications marked as read");
        verify(notificationService).markAllAsRead(userId);
    }

    @Test
    @DisplayName("delete - Returns 200 OK")
    void delete_Success() {
        ResponseEntity<String> result = notificationController.delete(notificationId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Notification deleted");
        verify(notificationService).deleteNotification(notificationId, userId);
    }

    @Test
    @DisplayName("deleteRead - Returns 200 OK")
    void deleteRead_Success() {
        ResponseEntity<String> result = notificationController.deleteRead(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Read notifications deleted");
        verify(notificationService).deleteReadNotifications(userId);
    }
}