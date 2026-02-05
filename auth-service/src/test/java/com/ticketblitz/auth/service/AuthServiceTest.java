package com.ticketblitz.auth.service;

import com.ticketblitz.auth.client.UserClient;
import com.ticketblitz.auth.dto.*;
import com.ticketblitz.auth.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserDto mockUserDto;
    private UserRegistrationRequest mockRegistrationRequest;
    private AuthRequest mockAuthRequest;
    private Request mockFeignRequest;

    @BeforeEach
    void setUp() {
        // Mock Feign Request for exception creation
        mockFeignRequest = Request.create(
                Request.HttpMethod.GET,
                "http://user-service/users",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );

        // Create reusable mock objects
        mockUserDto = new UserDto();
        mockUserDto.setId(1L);
        mockUserDto.setUsername("testuser");
        mockUserDto.setPassword("encodedHash");
        mockUserDto.setRole(Role.USER);
        mockUserDto.setEmail("test@example.com");
        mockUserDto.setActive(true);

        mockRegistrationRequest = new UserRegistrationRequest();
        mockRegistrationRequest.setUsername("newuser");
        mockRegistrationRequest.setEmail("new@example.com");
        mockRegistrationRequest.setPassword("StrongP@ss1");

        mockAuthRequest = new AuthRequest();
        mockAuthRequest.setUsername("testuser");
        mockAuthRequest.setPassword("StrongP@ss1");
    }

    // ========================================================================
    // REGISTER METHOD TESTS
    // ========================================================================

    @Test
    @DisplayName("Register - Success: Should delegate to registerRemote and return UserDto")
    void register_Success() {
        UserDto responseDto = new UserDto();
        responseDto.setUsername("newuser");
        responseDto.setId(2L);

        when(userClient.registerUser(mockRegistrationRequest)).thenReturn(responseDto);

        UserDto result = authService.register(mockRegistrationRequest);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getId()).isEqualTo(2L);
        verify(userClient, times(1)).registerUser(mockRegistrationRequest);
    }

    @Test
    @DisplayName("Register - Fail: Should propagate RuntimeException when FeignException occurs")
    void register_Fail_UsernameTaken() {
        String errorMessage = "Username already taken!";
        FeignException badRequest = new FeignException.BadRequest(
                errorMessage,
                mockFeignRequest,
                errorMessage.getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap()
        );

        when(userClient.registerUser(mockRegistrationRequest)).thenThrow(badRequest);

        assertThatThrownBy(() -> authService.register(mockRegistrationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already taken!");

        verify(userClient, times(1)).registerUser(mockRegistrationRequest);
    }

    // ========================================================================
    // LOGIN METHOD TESTS
    // ========================================================================

    @Test
    @DisplayName("Login - Success with USER role: Should return token when credentials are valid")
    void login_Success_UserRole() {
        when(userClient.getUserByUsernameInternal("testuser")).thenReturn(mockUserDto);
        when(passwordEncoder.matches("StrongP@ss1", "encodedHash")).thenReturn(true);
        when(jwtService.generateToken("testuser", Role.USER)).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(mockAuthRequest);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(userClient, times(1)).getUserByUsernameInternal("testuser");
        verify(passwordEncoder, times(1)).matches("StrongP@ss1", "encodedHash");
        verify(jwtService, times(1)).generateToken("testuser", Role.USER);
    }

    @Test
    @DisplayName("Login - Success with ADMIN role: Should generate token with ADMIN role")
    void login_Success_AdminRole() {
        mockUserDto.setRole(Role.ADMIN);

        when(userClient.getUserByUsernameInternal("testuser")).thenReturn(mockUserDto);
        when(passwordEncoder.matches("StrongP@ss1", "encodedHash")).thenReturn(true);
        when(jwtService.generateToken("testuser", Role.ADMIN)).thenReturn("admin-jwt-token");

        AuthResponse response = authService.login(mockAuthRequest);

        assertThat(response.getToken()).isEqualTo("admin-jwt-token");
        verify(jwtService, times(1)).generateToken("testuser", Role.ADMIN);
    }

    @Test
    @DisplayName("Login - Success with ORGANIZER role: Should generate token with ORGANIZER role")
    void login_Success_OrganizerRole() {
        mockUserDto.setRole(Role.ORGANIZER);

        when(userClient.getUserByUsernameInternal("testuser")).thenReturn(mockUserDto);
        when(passwordEncoder.matches("StrongP@ss1", "encodedHash")).thenReturn(true);
        when(jwtService.generateToken("testuser", Role.ORGANIZER)).thenReturn("organizer-jwt-token");

        AuthResponse response = authService.login(mockAuthRequest);

        assertThat(response.getToken()).isEqualTo("organizer-jwt-token");
        verify(jwtService, times(1)).generateToken("testuser", Role.ORGANIZER);
    }

    @Test
    @DisplayName("Login - Fail: Should throw RuntimeException when password does not match")
    void login_WrongPassword() {
        when(userClient.getUserByUsernameInternal("testuser")).thenReturn(mockUserDto);
        when(passwordEncoder.matches("StrongP@ss1", "encodedHash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(mockAuthRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");

        verify(userClient, times(1)).getUserByUsernameInternal("testuser");
        verify(passwordEncoder, times(1)).matches("StrongP@ss1", "encodedHash");
        verify(jwtService, never()).generateToken(anyString(), any(Role.class));
    }

    @Test
    @DisplayName("Login - Fail: Should throw RuntimeException when user is not found (404)")
    void login_UserNotFound() {
        FeignException.NotFound notFoundException = new FeignException.NotFound(
                "User not found",
                mockFeignRequest,
                null,
                null
        );

        when(userClient.getUserByUsernameInternal("unknown")).thenThrow(notFoundException);

        mockAuthRequest.setUsername("unknown");

        assertThatThrownBy(() -> authService.login(mockAuthRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");

        verify(userClient, times(1)).getUserByUsernameInternal("unknown");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Login - Fail: Should throw RuntimeException when getUserRemote throws generic FeignException")
    void login_FeignExceptionFromGetUser() {
        String errorContent = "Database connection error";
        FeignException.InternalServerError serverError = new FeignException.InternalServerError(
                "Internal Server Error",
                mockFeignRequest,
                errorContent.getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap()
        );

        when(userClient.getUserByUsernameInternal("testuser")).thenThrow(serverError);

        assertThatThrownBy(() -> authService.login(mockAuthRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");

        verify(userClient, times(1)).getUserByUsernameInternal("testuser");
    }

    // ========================================================================
    // VALIDATE TOKEN TESTS
    // ========================================================================

    @Test
    @DisplayName("Validate Token - Success: Should call JwtService.validateToken")
    void validateToken_Success() {
        String token = "valid.jwt.token";
        doNothing().when(jwtService).validateToken(token);

        authService.validateToken(token);

        verify(jwtService, times(1)).validateToken(token);
    }

    @Test
    @DisplayName("Validate Token - Fail: Should propagate exception from JwtService")
    void validateToken_InvalidToken() {
        String token = "invalid.jwt.token";
        doThrow(new RuntimeException("Invalid JWT signature")).when(jwtService).validateToken(token);

        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid JWT signature");

        verify(jwtService, times(1)).validateToken(token);
    }

    // ========================================================================
    // PROTECTED METHOD TESTS (registerRemote)
    // ========================================================================

    @Test
    @DisplayName("RegisterRemote - Success: Should call userClient and return UserDto")
    void registerRemote_Success() {
        UserDto responseDto = new UserDto();
        responseDto.setUsername("newuser");
        responseDto.setId(3L);

        when(userClient.registerUser(mockRegistrationRequest)).thenReturn(responseDto);

        UserDto result = authService.registerRemote(mockRegistrationRequest);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getId()).isEqualTo(3L);
        verify(userClient, times(1)).registerUser(mockRegistrationRequest);
    }

    @Test
    @DisplayName("RegisterRemote - Fail: Should throw RuntimeException with contentUTF8 on FeignException")
    void registerRemote_FeignException() {
        String errorContent = "Email already exists";
        FeignException.Conflict conflictException = new FeignException.Conflict(
                "Conflict",
                mockFeignRequest,
                errorContent.getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap()
        );

        when(userClient.registerUser(mockRegistrationRequest)).thenThrow(conflictException);

        assertThatThrownBy(() -> authService.registerRemote(mockRegistrationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");

        verify(userClient, times(1)).registerUser(mockRegistrationRequest);
    }

    // ========================================================================
    // PROTECTED METHOD TESTS (getUserRemote)
    // ========================================================================

    @Test
    @DisplayName("GetUserRemote - Success: Should call userClient and return UserDto")
    void getUserRemote_Success() {
        when(userClient.getUserByUsernameInternal("testuser")).thenReturn(mockUserDto);

        UserDto result = authService.getUserRemote("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getId()).isEqualTo(1L);
        verify(userClient, times(1)).getUserByUsernameInternal("testuser");
    }

    @Test
    @DisplayName("GetUserRemote - Fail: Should re-throw NotFound exception")
    void getUserRemote_NotFound() {
        FeignException.NotFound notFoundException = new FeignException.NotFound(
                "User not found",
                mockFeignRequest,
                null,
                null
        );

        when(userClient.getUserByUsernameInternal("unknown")).thenThrow(notFoundException);

        assertThatThrownBy(() -> authService.getUserRemote("unknown"))
                .isInstanceOf(FeignException.NotFound.class);

        verify(userClient, times(1)).getUserByUsernameInternal("unknown");
    }

    @Test
    @DisplayName("GetUserRemote - Fail: Should throw RuntimeException with contentUTF8 on generic FeignException")
    void getUserRemote_GenericFeignException() {
        String errorContent = "Service temporarily unavailable";
        FeignException.ServiceUnavailable serviceUnavailable = new FeignException.ServiceUnavailable(
                "Service Unavailable",
                mockFeignRequest,
                errorContent.getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap()
        );

        when(userClient.getUserByUsernameInternal("testuser")).thenThrow(serviceUnavailable);

        assertThatThrownBy(() -> authService.getUserRemote("testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service temporarily unavailable");

        verify(userClient, times(1)).getUserByUsernameInternal("testuser");
    }

    // ========================================================================
    // RESILIENCE FALLBACK TESTS
    // ========================================================================

    @Test
    @DisplayName("Resilience: fallbackGetUser should throw ServiceUnavailableException")
    void resilience_FallbackGetUser() {
        Throwable circuitOpenException = new RuntimeException("Circuit breaker open");

        assertThatThrownBy(() -> authService.fallbackGetUser("anyUser", circuitOpenException))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("User Service is unavailable");
    }

    @Test
    @DisplayName("Resilience: fallbackRegisterUser should throw ServiceUnavailableException")
    void resilience_FallbackRegisterUser() {
        Throwable retryExhaustedException = new RuntimeException("Max retries exceeded");

        assertThatThrownBy(() -> authService.fallbackRegisterUser(mockRegistrationRequest, retryExhaustedException))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("User Service is unavailable");
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Test
    @DisplayName("Edge Case: Login with empty password hash should fail password match")
    void login_EmptyPasswordHash() {
        mockUserDto.setPassword("");

        when(userClient.getUserByUsernameInternal("testuser")).thenReturn(mockUserDto);
        when(passwordEncoder.matches("StrongP@ss1", "")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(mockAuthRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("Edge Case: Register with FeignException containing null contentUTF8")
    void registerRemote_FeignExceptionWithNullContent() {
        FeignException.BadRequest badRequest = new FeignException.BadRequest(
                "Bad Request",
                mockFeignRequest,
                null,
                Collections.emptyMap()
        );

        when(userClient.registerUser(mockRegistrationRequest)).thenThrow(badRequest);

        assertThatThrownBy(() -> authService.registerRemote(mockRegistrationRequest))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Edge Case: getUserRemote with FeignException containing empty contentUTF8")
    void getUserRemote_FeignExceptionWithEmptyContent() {
        FeignException.BadGateway badGateway = new FeignException.BadGateway(
                "Bad Gateway",
                mockFeignRequest,
                "".getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap()
        );

        when(userClient.getUserByUsernameInternal("testuser")).thenThrow(badGateway);

        assertThatThrownBy(() -> authService.getUserRemote("testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("");
    }
}
