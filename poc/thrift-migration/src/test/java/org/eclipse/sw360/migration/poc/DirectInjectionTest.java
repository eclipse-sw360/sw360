/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc;

import org.eclipse.sw360.migration.poc.handler.Sw360UserHandler;
import org.eclipse.sw360.migration.poc.model.RequestStatus;
import org.eclipse.sw360.migration.poc.model.User;
import org.eclipse.sw360.migration.poc.service.DirectInjectionUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test proving that the Spring wiring works with zero Thrift infrastructure.
 *
 * <p>The context loads only:
 * <ul>
 *   <li>{@code UserDatabaseHandler} (@Repository)</li>
 *   <li>{@code UserHandlerBean} (@Service, implements Sw360UserHandler)</li>
 *   <li>{@code DirectInjectionUserService} (@Service, injects Sw360UserHandler)</li>
 * </ul>
 *
 * <p>No {@code ThriftClients}, no {@code THttpClient}, no backend URL, no network.
 * In the current codebase, equivalent tests mock {@code ThriftClients} via
 * {@code @MockBean} — after migration they mock {@code Sw360UserHandler} directly,
 * which is simpler and does not require Thrift on the test classpath.
 */
@SpringBootTest
class DirectInjectionTest {

    @Autowired
    DirectInjectionUserService userService;

    @Autowired
    Sw360UserHandler userHandler;

    @Test
    void contextLoads_handlerWiredWithoutThriftInfrastructure() {
        // If this passes, the @Service chain is fully wired:
        //   UserDatabaseHandler <- UserHandlerBean <- DirectInjectionUserService
        // with no THttpClient, TCompactProtocol, or ThriftClients anywhere.
        assertThat(userService).isNotNull();
        assertThat(userHandler).isNotNull();
    }

    @Test
    void createUser_directCall_persistsAndReturnsSuccess() {
        User user = new User("u-test-001", "alice@sw360.org", "Engineering");
        user.setFullname("Alice Doe");

        RequestStatus status = userService.createUser(user);

        assertThat(status).isEqualTo(RequestStatus.SUCCESS);
    }

    @Test
    void getByEmail_afterCreate_returnsMatchingUser() {
        User user = new User("u-test-002", "bob@sw360.org", "Legal");
        user.setFullname("Bob Smith");
        userService.createUser(user);

        User retrieved = userService.getUserByEmail("bob@sw360.org");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getFullname()).isEqualTo("Bob Smith");
        assertThat(retrieved.getDepartment()).isEqualTo("Legal");
    }

    @Test
    void getAllUsers_afterMultipleCreates_returnsAll() {
        userService.createUser(new User("u-test-003", "carol@sw360.org", "Security"));
        userService.createUser(new User("u-test-004", "dave@sw360.org", "Security"));

        List<User> users = userService.getAllUsers();

        assertThat(users).extracting(User::getEmail)
                .contains("carol@sw360.org", "dave@sw360.org");
    }

    @Test
    void updateUser_changedFields_persistsChange() {
        User user = new User("u-test-005", "eve@sw360.org", "Ops");
        userService.createUser(user);

        user.setFullname("Eve Updated");
        RequestStatus status = userService.updateUser(user);

        assertThat(status).isEqualTo(RequestStatus.SUCCESS);
        assertThat(userService.getUser("u-test-005").getFullname()).isEqualTo("Eve Updated");
    }

    @Test
    void deleteUser_withAdmin_removesUser() {
        User user = new User("u-test-006", "frank@sw360.org", "Ops");
        User admin = new User("admin-001", "admin@sw360.org", "IT");
        userService.createUser(user);

        RequestStatus status = userService.deleteUser(user, admin);

        assertThat(status).isEqualTo(RequestStatus.SUCCESS);
        assertThat(userService.getUser("u-test-006")).isNull();
    }

    @Test
    void deleteUser_withoutAdmin_returnsAccessDenied() {
        User user = new User("u-test-007", "grace@sw360.org", "Ops");
        userService.createUser(user);

        RequestStatus status = userService.deleteUser(user, null);

        assertThat(status).isEqualTo(RequestStatus.ACCESS_DENIED);
    }
}
