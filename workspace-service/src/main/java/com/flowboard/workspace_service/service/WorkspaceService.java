package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.dto.*;

import java.util.List;

public interface WorkspaceService {

    // workspace CRUD
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId);
    WorkspaceResponse getById(Long workspaceId, Long requesterId, String userRole, String authorizationHeader);
    List<WorkspaceResponse> getByOwner(Long ownerId, String authorizationHeader);
    List<WorkspaceResponse> getByMember(Long userId, String authorizationHeader);
    List<WorkspaceResponse> getPublicWorkspaces(String authorizationHeader);
    List<WorkspaceResponse> getAllWorkspaces(String authorizationHeader);
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, Long requesterId, String userRole, String authorizationHeader);
    void deleteWorkspace(Long workspaceId, Long requesterId, String userRole);

    // Member management
    WorkspaceResponse.MemberDto addMember(Long workspaceId, AddMemberRequest request, Long requesterId, String userRole, String authorizationHeader);
    void removeMember(Long workspaceId, Long userId, Long requesterId, String userRole);
    void updateMemberRole(Long workspaceId,Long userId, UpdateMemberRoleRequest request, Long requesterId, String userRole);
    List<WorkspaceResponse.MemberDto> getMembers(Long workspaceId, String authorizationHeader);

    void inviteMember(Long workspaceId, InviteMemberRequest request, Long requesterId, String userRole, String authorizationHeader);
    void acceptInvitation(String token, Long userId, String authorizationHeader);
    void declineInvitation(String token, Long userId, String authorizationHeader);
    void revokeInvitation(Long workspaceId, Long invitationId, Long requesterId, String userRole);
    List<com.flowboard.workspace_service.entity.WorkspaceInvitation>
    getPendingInvitations(Long workspaceId, Long requesterId, String userRole);
}

