package com.eams;

import com.eams.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class EamsBackendApplicationTests extends IntegrationTestBase {

    @Autowired
    private RoleRepository roles;

    @Test
    void contextLoadsAndRolesAreSeeded() {
        for (String role : new String[]{"ADMIN", "MANAGER", "EMPLOYEE", "TECHNICIAN"}) {
            assertThat(roles.findByName(role)).as("role " + role).isPresent();
        }
    }
}
