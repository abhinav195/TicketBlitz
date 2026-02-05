package com.ticketblitz.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketblitz.user.dto.UserRegistrationRequest;
import com.ticketblitz.user.dto.UserResponse;
import com.ticketblitz.user.entity.Role;
import com.ticketblitz.user.entity.User;
import com.ticketblitz.user.exception.GlobalExceptionHandler;
import com.ticketblitz.user.service.UserService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- REGISTER ENDPOINT ---

    @Test
    @DisplayName("Register - Success")
    void register_Success() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser");
        req.setEmail("valid@test.com");
        req.setPassword("ValidP@ss1");

        UserResponse res = new UserResponse(1L, "validUser", "valid@test.com", Role.USER, true);

        when(userService.registerUser(any())).thenReturn(res);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("validUser"));
    }

    @Test
    @DisplayName("Register - Validation Error (Short Password)")
    void register_ShortPassword() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser");
        req.setEmail("valid@test.com");
        req.setPassword("short");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("Register - Validation Error (Invalid Email)")
    void register_InvalidEmail() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser");
        req.setEmail("notanemail");
        req.setPassword("ValidPass1!");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("Register - Validation Error (Invalid Username Pattern)")
    void register_InvalidUsernamePattern() throws Exception {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("invalid user!");
        req.setEmail("valid@test.com");
        req.setPassword("ValidPass1!");

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    // --- INTERNAL GET USER ---

    @Test
    @DisplayName("Get Internal User - Success")
    void getInternalUser_Success() throws Exception {
        User user = new User();
        user.setUsername("internalUser");

        when(userService.getUserEntity("internalUser")).thenReturn(user);

        mockMvc.perform(get("/users/internal/internalUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("internalUser"));
    }

    // --- ADMIN ENDPOINTS ---

    @Test
    @DisplayName("Get All Users - Success")
    void getAllUsers_Success() throws Exception {
        UserResponse u1 = new UserResponse(1L, "u1", "e1", Role.USER, true);
        when(userService.getAllUsers()).thenReturn(List.of(u1));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("u1"));
    }

    @Test
    @DisplayName("Deactivate User - Success")
    void deactivateUser_Success() throws Exception {
        doNothing().when(userService).deactivateUser(1L);

        mockMvc.perform(put("/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string("User deactivated. Please contact support."));
    }

    @Test
    @DisplayName("Update Role - Success")
    void updateRole_Success() throws Exception {
        doNothing().when(userService).updateUserRole(1L, Role.ADMIN);

        mockMvc.perform(put("/users/1/role")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("User role updated to ADMIN"));
    }

    // --- VALIDATE USER ENDPOINT (FIXED from /exists to /validate) ---

    @Test
    @DisplayName("Validate User - True")
    void validateUser_True() throws Exception {
        when(userService.existsById(1L)).thenReturn(true);

        mockMvc.perform(get("/users/1/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Validate User - False")
    void validateUser_False() throws Exception {
        when(userService.existsById(999L)).thenReturn(false);

        mockMvc.perform(get("/users/999/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Get User By ID - Success")
    void getUserById_Success() throws Exception {
        UserResponse res = new UserResponse(1L, "u1", "e1", Role.USER, true);
        when(userService.getUserById(1L)).thenReturn(res);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("u1"));
    }
}
