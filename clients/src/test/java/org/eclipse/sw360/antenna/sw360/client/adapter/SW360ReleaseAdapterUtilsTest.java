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
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SW360ReleaseAdapterUtilsTest {

    @Test
    public void validateReleaseWithValidRelease() {
        SW360Release release = new SW360Release()
                .setName("releaseName")
                .setVersion("1.0-SNAPSHOT");

        assertThat(SW360ReleaseAdapterUtils.validateRelease(release)).isSameAs(release);
    }

    @Test
    public void ValidateReleaseNoName() {
        SW360Release release = new SW360Release();
        release.setVersion("1.1");

        try {
            SW360ReleaseAdapterUtils.validateRelease(release);
            fail("Invalid release not detected!");
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'name'");
        }
    }

    @Test
    public void testIsValidReleaseEmptyName() {
        SW360Release release = new SW360Release();
        release.setVersion("1.2");
        release.setName("");

        try {
            SW360ReleaseAdapterUtils.validateRelease(release);
            fail("Invalid release not detected!");
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'name'");
        }
    }

    @Test
    public void testIsValidReleaseNoVersion() {
        SW360Release release = new SW360Release();
        release.setName("myRelease");

        try {
            SW360ReleaseAdapterUtils.validateRelease(release);
            fail("Invalid release not detected!");
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'version'");
        }
    }

    @Test
    public void testIsValidReleaseEmptyVersion() {
        SW360Release release = new SW360Release();
        release.setName("releaseName");
        release.setVersion("");

        try {
            SW360ReleaseAdapterUtils.validateRelease(release);
            fail("Invalid release not detected!");
        }
        catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'version'");
        }
    }
}