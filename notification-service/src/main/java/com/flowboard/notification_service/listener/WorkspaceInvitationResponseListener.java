package com.flowboard.notification_service.listener;

import com.flowboard.notification_service.config.RabbitMQConfig;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.event.WorkspaceInvitationResponseEvent;
import com.flowboard.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInvitationResponseListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.INVITE_RESPONSE_QUEUE)
    public void handleWorkspaceInviteResponse(WorkspaceInvitationResponseEvent event) {
        String responseLabel = "ACCEPTED".equalsIgnoreCase(event.getResponse()) ? "accepted" : "declined";

        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientId(event.getInviterUserId());
        request.setRecipientEmail(event.getInviterEmail());
        request.setActorId(event.getResponderUserId());
        request.setType(NotificationType.BROADCAST);
        request.setTitle("Invitation " + responseLabel);
        request.setMessage(event.getResponderName() + " " + responseLabel
                + " your invitation to join '" + event.getWorkspaceName() + "'.");
        request.setRelatedId(event.getWorkspaceId());
        request.setRelatedType("WORKSPACE");
        request.setDeepLinkUrl("/workspace/" + event.getWorkspaceId());
        request.setSendEmail(true);

        notificationService.send(request);
        log.info("Invite response notification sent to inviter={} response={}",
                event.getInviterUserId(), event.getResponse());
    }
}
