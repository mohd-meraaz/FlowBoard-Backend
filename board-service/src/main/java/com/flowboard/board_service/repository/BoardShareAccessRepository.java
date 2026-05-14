package com.flowboard.board_service.repository;

import com.flowboard.board_service.entity.BoardShareAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardShareAccessRepository extends JpaRepository<BoardShareAccess, Long> {

    List<BoardShareAccess> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    Optional<BoardShareAccess> findByBoardIdAndEmailIgnoreCase(Long boardId, String email);

    boolean existsByBoardIdAndEmailIgnoreCase(Long boardId, String email);

    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    void deleteByBoardIdAndEmailIgnoreCase(Long boardId, String email);
}
