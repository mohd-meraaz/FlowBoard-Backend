package com.flowBoard.auth_service.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void blacklist_and_check() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        TokenBlacklistService svc = new TokenBlacklistService(redis);
        svc.blacklist("tok", 60);
        verify(ops).set(eq("blacklist:tok"), eq("revoked"), eq(60L), eq(TimeUnit.SECONDS));

        when(redis.hasKey("blacklist:tok")).thenReturn(true);
        assertThat(svc.isBlacklisted("tok")).isTrue();

        when(redis.hasKey("blacklist:tok")).thenReturn(false);
        assertThat(svc.isBlacklisted("tok")).isFalse();
    }
}

