/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentListEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360LicenseListEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360ProjectEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360ProjectListEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360ProjectReleaseRelationship;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360ReleaseEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360ReleaseLinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360ReleaseListEmbedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360SparseRelease;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.users.SW360SparseUser;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.users.SW360User;
import org.junit.Test;

/**
 * Test class for the equals() implementations of various resource classes
 * that do not have their own test class.
 */
public class SW360ResourcesEqualsTest {
    private static void testEqualsWithInheritance(Class<?> resourceClass) {
        EqualsVerifier.forClass(resourceClass)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsSparseAttachment() {
        testEqualsWithInheritance(SW360SparseAttachment.class);
    }

    @Test
    public void testEqualsSparseComponent() {
        testEqualsWithInheritance(SW360SparseComponent.class);
    }

    @Test
    public void testEqualsSparseLicense() {
        testEqualsWithInheritance(SW360SparseLicense.class);
    }

    @Test
    public void testEqualsSparseRelease() {
        testEqualsWithInheritance(SW360SparseRelease.class);
    }

    @Test
    public void testEqualsSparseUser() {
        testEqualsWithInheritance(SW360SparseUser.class);
    }

    @Test
    public void testEqualsUser() {
        testEqualsWithInheritance(SW360User.class);
    }

    @Test
    public void testEqualsLicenseListEmbedded() {
        EqualsVerifier.forClass(SW360LicenseListEmbedded.class)
                .withRedefinedSubclass(SW360ReleaseEmbedded.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsReleaseEmbedded() {
        testEqualsWithInheritance(SW360ReleaseEmbedded.class);
    }

    @Test
    public void testEqualsComponentEmbedded() {
        EqualsVerifier.forClass(SW360ComponentEmbedded.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsComponentListEmbedded() {
        EqualsVerifier.forClass(SW360ComponentListEmbedded.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsProjectEmbedded() {
        EqualsVerifier.forClass(SW360ProjectEmbedded.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsProjectListEmbedded() {
        EqualsVerifier.forClass(SW360ProjectListEmbedded.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsProjectReleaseRelationship() {
        EqualsVerifier.forClass(SW360ProjectReleaseRelationship.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsReleaseListEmbedded() {
        EqualsVerifier.forClass(SW360ReleaseListEmbedded.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsLinkObject() {
        EqualsVerifier.forClass(LinkObjects.class)
                .usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void testEqualsReleaseLinkObjects() {
        EqualsVerifier.forClass(SW360ReleaseLinkObjects.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}