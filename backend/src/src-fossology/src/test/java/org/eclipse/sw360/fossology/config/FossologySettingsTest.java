/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.config;

import com.jcraft.jsch.JSch;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FossologySettingsTest {

    FossologySettings fossologySettings;

    @Before
    public void setUp() throws Exception {
        fossologySettings = new FossologySettings();
    }

    @Test
    public void testGetFossologyConnectionTimeout() throws Exception {
        final long connectionTimeout = fossologySettings.getFossologyConnectionTimeout();
        assertThat(connectionTimeout, is(greaterThan(100L)));
        assertThat(connectionTimeout, is(lessThan(100000L)));
    }

    @Test
    public void testGetFossologyExecutionTimeout() throws Exception {
        final long executionTimeout = fossologySettings.getFossologyExecutionTimeout();
        assertThat(executionTimeout, is(greaterThan(1000L)));
        assertThat(executionTimeout, is(lessThan(1000000L)));
    }

    @Test
    public void testGetFossologyHost() throws Exception {
        final String fossologyHost = fossologySettings.getFossologyHost();
        assertThat(fossologyHost, not(isEmptyOrNullString()));
    }

    @Test
    public void testGetFossologyPort() throws Exception {
        final int fossologySshUsername = fossologySettings.getFossologyPort();
        assertThat(fossologySshUsername, is(greaterThan(0)));
        assertThat(fossologySshUsername, is(lessThan(65536)));
    }

    @Test
    public void testGetFossologySshUsername() throws Exception {
        final String fossologySshUsername = fossologySettings.getFossologySshUsername();
        assertThat(fossologySshUsername, not(isEmptyOrNullString()));
    }

    @Test
    public void testKeyIsAValidPrivateKey() throws Exception {
        final String msg = /* this tests that the */ "Private key defined in property files" /* is valid */;

        final byte[] fossologyPrivateKey = fossologySettings.getFossologyPrivateKey();

        assertThat(msg + "is not readable",
                fossologyPrivateKey, notNullValue());

        assertThat(msg + "is empty",
                fossologyPrivateKey.length, is(greaterThan(0)));

        try {
            final JSch jSch = new JSch();
            jSch.addIdentity("test", fossologyPrivateKey, null, null);
        } catch (Exception e) {
            fail(msg + "is not a valid private key");
        }
    }
}