package com.ticketblitz.user.dto;

import com.ticketblitz.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseTest {

    @Test
    @DisplayName("Constructor: Should create with all fields")
    void constructor_AllFields() {
        UserResponse response = new UserResponse(1L, "user", "email@test.com", Role.USER, true);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("user");
        assertThat(response.getEmail()).isEqualTo("email@test.com");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Setters: Should update all fields")
    void setters_UpdateFields() {
        UserResponse response = new UserResponse(1L, "user", "email@test.com", Role.USER, true);

        response.setId(2L);
        response.setUsername("newUser");
        response.setEmail("new@test.com");
        response.setRole(Role.ADMIN);
        response.setActive(false);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getUsername()).isEqualTo("newUser");
        assertThat(response.getEmail()).isEqualTo("new@test.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("Equals: Should return true for same instance")
    void equals_SameInstance() {
        UserResponse response = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        assertThat(response).isEqualTo(response);
    }

    @Test
    @DisplayName("Equals: Should return true for equal objects")
    void equals_EqualObjects() {
        UserResponse r1 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        UserResponse r2 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        assertThat(r1).isEqualTo(r2);
    }

    @Test
    @DisplayName("Equals: Should return false for null")
    void equals_Null() {
        UserResponse response = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        assertThat(response).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Equals: Should return false for different class")
    void equals_DifferentClass() {
        UserResponse response = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        assertThat(response).isNotEqualTo("NotAUserResponse");
    }

    @Test
    @DisplayName("Equals: Should return false for different id")
    void equals_DifferentId() {
        UserResponse r1 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        UserResponse r2 = new UserResponse(2L, "user", "email@test.com", Role.USER, true);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("Equals: Should return false for different username")
    void equals_DifferentUsername() {
        UserResponse r1 = new UserResponse(1L, "user1", "email@test.com", Role.USER, true);
        UserResponse r2 = new UserResponse(1L, "user2", "email@test.com", Role.USER, true);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("Equals: Should return false for different role")
    void equals_DifferentRole() {
        UserResponse r1 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        UserResponse r2 = new UserResponse(1L, "user", "email@test.com", Role.ADMIN, true);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("Equals: Should return false for different active status")
    void equals_DifferentActiveStatus() {
        UserResponse r1 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        UserResponse r2 = new UserResponse(1L, "user", "email@test.com", Role.USER, false);
        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    @DisplayName("HashCode: Should return same hashCode for equal objects")
    void hashCode_EqualObjects() {
        UserResponse r1 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        UserResponse r2 = new UserResponse(1L, "user", "email@test.com", Role.USER, true);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("ToString: Should contain all fields")
    void toString_ContainsAllFields() {
        UserResponse response = new UserResponse(1L, "testUser", "test@test.com", Role.ADMIN, true);
        String toString = response.toString();

        assertThat(toString).contains("1");
        assertThat(toString).contains("testUser");
        assertThat(toString).contains("test@test.com");
        assertThat(toString).contains("ADMIN");
    }
}
