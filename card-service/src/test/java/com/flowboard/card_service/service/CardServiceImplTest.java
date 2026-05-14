package com.flowboard.card_service.service;

import com.flowboard.card_service.config.RabbitMQConfig;
import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.entity.Card;
import com.flowboard.card_service.entity.CardActivity;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import com.flowboard.card_service.exception.CustomException;
import com.flowboard.card_service.repository.CardActivityRepository;
import com.flowboard.card_service.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardServiceImpl - Unit Tests")
class CardServiceImplTest {

    @Mock private CardRepository cardRepository;
    @Mock private CardActivityRepository activityRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card testCard;
    private final Long cardId = 100L;
    private final Long listId = 10L;
    private final Long boardId = 1L;
    private final Long userId = 99L;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .id(cardId)
                .listId(listId)
                .boardId(boardId)
                .title("Test Card")
                .position(1)
                .priority(Priority.MEDIUM)
                .status(CardStatus.TO_DO)
                .isArchived(false)
                .build();

        // Default mock behavior for finding the card
        lenient().when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        lenient().when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── Create & Update ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Create & Update Operations")
    class CreateUpdateTests {

        @Test
        @DisplayName("Create card without explicit position assigns max position + 1")
        void createCard_NoPosition_AppendsToEnd() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(listId);
            req.setBoardId(boardId);
            req.setTitle("New Card");

            when(cardRepository.findMaxPositionByListId(listId)).thenReturn(Optional.of(5));

            CardResponse res = cardService.createCard(req, userId);

            assertThat(res.getPosition()).isEqualTo(6); // 5 + 1
            verify(cardRepository).save(any(Card.class));
            verify(activityRepository).save(any(CardActivity.class)); // Verifies logActivity was called
        }

        @Test
        @DisplayName("Create card with explicit position shifts existing cards")
        void createCard_WithPosition_ShiftsRight() {
            CreateCardRequest req = new CreateCardRequest();
            req.setListId(listId);
            req.setPosition(2);
            req.setAssigneeId(88L); // Should trigger assignment activity log

            CardResponse res = cardService.createCard(req, userId);

            assertThat(res.getPosition()).isEqualTo(2);
            verify(cardRepository).shiftPositionsRight(listId, 2);
            verify(activityRepository, times(2)).save(any(CardActivity.class)); // 1 for create, 1 for assign
        }

        @Test
        @DisplayName("Update card changes values and logs activities")
        void updateCard_Success() {
            UpdateCardRequest req = new UpdateCardRequest();
            req.setTitle("Updated Title");
            req.setStatus(CardStatus.IN_PROGRESS);
            req.setPriority(Priority.HIGH);
            req.setDueDate(LocalDate.now().plusDays(5));

            CardResponse res = cardService.updateCard(cardId, req, userId);

            assertThat(res.getTitle()).isEqualTo("Updated Title");
            assertThat(res.getStatus()).isEqualTo(CardStatus.IN_PROGRESS);

            verify(cardRepository).save(testCard);
            // 3 activities logged: Status, Priority, DueDate
            verify(activityRepository, times(3)).save(any(CardActivity.class));
        }

