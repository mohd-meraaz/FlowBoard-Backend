package com.flowboard.workspace_service.controller;

import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.WorkspaceInvitation;
import com.flowboard.workspace_service.exception.CustomException;
import com.flowboard.workspace_service.service.WorkspaceService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceController - Unit Tests")
class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private WorkspaceController workspaceController;

    private final Long userId = 1L;
    private final Long workspaceId = 100L;
    private final String userRole = "USER";
    private final String authHeader = "Bearer mock-jwt-token";

    @Test
    @DisplayName("resolveUserId throws exception when X-User-Id header is missing")
    void resolveUserId_MissingHeader_ThrowsException() {
        CreateWorkspaceRequest request = mock(CreateWorkspaceRequest.class);

        assertThatThrownBy(() -> workspaceController.create(request, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    @DisplayName("create - Returns 201 Created")
    void create_Success() {
        CreateWorkspaceRequest request = mock(CreateWorkspaceRequest.class);
        WorkspaceResponse response = mock(WorkspaceResponse.class);
        when(workspaceService.createWorkspace(request, userId)).thenReturn(response);

        ResponseEntity<WorkspaceResponse> result = workspaceController.create(request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getById - Returns 200 OK")
    void getById_Success() {
        WorkspaceResponse response = mock(WorkspaceResponse.class);
        when(workspaceService.getById(workspaceId, userId, userRole, authHeader)).thenReturn(response);

        ResponseEntity<WorkspaceResponse> result = workspaceController.getById(workspaceId, userId, userRole, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getAllWorkspaces (Admin) - Returns 200 OK")
    void getAllWorkspaces_Success() {
        List<WorkspaceResponse> responses = Collections.singletonList(mock(WorkspaceResponse.class));
        when(workspaceService.getAllWorkspaces(authHeader)).thenReturn(responses);

        ResponseEntity<List<WorkspaceResponse>> result = workspaceController.getAllWorkspaces(authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByOwner - Returns 200 OK")
    void getByOwner_Success() {
        List<WorkspaceResponse> responses = Collections.singletonList(mock(WorkspaceResponse.class));
        when(workspaceService.getByOwner(userId, authHeader)).thenReturn(responses);

        ResponseEntity<List<WorkspaceResponse>> result = workspaceController.getByOwner(userId, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByMember - Returns 200 OK")
    void getByMember_Success() {
        List<WorkspaceResponse> responses = Collections.singletonList(mock(WorkspaceResponse.class));
        when(workspaceService.getByMember(userId, authHeader)).thenReturn(responses);

        ResponseEntity<List<WorkspaceResponse>> result = workspaceController.getByMember(userId, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getPublic - Returns 200 OK")
    void getPublic_Success() {
        List<WorkspaceResponse> responses = Collections.singletonList(mock(WorkspaceResponse.class));
        when(workspaceService.getPublicWorkspaces(authHeader)).thenReturn(responses);

        ResponseEntity<List<WorkspaceResponse>> result = workspaceController.getPublic(authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("update - Returns 200 OK")
    void update_Success() {
        UpdateWorkspaceRequest request = mock(UpdateWorkspaceRequest.class);
        WorkspaceResponse response = mock(WorkspaceResponse.class);
        when(workspaceService.updateWorkspace(workspaceId, request, userId, userRole, authHeader)).thenReturn(response);

        ResponseEntity<WorkspaceResponse> result = workspaceController.update(workspaceId, request, userId, userRole, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("delete - Returns 200 OK")
    void delete_Success() {
        doNothing().when(workspaceService).deleteWorkspace(workspaceId, userId, userRole);

        ResponseEntity<String> result = workspaceController.delete(workspaceId, userId, userRole);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Workspace deleted successfully");
        verify(workspaceService).deleteWorkspace(workspaceId, userId, userRole);
    }

    @Test
    @DisplayName("addMember - Returns 201 Created")
    void addMember_Success() {
        AddMemberRequest request = mock(AddMemberRequest.class);
        WorkspaceResponse.MemberDto response = mock(WorkspaceResponse.MemberDto.class);
        when(workspaceService.addMember(workspaceId, request, userId, userRole, authHeader)).thenReturn(response);

        ResponseEntity<WorkspaceResponse.MemberDto> result = workspaceController.addMember(workspaceId, request, userId, userRole, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("removeMember - Returns 200 OK")
    void removeMember_Success() {
        Long targetMemberId = 2L;
        doNothing().when(workspaceService).removeMember(workspaceId, targetMemberId, userId, userRole);

        ResponseEntity<String> result = workspaceController.removeMember(workspaceId, targetMemberId, userId, userRole);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Member removed successfully");
        verify(workspaceService).removeMember(workspaceId, targetMemberId, userId, userRole);
    }

    @Test
    @DisplayName("updateMemberRole - Returns 200 OK")
    void updateMemberRole_Success() {
        Long targetMemberId = 2L;
        UpdateMemberRoleRequest request = mock(UpdateMemberRoleRequest.class);
        doNothing().when(workspaceService).updateMemberRole(workspaceId, targetMemberId, request, userId, userRole);

        ResponseEntity<String> result = workspaceController.updateMemberRole(workspaceId, targetMemberId, request, userId, userRole);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Member role updated successfully");
    }

    @Test
    @DisplayName("getMembers - Returns 200 OK")
    void getMembers_Success() {
        List<WorkspaceResponse.MemberDto> responses = Collections.singletonList(mock(WorkspaceResponse.MemberDto.class));
        when(workspaceService.getMembers(workspaceId, authHeader)).thenReturn(responses);

        ResponseEntity<List<WorkspaceResponse.MemberDto>> result = workspaceController.getMembers(workspaceId, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("inviteMember - Returns 200 OK")
    void inviteMember_Success() {
        InviteMemberRequest request = new InviteMemberRequest();
        request.setEmail("test@flowboard.com");

        doNothing().when(workspaceService).inviteMember(workspaceId, request, userId, userRole, authHeader);

        ResponseEntity<String> result = workspaceController.inviteMember(workspaceId, request, userId, userRole, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Invitation sent to test@flowboard.com");
    }

    @Test
    @DisplayName("acceptInvitation - Returns 200 OK")
    void acceptInvitation_Success() {
        String token = "valid-token";
        doNothing().when(workspaceService).acceptInvitation(token, userId, authHeader);

        ResponseEntity<String> result = workspaceController.acceptInvitation(token, userId, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Invitation accepted! You have joined the workspace.");
    }

    @Test
    @DisplayName("declineInvitation - Returns 200 OK")
    void declineInvitation_Success() {
        String token = "valid-token";
        doNothing().when(workspaceService).declineInvitation(token, userId, authHeader);

        ResponseEntity<String> result = workspaceController.declineInvitation(token, userId, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Invitation declined.");
    }

    @Test
    @DisplayName("revokeInvitation - Returns 200 OK")
    void revokeInvitation_Success() {
        Long invitationId = 5L;
        doNothing().when(workspaceService).revokeInvitation(workspaceId, invitationId, userId, userRole);

        ResponseEntity<String> result = workspaceController.revokeInvitation(workspaceId, invitationId, userId, userRole);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Invitation revoked");
    }

    @Test
    @DisplayName("getPendingInvitations - Returns 200 OK")
    void getPendingInvitations_Success() {
        List<WorkspaceInvitation> responses = Collections.singletonList(mock(WorkspaceInvitation.class));
        when(workspaceService.getPendingInvitations(workspaceId, userId, userRole)).thenReturn(responses);

        ResponseEntity<List<WorkspaceInvitation>> result = workspaceController.getPendingInvitations(workspaceId, userId, userRole);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }
}