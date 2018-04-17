/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.util;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class LicenseNameWithTextUtilsTest {

    @Test
    public void testSanitize() {
        LicenseNameWithText licenseNameWithText;


        licenseNameWithText = createLicense("name", null, null);
        LicenseNameWithTextUtils.sanitize(licenseNameWithText);
        Assert.assertThat(licenseNameWithText.getLicenseName(), Matchers.is("name"));

        licenseNameWithText = createLicense("\n", null, null);
        LicenseNameWithTextUtils.sanitize(licenseNameWithText);
        Assert.assertNull(licenseNameWithText.getLicenseName());

        licenseNameWithText = createLicense(null, "text", null);
        LicenseNameWithTextUtils.sanitize(licenseNameWithText);
        Assert.assertThat(licenseNameWithText.getLicenseText(), Matchers.is("text"));

        licenseNameWithText = createLicense(null, " ", null);
        LicenseNameWithTextUtils.sanitize(licenseNameWithText);
        Assert.assertNull(licenseNameWithText.getLicenseText());

        licenseNameWithText = createLicense(null, null, "acks");
        LicenseNameWithTextUtils.sanitize(licenseNameWithText);
        Assert.assertThat(licenseNameWithText.getAcknowledgements(), Matchers.is("acks"));

        licenseNameWithText = createLicense(null, null, "  ");
        LicenseNameWithTextUtils.sanitize(licenseNameWithText);
        Assert.assertNull(licenseNameWithText.getAcknowledgements());

    }

    @Test
    public void testIsEmpty() {
        Assert.assertTrue(LicenseNameWithTextUtils.isEmpty(createLicense(null, null, null)));
        Assert.assertTrue(LicenseNameWithTextUtils.isEmpty(createLicense("", null, null)));
        Assert.assertTrue(LicenseNameWithTextUtils.isEmpty(createLicense(null, "", null)));
        Assert.assertTrue(LicenseNameWithTextUtils.isEmpty(createLicense(null, null, "")));
        Assert.assertTrue(LicenseNameWithTextUtils.isEmpty(createLicense("", "", "")));
        Assert.assertTrue(LicenseNameWithTextUtils.isEmpty(createLicense("   ", "\n", "\t")));

        Assert.assertFalse(LicenseNameWithTextUtils.isEmpty(createLicense("name", null, null)));
        Assert.assertFalse(LicenseNameWithTextUtils.isEmpty(createLicense(null, "text", null)));
        Assert.assertFalse(LicenseNameWithTextUtils.isEmpty(createLicense(null, null, "acks")));
    }

    @Test
    public void testLicenseNameWithTextEquals() {
        // @formatter:off
        Assert.assertTrue(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(null, null, null), createLicense(null, null, null)));
        Assert.assertTrue(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(" ", "", "\t"), createLicense(null, null, null)));
        Assert.assertTrue(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense("name", "  ", null), createLicense("name", null, null)));
        Assert.assertTrue(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(null, "text", "\n"), createLicense(null, "text", null)));
        Assert.assertTrue(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(null, null, "ack"), createLicense(null, null, "ack")));

        Assert.assertFalse(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(null, null, null), createLicense("name", null, null)));
        Assert.assertFalse(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(null, null, null), createLicense(null, "text", null)));
        Assert.assertFalse(LicenseNameWithTextUtils.licenseNameWithTextEquals(
                createLicense(null, null, null), createLicense(null, null, "ack")));
     // @formatter:on
    }

    private LicenseNameWithText createLicense(String name, String text, String acknowledgements) {
        LicenseNameWithText licenseNameWithText = new LicenseNameWithText();
        licenseNameWithText.setLicenseName(name);
        licenseNameWithText.setLicenseText(text);
        licenseNameWithText.setAcknowledgements(acknowledgements);
        return licenseNameWithText;
    }
}
