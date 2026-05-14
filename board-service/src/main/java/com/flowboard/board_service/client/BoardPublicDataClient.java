package com.flowboard.board_service.client;

import com.flowboard.board_service.dto.PublicBoardDetailsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class BoardPublicDataClient {

    private final RestTemplate restTemplate;
    private final String listBaseUrl;
    private final String cardBaseUrl;

    public BoardPublicDataClient(
            RestTemplate restTemplate,
            @Value("${services.list.base-url:http://localhost:8084/api/v1/lists}") String listBaseUrl,
            @Value("${services.card.base-url:http://localhost:8085/api/v1/cards}") String cardBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.listBaseUrl = listBaseUrl;
        this.cardBaseUrl = cardBaseUrl;
    }

    public List<PublicBoardDetailsResponse.PublicListDto> getListsByBoard(Long boardId) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(listBaseUrl)
                    .pathSegment("board", String.valueOf(boardId))
                    .build(true)
                    .toUri();

            RequestEntity<Void> request = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseEntity<PublicBoardDetailsResponse.PublicListDto[]> response =
                    restTemplate.exchange(request, PublicBoardDetailsResponse.PublicListDto[].class);

            PublicBoardDetailsResponse.PublicListDto[] body = response.getBody();
            return body == null ? List.of() : Arrays.asList(body);
        } catch (Exception ex) {
            log.warn("Failed to fetch public lists for boardId={}: {}", boardId, ex.getMessage());
            throw new IllegalStateException("Failed to fetch board lists", ex);
        }
    }

    public List<PublicBoardDetailsResponse.PublicCardDto> getCardsByBoard(Long boardId) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(cardBaseUrl)
                    .pathSegment("board", String.valueOf(boardId))
                    .build(true)
                    .toUri();

            RequestEntity<Void> request = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseEntity<PublicBoardDetailsResponse.PublicCardDto[]> response =
                    restTemplate.exchange(request, PublicBoardDetailsResponse.PublicCardDto[].class);

            PublicBoardDetailsResponse.PublicCardDto[] body = response.getBody();
            return body == null ? List.of() : Arrays.asList(body);
        } catch (Exception ex) {
            log.warn("Failed to fetch public cards for boardId={}: {}", boardId, ex.getMessage());
            throw new IllegalStateException("Failed to fetch board cards", ex);
        }
    }
}
