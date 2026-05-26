/*
 * Copyright SW360 Contributor, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ProjectController#parseVisibility(String)}.
 *
 * <p>Regression coverage for
 * <a href="https://github.com/eclipse-sw360/sw360/issues/2569">issue #2569</a>:
 * the REST {@code visibility} field must be parsed case-insensitively.</p>
 */
public class ProjectControllerVisibilityTest {

    @Test
    public void parseVisibility_acceptsExactCanonicalUppercase() {
        assertEquals(Visibility.PRIVATE, ProjectController.parseVisibility("PRIVATE"));
        assertEquals(Visibility.EVERYONE, ProjectController.parseVisibility("EVERYONE"));
        assertEquals(Visibility.ME_AND_MODERATORS,
                ProjectController.parseVisibility("ME_AND_MODERATORS"));
        assertEquals(Visibility.BUISNESSUNIT_AND_MODERATORS,
                ProjectController.parseVisibility("BUISNESSUNIT_AND_MODERATORS"));
    }

    @Test
    public void parseVisibility_acceptsLowercase() {
        assertEquals(Visibility.PRIVATE, ProjectController.parseVisibility("private"));
        assertEquals(Visibility.EVERYONE, ProjectController.parseVisibility("everyone"));
        assertEquals(Visibility.ME_AND_MODERATORS,
                ProjectController.parseVisibility("me_and_moderators"));
    }

    @Test
    public void parseVisibility_acceptsMixedCase() {
        assertEquals(Visibility.PRIVATE, ProjectController.parseVisibility("Private"));
        assertEquals(Visibility.EVERYONE, ProjectController.parseVisibility("Everyone"));
        assertEquals(Visibility.ME_AND_MODERATORS,
                ProjectController.parseVisibility("Me_And_Moderators"));
    }

    @Test
    public void parseVisibility_trimsWhitespace() {
        assertEquals(Visibility.PRIVATE, ProjectController.parseVisibility("  private  "));
    }

    @Test
    public void parseVisibility_returnsNullForNull() {
        assertNull(ProjectController.parseVisibility(null));
    }

    @Test
    public void parseVisibility_returnsNullForBlank() {
        assertNull(ProjectController.parseVisibility(""));
        assertNull(ProjectController.parseVisibility("   "));
    }

    @Test
    public void parseVisibility_throwsHelpfulErrorForInvalidValue() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ProjectController.parseVisibility("not_a_value"));
        // Error message must include the offending input and the list of valid enum names.
        assertTrue(ex.getMessage().contains("not_a_value"));
        assertTrue(ex.getMessage().contains("PRIVATE"));
        assertTrue(ex.getMessage().contains("EVERYONE"));
    }
}
