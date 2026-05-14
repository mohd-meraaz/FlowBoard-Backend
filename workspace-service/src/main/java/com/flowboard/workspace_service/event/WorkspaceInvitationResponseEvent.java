package com.flowboard.workspace_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitationResponseEvent implements Serializable {
    private Long workspaceId;
    private String workspaceName;
    private Long inviterUserId;
    private String inviterEmail;
    private Long responderUserId;
    private String responderName;
    private String responderEmail;
    private String response;
}
