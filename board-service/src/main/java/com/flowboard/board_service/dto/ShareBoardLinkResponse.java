package com.flowboard.board_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShareBoardLinkResponse {
    private String shareToken;
    private String path;
}
