package com.flowboard.board_service.service;

import com.flowboard.board_service.client.AuthLookupClient;
import com.flowboard.board_service.client.BoardPublicDataClient;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService{

    private final BoardRepository boardRepository;
    private final BoardMemberRepository memberRepository;
    private final BoardShareAccessRepository boardShareAccessRepository;
    private final WorkspaceAccessClient workspaceAccessClient;
    private final AuthLookupClient authLookupClient;
    private final BoardPublicDataClient boardPublicDataClient;
    private final BoardShareEmailService boardShareEmailService;

    // ── Board CRUD ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoardResponse createBoard(CreateBoardRequest request, Long createdById) {

        Board board = Board.builder()
                .workspaceId(request.getWorkspaceId())
                .name(request.getName())
                .description(request.getDescription())
                .background(request.getBackground())
                .visibility(request.getVisibility() != null
                        ? request.getVisibility() : Visibility.PRIVATE)
                .createdById(createdById)
                .isClosed(false)
                .createdAt(LocalDateTime.now())
                .build();

        boardRepository.save(board);

        // Creator is automatically added as ADMIN member
        BoardMember creatorMember = BoardMember.builder()
                .board(board)
                .userId(createdById)
                .role(BoardMemberRole.ADMIN)
                .addedAt(LocalDateTime.now())
                .build();

        memberRepository.save(creatorMember);

        log.info("Board created: id={} name={} workspaceId={} createdBy={}",
                board.getId(), board.getName(), board.getWorkspaceId(), createdById);

        return toResponse(board);
    }

    @Override
    public BoardResponse getBoardById(Long boardId, Long requesterId, String requesterEmail, String authorizationHeader) {
        Board board = findBoard(boardId);

        // Private boards visible only to members
        if (board.getVisibility() == Visibility.PRIVATE) {
            requireMember(board, requesterId, requesterEmail, authorizationHeader);
        }

        return toResponse(board);
    }

    @Override
    public BoardResponse getPublicBoardByToken(String shareToken) {
        return toResponse(findPublicBoardByToken(shareToken));
    }

    @Override
    public PublicBoardDetailsResponse getPublicBoardDetailsByToken(String shareToken) {
        Board board = findPublicBoardByToken(shareToken);

        try {
            return PublicBoardDetailsResponse.builder()
                    .board(toResponse(board))
                    .lists(boardPublicDataClient.getListsByBoard(board.getId()))
                    .cards(boardPublicDataClient.getCardsByBoard(board.getId()))
                    .build();
        } catch (IllegalStateException ex) {
            throw new CustomException("Failed to load public board details", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public List<BoardResponse> getBoardsByWorkspace(Long workspaceId, Long requesterId, String requesterEmail, String authorizationHeader) {
        return boardRepository.findByWorkspaceId(workspaceId)
                .stream()
                // Filter out private boards the requester is not a member of
                .filter(b -> b.getVisibility() == Visibility.PUBLIC
                        || hasBoardAccess(b, requesterId, requesterEmail, authorizationHeader))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<BoardResponse> getBoardsByMember(Long userId) {
        return boardRepository.findByMemberUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BoardResponse> getBoardsByCreator(Long createdById) {
        return boardRepository.findByCreatedById(createdById)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BoardResponse> getPublicBoards() {
        return boardRepository.findByVisibility(Visibility.PUBLIC)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BoardResponse> getClosedBoards(Long workspaceId, Long requesterId, String requesterEmail, String authorizationHeader) {
        return boardRepository.findByWorkspaceIdAndIsClosed(workspaceId, true)
                .stream()
                .filter(b -> hasBoardAccess(b, requesterId, requesterEmail, authorizationHeader))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Cannot update a closed board — reopen it first",
                    HttpStatus.BAD_REQUEST);
        }

        board.setName(request.getName());
        board.setDescription(request.getDescription());
        if (request.getBackground() != null) board.setBackground(request.getBackground());
        if (request.getVisibility() != null) board.setVisibility(request.getVisibility());
        board.setUpdatedAt(LocalDateTime.now());

        boardRepository.save(board);
        log.info("Board updated: id={}", boardId);
        return toResponse(board);
    }

    @Override
    @Transactional
    public BoardResponse closeBoard(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Board is already closed", HttpStatus.BAD_REQUEST);
        }

        board.setClosed(true);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
        log.info("Board closed: id={} by userId={}", boardId, requesterId);
        return toResponse(board);
    }

    @Override
    @Transactional
    public BoardResponse reopenBoard(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (!board.isClosed()) {
            throw new CustomException("Board is already open", HttpStatus.BAD_REQUEST);
        }

        board.setClosed(false);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);
        log.info("Board reopened: id={} by userId={}", boardId, requesterId);
        return toResponse(board);
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);

        // Only the board creator can delete it
        if (!board.getCreatedById().equals(requesterId)) {
            throw new CustomException("Only the board creator can delete this board",
                    HttpStatus.FORBIDDEN);
        }

        boardRepository.delete(board);
        log.info("Board deleted: id={} by userId={}", boardId, requesterId);
    }

    @Override
    @Transactional
    public ShareBoardLinkResponse createOrGetShareLink(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.getVisibility() != Visibility.PUBLIC) {
            throw new CustomException("Public links are available only for public boards", HttpStatus.BAD_REQUEST);
        }

        if (board.getShareToken() == null || board.getShareToken().isBlank()) {
            board.setShareToken(generateShareToken());
            board.setUpdatedAt(LocalDateTime.now());
            boardRepository.save(board);
        }

        return ShareBoardLinkResponse.builder()
                .shareToken(board.getShareToken())
                .path("/boards/public/" + board.getShareToken())
                .build();
    }

    @Override
    @Transactional
    public BoardShareResponse shareBoardByEmail(Long boardId,
                                                ShareBoardByEmailRequest request,
                                                Long requesterId,
                                                String authorizationHeader) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Cannot share a closed board", HttpStatus.BAD_REQUEST);
        }

        List<String> normalizedEmails = request.getEmails().stream()
                .map(this::normalizeEmail)
                .filter(email -> !email.isBlank())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));

        if (normalizedEmails.isEmpty()) {
            throw new CustomException("At least one valid email is required", HttpStatus.BAD_REQUEST);
        }

        String inviterName = authLookupClient.findUserById(requesterId, authorizationHeader)
                .map(AuthLookupClient.AuthUserDto::getFullName)
                .orElse("A teammate");

        String sharePath = board.getVisibility() == Visibility.PUBLIC
                ? createOrGetShareLink(boardId, requesterId).getPath()
                : "/board/" + boardId;

        int invitedCount = 0;
        int skippedCount = 0;

        for (String email : normalizedEmails) {
            if (boardShareAccessRepository.existsByBoardIdAndEmailIgnoreCase(boardId, email)) {
                skippedCount++;
                continue;
            }

            Long invitedUserId = authLookupClient.findUserByEmail(email, authorizationHeader)
                    .map(AuthLookupClient.AuthUserDto::getId)
                    .orElse(null);

            BoardShareAccess access = BoardShareAccess.builder()
                    .board(board)
                    .email(email)
                    .userId(invitedUserId)
                    .invitedById(requesterId)
                    .createdAt(LocalDateTime.now())
                    .build();

            boardShareAccessRepository.save(access);
            invitedCount++;

            boardShareEmailService.sendBoardInviteEmail(
                    email,
                    inviterName,
                    board.getName(),
                    sharePath,
                    board.getVisibility() == Visibility.PUBLIC
            );
        }

        return BoardShareResponse.builder()
                .invitedCount(invitedCount)
                .skippedCount(skippedCount)
                .shared(getSharedEmails(boardId, requesterId))
                .build();
    }

    @Override
    public List<BoardShareResponse.SharedEmailDto> getSharedEmails(Long boardId, Long requesterId) {
        findBoard(boardId);
        requireAdmin(boardId, requesterId);

        return boardShareAccessRepository.findByBoardIdOrderByCreatedAtAsc(boardId)
                .stream()
                .map(this::toSharedEmailDto)
                .toList();
    }

    @Override
    @Transactional
    public void revokeSharedEmail(Long boardId, String email, Long requesterId) {
        findBoard(boardId);
        requireAdmin(boardId, requesterId);

        String normalizedEmail = normalizeEmail(email);
        if (!boardShareAccessRepository.existsByBoardIdAndEmailIgnoreCase(boardId, normalizedEmail)) {
            throw new CustomException("Shared email not found", HttpStatus.NOT_FOUND);
        }

        boardShareAccessRepository.deleteByBoardIdAndEmailIgnoreCase(boardId, normalizedEmail);
    }

    // ── Member Management ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoardMember addMember(Long boardId, AddBoardMemberRequest request, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Cannot add members to a closed board", HttpStatus.BAD_REQUEST);
        }

        if (memberRepository.existsByBoardIdAndUserId(boardId, request.getUserId())) {
            throw new CustomException("User is already a member of this board",
                    HttpStatus.BAD_REQUEST);
        }

        BoardMember member = BoardMember.builder()
                .board(board)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : BoardMemberRole.MEMBER)
                .addedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);
        log.info("Board member added: boardId={} userId={} role={}",
                boardId, request.getUserId(), member.getRole());
        return member;
    }

    @Override
    @Transactional
    public void removeMember(Long boardId, Long userId, Long requesterId) {
        findBoard(boardId);
        requireAdmin(boardId, requesterId);

        // Creator cannot be removed from their own board
        Board board = findBoard(boardId);
        if (board.getCreatedById().equals(userId)) {
            throw new CustomException("Cannot remove the board creator", HttpStatus.BAD_REQUEST);
        }

        if (!memberRepository.existsByBoardIdAndUserId(boardId, userId)) {
            throw new CustomException("User is not a member of this board", HttpStatus.NOT_FOUND);
        }

        memberRepository.deleteByBoardIdAndUserId(boardId, userId);
        log.info("Board member removed: boardId={} userId={}", boardId, userId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long boardId, Long userId,
                                 UpdateBoardMemberRoleRequest request,
                                 Long requesterId) {
        findBoard(boardId);
        requireAdmin(boardId, requesterId);

        BoardMember member = memberRepository
                .findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new CustomException(
                        "User is not a member of this board", HttpStatus.NOT_FOUND));

        member.setRole(request.getRole());
        memberRepository.save(member);
        log.info("Board member role updated: boardId={} userId={} newRole={}",
                boardId, userId, request.getRole());
    }

    @Override
    public List<BoardMember> getMembers(Long boardId) {
        findBoard(boardId);
        return memberRepository.findByBoardId(boardId);
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Override
    public BoardResponse.BoardAnalytics getBoardAnalytics(Long boardId, Long requesterId, String requesterEmail, String authorizationHeader) {
        Board board = findBoard(boardId);
        requireMember(board, requesterId, requesterEmail, authorizationHeader);

        List<BoardMember> members = memberRepository.findByBoardId(boardId);

        return BoardResponse.BoardAnalytics.builder()
                .totalMembers(members.size())
                .observerCount(members.stream()
                        .filter(m -> m.getRole() == BoardMemberRole.OBSERVER).count())
                .memberCount(members.stream()
                        .filter(m -> m.getRole() == BoardMemberRole.MEMBER).count())
                .adminCount(members.stream()
                        .filter(m -> m.getRole() == BoardMemberRole.ADMIN).count())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(
                        "Board not found", HttpStatus.NOT_FOUND));
    }

    private Board findPublicBoardByToken(String shareToken) {
        Board board = boardRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new CustomException("Board share link is invalid", HttpStatus.NOT_FOUND));

        if (board.getVisibility() != Visibility.PUBLIC) {
            throw new CustomException("Board share link is no longer public", HttpStatus.FORBIDDEN);
        }

        return board;
    }

    private void requireMember(Board board, Long userId, String requesterEmail, String authorizationHeader) {
        if (!hasBoardAccess(board, userId, requesterEmail, authorizationHeader)) {
            throw new CustomException(
                    "Access denied — you are not a member of this board",
                    HttpStatus.FORBIDDEN);
        }
    }

    private boolean hasBoardAccess(Board board, Long userId, String requesterEmail, String authorizationHeader) {
        return memberRepository.existsByBoardIdAndUserId(board.getId(), userId)
                || boardShareAccessRepository.existsByBoardIdAndUserId(board.getId(), userId)
                || hasEmailShareAccess(board.getId(), requesterEmail)
                || workspaceAccessClient.isWorkspaceMember(board.getWorkspaceId(), userId, authorizationHeader);
    }

    private void requireAdmin(Long boardId, Long userId) {
        BoardMember member = memberRepository
                .findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new CustomException(
                        "Access denied — you are not a member of this board",
                        HttpStatus.FORBIDDEN));

        if (member.getRole() != BoardMemberRole.ADMIN) {
            throw new CustomException(
                    "Access denied — admin role required", HttpStatus.FORBIDDEN);
        }
    }

    private BoardResponse toResponse(Board board) {
        List<BoardMember> members = memberRepository.findByBoardId(board.getId());

        List<BoardResponse.MemberDTO> memberDtos = members.stream()
                .map(m -> BoardResponse.MemberDTO.builder()
                        .userId(m.getUserId())
                        .role(m.getRole())
                        .addedAt(m.getAddedAt())
                        .build())
                .toList();

        BoardResponse.BoardAnalytics analytics = BoardResponse.BoardAnalytics.builder()
                .totalMembers(members.size())
                .observerCount(members.stream()
                        .filter(m -> m.getRole() == BoardMemberRole.OBSERVER).count())
                .memberCount(members.stream()
                        .filter(m -> m.getRole() == BoardMemberRole.MEMBER).count())
                .adminCount(members.stream()
                        .filter(m -> m.getRole() == BoardMemberRole.ADMIN).count())
                .build();

        return BoardResponse.builder()
                .id(board.getId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .description(board.getDescription())
                .background(board.getBackground())
                .visibility(board.getVisibility())
                .createdById(board.getCreatedById())
                .isClosed(board.isClosed())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .memberCount(members.size())
                .members(memberDtos)
                .analytics(analytics)
                .build();
    }

    private boolean hasEmailShareAccess(Long boardId, String requesterEmail) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            return false;
        }
        return boardShareAccessRepository.existsByBoardIdAndEmailIgnoreCase(boardId, normalizeEmail(requesterEmail));
    }

    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private BoardShareResponse.SharedEmailDto toSharedEmailDto(BoardShareAccess access) {
        return BoardShareResponse.SharedEmailDto.builder()
                .email(access.getEmail())
                .userId(access.getUserId())
                .createdAt(access.getCreatedAt())
                .build();
    }
}
