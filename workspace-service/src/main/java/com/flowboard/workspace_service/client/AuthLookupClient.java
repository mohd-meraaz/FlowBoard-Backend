package com.flowboard.workspace_service.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class AuthLookupClient {

    private final RestTemplate restTemplate;
    private final String authBaseUrl;

    public AuthLookupClient(
            RestTemplate restTemplate,
            @Value("${services.auth.base-url:http://localhost:8080/api/v1/auth}") String authBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.authBaseUrl = authBaseUrl;
    }

    public Optional<Long> findUserIdByEmail(String email, String authorizationHeader) {
        if (email == null || email.isBlank() || authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }

        try {
            URI byEmailUri = UriComponentsBuilder.fromHttpUrl(authBaseUrl)
                    .path("/by-email")
                    .queryParam("email", email)
                    .build(true)
                    .toUri();

            HttpHeaders byEmailHeaders = new HttpHeaders();
            byEmailHeaders.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            RequestEntity<Void> byEmailRequest = new RequestEntity<>(byEmailHeaders, HttpMethod.GET, byEmailUri);

            ResponseEntity<AuthUserDto> byEmailResponse = restTemplate.exchange(
                    byEmailRequest, AuthUserDto.class
            );
            AuthUserDto direct = byEmailResponse.getBody();
            if (direct != null && direct.getId() != null && direct.getId() > 0) {
                return Optional.of(direct.getId());
            }
        } catch (Exception ex) {
            log.warn("Direct auth lookup failed for email {}: {}", email, ex.getMessage());
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(authBaseUrl)
                    .path("/search")
                    .queryParam("key", email)
                    .build(true)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);

            RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, uri);
            ResponseEntity<List<AuthUserDto>> response = restTemplate.exchange(
                    request, new ParameterizedTypeReference<>() {}
            );

            List<AuthUserDto> users = response.getBody();
            if (users == null) return Optional.empty();

            return users.stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                    .map(AuthUserDto::getId)
                    .filter(id -> id != null && id > 0)
                    .findFirst();
        } catch (Exception ex) {
            log.warn("Fallback auth lookup failed for email {}: {}", email, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<AuthUserDto> findUserById(Long id, String authorizationHeader) {
        if (id == null || id <= 0 || authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }

        try {
            URI byIdUri = UriComponentsBuilder.fromHttpUrl(authBaseUrl)
                    .path("/by-id")
                    .queryParam("id", id)
                    .build(true)
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, byIdUri);

            ResponseEntity<AuthUserDto> response = restTemplate.exchange(request, AuthUserDto.class);
            AuthUserDto user = response.getBody();

            if (user != null && user.getId() != null && user.getId() > 0) {
                return Optional.of(user);
            }
        } catch (Exception ex) {
            log.warn("Auth lookup by id failed for user {}: {}", id, ex.getMessage());
        }

        return Optional.empty();
    }

    @Data
    public static class AuthUserDto {
        private Long id;
        private String fullName;
        private String username;
        private String email;
        private String avatarUrl;
    }
}
