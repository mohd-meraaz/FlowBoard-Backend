package com.flowboard.notification_service.listener;

import com.flowboard.notification_service.config.RabbitMQConfig;
import com.flowboard.notification_service.dto.SendNotificationRequest;
import com.flowboard.notification_service.enums.NotificationType;
import com.flowboard.notification_service.event.WorkspaceInviteEvent;
import com.flowboard.notification_service.service.EmailNotificationService;
import com.flowboard.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInviteListener {

    private final EmailNotificationService emailService;
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.INVITE_QUEUE)
    public void handleWorkspaceInvite(WorkspaceInviteEvent event) {
        log.info("Received WorkspaceInviteEvent: workspaceId={} invitee={}",
                event.getWorkspaceId(), event.getInviteeEmail());

        try {
            String subject = "You're invited to join " +
                    event.getWorkspaceName() + " on FlowBoard";

            String message = "You have been invited to join the workspace '"
                    + event.getWorkspaceName() + "' as "
                    + event.getRole() + ".\n\n"
                    + "Use the link below to accept or decline the invitation:\n"
                    + event.getAcceptUrl() + "\n\n"
                    + "This invitation expires in 7 days.";

            emailService.sendNotificationEmail(
                    event.getInviteeEmail(),
                    subject,
                    message,
                    event.getAcceptUrl()
            );

            log.info("Invite email sent to {}", event.getInviteeEmail());

            if (event.getInviteeUserId() != null) {
                SendNotificationRequest req = new SendNotificationRequest();
                req.setRecipientId(event.getInviteeUserId());
                req.setActorId(event.getInvitedByUserId());
                req.setType(NotificationType.BROADCAST);
                req.setTitle("Workspace invitation");
                req.setMessage("You were invited to join '" + event.getWorkspaceName() + "' as " + event.getRole() + ".");
                req.setRelatedId(event.getWorkspaceId());
                req.setRelatedType("WORKSPACE");
                req.setDeepLinkUrl("/invite/respond?token=" + event.getToken());
                req.setSendEmail(false);
                notificationService.send(req);
                log.info("In-app invite notification created for userId={}", event.getInviteeUserId());
            } else {
                log.info("No invitee userId resolved for {}. Skipping in-app notification.", event.getInviteeEmail());
            }

        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}",
                    event.getInviteeEmail(), e.getMessage());
        }
    }
}
