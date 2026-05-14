package com.flowboard.board_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "board_share_access",
        uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "email"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardShareAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 255)
    private String email;

    private Long userId;

    @Column(nullable = false)
    private Long invitedById;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
