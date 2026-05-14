package com.flowBoard.list_service.service;

import com.flowBoard.list_service.dto.*;
import com.flowBoard.list_service.entity.TaskList;
import com.flowBoard.list_service.exception.CustomException;
import com.flowBoard.list_service.repository.ListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListServiceImpl - Unit Tests")
class ListServiceImplTest {

    @Mock
    private ListRepository listRepository;

    @InjectMocks
    private ListServiceImpl listService;

    private TaskList testList;
    private final Long listId = 100L;
    private final Long boardId = 10L;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        testList = TaskList.builder()
                .id(listId)
                .boardId(boardId)
                .name("To Do")
                .position(1)
                .color("#FFFFFF")
                .isArchived(false)
                .build();

        // Default mock behavior for finding the list
        lenient().when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        lenient().when(listRepository.save(any(TaskList.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── Create & Read ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Create & Read Operations")
    class CreateAndReadTests {

        @Test
        @DisplayName("Create list without explicit position appends to end")
        void createList_NoPosition_AppendsToEnd() {
            CreateListRequest req = new CreateListRequest();
            req.setBoardId(boardId);
            req.setName("In Progress");

            when(listRepository.findMaxPositionByBoardId(boardId)).thenReturn(Optional.of(5));

            ListResponse res = listService.createList(req, userId);

            assertThat(res.getName()).isEqualTo("In Progress");
            assertThat(res.getPosition()).isEqualTo(6); // 5 + 1
            verify(listRepository).save(any(TaskList.class));
            verify(listRepository, never()).shiftPositionsRight(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Create list with explicit position shifts existing lists right")
        void createList_WithPosition_ShiftsRight() {
            CreateListRequest req = new CreateListRequest();
            req.setBoardId(boardId);
            req.setName("Backlog");
            req.setPosition(0); // Insert at beginning

            ListResponse res = listService.createList(req, userId);

            assertThat(res.getPosition()).isZero();
            verify(listRepository).shiftPositionsRight(boardId, 0);
            verify(listRepository).save(any(TaskList.class));
        }

        @Test
        @DisplayName("Get list by ID returns successfully")
        void getListById_Success() {
            ListResponse res = listService.getListById(listId);
            assertThat(res.getId()).isEqualTo(listId);
        }

        @Test
        @DisplayName("Get list by ID throws exception when not found")
        void getListById_NotFound_ThrowsException() {
            when(listRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listService.getListById(999L))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("List not found");
        }

        @Test
        @DisplayName("Get lists by board retrieves active lists ordered by position")
        void getListsByBoard_Success() {
            when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(boardId))
                    .thenReturn(Collections.singletonList(testList));

            List<ListResponse> res = listService.getListsByBoard(boardId);

            assertThat(res).hasSize(1);
            assertThat(res.get(0).getId()).isEqualTo(listId);
        }
    }

    // ── Update, Delete & Reorder ─────────────────────────────────────────────

    @Nested
    @DisplayName("Update, Delete & Reorder Operations")
    class UpdateDeleteReorderTests {

        @Test
        @DisplayName("Update list changes name and color")
        void updateList_Success() {
            UpdateListRequest req = new UpdateListRequest();
            req.setName("Done");
            req.setColor("#00FF00");

            ListResponse res = listService.updateList(listId, req, userId);

            assertThat(res.getName()).isEqualTo("Done");
            assertThat(res.getColor()).isEqualTo("#00FF00");
            verify(listRepository).save(testList);
        }

        @Test
        @DisplayName("Update list throws exception if list is archived")
        void updateList_Archived_ThrowsException() {
            testList.setArchived(true);

            assertThatThrownBy(() -> listService.updateList(listId, new UpdateListRequest(), userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("unarchive if first");
        }

        @Test
        @DisplayName("Delete list shifts remaining lists left if not archived")
        void deleteList_NotArchived_ShiftsLeft() {
            listService.deleteList(listId, userId);

            verify(listRepository).shiftPositionsLeft(boardId, testList.getPosition());
            verify(listRepository).delete(testList);
        }

        @Test
        @DisplayName("Delete list does not shift if already archived")
        void deleteList_Archived_NoShift() {
            testList.setArchived(true);
            listService.deleteList(listId, userId);

            verify(listRepository, never()).shiftPositionsLeft(anyLong(), anyInt());
            verify(listRepository).delete(testList);
        }

        @Test
        @DisplayName("Reorder lists successfully updates positions sequentially")
        void reorderLists_Success() {
            TaskList list1 = TaskList.builder().id(101L).position(0).build();
            TaskList list2 = TaskList.builder().id(102L).position(1).build();

            ReorderListRequest req = new ReorderListRequest();
            req.setBoardId(boardId);
            req.setOrderedListIds(Arrays.asList(102L, 101L)); // Swapping order

            when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(boardId))
                    .thenReturn(Arrays.asList(list1, list2));

            listService.reorderLists(req, userId);

            assertThat(list2.getPosition()).isZero(); // 102 was moved to front
            assertThat(list1.getPosition()).isEqualTo(1);
            verify(listRepository, times(2)).save(any(TaskList.class));
        }

        @Test
        @DisplayName("Reorder lists throws exception if foreign list ID is provided")
        void reorderLists_ForeignId_ThrowsException() {
            TaskList list1 = TaskList.builder().id(101L).build();

            ReorderListRequest req = new ReorderListRequest();
            req.setBoardId(boardId);
            req.setOrderedListIds(Arrays.asList(101L, 999L)); // 999 does not belong to board

            when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(boardId))
                    .thenReturn(List.of(list1));

            assertThatThrownBy(() -> listService.reorderLists(req, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("does not belong to board id");
        }
    }

    // ── Archive, Unarchive & Move ─────────────────────────────────────────────

    @Nested
    @DisplayName("Archive & Move Operations")
    class ArchiveAndMoveTests {

        @Test
        @DisplayName("Archive list shifts positions left and sets archived flag")
        void archiveList_Success() {
            ListResponse res = listService.archiveList(listId, userId);

            assertThat(res.isArchived()).isTrue();
            verify(listRepository).shiftPositionsLeft(boardId, 1);
            verify(listRepository).save(testList);
        }

        @Test
        @DisplayName("Archive list throws exception if already archived")
        void archiveList_AlreadyArchived_ThrowsException() {
            testList.setArchived(true);

            assertThatThrownBy(() -> listService.archiveList(listId, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("already archived");
        }

        @Test
        @DisplayName("Unarchive list moves to the end of active lists")
        void unarchiveList_Success() {
            testList.setArchived(true);
            when(listRepository.findMaxPositionByBoardId(boardId)).thenReturn(Optional.of(8));

            ListResponse res = listService.unarchiveList(listId, userId);

            assertThat(res.isArchived()).isFalse();
            assertThat(res.getPosition()).isEqualTo(9); // Max + 1
            verify(listRepository).save(testList);
        }

        @Test
        @DisplayName("Unarchive list throws exception if not archived")
        void unarchiveList_NotArchived_ThrowsException() {
            assertThatThrownBy(() -> listService.unarchiveList(listId, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("not archived");
        }

        @Test
        @DisplayName("Get archived lists returns successfully")
        void getArchivedLists_Success() {
            when(listRepository.findByBoardIdAndIsArchivedTrue(boardId))
                    .thenReturn(Collections.singletonList(testList));

            List<ListResponse> res = listService.getArchivedLists(boardId);

            assertThat(res).hasSize(1);
            assertThat(res.get(0).getId()).isEqualTo(listId);
        }

        @Test
        @DisplayName("Move list across boards shifts correctly without explicit target position")
        void moveList_DifferentBoardNoTargetPosition_Success() {
            Long newBoardId = 20L;
            MoveListRequest req = new MoveListRequest();
            req.setTargetBoardId(newBoardId);

            when(listRepository.findMaxPositionByBoardId(newBoardId)).thenReturn(Optional.of(3));

            ListResponse res = listService.moveList(listId, req, userId);

            assertThat(res.getBoardId()).isEqualTo(newBoardId);
            assertThat(res.getPosition()).isEqualTo(4); // 3 + 1
            verify(listRepository).shiftPositionsLeft(boardId, 1); // Left on source
            verify(listRepository, never()).shiftPositionsRight(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Move list throws exception if target board is the same as source")
        void moveList_SameBoard_ThrowsException() {
            MoveListRequest req = new MoveListRequest();
            req.setTargetBoardId(boardId); // Same as current

            assertThatThrownBy(() -> listService.moveList(listId, req, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("already on the target board");
        }

        @Test
        @DisplayName("Move list with explicit target position shifts target board right")
        void moveList_WithTargetPosition_Success() {
            Long newBoardId = 20L;
            MoveListRequest req = new MoveListRequest();
            req.setTargetBoardId(newBoardId);
            req.setTargetPosition(0);

            ListResponse res = listService.moveList(listId, req, userId);

            assertThat(res.getBoardId()).isEqualTo(newBoardId);
            assertThat(res.getPosition()).isZero();
            verify(listRepository).shiftPositionsRight(newBoardId, 0); // Target board space created
        }
    }
}