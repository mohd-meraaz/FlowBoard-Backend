package com.flowboard.board_service.service;

import com.flowboard.board_service.client.AuthLookupClient;
import com.flowboard.board_service.client.WorkspaceAccessClient;
import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.entity.BoardShareAccess;
import com.flowboard.board_service.enums.BoardMemberRole;
import com.flowboard.board_service.enums.Visibility;
import com.flowboard.board_service.exception.CustomException;
import com.flowboard.board_service.repository.BoardMemberRepository;
import com.flowboard.board_service.repository.BoardRepository;
import com.flowboard.board_service.repository.BoardShareAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardServiceImpl - Unit Tests")
class BoardServiceImplTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardMemberRepository memberRepository;
    @Mock private BoardShareAccessRepository boardShareAccessRepository;
    @Mock private WorkspaceAccessClient workspaceAccessClient;
    @Mock private AuthLookupClient authLookupClient;
    @Mock private BoardShareEmailService boardShareEmailService;

    @InjectMocks
    private BoardServiceImpl boardService;

    private Board testBoard;
    private BoardMember adminMember;
    private final Long boardId = 1L;
    private final Long workspaceId = 10L;
    private final Long creatorId = 100L;
    private final Long memberId = 101L;
    private final String authHeader = "Bearer mock-token";
    private final String userEmail = "test@domain.com";

    @BeforeEach
    void setUp() {
        testBoard = Board.builder()
                .id(boardId)
                .workspaceId(workspaceId)
                .name("Project Alpha")
                .visibility(Visibility.PRIVATE)
                .createdById(creatorId)
                .isClosed(false)
                .createdAt(LocalDateTime.now())
                .build();

        adminMember = BoardMember.builder()
                .board(testBoard)
                .userId(creatorId)
                .role(BoardMemberRole.ADMIN)
                .build();

        // Default stubs to avoid repetitive mocking for `toResponse()` building
        lenient().when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));
        lenient().when(memberRepository.findByBoardId(boardId)).thenReturn(List.of(adminMember));
    }

    private void mockAdminAccess(Long userId) {
        BoardMember mockAdmin = BoardMember.builder().role(BoardMemberRole.ADMIN).build();
        when(memberRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(mockAdmin));
    }

    // ── CRUD Operations ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Create & Read Operations")
    class CreateAndReadTests {

        @Test
        @DisplayName("Create board successfully adds creator as admin")
        void createBoard_Success() {
            CreateBoardRequest req = new CreateBoardRequest();
            req.setName("New Board");
            req.setWorkspaceId(workspaceId);

            when(boardRepository.save(any(Board.class))).thenAnswer(i -> i.getArgument(0));
            when(memberRepository.save(any(BoardMember.class))).thenAnswer(i -> i.getArgument(0));
            when(memberRepository.findByBoardId(any())).thenReturn(List.of(adminMember)); // For toResponse

            BoardResponse res = boardService.createBoard(req, creatorId);

            assertThat(res.getName()).isEqualTo("New Board");
            verify(boardRepository).save(any(Board.class));
            verify(memberRepository).save(any(BoardMember.class));
        }

        @Test
        @DisplayName("getBoardById - Success for public board")
        void getBoardById_Public_Success() {
            testBoard.setVisibility(Visibility.PUBLIC);
            BoardResponse res = boardService.getBoardById(boardId, 999L, "other@test.com", authHeader);
            assertThat(res.getId()).isEqualTo(boardId);
        }

        @Test
        @DisplayName("getBoardById - Success for private board when user is member")
        void getBoardById_Private_MemberSuccess() {
            when(memberRepository.existsByBoardIdAndUserId(boardId, creatorId)).thenReturn(true);
            BoardResponse res = boardService.getBoardById(boardId, creatorId, userEmail, authHeader);
            assertThat(res.getId()).isEqualTo(boardId);
        }

        @Test
        @DisplayName("getBoardById - Throws 403 when non-member accesses private board")
        void getBoardById_Private_AccessDenied() {
            when(memberRepository.existsByBoardIdAndUserId(boardId, 999L)).thenReturn(false);
            when(boardShareAccessRepository.existsByBoardIdAndUserId(boardId, 999L)).thenReturn(false);
            when(boardShareAccessRepository.existsByBoardIdAndEmailIgnoreCase(eq(boardId), anyString())).thenReturn(false);
            when(workspaceAccessClient.isWorkspaceMember(workspaceId, 999L, authHeader)).thenReturn(false);

            assertThatThrownBy(() -> boardService.getBoardById(boardId, 999L, "stranger@domain.com", authHeader))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    // ── Update, Close, Reopen, Delete ─────────────────────────────────────────

    @Nested
    @DisplayName("Update & Delete Operations")
    class UpdateAndDeleteTests {

        @Test
        @DisplayName("Update board - Success")
        void updateBoard_Success() {
            mockAdminAccess(creatorId);
            UpdateBoardRequest req = new UpdateBoardRequest();
            req.setName("Updated Name");

            BoardResponse res = boardService.updateBoard(boardId, req, creatorId);

            assertThat(res.getName()).isEqualTo("Updated Name");
            verify(boardRepository).save(testBoard);
        }

        @Test
        @DisplayName("Update board - Throws if board is closed")
        void updateBoard_Closed_ThrowsException() {
            testBoard.setClosed(true);
            mockAdminAccess(creatorId);

            assertThatThrownBy(() -> boardService.updateBoard(boardId, new UpdateBoardRequest(), creatorId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Cannot update a closed board");
        }

        @Test
        @DisplayName("Close and Reopen board - Success")
        void closeAndReopenBoard_Success() {
            mockAdminAccess(creatorId);
            boardService.closeBoard(boardId, creatorId);
            assertThat(testBoard.isClosed()).isTrue();

            boardService.reopenBoard(boardId, creatorId);
            assertThat(testBoard.isClosed()).isFalse();
        }

        @Test
        @DisplayName("Delete board - Success by creator")
        void deleteBoard_Success() {
            boardService.deleteBoard(boardId, creatorId);
            verify(boardRepository).delete(testBoard);
        }

        @Test
        @DisplayName("Delete board - Throws 403 if not creator")
        void deleteBoard_NotCreator_ThrowsException() {
            assertThatThrownBy(() -> boardService.deleteBoard(boardId, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Only the board creator can delete");
        }
    }

    // ── Member Management ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Member Management")
    class MemberTests {

        @Test
        @DisplayName("Add member - Success")
        void addMember_Success() {
            mockAdminAccess(creatorId);
            AddBoardMemberRequest req = new AddBoardMemberRequest();
            req.setUserId(memberId);

            when(memberRepository.existsByBoardIdAndUserId(boardId, memberId)).thenReturn(false);

            BoardMember res = boardService.addMember(boardId, req, creatorId);

            assertThat(res.getUserId()).isEqualTo(memberId);
            verify(memberRepository).save(any(BoardMember.class));
        }

        @Test
        @DisplayName("Remove member - Fails if trying to remove creator")
        void removeMember_Creator_ThrowsException() {
            mockAdminAccess(creatorId);

            assertThatThrownBy(() -> boardService.removeMember(boardId, creatorId, creatorId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Cannot remove the board creator");
        }

        @Test
        @DisplayName("Update member role - Success")
        void updateMemberRole_Success() {
            mockAdminAccess(creatorId);
            BoardMember existingMember = BoardMember.builder().role(BoardMemberRole.MEMBER).build();
            when(memberRepository.findByBoardIdAndUserId(boardId, memberId)).thenReturn(Optional.of(existingMember));

            UpdateBoardMemberRoleRequest req = new UpdateBoardMemberRoleRequest();
            req.setRole(BoardMemberRole.OBSERVER);

            boardService.updateMemberRole(boardId, memberId, req, creatorId);

            assertThat(existingMember.getRole()).isEqualTo(BoardMemberRole.OBSERVER);
            verify(memberRepository).save(existingMember);
        }
    }

    // ── Sharing and Links ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sharing & Links")
    class SharingTests {

        @Test
        @DisplayName("Create share link - Fails if board is private")
        void createShareLink_PrivateBoard_ThrowsException() {
            mockAdminAccess(creatorId);
            assertThatThrownBy(() -> boardService.createOrGetShareLink(boardId, creatorId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("available only for public boards");
        }

        @Test
        @DisplayName("Share board by email - Success loop test")
        void shareBoardByEmail_Success() {
            mockAdminAccess(creatorId);
            testBoard.setVisibility(Visibility.PRIVATE); // Keep private, link logic falls back

            ShareBoardByEmailRequest req = new ShareBoardByEmailRequest();
            req.setEmails(List.of(" USER1@test.com ", "user2@test.com"));

             // Mock external clients
             AuthLookupClient.AuthUserDto creator = mock(AuthLookupClient.AuthUserDto.class);
             when(creator.getFullName()).thenReturn("Creator Name");
             when(authLookupClient.findUserById(creatorId, authHeader)).thenReturn(Optional.of(creator));
            when(authLookupClient.findUserByEmail(anyString(), anyString()))
                    .thenReturn(Optional.empty()); // Assume users aren't fully registered yet

            when(boardShareAccessRepository.existsByBoardIdAndEmailIgnoreCase(eq(boardId), anyString())).thenReturn(false);

            BoardShareResponse res = boardService.shareBoardByEmail(boardId, req, creatorId, authHeader);

            assertThat(res.getInvitedCount()).isEqualTo(2);
            verify(boardShareAccessRepository, times(2)).save(any(BoardShareAccess.class));
            verify(boardShareEmailService, times(2)).sendBoardInviteEmail(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Get public board by token - Validates visibility")
        void getPublicBoardByToken_Success() {
            testBoard.setVisibility(Visibility.PUBLIC);
            testBoard.setShareToken("valid-token");
            when(boardRepository.findByShareToken("valid-token")).thenReturn(Optional.of(testBoard));

            BoardResponse res = boardService.getPublicBoardByToken("valid-token");
            assertThat(res.getId()).isEqualTo(boardId);
        }

        @Test
        @DisplayName("Revoke shared email - Success")
        void revokeSharedEmail_Success() {
            mockAdminAccess(creatorId);
            when(boardShareAccessRepository.existsByBoardIdAndEmailIgnoreCase(boardId, "target@test.com")).thenReturn(true);

            boardService.revokeSharedEmail(boardId, "Target@Test.com", creatorId);

            verify(boardShareAccessRepository).deleteByBoardIdAndEmailIgnoreCase(boardId, "target@test.com");
        }
    }

    // ── Queries & Analytics ───────────────────────────────────────────────────

    @Nested
    @DisplayName("List Queries & Analytics")
    class QueryTests {

        @Test
        @DisplayName("Get boards by workspace - Filters correctly")
        void getBoardsByWorkspace_FiltersCorrectly() {
            Board publicBoard = Board.builder().visibility(Visibility.PUBLIC).build();
            Board privateBoard = Board.builder().id(99L).visibility(Visibility.PRIVATE).build();
            when(boardRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(publicBoard, privateBoard));

            // User has no access to private board
            lenient().when(memberRepository.existsByBoardIdAndUserId(99L, memberId)).thenReturn(false);

            List<BoardResponse> res = boardService.getBoardsByWorkspace(workspaceId, memberId, userEmail, authHeader);

            assertThat(res).hasSize(1); // Only public board
        }

        @Test
        @DisplayName("Get board analytics - Success")
        void getBoardAnalytics_Success() {
            when(memberRepository.existsByBoardIdAndUserId(boardId, creatorId)).thenReturn(true);

            BoardMember observer = BoardMember.builder().role(BoardMemberRole.OBSERVER).build();
            when(memberRepository.findByBoardId(boardId)).thenReturn(List.of(adminMember, observer));

            BoardResponse.BoardAnalytics analytics = boardService.getBoardAnalytics(boardId, creatorId, userEmail, authHeader);

            assertThat(analytics.getTotalMembers()).isEqualTo(2);
            assertThat(analytics.getAdminCount()).isEqualTo(1);
            assertThat(analytics.getObserverCount()).isEqualTo(1);
        }
    }
}