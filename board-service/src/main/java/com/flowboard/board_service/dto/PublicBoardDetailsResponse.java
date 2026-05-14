package com.flowboard.board_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PublicBoardDetailsResponse {
    private BoardResponse board;
    private List<PublicListDto> lists;
    private List<PublicCardDto> cards;

    @Data
    @Builder
    public static class PublicListDto {
        private Long id;
        private Long boardId;
        private String name;
        private Integer position;
        private String color;
        private boolean isArchived;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private int cardCount;
    }

    @Data
    @Builder
    public static class PublicCardDto {
        private Long id;
        private Long listId;
        private Long boardId;
        private String title;
        private String description;
        private Integer position;
        private String priority;
        private String status;
        private LocalDate dueDate;
        private LocalDate startDate;
        private Long assigneeId;
        private Long createdById;
        private boolean isArchived;
        private boolean isOverdue;
        private String coverColor;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
