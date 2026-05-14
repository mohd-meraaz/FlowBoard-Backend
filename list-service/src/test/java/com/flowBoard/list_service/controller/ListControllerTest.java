package com.flowBoard.list_service.controller;

import com.flowBoard.list_service.dto.*;
import com.flowBoard.list_service.exception.CustomException;
import com.flowBoard.list_service.service.ListService;
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
@DisplayName("ListController - Unit Tests")
class ListControllerTest {

    @Mock
    private ListService listService;

    @InjectMocks
    private ListController listController;

    private final Long userId = 1L;
    private final Long listId = 100L;
    private final Long boardId = 10L;

    @Test
    @DisplayName("resolveUserId throws exception when X-User-Id header is missing")
    void resolveUserId_MissingHeader_ThrowsException() {
        CreateListRequest request = mock(CreateListRequest.class);

        assertThatThrownBy(() -> listController.create(request, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("X-User-Id header is required");
    }

    @Test
    @DisplayName("create - Returns 201 Created")
    void create_Success() {
        CreateListRequest request = mock(CreateListRequest.class);
        ListResponse response = mock(ListResponse.class);
        when(listService.createList(request, userId)).thenReturn(response);

        ResponseEntity<ListResponse> result = listController.create(request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getById - Returns 200 OK")
    void getById_Success() {
        ListResponse response = mock(ListResponse.class);
        when(listService.getListById(listId)).thenReturn(response);

        ResponseEntity<ListResponse> result = listController.getById(listId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getByBoard - Returns 200 OK")
    void getByBoard_Success() {
        List<ListResponse> responses = Collections.singletonList(mock(ListResponse.class));
        when(listService.getListsByBoard(boardId)).thenReturn(responses);

        ResponseEntity<List<ListResponse>> result = listController.getByBoard(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("update - Returns 200 OK")
    void update_Success() {
        UpdateListRequest request = mock(UpdateListRequest.class);
        ListResponse response = mock(ListResponse.class);
        when(listService.updateList(listId, request, userId)).thenReturn(response);

        ResponseEntity<ListResponse> result = listController.update(listId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("delete - Returns 200 OK")
    void delete_Success() {
        doNothing().when(listService).deleteList(listId, userId);

        ResponseEntity<String> result = listController.delete(listId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("List deleted successfully");
        verify(listService).deleteList(listId, userId);
    }

    @Test
    @DisplayName("reorder - Returns 200 OK")
    void reorder_Success() {
        ReorderListRequest request = mock(ReorderListRequest.class);
        List<ListResponse> responses = Collections.singletonList(mock(ListResponse.class));
        when(listService.reorderLists(request, userId)).thenReturn(responses);

        ResponseEntity<List<ListResponse>> result = listController.reorder(request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("archive - Returns 200 OK")
    void archive_Success() {
        ListResponse response = mock(ListResponse.class);
        when(listService.archiveList(listId, userId)).thenReturn(response);

        ResponseEntity<ListResponse> result = listController.archive(listId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("unarchive - Returns 200 OK")
    void unarchive_Success() {
        ListResponse response = mock(ListResponse.class);
        when(listService.unarchiveList(listId, userId)).thenReturn(response);

        ResponseEntity<ListResponse> result = listController.unarchive(listId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    @DisplayName("getArchived - Returns 200 OK")
    void getArchived_Success() {
        List<ListResponse> responses = Collections.singletonList(mock(ListResponse.class));
        when(listService.getArchivedLists(boardId)).thenReturn(responses);

        ResponseEntity<List<ListResponse>> result = listController.getArchived(boardId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
    }

    @Test
    @DisplayName("move - Returns 200 OK")
    void move_Success() {
        MoveListRequest request = mock(MoveListRequest.class);
        ListResponse response = mock(ListResponse.class);
        when(listService.moveList(listId, request, userId)).thenReturn(response);

        ResponseEntity<ListResponse> result = listController.move(listId, request, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }
}