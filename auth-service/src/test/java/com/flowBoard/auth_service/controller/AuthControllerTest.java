package com.flowBoard.auth_service.controller;

import com.flowBoard.auth_service.dto.*;
import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import com.flowBoard.auth_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setRole(ROLE.MEMBER); // Assuming ROLE.USER exists
        mockUser.setActive(true);
        mockUser.setEmailVerified(true);
        mockUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void register_ShouldReturnOk() {
        RegisterRequest request = new RegisterRequest();
        AuthResponse expectedResponse = new AuthResponse("User registered successfully", "mockToken123");
        when(authService.register(any(RegisterRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService, times(1)).register(request);
    }

    @Test
    void login_ShouldReturnOk() {
        LoginRequest request = new LoginRequest();
        AuthResponse expectedResponse = new AuthResponse("Login successful", "mockJwtToken123");
        when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService, times(1)).login(request);
    }

    @Test
    void logout_ShouldExtractTokenAndReturnOk() {
        String authHeader = "Bearer mockJwtToken123";

        ResponseEntity<String> response = authController.logout(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully", response.getBody());
        verify(authService, times(1)).logout("mockJwtToken123");
    }

    @Test
    void getProfile_ShouldReturnUserProfileDto() {
        when(authService.getUserById(mockUser.getId())).thenReturn(mockUser);

        ResponseEntity<UserProfileDto> response = authController.getProfile(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockUser.getEmail(), response.getBody().getEmail());
        verify(authService, times(1)).getUserById(mockUser.getId());
    }

    @Test
    void updateProfile_ShouldReturnOk() {
        UpdateProfileRequest request = new UpdateProfileRequest();

        ResponseEntity<String> response = authController.updateProfile(mockUser, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Profile updated successfully", response.getBody());
        verify(authService, times(1)).updateProfile(mockUser.getId(), request);
    }

    @Test
    void changePassword_ShouldReturnOk() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        ResponseEntity<String> response = authController.changePassword(mockUser, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", response.getBody());
        verify(authService, times(1)).changePassword(mockUser.getId(), request);
    }

    @Test
    void getUserByEmail_ShouldReturnUserProfileDto() {
        String email = "john@example.com";
        when(authService.getUserByEmail(email)).thenReturn(mockUser);

        ResponseEntity<UserProfileDto> response = authController.getUserByEmail(email);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService, times(1)).getUserByEmail(email);
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        List<User> users = Collections.singletonList(mockUser);
        when(authService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<User>> response = authController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
        verify(authService, times(1)).getAllUsers();
    }

    @Test
    void updateUserRole_ShouldReturnOk() {
        Long userId = 1L;
        // Adjust based on the actual enum values inside your ROLE class
        Map<String, String> payload = Map.of("role", "PLATFORM_ADMIN");

        ResponseEntity<String> response = authController.updateUserRole(userId, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService, times(1)).updateUserRole(userId, ROLE.valueOf("PLATFORM_ADMIN"));
    }

    @Test
    void suspendUser_ShouldReturnOk() {
        Long userId = 1L;

        ResponseEntity<String> response = authController.suspendUser(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User suspended", response.getBody());
        verify(authService, times(1)).suspendUser(userId);
    }

    @Test
    void deleteUser_ShouldReturnOk() {
        Long userId = 1L;

        ResponseEntity<String> response = authController.deleteUser(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User permanently deleted", response.getBody());
        verify(authService, times(1)).deleteUser(userId);
    }

    @Test
    void verifyEmail_ShouldReturnOk() {
        VerifyOtpRequest request = mock(VerifyOtpRequest.class);
        when(request.getEmail()).thenReturn("john@example.com");
        when(request.getOtp()).thenReturn("123456");

        ResponseEntity<String> response = authController.verifyEmail(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Email verified successfully", response.getBody());
        verify(authService, times(1)).verifyEmail("john@example.com", "123456");
    }

    @Test
    void resetPassword_ShouldReturnOk() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        ResponseEntity<String> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset successfully", response.getBody());
        verify(authService, times(1)).resetPassword(request);
    }

    @Test
    void reactivateAccount_ShouldReturnOk() {
        String email = "john@example.com";
        String otp = "123456";

        ResponseEntity<String> response = authController.reactivateAccount(email, otp);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Account successfully reactivated. You can now log in.", response.getBody());
        verify(authService, times(1)).reactivateWithOtp(email, otp);
    }
}