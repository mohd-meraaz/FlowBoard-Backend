package com.flowboard.board_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BoardShareResponse {

    private int invitedCount;
    private int skippedCount;
    private List<SharedEmailDto> shared;

    @Data
    @Builder
    public static class SharedEmailDto {
        private String email;
        private Long userId;
        private LocalDateTime createdAt;
    }
}
