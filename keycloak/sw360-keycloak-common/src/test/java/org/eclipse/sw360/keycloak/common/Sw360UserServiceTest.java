/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.common;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the package-private helper logic in {@link Sw360UserService}
 * that keeps a single SW360 user record when a user's email changes at the
 * identity provider while their external ID (GID) stays the same - e.g. a
 * ".ext" contractor becoming an FTE and losing the ".ext" suffix from their
 * email address.
 */
class Sw360UserServiceTest {

    @Test
    void shouldMoveOldEmailIntoFormerEmailAddresses_whenEmailChangedForSameExternalId() {
        User existingUser = new User()
                .setEmail("user.ext@siemens.com")
                .setExternalid("GID12345");

        Set<String> result = Sw360UserService.prepareFormerEmailAddresses(existingUser, "user@siemens.com");

        assertThat(result).containsExactly("user.ext@siemens.com");
    }

    @Test
    void shouldKeepPreviousFormerEmailAddresses_whenUserAlreadyHadSome() {
        User existingUser = new User()
                .setEmail("user.ext@siemens.com")
                .setExternalid("GID12345")
                .setFormerEmailAddresses(new HashSet<>(Set.of("very.old@siemens.com")));

        Set<String> result = Sw360UserService.prepareFormerEmailAddresses(existingUser, "user@siemens.com");

        assertThat(result).containsExactlyInAnyOrder("very.old@siemens.com", "user.ext@siemens.com");
    }

    @Test
    void shouldNotIncludeNewEmail_whenItWasPreviouslyAFormerEmailAddress() {
        // Guards against the edge case where a user's email flips back and
        // forth; the current new email must never remain in the former set.
        User existingUser = new User()
                .setEmail("user.ext@siemens.com")
                .setExternalid("GID12345")
                .setFormerEmailAddresses(new HashSet<>(Set.of("user@siemens.com")));

        Set<String> result = Sw360UserService.prepareFormerEmailAddresses(existingUser, "user@siemens.com");

        assertThat(result).containsExactly("user.ext@siemens.com");
    }

    @Test
    void shouldReturnEmptySetOfFormerEmailAddresses_whenExistingUserHasNoneSetAndEmailIsUnchanged() {
        // Defensive edge case: should not be reachable from createOrUpdateUser
        // (which only calls this when the email actually differs), but the
        // helper itself must not throw and must exclude the "new" email.
        User existingUser = new User().setEmail("user@siemens.com").setExternalid("GID12345");

        Set<String> result = Sw360UserService.prepareFormerEmailAddresses(existingUser, "user@siemens.com");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldTreatExternalIdAsUsable_whenItIsARealGid() {
        assertThat(Sw360UserService.isUsableExternalId("GID12345")).isTrue();
    }

    @Test
    void shouldTreatExternalIdAsNotUsable_whenNullOrBlankOrDefaultPlaceholder() {
        assertThat(Sw360UserService.isUsableExternalId(null)).isFalse();
        assertThat(Sw360UserService.isUsableExternalId("")).isFalse();
        assertThat(Sw360UserService.isUsableExternalId("   ")).isFalse();
        assertThat(Sw360UserService.isUsableExternalId(KeycloakConstants.DEFAULT_EXTERNAL_ID)).isFalse();
    }
}
