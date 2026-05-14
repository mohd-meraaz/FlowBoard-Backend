package com.flowBoard.auth_service.service;

import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserService extends DefaultOAuth2UserService {

    private final UserRepository repository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String providerId = userRequest.getClientRegistration().getRegistrationId();

        // 1. Delegate attribute extraction to the Strategy Enum
        OAuth2ProviderStrategy strategy = OAuth2ProviderStrategy.fromId(providerId);
        String email = strategy.extractEmail(oAuth2User);
        String name = strategy.extractName(oAuth2User);
        String login = strategy.extractLogin(oAuth2User);

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 login from {} returned no email", providerId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info_response", "No Email returned from OAuth provider", null)
            );
        }

        Optional<User> existingUser = repository.findByEmail(email);

        User user = existingUser.orElseGet(() -> {
            // First-time OAuth2 login — create a new local user account
            String usernameCandidate = (login != null && !login.isBlank())
                    ? login.toLowerCase(Locale.ROOT)
                    : email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);

            String uniqueUsername = makeUniqueUsername(usernameCandidate);

            User newUser = User.builder()
                    .fullName(name != null ? name : login)
                    .email(email)
                    .username(uniqueUsername)
                    .password("") // No password for OAuth users — login is via token only
                    .role(ROLE.MEMBER)
                    .provider(providerId.toUpperCase())
                    .active(true)
                    .emailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(newUser);
            log.info("New user created via OAuth2 provider={} email={}", providerId, email);
            return newUser;
        });

        if (!user.isActive()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_inactive", "Account is deactivated", null)
            );
        }

        boolean changed = false;

        if (user.getProvider() == null || user.getProvider().isBlank()) {
            user.setProvider(providerId.toUpperCase());
            changed = true;
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            changed = true;
        }

        if (changed) {
            repository.save(user);
        }

        return oAuth2User;
    }

    private String makeUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (repository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    // --- STRATEGY PATTERN FOR OAUTH2 PROVIDERS ---

    private enum OAuth2ProviderStrategy {
        GITHUB("github") {
            @Override
            public String extractEmail(OAuth2User user) {
                return getAttribute(user, "email");
            }

            @Override
            public String extractName(OAuth2User user) {
                String name = getAttribute(user, "name");
                return name != null ? name : extractLogin(user);
            }

            @Override
            public String extractLogin(OAuth2User user) {
                return getAttribute(user, "login");
            }
        },
        // DEFAULT catches Google, Facebook, etc., using standard OpenID Connect attributes
        DEFAULT("default") {
            @Override
            public String extractEmail(OAuth2User user) {
                return getAttribute(user, "email");
            }

            @Override
            public String extractName(OAuth2User user) {
                return getAttribute(user, "name");
            }

            @Override
            public String extractLogin(OAuth2User user) {
                return null;
            }
        };

        private final String providerId;

        OAuth2ProviderStrategy(String providerId) {
            this.providerId = providerId;
        }

        public abstract String extractEmail(OAuth2User user);
        public abstract String extractName(OAuth2User user);
        public abstract String extractLogin(OAuth2User user);

        protected String getAttribute(OAuth2User user, String key) {
            Object value = user.getAttribute(key);
            return value != null ? value.toString() : null;
        }

        public static OAuth2ProviderStrategy fromId(String providerId) {
            for (OAuth2ProviderStrategy strategy : values()) {
                if (strategy.providerId.equalsIgnoreCase(providerId)) {
                    return strategy;
                }
            }
            return DEFAULT;
        }
    }
}