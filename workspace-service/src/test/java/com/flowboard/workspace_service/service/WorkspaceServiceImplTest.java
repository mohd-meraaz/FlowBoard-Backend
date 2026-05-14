package com.flowboard.workspace_service.service;

import com.flowboard.workspace_service.client.AuthLookupClient;
import com.flowboard.workspace_service.config.RabbitMQConfig;
import com.flowboard.workspace_service.dto.*;
import com.flowboard.workspace_service.entity.Workspace;
import com.flowboard.workspace_service.entity.WorkspaceInvitation;
import com.flowboard.workspace_service.entity.WorkspaceMember;
import com.flowboard.workspace_service.enums.MemberRole;
import com.flowboard.workspace_service.enums.Visibility;
import com.flowboard.workspace_service.exception.CustomException;
import com.flowboard.workspace_service.repository.WorkspaceInvitationRepository;
import com.flowboard.workspace_service.repository.WorkspaceMemberRepository;
import com.flowboard.workspace_service.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceServiceImpl - Unit Tests (>90% Coverage)")
class WorkspaceServiceImplTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private WorkspaceInvitationRepository invitationRepository;
    @Mock private AuthLookupClient authLookupClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private Workspace testWorkspace;
    private WorkspaceMember adminMember;
    private AuthLookupClient.AuthUserDto authUserDto;

    private final Long workspaceId = 100L;
    private final Long ownerId = 1L;
    private final Long targetUserId = 2L;
    private final String authHeader = "Bearer mock-token";
    private final String inviteToken = "mock-uuid-token";
    private final String inviteeEmail = "invitee@flowboard.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workspaceService, "workspaceInviteBaseUrl", "http://localhost:4200");

        testWorkspace = Workspace.builder()
                .id(workspaceId)
                .name("Dev Team")
                .ownerId(ownerId)
                .visibility(Visibility.PRIVATE)
                .build();

        adminMember = WorkspaceMember.builder()
                .workspace(testWorkspace)
                .userId(ownerId)
                .role(MemberRole.ADMIN)
                .build();

        authUserDto = new AuthLookupClient.AuthUserDto();
        authUserDto.setId(targetUserId);
        authUserDto.setEmail(inviteeEmail);
        authUserDto.setFullName("John Doe");

        lenient().when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(testWorkspace));
        lenient().when(memberRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(adminMember));
    }

    private void mockAdminAccess() {
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId)).thenReturn(Optional.of(adminMember));
    }

    // ── Create & Update & Delete ─────────────────────────────────────────────

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("createWorkspace - Success (Assigns PRIVATE default and creates Admin)")
        void createWorkspace_Success() {
            CreateWorkspaceRequest req = new CreateWorkspaceRequest();
            req.setName("New Workspace");

            when(workspaceRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).thenReturn(false);
            when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

            WorkspaceResponse res = workspaceService.createWorkspace(req, ownerId);

            assertThat(res.getName()).isEqualTo("New Workspace");
            assertThat(res.getVisibility()).isEqualTo(Visibility.PRIVATE);
            verify(workspaceRepository).save(any(Workspace.class));
            verify(memberRepository).save(any(WorkspaceMember.class));
        }

        @Test
        @DisplayName("createWorkspace - Fails on duplicate name")
        void createWorkspace_DuplicateName_ThrowsException() {
            CreateWorkspaceRequest req = new CreateWorkspaceRequest();
            req.setName("Dev Team");

            when(workspaceRepository.existsByNameAndOwnerId("Dev Team", ownerId)).thenReturn(true);

            assertThatThrownBy(() -> workspaceService.createWorkspace(req, ownerId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("already have a workspace named");
        }

        @Test
        @DisplayName("updateWorkspace - Success by Admin")
        void updateWorkspace_Success() {
            mockAdminAccess();
            UpdateWorkspaceRequest req = new UpdateWorkspaceRequest();
            req.setName("Updated Name");
            req.setVisibility(Visibility.PUBLIC);

            WorkspaceResponse res = workspaceService.updateWorkspace(workspaceId, req, ownerId, "USER", authHeader);

            assertThat(res.getName()).isEqualTo("Updated Name");
            assertThat(res.getVisibility()).isEqualTo(Visibility.PUBLIC);
        }

        @Test
        @DisplayName("deleteWorkspace - Success by Owner")
        void deleteWorkspace_ByOwner_Success() {
            workspaceService.deleteWorkspace(workspaceId, ownerId, "USER");
            verify(workspaceRepository).delete(testWorkspace);
        }

        @Test
        @DisplayName("deleteWorkspace - Success by PLATFORM_ADMIN (not owner)")
        void deleteWorkspace_ByAdmin_Success() {
            workspaceService.deleteWorkspace(workspaceId, 999L, "PLATFORM_ADMIN");
            verify(workspaceRepository).delete(testWorkspace);
        }

        @Test
        @DisplayName("deleteWorkspace - Fails by regular member")
        void deleteWorkspace_ByMember_ThrowsException() {
            assertThatThrownBy(() -> workspaceService.deleteWorkspace(workspaceId, 999L, "USER"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Only the workspace owner can delete it");
        }
    }

    // ── Member Management ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Member Operations")
    class MemberTests {

        @Test
        @DisplayName("addMember - Success with enrichment")
        void addMember_Success() {
            mockAdminAccess();
            AddMemberRequest req = new AddMemberRequest();
            req.setUserId(targetUserId);

            when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)).thenReturn(false);
            when(authLookupClient.findUserById(targetUserId, authHeader)).thenReturn(Optional.of(authUserDto));

            WorkspaceResponse.MemberDto res = workspaceService.addMember(workspaceId, req, ownerId, "USER", authHeader);

            assertThat(res.getUserId()).isEqualTo(targetUserId);
            assertThat(res.getUser().getEmail()).isEqualTo(inviteeEmail); // Enrichment worked
            verify(memberRepository).save(any(WorkspaceMember.class));
        }

        @Test
        @DisplayName("removeMember - Fails if trying to remove owner")
        void removeMember_Owner_ThrowsException() {
            mockAdminAccess();
            when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, ownerId)).thenReturn(true);

            assertThatThrownBy(() -> workspaceService.removeMember(workspaceId, ownerId, ownerId, "USER"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Cannot remove the workspace owner");
        }

        @Test
        @DisplayName("updateMemberRole - Requires Admin access")
        void updateMemberRole_NotAdmin_ThrowsException() {
            WorkspaceMember normalMember = WorkspaceMember.builder().role(MemberRole.MEMBER).build();
            when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, 999L)).thenReturn(Optional.of(normalMember));

            UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();

            assertThatThrownBy(() -> workspaceService.updateMemberRole(workspaceId, targetUserId, req, 999L, "USER"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    // ── Invitation Lifecycle ────────────────────────────────────────────────

    @Nested
    @DisplayName("Invitation Operations")
    class InvitationTests {

        @Test
        @DisplayName("inviteMember - Success publishes RabbitMQ event")
        void inviteMember_Success() {
            mockAdminAccess();
            InviteMemberRequest req = new InviteMemberRequest();
            req.setEmail(inviteeEmail);
            req.setRole(MemberRole.MEMBER);

            when(invitationRepository.existsByWorkspaceIdAndInviteeEmailAndStatus(workspaceId, inviteeEmail, "PENDING")).thenReturn(false);
            when(authLookupClient.findUserIdByEmail(inviteeEmail, authHeader)).thenReturn(Optional.of(targetUserId));

            workspaceService.inviteMember(workspaceId, req, ownerId, "USER", authHeader);

            verify(invitationRepository).save(any(WorkspaceInvitation.class));
            verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.FLOWBOARD_EXCHANGE), eq(RabbitMQConfig.INVITE_KEY), any(Object.class));
        }

        @Test
        @DisplayName("acceptInvitation - Success for new member")
        void acceptInvitation_NewMember_Success() {
            WorkspaceInvitation inv = WorkspaceInvitation.builder()
                    .workspaceId(workspaceId).inviteeEmail(inviteeEmail)
                    .status("PENDING").role(MemberRole.MEMBER).invitedBy(ownerId)
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();

            when(invitationRepository.findByToken(inviteToken)).thenReturn(Optional.of(inv));
            when(authLookupClient.findUserById(targetUserId, authHeader)).thenReturn(Optional.of(authUserDto)); // Responder
            when(authLookupClient.findUserById(ownerId, authHeader)).thenReturn(Optional.of(authUserDto)); // Inviter (mocked same for simplicity)
            when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)).thenReturn(false);

            workspaceService.acceptInvitation(inviteToken, targetUserId, authHeader);

            assertThat(inv.getStatus()).isEqualTo("ACCEPTED");
            verify(memberRepository).save(any(WorkspaceMember.class));
            verify(rabbitTemplate).convertAndSend(anyString(), eq(RabbitMQConfig.INVITE_RESPONSE_KEY), any(Object.class));
        }

        @Test
        @DisplayName("acceptInvitation - Fails if Email does not match")
        void acceptInvitation_EmailMismatch_ThrowsException() {
            WorkspaceInvitation inv = WorkspaceInvitation.builder()
                    .workspaceId(workspaceId).inviteeEmail("original@flowboard.com")
                    .status("PENDING").expiresAt(LocalDateTime.now().plusDays(1)).build();

            when(invitationRepository.findByToken(inviteToken)).thenReturn(Optional.of(inv));
            when(authLookupClient.findUserById(targetUserId, authHeader)).thenReturn(Optional.of(authUserDto)); // AuthDto has invitee@flowboard.com

            assertThatThrownBy(() -> workspaceService.acceptInvitation(inviteToken, targetUserId, authHeader))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("different email address");
        }

        @Test
        @DisplayName("acceptInvitation - Fails if Expired")
        void acceptInvitation_Expired_ThrowsException() {
            WorkspaceInvitation inv = WorkspaceInvitation.builder()
                    .status("PENDING").expiresAt(LocalDateTime.now().minusDays(1)).build();

            when(invitationRepository.findByToken(inviteToken)).thenReturn(Optional.of(inv));

            assertThatThrownBy(() -> workspaceService.acceptInvitation(inviteToken, targetUserId, authHeader))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Invitation has expired");

            assertThat(inv.getStatus()).isEqualTo("EXPIRED"); // Status should be updated to EXPIRED
        }

        @Test
        @DisplayName("revokeInvitation - Success")
        void revokeInvitation_Success() {
            mockAdminAccess();
            WorkspaceInvitation inv = WorkspaceInvitation.builder()
                    .id(5L).workspaceId(workspaceId).status("PENDING").build();

            when(invitationRepository.findById(5L)).thenReturn(Optional.of(inv));

            workspaceService.revokeInvitation(workspaceId, 5L, ownerId, "USER");

            assertThat(inv.getStatus()).isEqualTo("REVOKED");
            verify(invitationRepository).save(inv);
        }
    }

    // ── Queries & Read Access ───────────────────────────────────────────────

    @Nested
    @DisplayName("Queries and Retrieval")
    class QueryTests {

        @Test
        @DisplayName("getById - Success for Private board if User is Member")
        void getById_Private_Member_Success() {
            when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)).thenReturn(true);
            WorkspaceResponse res = workspaceService.getById(workspaceId, targetUserId, "USER", authHeader);
            assertThat(res.getId()).isEqualTo(workspaceId);
        }

        @Test
        @DisplayName("getById - Fails for Private board if User is NOT Member")
        void getById_Private_NotMember_ThrowsException() {
            when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)).thenReturn(false);

            assertThatThrownBy(() -> workspaceService.getById(workspaceId, targetUserId, "USER", authHeader))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("getAllWorkspaces - Maps to Response")
        void getAllWorkspaces_Success() {
            when(workspaceRepository.findAll()).thenReturn(Collections.singletonList(testWorkspace));
            List<WorkspaceResponse> res = workspaceService.getAllWorkspaces(authHeader);
            assertThat(res).hasSize(1);
        }
    }
}