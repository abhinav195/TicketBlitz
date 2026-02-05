package com.ticketblitz.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    @DisplayName("Enum: Should have USER role")
    void enum_HasUserRole() {
        assertThat(Role.USER).isNotNull();
        assertThat(Role.valueOf("USER")).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Enum: Should have ORGANIZER role")
    void enum_HasOrganizerRole() {
        assertThat(Role.ORGANIZER).isNotNull();
        assertThat(Role.valueOf("ORGANIZER")).isEqualTo(Role.ORGANIZER);
    }

    @Test
    @DisplayName("Enum: Should have ADMIN role")
    void enum_HasAdminRole() {
        assertThat(Role.ADMIN).isNotNull();
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Enum: Should have exactly 3 values")
    void enum_HasThreeValues() {
        assertThat(Role.values()).hasSize(3);
    }

    @Test
    @DisplayName("Enum: Values should match names")
    void enum_ValuesMatchNames() {
        assertThat(Role.USER.name()).isEqualTo("USER");
        assertThat(Role.ORGANIZER.name()).isEqualTo("ORGANIZER");
        assertThat(Role.ADMIN.name()).isEqualTo("ADMIN");
    }
}
