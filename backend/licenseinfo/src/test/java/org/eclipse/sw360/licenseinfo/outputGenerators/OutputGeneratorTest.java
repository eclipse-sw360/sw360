/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.outputGenerators;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.junit.jupiter.api.Test;

import static org.eclipse.sw360.licenseinfo.outputGenerators.OutputGenerator.normaliseLicenseText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OutputGeneratorTest {

    @Test
    public void licensesWithoutATextStayApart() {
        // regression: all of these used to normalise to "", which merged them into a
        // single entry of the license list and made every release cite the same text
        String mit = normaliseLicenseText(license("MIT", ""));
        String apache = normaliseLicenseText(license("Apache-2.0", ""));
        String gpl = normaliseLicenseText(license("GPL-2.0-only", null));

        assertNotEquals(mit, apache);
        assertNotEquals(mit, gpl);
        assertNotEquals(apache, gpl);
    }

    @Test
    public void aMissingTextIsTheSameAsAnEmptyOne() {
        assertEquals(normaliseLicenseText(license("MIT", null)), normaliseLicenseText(license("MIT", "")));
    }

    @Test
    public void textsDifferingOnlyInFormattingDescribeTheSameLicense() {
        assertEquals(normaliseLicenseText(license("MIT", "Permission is hereby granted, free of charge")),
                normaliseLicenseText(license("MIT", "  permission IS hereby granted -- free of charge.  ")));
    }

    @Test
    public void differentTextsUnderTheSameNameStayApart() {
        assertNotEquals(normaliseLicenseText(license("BSD", "Redistribution in source form")),
                normaliseLicenseText(license("BSD", "Redistribution in binary form")));
    }

    @Test
    public void theSameTextUnderDifferentNamesStaysApart() {
        assertNotEquals(normaliseLicenseText(license("MIT", "Permission is hereby granted")),
                normaliseLicenseText(license("MIT-0", "Permission is hereby granted")));
    }

    @Test
    public void textWithoutLatinCharactersIsKeptVerbatim() {
        // nothing survives the normalisation of these, so the original text has to be
        // part of the key to tell them apart
        assertNotEquals(normaliseLicenseText(license("Custom", "ライセンス")),
                normaliseLicenseText(license("Custom", "许可证")));
    }

    @Test
    public void aMissingNameIsTolerated() {
        assertEquals(normaliseLicenseText(license(null, "")), normaliseLicenseText(license("", "")));
        assertNotEquals(normaliseLicenseText(license(null, "some text")),
                normaliseLicenseText(license("MIT", "some text")));
    }

    private static LicenseNameWithText license(String name, String text) {
        return new LicenseNameWithText().setLicenseName(name).setLicenseText(text);
    }
}
