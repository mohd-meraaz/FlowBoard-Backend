package com.flowboard.board_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ShareBoardByEmailRequest {

    @NotEmpty(message = "At least one email is required")
    private List<@Email(message = "Each email must be valid") String> emails;
}
