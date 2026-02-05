package com.ticketblitz.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketblitz.auth.dto.AuthRequest;
import com.ticketblitz.auth.dto.AuthResponse;
import com.ticketblitz.auth.dto.UserDto;
import com.ticketblitz.auth.dto.UserRegistrationRequest;
import com.ticketblitz.auth.exception.GlobalExceptionHandler;
import com.ticketblitz.auth.exception.ServiceUnavailableException;
import com.ticketblitz.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========================================================================
    // REGISTER ENDPOINT TESTS
    // ========================================================================

    @Test
    @DisplayName("Register - Success: Should return 200 and UserDto")
    void register_Success() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("validUser");
        request.setEmail("test@test.com");
        request.setPassword("ValidP@ss1");

        UserDto responseDto = new UserDto();
        responseDto.setUsername("validUser");
        responseDto.setId(1L);

        when(authService.register(any(UserRegistrationRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("validUser"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Validation: Register should fail with Invalid Email")
    void register_InvalidEmail() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("validUser");
        request.setEmail("not-an-email");
        request.setPassword("ValidP@ss1");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"));
    }

    @Test
    @DisplayName("Validation: Register should fail with Short Password")
    void register_ShortPassword() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("validUser");
        request.setEmail("test@test.com");
        request.setPassword("short");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Resilience: Register should return 503 when ServiceUnavailableException is thrown")
    void register_ServiceUnavailable() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("validUser");
        request.setEmail("test@test.com");
        request.setPassword("ValidP@ss1");

        when(authService.register(any(UserRegistrationRequest.class)))
                .thenThrow(new ServiceUnavailableException("User Service down", new RuntimeException()));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    // ========================================================================
    // LOGIN ENDPOINT TESTS
    // ========================================================================

    @Test
    @DisplayName("Login - Success: Should return 200 and Token")
    void login_Success() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("Pass123!");

        AuthResponse response = new AuthResponse("jwt-token", "testuser", 1L);

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("Validation: Login should fail with Null Username")
    void login_NullUsername() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername(null);
        request.setPassword("Pass123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").value("Username is required"));
    }

    @Test
    @DisplayName("Resilience: Login should return 503 when ServiceUnavailableException is thrown")
    void login_ServiceUnavailable() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("Pass123!");

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new ServiceUnavailableException("User Service down", new RuntimeException()));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    // ========================================================================
    // VALIDATE ENDPOINT TESTS
    // ========================================================================

    @Test
    @DisplayName("Validate - Success: Should return 200")
    void validate_Success() throws Exception {
        doNothing().when(authService).validateToken("valid-token");

        mockMvc.perform(get("/auth/validate")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Token is valid"));
    }

    @Test
    @DisplayName("Validate - Fail: Should return 401 when token is invalid")
    void validate_InvalidToken() throws Exception {
        doThrow(new RuntimeException("Invalid JWT")).when(authService).validateToken("invalid-token");

        mockMvc.perform(get("/auth/validate")
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid JWT"));
    }
}
