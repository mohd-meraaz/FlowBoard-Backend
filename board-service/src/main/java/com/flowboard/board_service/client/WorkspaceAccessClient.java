package com.flowboard.board_service.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
@Slf4j
public class WorkspaceAccessClient {

    private final RestTemplate restTemplate;
    private final String workspaceBaseUrl;

    public WorkspaceAccessClient(
            RestTemplate restTemplate,
            @Value("${services.workspace.base-url:http://localhost:8082/api/v1/workspaces}") String workspaceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.workspaceBaseUrl = workspaceBaseUrl;
    }

    public boolean isWorkspaceMember(Long workspaceId, Long userId, String authorizationHeader) {
        if (workspaceId == null || userId == null || authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(workspaceBaseUrl)
                    .pathSegment(String.valueOf(workspaceId), "members")
                    .build(true)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, uri);

            ResponseEntity<WorkspaceMemberDto[]> response = restTemplate.exchange(request, WorkspaceMemberDto[].class);
            WorkspaceMemberDto[] members = response.getBody();

            if (members == null) {
                return false;
            }

            for (WorkspaceMemberDto member : members) {
                if (member != null && userId.equals(member.getUserId())) {
                    return true;
                }
            }
        } catch (Exception ex) {
            log.warn("Workspace membership lookup failed for workspaceId={} userId={}: {}",
                    workspaceId, userId, ex.getMessage());
        }

        return false;
    }

    @Data
    public static class WorkspaceMemberDto {
        private Long userId;
    }
}
