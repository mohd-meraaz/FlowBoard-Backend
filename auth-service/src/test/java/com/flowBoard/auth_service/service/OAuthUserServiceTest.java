package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthUserService – Full Coverage")
class OAuthUserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private RestOperations restOperations;

    private OAuthUserService oauthUserService;

    @BeforeEach
    void setUp() {
        oauthUserService = new OAuthUserService(repository);
        // By injecting a mocked RestOperations, we bypass actual HTTP calls to GitHub/Google
        oauthUserService.setRestOperations(restOperations);
    }

    private OAuth2UserRequest createOAuth2UserRequest(String provider) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(provider)
                .clientId("dummy-client")
                .clientSecret("dummy-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/" + provider)
                .authorizationUri("http://localhost/oauth/authorize")
                .tokenUri("http://localhost/oauth/token")
                .userInfoUri("http://localhost/userinfo")
                .userNameAttributeName("id")
                .clientName(provider.toUpperCase())
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "mock-token", null, null);

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private void mockUserInfoResponse(Map<String, Object> attributes) {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(attributes);
        when(restOperations.exchange(any(RequestEntity.class), eq(new ParameterizedTypeReference<Map<String, Object>>() {})))
                .thenReturn(response);
    }

    @Test
    @DisplayName("loadUser - first time GitHub login creates new user")
    void loadUser_NewUserCreated_Github() {
        OAuth2UserRequest request = createOAuth2UserRequest("github");
        Map<String, Object> attributes = Map.of(
                "id", "12345",
                "email", "githubuser@example.com",
                "login", "githubUserLogin"
        );
        mockUserInfoResponse(attributes);

        when(repository.findByEmail("githubuser@example.com")).thenReturn(Optional.empty());
        when(repository.existsByUsername(anyString())).thenReturn(false);

        OAuth2User oAuth2User = oauthUserService.loadUser(request);

        assertThat(oAuth2User).isNotNull();
        verify(repository).save(argThat(user ->
                user.getEmail().equals("githubuser@example.com") &&
                        user.getProvider().equals("GITHUB") &&
                        user.getProfileUsername().equals("githubuserlogin")
        ));
    }

    @Test
    @DisplayName("loadUser - existing user without provider gets updated")
    void loadUser_ExistingUser_UpdatesProvider() {
        OAuth2UserRequest request = createOAuth2UserRequest("google");
        Map<String, Object> attributes = Map.of(
                "id", "67890",
                "email", "existing@example.com",
                "name", "Existing User"
        );
        mockUserInfoResponse(attributes);

        User existingUser = User.builder()
                .email("existing@example.com")
                .active(true)
                .emailVerified(false) // Simulating an unverified manual signup
                .provider(null)       // No provider set yet
                .build();

        when(repository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        OAuth2User oAuth2User = oauthUserService.loadUser(request);

        assertThat(oAuth2User).isNotNull();
        assertThat(existingUser.isEmailVerified()).isTrue();
        assertThat(existingUser.getProvider()).isEqualTo("GOOGLE");
        verify(repository).save(existingUser);
    }

    @Test
    @DisplayName("loadUser - throws exception if account deactivated")
    void loadUser_DeactivatedAccount_ThrowsException() {
        OAuth2UserRequest request = createOAuth2UserRequest("google");
        Map<String, Object> attributes = Map.of("id", "111", "email", "banned@example.com");
        mockUserInfoResponse(attributes);

        User existingUser = User.builder().email("banned@example.com").active(false).build();
        when(repository.findByEmail("banned@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> oauthUserService.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    @DisplayName("loadUser - throws exception if provider returns no email")
    void loadUser_NoEmail_ThrowsException() {
        OAuth2UserRequest request = createOAuth2UserRequest("github");
        Map<String, Object> attributes = Map.of("id", "999"); // Deliberately omitting email
        mockUserInfoResponse(attributes);

        assertThatThrownBy(() -> oauthUserService.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("No Email returned");
    }
}