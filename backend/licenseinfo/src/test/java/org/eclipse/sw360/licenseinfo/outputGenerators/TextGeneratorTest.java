/*
 * Copyright Bavith Babu, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.outputGenerators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TextGenerator}, which renders the plain-text license
 * disclosure from the {@code textLicenseInfoFile.vm} template.
 */
class TextGeneratorTest {

    @Test
    void shouldThrowIllegalArgumentException_whenVariantIsReport() {
        // given -- TextGenerator only implements DISCLOSURE
        TextGenerator generator = new TextGenerator(OutputFormatVariant.REPORT, "License Report as TXT");

        // when / then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> generator.generateOutputFile(List.of(), project(), List.of(), null,
                        Map.of(), Map.of(), "", false));
        assertTrue(thrown.getMessage().contains("Unknown generator variant type"));
    }


    @Test
    void shouldExposeTextOutputMetadata_whenConstructedForDisclosure() {
        // given
        TextGenerator generator = new TextGenerator(OutputFormatVariant.DISCLOSURE, "License Disclosure as TXT");

        // when
        OutputFormatInfo result = generator.getOutputFormatInfo();

        // then
        assertEquals("txt", result.getFileExtension());
        assertEquals("text/plain", result.getMimeType());
        assertFalse(result.isIsOutputBinary());
        assertEquals(OutputFormatVariant.DISCLOSURE, result.getVariant());
        assertEquals("TextGenerator", result.getGeneratorClassName());
        assertEquals("License Disclosure as TXT", result.getDescription());
    }

    @Test
    void shouldRenderDisclosure_withProjectLicenseAndCopyright() throws Exception {
        // given
        TextGenerator generator = new TextGenerator(OutputFormatVariant.DISCLOSURE, "License Disclosure as TXT");
        LicenseInfoParsingResult release = new LicenseInfoParsingResult()
                .setStatus(LicenseInfoRequestStatus.SUCCESS)
                .setName("commons-io")
                .setVendor("Apache")
                .setVersion("2.11.0")
                .setLicenseInfo(new LicenseInfo()
                        .setLicenseNamesWithTexts(Set.of(new LicenseNameWithText()
                                .setLicenseName("Apache-2.0")
                                .setLicenseText("Licensed under the Apache License, Version 2.0")))
                        .setCopyrights(Set.of("Copyright 2002-2021 The Apache Software Foundation")));

        // when
        String output = generator.generateOutputFile(List.of(release), project(), List.of(), null,
                Map.of(), Map.of(), "", false);

        // then
        assertFalse(output.startsWith("License information could not be generated"));
        assertTrue(output.contains("Installer 1.0"));
        assertTrue(output.contains("Header text for the disclosure"));
        assertTrue(output.contains("commons-io"));
        assertTrue(output.contains("Apache-2.0"));
        assertTrue(output.contains("Licensed under the Apache License, Version 2.0"));
        assertTrue(output.contains("Copyright 2002-2021 The Apache Software Foundation"));
    }

    @Test
    void shouldReportNoReleases_whenResultsAreEmpty() throws Exception {
        // given
        TextGenerator generator = new TextGenerator(OutputFormatVariant.DISCLOSURE, "License Disclosure as TXT");

        // when
        String output = generator.generateOutputFile(List.of(), project(), List.of(), null,
                Map.of(), Map.of(), "", false);

        // then
        assertTrue(output.contains("Either there is no releases based on your selection"));
    }

    @Test
    void shouldReturnFallbackMessage_whenRenderingFails() throws Exception {
        // given -- null results make the template rendering throw internally
        TextGenerator generator = new TextGenerator(OutputFormatVariant.DISCLOSURE, "License Disclosure as TXT");

        // when
        String output = generator.generateOutputFile(null, project(), List.of(), null,
                Map.of(), Map.of(), "", false);

        // then -- the exception is caught and replaced with a readable message
        assertTrue(output.startsWith("License information could not be generated."));
    }

    // =========================================================================
    //  Fixtures
    // =========================================================================

    // generateOutputFile reads name, version, header and obligations text from the
    // project before switching on the variant, so the project must not be null.
    private static Project project() {
        return new Project()
                .setName("Installer")
                .setVersion("1.0")
                .setLicenseInfoHeaderText("Header text for the disclosure")
                .setObligationsText("Obligations text");
    }
}
