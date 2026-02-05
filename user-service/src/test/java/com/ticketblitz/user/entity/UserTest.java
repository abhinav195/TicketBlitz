package com.ticketblitz.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("User: Should create with default values")
    void user_DefaultValues() {
        User user = new User();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("User: Should set and get all fields")
    void user_SettersAndGetters() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setEmail("test@test.com");
        user.setPassword("hashedPass");
        user.setRole(Role.ADMIN);
        user.setActive(false);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("testUser");
        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getPassword()).isEqualTo("hashedPass");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Equals: Should return true for same instance")
    void equals_SameInstance() {
        User user = new User();
        user.setId(1L);
        assertThat(user).isEqualTo(user);
    }

    @Test
    @DisplayName("Equals: Should return true for equal objects")
    void equals_EqualObjects() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user");
        user1.setEmail("test@test.com");

        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("user");
        user2.setEmail("test@test.com");

        assertThat(user1).isEqualTo(user2);
    }

    @Test
    @DisplayName("Equals: Should return false for null")
    void equals_Null() {
        User user = new User();
        assertThat(user).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Equals: Should return false for different class")
    void equals_DifferentClass() {
        User user = new User();
        assertThat(user).isNotEqualTo("NotAUser");
    }

    @Test
    @DisplayName("Equals: Should return false for different id")
    void equals_DifferentId() {
        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        assertThat(user1).isNotEqualTo(user2);
    }

    @Test
    @DisplayName("HashCode: Should return same hashCode for equal objects")
    void hashCode_EqualObjects() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user");

        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("user");

        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("ToString: Should contain username")
    void toString_ContainsUsername() {
        User user = new User();
        user.setUsername("testUser");
        user.setEmail("test@test.com");

        String toString = user.toString();
        assertThat(toString).contains("testUser");
        assertThat(toString).contains("test@test.com");
    }
}
