package com.flowboard.card_service.controller;

import com.flowboard.card_service.dto.*;
import com.flowboard.card_service.enums.CardStatus;
import com.flowboard.card_service.enums.Priority;
import com.flowboard.card_service.exception.CustomException;
import com.flowboard.card_service.service.CardService;
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
@DisplayName("CardController - Unit Tests")
class CardControllerTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardController cardController;

    private final Long userId = 1L;
    private final Long cardId = 100L;
    private final Long listId = 50L;
    private final Long boardId = 10L;

    @Test
    @DisplayName("resolveUserId throws exception when header is missing")
    void resolveUserId_MissingHeader_ThrowsException() {
        CreateCardRequest request = mock(CreateCardRequest.class);

        assertThatThrownBy(() -> cardController.create(request, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    @DisplayName("create - Returns 201 Created")
    void create_Success() {
        CreateCardRequest request = mock(CreateCardRequest.class);
        CardResponse response = mock(CardResponse.class);
        when(cardService.createCard(request, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.create(request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getById - Returns 200 OK")
    void getById_Success() {
        CardResponse response = mock(CardResponse.class);
        when(cardService.getCardById(cardId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.getById(cardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getByList - Returns 200 OK")
    void getByList_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getCardByList(listId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getByList(listId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByBoard - Returns 200 OK")
    void getByBoard_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getCardByBoard(boardId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getByBoard(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByAssignee - Returns 200 OK")
    void getByAssignee_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getCardByAssignee(userId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getByAssignee(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("update - Returns 200 OK")
    void update_Success() {
        UpdateCardRequest request = mock(UpdateCardRequest.class);
        CardResponse response = mock(CardResponse.class);
        when(cardService.updateCard(cardId, request, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.update(cardId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("delete - Returns 200 OK")
    void delete_Success() {
        doNothing().when(cardService).deleteCard(cardId, userId);

        ResponseEntity<String> result = cardController.delete(cardId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("Card deleted successfully");
        verify(cardService).deleteCard(cardId, userId);
    }

    @Test
    @DisplayName("move - Returns 200 OK")
    void move_Success() {
        MoveCardRequest request = mock(MoveCardRequest.class);
        CardResponse response = mock(CardResponse.class);
        when(cardService.moveCard(cardId, request, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.move(cardId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("reorder - Returns 200 OK")
    void reorder_Success() {
        ReorderCardRequest request = mock(ReorderCardRequest.class);
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.reorderCards(request, userId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.reorder(request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("archive - Returns 200 OK")
    void archive_Success() {
        CardResponse response = mock(CardResponse.class);
        when(cardService.archiveCard(cardId, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.archive(cardId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("unarchive - Returns 200 OK")
    void unarchive_Success() {
        CardResponse response = mock(CardResponse.class);
        when(cardService.unarchiveCard(cardId, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.unarchive(cardId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getArchivedByBoard - Returns 200 OK")
    void getArchivedByBoard_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getArchivedCardsByBoard(boardId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getArchivedByBoard(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getArchivedByList - Returns 200 OK")
    void getArchivedByList_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getArchivedCardsByList(listId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getArchivedByList(listId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("setAssignment - Returns 200 OK")
    void setAssignment_Success() {
        AssignCardRequest request = mock(AssignCardRequest.class);
        CardResponse response = mock(CardResponse.class);
        when(cardService.setAssignee(cardId, request, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.setAssignment(cardId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("setPriority - Returns 200 OK")
    void setPriority_Success() {
        SetPriorityRequest request = mock(SetPriorityRequest.class);
        CardResponse response = mock(CardResponse.class);
        when(cardService.setPriority(cardId, request, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.setPriority(cardId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("setStatus - Returns 200 OK")
    void setStatus_Success() {
        SetStatusRequest request = mock(SetStatusRequest.class);
        CardResponse response = mock(CardResponse.class);
        when(cardService.setStatus(cardId, request, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.setStatus(cardId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getByStatus - Returns 200 OK")
    void getByStatus_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getCardsByStatus(boardId, CardStatus.IN_PROGRESS)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getByStatus(boardId, CardStatus.IN_PROGRESS);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getByPriority - Returns 200 OK")
    void getByPriority_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getCardsByPriority(boardId, Priority.HIGH)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getByPriority(boardId, Priority.HIGH);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getOverdueByBoard - Returns 200 OK")
    void getOverdueByBoard_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getOverdueCardsByBoard(boardId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getOverdueByBoard(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getAllOverdue - Returns 200 OK")
    void getAllOverdue_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.getAllOverdueCards()).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.getAllOverdue();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("search - Returns 200 OK")
    void search_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.searchCards(boardId, "bug")).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.search(boardId, "bug");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("searchGlobal - Returns 200 OK")
    void searchGlobal_Success() {
        List<CardResponse> responses = Collections.singletonList(mock(CardResponse.class));
        when(cardService.searchByTitleOrAssignee("feature", userId)).thenReturn(responses);

        ResponseEntity<List<CardResponse>> result = cardController.searchGlobal("feature", userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getActivity - Returns 200 OK")
    void getActivity_Success() {
        List<CardActivityResponse> responses = Collections.singletonList(mock(CardActivityResponse.class));
        when(cardService.getCardActivity(cardId)).thenReturn(responses);

        ResponseEntity<List<CardActivityResponse>> result = cardController.getActivity(cardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("getActivityPaged - Returns 200 OK")
    void getActivityPaged_Success() {
        @SuppressWarnings("unchecked")
        PagedResponse<CardActivityResponse> response = mock(PagedResponse.class);
        when(cardService.getCardActivityPaged(cardId, 0, 20)).thenReturn(response);

        ResponseEntity<PagedResponse<CardActivityResponse>> result = cardController.getActivityPaged(cardId, 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getBoardStats - Returns 200 OK")
    void getBoardStats_Success() {
        BoardStatsResponse response = mock(BoardStatsResponse.class);
        when(cardService.getBoardStats(boardId)).thenReturn(response);

        ResponseEntity<BoardStatsResponse> result = cardController.getBoardStats(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("copyCard - Uses provided targetListId")
    void copyCard_WithTargetListId_Success() {
        CardResponse response = mock(CardResponse.class);
        CardResponse originalCard = mock(CardResponse.class); // Even though we don't strictly need it when targetListId is present
        when(cardService.getCardById(cardId)).thenReturn(originalCard);
        when(cardService.copyCard(cardId, 99L, userId)).thenReturn(response);

        ResponseEntity<CardResponse> result = cardController.copyCard(cardId, 99L, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("copyCard - Falls back to original listId when targetListId is null")
    void copyCard_NullTargetListId_UsesOriginalListId() {
        CardResponse originalCard = mock(CardResponse.class);
        when(originalCard.getListId()).thenReturn(listId);
        when(cardService.getCardById(cardId)).thenReturn(originalCard);

        CardResponse copiedResponse = mock(CardResponse.class);
        when(cardService.copyCard(cardId, listId, userId)).thenReturn(copiedResponse);

        ResponseEntity<CardResponse> result = cardController.copyCard(cardId, null, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(copiedResponse);
    }
}