        @Test
        @DisplayName("Update card throws exception if card is archived")
        void updateCard_Archived_ThrowsException() {
            testCard.setArchived(true);

            assertThatThrownBy(() -> cardService.updateCard(cardId, new UpdateCardRequest(), userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("unarchive it first");
        }
    }

    // ── Move, Reorder, Delete ────────────────────────────────────────────────

    @Nested
    @DisplayName("Move, Reorder & Delete Operations")
    class MoveReorderDeleteTests {

        @Test
        @DisplayName("Move card across lists shifts positions accurately")
        void moveCard_Success() {
            MoveCardRequest req = new MoveCardRequest();
            req.setTargetListId(20L);
            req.setTargetBoardId(boardId);
            req.setTargetPosition(0);

            CardResponse res = cardService.moveCard(cardId, req, userId);

            assertThat(res.getListId()).isEqualTo(20L);
            assertThat(res.getPosition()).isZero();

            // Verifies gap is closed in source
            verify(cardRepository).shiftPositionsLeft(listId, 1);
            // Verifies space is made in target
            verify(cardRepository).shiftPositionsRight(20L, 0);
        }

        @Test
        @DisplayName("Reorder cards successfully updates positions")
        void reorderCards_Success() {
            Card c1 = Card.builder().id(101L).position(0).build();
            Card c2 = Card.builder().id(102L).position(1).build();

            ReorderCardRequest req = new ReorderCardRequest();
            req.setListId(listId);
            req.setOrderedCardIds(Arrays.asList(102L, 101L)); // Swapping them

            when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(listId))
                    .thenReturn(Arrays.asList(c1, c2));

            cardService.reorderCards(req, userId);

            // c2 should now be position 0, c1 should be position 1
            assertThat(c2.getPosition()).isZero();
            assertThat(c1.getPosition()).isEqualTo(1);
            verify(cardRepository, times(2)).save(any(Card.class));
        }

        @Test
        @DisplayName("Reorder cards throws exception for foreign card ID")
        void reorderCards_ForeignId_ThrowsException() {
            Card c1 = Card.builder().id(101L).build();

            ReorderCardRequest req = new ReorderCardRequest();
            req.setListId(listId);
            req.setOrderedCardIds(Arrays.asList(101L, 999L)); // 999 is missing from the list

            when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(listId))
                    .thenReturn(List.of(c1));

            assertThatThrownBy(() -> cardService.reorderCards(req, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("does not belong to list");
        }

        @Test
        @DisplayName("Delete card shifts left if not archived")
        void deleteCard_NotArchived_ShiftsLeft() {
            cardService.deleteCard(cardId, userId);

            verify(cardRepository).shiftPositionsLeft(listId, testCard.getPosition());
            verify(cardRepository).delete(testCard);
        }
    }

    // ── Archive & Unarchive ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Archive Operations")
    class ArchiveTests {

        @Test
        @DisplayName("Archive card shifts remaining cards and sets flag")
        void archiveCard_Success() {
            CardResponse res = cardService.archiveCard(cardId, userId);

            assertThat(res.isArchived()).isTrue();
            verify(cardRepository).shiftPositionsLeft(listId, 1);
            verify(activityRepository).save(any(CardActivity.class));
        }

        @Test
        @DisplayName("Unarchive card appends to the end of the list")
        void unarchiveCard_Success() {
            testCard.setArchived(true);
            when(cardRepository.findMaxPositionByListId(listId)).thenReturn(Optional.of(10));

            CardResponse res = cardService.unarchiveCard(cardId, userId);

            assertThat(res.isArchived()).isFalse();
            assertThat(res.getPosition()).isEqualTo(11); // Max + 1
        }
    }

    // ── Assignment & RabbitMQ ────────────────────────────────────────────────

    @Nested
    @DisplayName("Assignment & Events")
    class AssignmentTests {

        @Test
        @DisplayName("Set assignee publishes event to RabbitMQ")
        void setAssignee_PublishesEvent() {
            AssignCardRequest req = new AssignCardRequest();
            req.setAssigneeId(500L);

            CardResponse res = cardService.setAssignee(cardId, req, userId);

            assertThat(res.getAssigneeId()).isEqualTo(500L);
            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConfig.FLOWBOARD_EXCHANGE),
                    eq(RabbitMQConfig.ASSIGNMENT_KEY),
                    any(Object.class)
            );
        }

        @Test
        @DisplayName("Unassigning a card does not publish an event")
        void setAssignee_Null_DoesNotPublish() {
            AssignCardRequest req = new AssignCardRequest();
            req.setAssigneeId(null);

            cardService.setAssignee(cardId, req, userId);

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    // ── Copy & Stats ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Copy & Analytics Operations")
    class CopyAndAnalyticsTests {

        @Test
        @DisplayName("Copy card duplicates essential fields to target list")
        void copyCard_Success() {
            Long targetListId = 99L;
            when(cardRepository.findMaxPositionByListId(targetListId)).thenReturn(Optional.empty());

            CardResponse res = cardService.copyCard(cardId, targetListId, userId);

            assertThat(res.getTitle()).startsWith("Copy of");
            assertThat(res.getListId()).isEqualTo(targetListId);
            assertThat(res.getPosition()).isZero();

            verify(cardRepository).save(any(Card.class));
            verify(activityRepository).save(any(CardActivity.class));
        }

        @Test
        @DisplayName("Get board stats calculates totals and percentages accurately")
        void getBoardStats_Success() {
            Card c1 = Card.builder().status(CardStatus.DONE).priority(Priority.HIGH).assigneeId(1L).build();
            Card c2 = Card.builder().status(CardStatus.TO_DO).priority(Priority.LOW).assigneeId(1L).build();

            when(cardRepository.findByBoardIdAndIsArchivedFalse(boardId)).thenReturn(Arrays.asList(c1, c2));
            when(cardRepository.findByBoardIdAndIsArchivedTrue(boardId)).thenReturn(List.of(new Card()));
            when(cardRepository.findOverdueByBoardId(eq(boardId), any(LocalDate.class))).thenReturn(List.of(c2));

            BoardStatsResponse stats = cardService.getBoardStats(boardId);

            assertThat(stats.getTotalCards()).isEqualTo(2);
            assertThat(stats.getCompletedCards()).isEqualTo(1);
            assertThat(stats.getCompletionRate()).isEqualTo(50.0); // 1/2 * 100
            assertThat(stats.getOverdueRate()).isEqualTo(50.0);
            assertThat(stats.getArchivedCards()).isEqualTo(1);
            assertThat(stats.getCardsByAssignee().get(1L)).isEqualTo(2);
            assertThat(stats.getCardsByStatus().get("DONE")).isEqualTo(1);
        }

        @Test
        @DisplayName("Get card activity paged")
        void getCardActivityPaged_Success() {
            Page<CardActivity> page = new PageImpl<>(List.of(new CardActivity()));
            when(activityRepository.findByCardIdOrderByCreatedAtDesc(eq(cardId), any(PageRequest.class)))
                    .thenReturn(page);

            PagedResponse<CardActivityResponse> res = cardService.getCardActivityPaged(cardId, 0, 10);

            assertThat(res.getContent()).hasSize(1);
            assertThat(res.getTotalElements()).isEqualTo(1);
        }
    }
}