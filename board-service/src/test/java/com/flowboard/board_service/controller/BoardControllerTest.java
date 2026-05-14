package com.flowboard.board_service.controller;

import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.exception.CustomException;
import com.flowboard.board_service.service.BoardService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardController - Unit Tests")
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    @InjectMocks
    private BoardController boardController;

    private final Long userId = 1L;
    private final String userEmail = "test@flowboard.com";
    private final String authHeader = "Bearer mock-token";
    private final Long boardId = 100L;
    private final Long workspaceId = 200L;

    @Test
    @DisplayName("resolveUserId throws exception when header is missing")
    void resolveUserId_MissingHeader_ThrowsException() {
        CreateBoardRequest request = mock(CreateBoardRequest.class);

        assertThatThrownBy(() -> boardController.create(request, null, userEmail))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    @DisplayName("create - Returns 201 Created")
    void create_Success() {
        CreateBoardRequest request = mock(CreateBoardRequest.class);
        BoardResponse response = mock(BoardResponse.class);

        when(boardService.createBoard(request, userId)).thenReturn(response);

        ResponseEntity<BoardResponse> result = boardController.create(request, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(boardService).createBoard(request, userId);
    }

    @Test
    @DisplayName("getById - Returns 200 OK")
    void getById_Success() {
        BoardResponse response = mock(BoardResponse.class);
        when(boardService.getBoardById(boardId, userId, userEmail, authHeader)).thenReturn(response);

        ResponseEntity<BoardResponse> result = boardController.getById(boardId, userId, userEmail, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getPublicByToken - Returns 200 OK")
    void getPublicByToken_Success() {
        String token = "public-token-123";
        BoardResponse response = mock(BoardResponse.class);
        when(boardService.getPublicBoardByToken(token)).thenReturn(response);

        ResponseEntity<BoardResponse> result = boardController.getPublicByToken(token);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getByWorkspace - Returns 200 OK")
    void getByWorkspace_Success() {
        List<BoardResponse> responses = Collections.singletonList(mock(BoardResponse.class));
        when(boardService.getBoardsByWorkspace(workspaceId, userId, userEmail, authHeader)).thenReturn(responses);

        ResponseEntity<List<BoardResponse>> result = boardController.getByWorkspace(workspaceId, userId, userEmail, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByMember - Returns 200 OK")
    void getByMember_Success() {
        List<BoardResponse> responses = Collections.singletonList(mock(BoardResponse.class));
        when(boardService.getBoardsByMember(userId)).thenReturn(responses);

        ResponseEntity<List<BoardResponse>> result = boardController.getByMember(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByCreator - Returns 200 OK")
    void getByCreator_Success() {
        List<BoardResponse> responses = Collections.singletonList(mock(BoardResponse.class));
        when(boardService.getBoardsByCreator(userId)).thenReturn(responses);

        ResponseEntity<List<BoardResponse>> result = boardController.getByCreator(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getPublic - Returns 200 OK")
    void getPublic_Success() {
        List<BoardResponse> responses = Collections.singletonList(mock(BoardResponse.class));
        when(boardService.getPublicBoards()).thenReturn(responses);

        ResponseEntity<List<BoardResponse>> result = boardController.getPublic();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getClosedBoards - Returns 200 OK")
    void getClosedBoards_Success() {
        List<BoardResponse> responses = Collections.singletonList(mock(BoardResponse.class));
        when(boardService.getClosedBoards(workspaceId, userId, userEmail, authHeader)).thenReturn(responses);

        ResponseEntity<List<BoardResponse>> result = boardController.getClosedBoards(workspaceId, userId, userEmail, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("update - Returns 200 OK")
    void update_Success() {
        UpdateBoardRequest request = mock(UpdateBoardRequest.class);
        BoardResponse response = mock(BoardResponse.class);
        when(boardService.updateBoard(boardId, request, userId)).thenReturn(response);

        ResponseEntity<BoardResponse> result = boardController.update(boardId, request, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("close - Returns 200 OK")
    void close_Success() {
        BoardResponse response = mock(BoardResponse.class);
        when(boardService.closeBoard(boardId, userId)).thenReturn(response);

        ResponseEntity<BoardResponse> result = boardController.close(boardId, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("reopen - Returns 200 OK")
    void reopen_Success() {
        BoardResponse response = mock(BoardResponse.class);
        when(boardService.reopenBoard(boardId, userId)).thenReturn(response);

        ResponseEntity<BoardResponse> result = boardController.reopen(boardId, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("delete - Returns 200 OK")
    void delete_Success() {
        doNothing().when(boardService).deleteBoard(boardId, userId);

        ResponseEntity<String> result = boardController.delete(boardId, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Board deleted successfully");
        verify(boardService).deleteBoard(boardId, userId);
    }

    @Test
    @DisplayName("addMember - Returns 200 OK")
    void addMember_Success() {
        AddBoardMemberRequest request = mock(AddBoardMemberRequest.class);
        BoardMember member = mock(BoardMember.class);
        when(boardService.addMember(boardId, request, userId)).thenReturn(member);

        ResponseEntity<BoardMember> result = boardController.addMember(boardId, request, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(member);
    }

    @Test
    @DisplayName("deleteMember - Returns 200 OK")
    void deleteMember_Success() {
        Long memberId = 5L;
        doNothing().when(boardService).removeMember(boardId, memberId, userId);

        ResponseEntity<String> result = boardController.deleteMember(boardId, memberId, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Member removed successfully");
    }

    @Test
    @DisplayName("updateMemberRole - Returns 200 OK")
    void updateMemberRole_Success() {
        Long memberId = 5L;
        UpdateBoardMemberRoleRequest request = mock(UpdateBoardMemberRoleRequest.class);
        doNothing().when(boardService).updateMemberRole(boardId, memberId, request, userId);

        ResponseEntity<String> result = boardController.updateMemberRole(boardId, memberId, request, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Member role updated successfully");
    }

    @Test
    @DisplayName("getMembers - Returns 200 OK")
    void getMembers_Success() {
        List<BoardMember> members = Collections.singletonList(mock(BoardMember.class));
        when(boardService.getMembers(boardId)).thenReturn(members);

        ResponseEntity<List<BoardMember>> result = boardController.getMembers(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(members);
    }

    @Test
    @DisplayName("createOrGetShareLink - Returns 200 OK")
    void createOrGetShareLink_Success() {
        ShareBoardLinkResponse response = mock(ShareBoardLinkResponse.class);
        when(boardService.createOrGetShareLink(boardId, userId)).thenReturn(response);

        ResponseEntity<ShareBoardLinkResponse> result = boardController.createOrGetShareLink(boardId, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("shareByEmail - Returns 200 OK")
    void shareByEmail_Success() {
        ShareBoardByEmailRequest request = mock(ShareBoardByEmailRequest.class);
        BoardShareResponse response = mock(BoardShareResponse.class);
        when(boardService.shareBoardByEmail(boardId, request, userId, authHeader)).thenReturn(response);

        ResponseEntity<BoardShareResponse> result = boardController.shareByEmail(boardId, request, userId, userEmail, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getSharedEmails - Returns 200 OK")
    void getSharedEmails_Success() {
        List<BoardShareResponse.SharedEmailDto> responses = Collections.singletonList(mock(BoardShareResponse.SharedEmailDto.class));
        when(boardService.getSharedEmails(boardId, userId)).thenReturn(responses);

        ResponseEntity<List<BoardShareResponse.SharedEmailDto>> result = boardController.getSharedEmails(boardId, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("revokeSharedEmail - Returns 200 OK")
    void revokeSharedEmail_Success() {
        String targetEmail = "remove@test.com";
        doNothing().when(boardService).revokeSharedEmail(boardId, targetEmail, userId);

        ResponseEntity<String> result = boardController.revokeSharedEmail(boardId, targetEmail, userId, userEmail);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Shared access removed");
    }

    @Test
    @DisplayName("getAnalytics - Returns 200 OK")
    void getAnalytics_Success() {
        BoardResponse.BoardAnalytics response = mock(BoardResponse.BoardAnalytics.class);
        when(boardService.getBoardAnalytics(boardId, userId, userEmail, authHeader)).thenReturn(response);

        ResponseEntity<BoardResponse.BoardAnalytics> result = boardController.getAnalytics(boardId, userId, userEmail, authHeader);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }
}