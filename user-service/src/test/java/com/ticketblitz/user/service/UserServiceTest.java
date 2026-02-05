package com.ticketblitz.user.service;

import com.ticketblitz.user.dto.UserRegistrationRequest;
import com.ticketblitz.user.dto.UserResponse;
import com.ticketblitz.user.entity.Role;
import com.ticketblitz.user.entity.User;
import com.ticketblitz.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // --- REGISTER TESTS ---

    @Test
    @DisplayName("Register - Success")
    void registerUser_Success() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("validUser");
        req.setEmail("test@test.com");
        req.setPassword("StrongPass1!");

        when(userRepository.existsByUsername("validUser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1!")).thenReturn("hashedPass");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.registerUser(req);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("validUser");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Register - Fail: Username Taken")
    void registerUser_UsernameTaken() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("takenUser");

        when(userRepository.existsByUsername("takenUser")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username taken!");
    }

    @Test
    @DisplayName("Register - Fail: Email Taken")
    void registerUser_EmailTaken() {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setUsername("newUser");
        req.setEmail("taken@test.com");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email taken!");
    }

    // --- GET USER ENTITY (Internal) TESTS ---

    @Test
    @DisplayName("Get User Entity - Success")
    void getUserEntity_Success() {
        User user = new User();
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.getUserEntity("testuser");
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Get User Entity - Not Found")
    void getUserEntity_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEntity("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // --- DEACTIVATE USER TESTS ---

    @Test
    @DisplayName("Deactivate User - Success")
    void deactivateUser_Success() {
        User user = new User();
        user.setId(1L);
        user.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deactivate User - Not Found")
    void deactivateUser_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // --- UPDATE ROLE TESTS ---

    @Test
    @DisplayName("Update Role - Success")
    void updateUserRole_Success() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserRole(1L, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Update Role - Not Found")
    void updateUserRole_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(999L, Role.ADMIN))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    // --- GET ALL USERS TESTS ---

    @Test
    @DisplayName("Get All Users - Success")
    void getAllUsers_Success() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("u1");
        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> responses = userService.getAllUsers();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getUsername()).isEqualTo("u1");
    }

    @Test
    @DisplayName("Get All Users - Empty List")
    void getAllUsers_EmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> responses = userService.getAllUsers();
        assertThat(responses).isEmpty();
    }

    // --- EXISTS / GET BY ID TESTS ---

    @Test
    @DisplayName("Exists By Id - True")
    void existsById_True() {
        when(userRepository.existsById(1L)).thenReturn(true);
        assertThat(userService.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("Exists By Id - False")
    void existsById_False() {
        when(userRepository.existsById(999L)).thenReturn(false);
        assertThat(userService.existsById(999L)).isFalse();
    }

    @Test
    @DisplayName("Get User By Id - Success")
    void getUserById_Success() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse res = userService.getUserById(1L);
        assertThat(res.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get User By Id - Not Found")
    void getUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }
}
