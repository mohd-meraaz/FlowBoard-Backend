package com.flowboard.board_service.service;

import com.flowboard.board_service.dto.*;
import com.flowboard.board_service.entity.BoardMember;

import java.util.List;

public interface BoardService {

    BoardResponse createBoard(CreateBoardRequest request, Long createdById);
    BoardResponse getBoardById(Long workspace, Long requesterId, String requesterEmail, String authorizationHeader);
    BoardResponse getPublicBoardByToken(String shareToken);
    PublicBoardDetailsResponse getPublicBoardDetailsByToken(String shareToken);
    List<BoardResponse> getBoardsByWorkspace(Long workspaceId, Long requesterId, String requesterEmail, String authorizationHeader);
    List<BoardResponse> getBoardsByMember(Long userId);
    List<BoardResponse> getBoardsByCreator(Long createdById);
    List<BoardResponse> getPublicBoards();
    List<BoardResponse> getClosedBoards(Long workspaceId, Long requesterId, String requesterEmail, String authorizationHeader);
    BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, Long requesterId);
    BoardResponse closeBoard(Long boardId, Long requesterId);
    BoardResponse reopenBoard(Long boardId, Long requesterId);
    void deleteBoard(Long boardId, Long requesterId);
    ShareBoardLinkResponse createOrGetShareLink(Long boardId, Long requesterId);
    BoardShareResponse shareBoardByEmail(Long boardId, ShareBoardByEmailRequest request, Long requesterId, String authorizationHeader);
    List<BoardShareResponse.SharedEmailDto> getSharedEmails(Long boardId, Long requesterId);
    void revokeSharedEmail(Long boardId, String email, Long requesterId);

    //Member management
    BoardMember addMember(Long boardId, AddBoardMemberRequest request, Long requesterId);
    void removeMember(Long boardId, Long userId, Long requesterId);
    void updateMemberRole(Long boardId, Long userId, UpdateBoardMemberRoleRequest request, Long requesterId);
    List<BoardMember> getMembers(Long boardId);

    BoardResponse.BoardAnalytics getBoardAnalytics(Long boardId, Long requesterId, String requesterEmail, String authorizationHeader);
}
