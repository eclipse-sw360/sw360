/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for updateWhitelist and addObligationsToLicense.
 * These methods had no coverage in LicenseHandlerTest.
 */
public class AdditionalLicenseHandlerTest extends AbstractLicenseHandlerTest {

    // -- updateWhitelist tests --

    @Test
    public void testUpdateWhitelistWithValidData() throws Exception {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("CT BE OP SWI OSS");

        // should succeed for an existing license
        RequestStatus status = handler.updateWhitelist("Apache-1.1", whitelist, user);
        assertEquals(RequestStatus.SUCCESS, status);
    }

    @Test
    public void testUpdateWhitelistWithEmptySet() throws Exception {
        Set<String> emptyWhitelist = new HashSet<>();

        // clearing the whitelist should also work fine
        RequestStatus status = handler.updateWhitelist("Apache-1.1", emptyWhitelist, user);
        assertEquals(RequestStatus.SUCCESS, status);
    }

    // -- addObligationsToLicense tests --

    @Test
    public void testAddExistingObligationToLicense() throws Exception {
        License license = licenses.get("Apache-2.0");
        assertNotNull(license);

        // T1 is used by Apache-1.1 but not Apache-2.0, try adding it
        Obligation existingOblig = obligs.get("T1");
        assertNotNull(existingOblig);

        Set<Obligation> obligsToAdd = new HashSet<>();
        obligsToAdd.add(existingOblig);

        RequestStatus status = handler.addObligationsToLicense(obligsToAdd, license, user);
        assertEquals(RequestStatus.SUCCESS, status);
    }

    @Test
    public void testAddEmptyObligationSetToLicense() throws Exception {
        License license = licenses.get("Apache-2.0");
        assertNotNull(license);

        // passing an empty set shouldn't break anything
        Set<Obligation> emptySet = new HashSet<>();

        RequestStatus status = handler.addObligationsToLicense(emptySet, license, user);
        assertEquals(RequestStatus.SUCCESS, status);
    }
}