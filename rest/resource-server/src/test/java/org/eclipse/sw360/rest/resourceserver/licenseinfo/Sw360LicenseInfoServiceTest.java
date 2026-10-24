/*
 * Copyright Bavith Babu, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.licenseinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link Sw360LicenseInfoService}.
 * <p>
 * The service is a thin adapter over the Thrift license info backend: it forwards each
 * call and translates backend failures into the exception types the REST layer expects.
 * A {@link SW360Exception} must surface as a {@link BadRequestClientException} (HTTP
 * 400, caller's fault), while a transport-level {@link TException} must surface as a
 * plain {@link RuntimeException} (HTTP 500, server's fault).
 * <p>
 * The static Thrift client factory is replaced with a mock so that the translation can
 * be verified without a running Thrift service.
 */
class Sw360LicenseInfoServiceTest {

    private static final String GENERATOR_CLASS_NAME = "DocxGenerator";
    private static final String DOCX_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final Sw360LicenseInfoService licenseInfoService = new Sw360LicenseInfoService();

    private MockedStatic<ThriftClients> thriftClientsMock;
    private LicenseInfoService.Client licenseInfoClient;

    @BeforeEach
    void setUp() {
        thriftClientsMock = mockStatic(ThriftClients.class);
        licenseInfoClient = mock(LicenseInfoService.Client.class);
        thriftClientsMock.when(ThriftClients::makeLicenseInfoClient).thenReturn(licenseInfoClient);
    }

    @AfterEach
    void tearDown() {
        thriftClientsMock.close();
    }

    // =========================================================================
    //  getOutputFormatInfoForGeneratorClass
    // =========================================================================

    @Test
    void shouldReturnOutputFormatInfo_whenBackendRespondsSuccessfully() throws TException {
        // given
        OutputFormatInfo expected = new OutputFormatInfo()
                .setGeneratorClassName(GENERATOR_CLASS_NAME)
                .setFileExtension("docx")
                .setMimeType(DOCX_MIME_TYPE)
                .setIsOutputBinary(true);
        when(licenseInfoClient.getOutputFormatInfoForGeneratorClass(GENERATOR_CLASS_NAME))
                .thenReturn(expected);

        // when
        OutputFormatInfo result =
                licenseInfoService.getOutputFormatInfoForGeneratorClass(GENERATOR_CLASS_NAME);

        // then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void shouldThrowBadRequest_whenBackendRejectsGeneratorClass() throws TException {
        // given
        SW360Exception backendRejection = new SW360Exception("Unknown generator class");
        when(licenseInfoClient.getOutputFormatInfoForGeneratorClass("NoSuchGenerator"))
                .thenThrow(backendRejection);

        // when / then
        assertThatThrownBy(() ->
                licenseInfoService.getOutputFormatInfoForGeneratorClass("NoSuchGenerator"))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining("Unknown generator class")
                .hasCause(backendRejection);
    }

    @Test
    void shouldThrowRuntimeException_whenTransportFailsForOutputFormatInfo() throws TException {
        // given
        TException transportFailure = new TException("backend unreachable");
        when(licenseInfoClient.getOutputFormatInfoForGeneratorClass(GENERATOR_CLASS_NAME))
                .thenThrow(transportFailure);

        // when / then
        assertThatThrownBy(() ->
                licenseInfoService.getOutputFormatInfoForGeneratorClass(GENERATOR_CLASS_NAME))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(transportFailure);
    }

    // =========================================================================
    //  getLicenseInfoFile
    // =========================================================================

    @Test
    void shouldReturnLicenseInfoFileWithReleaseVersion_whenExcludeReleaseVersionIsFalse()
            throws TException {
        // given
        LicenseInfoFile expected = new LicenseInfoFile();
        when(licenseInfoClient.getLicenseInfoFile(project(), user(), GENERATOR_CLASS_NAME,
                selectedReleaseAndAttachmentIds(), excludedLicenses(), "external-id", "report.docx"))
                .thenReturn(expected);

        // when
        LicenseInfoFile result = licenseInfoService.getLicenseInfoFile(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", false);

        // then
        assertThat(result).isSameAs(expected);
        verify(licenseInfoClient).getLicenseInfoFile(project(), user(), GENERATOR_CLASS_NAME,
                selectedReleaseAndAttachmentIds(), excludedLicenses(), "external-id", "report.docx");
        verify(licenseInfoClient, never()).getLicenseInfoFileWithoutReleaseVersion(any(), any(),
                any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void shouldReturnLicenseInfoFileWithoutReleaseVersion_whenExcludeReleaseVersionIsTrue()
            throws TException {
        // given
        LicenseInfoFile expected = new LicenseInfoFile();
        when(licenseInfoClient.getLicenseInfoFileWithoutReleaseVersion(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true))
                .thenReturn(expected);

        // when
        LicenseInfoFile result = licenseInfoService.getLicenseInfoFile(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true);

        // then
        assertThat(result).isSameAs(expected);
        verify(licenseInfoClient).getLicenseInfoFileWithoutReleaseVersion(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true);
        verify(licenseInfoClient, never()).getLicenseInfoFile(any(), any(), any(), any(), any(),
                any(), any());
    }

    @Test
    void shouldThrowBadRequest_whenBackendRejectsLicenseInfoFileRequest() throws TException {
        // given
        SW360Exception backendRejection = new SW360Exception("No attachment selected");
        when(licenseInfoClient.getLicenseInfoFile(project(), user(), GENERATOR_CLASS_NAME,
                selectedReleaseAndAttachmentIds(), excludedLicenses(), "external-id", "report.docx"))
                .thenThrow(backendRejection);

        // when / then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", false))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining("No attachment selected")
                .hasCause(backendRejection);
    }

    @Test
    void shouldThrowRuntimeException_whenTransportFailsForLicenseInfoFile() throws TException {
        // given
        TException transportFailure = new TException("backend unreachable");
        when(licenseInfoClient.getLicenseInfoFile(project(), user(), GENERATOR_CLASS_NAME,
                selectedReleaseAndAttachmentIds(), excludedLicenses(), "external-id", "report.docx"))
                .thenThrow(transportFailure);

        // when / then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", false))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(transportFailure);
    }

    @Test
    void shouldThrowBadRequest_whenBackendRejectsRequestWithoutReleaseVersion() throws TException {
        // given
        SW360Exception backendRejection = new SW360Exception("No attachment selected");
        when(licenseInfoClient.getLicenseInfoFileWithoutReleaseVersion(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true))
                .thenThrow(backendRejection);

        // when / then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining("No attachment selected")
                .hasCause(backendRejection);
    }

    @Test
    void shouldThrowRuntimeException_whenTransportFailsWithoutReleaseVersion() throws TException {
        // given
        TException transportFailure = new TException("backend unreachable");
        when(licenseInfoClient.getLicenseInfoFileWithoutReleaseVersion(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true))
                .thenThrow(transportFailure);

        // when / then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(project(), user(),
                GENERATOR_CLASS_NAME, selectedReleaseAndAttachmentIds(), excludedLicenses(),
                "external-id", "report.docx", true))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(transportFailure);
    }

    // =========================================================================
    //  getLicenseInfoForAttachment
    // =========================================================================

    @Test
    void shouldReturnParsingResults_andPassUserAsLastArgument() throws TException {
        // given
        List<LicenseInfoParsingResult> expected = List.of(new LicenseInfoParsingResult());
        when(licenseInfoClient.getLicenseInfoForAttachment(release(), "attachment-1", true, user()))
                .thenReturn(expected);

        // when
        List<LicenseInfoParsingResult> result = licenseInfoService
                .getLicenseInfoForAttachment(release(), user(), "attachment-1", true);

        // then
        assertThat(result).isSameAs(expected);
        // the service reorders its arguments: the user moves from second to last position
        verify(licenseInfoClient).getLicenseInfoForAttachment(release(), "attachment-1", true, user());
    }

    @Test
    void shouldThrowRuntimeException_whenTransportFailsForAttachmentParsing() throws TException {
        // given
        TException transportFailure = new TException("backend unreachable");
        when(licenseInfoClient.getLicenseInfoForAttachment(release(), "attachment-1", false, user()))
                .thenThrow(transportFailure);

        // when / then
        assertThatThrownBy(() -> licenseInfoService
                .getLicenseInfoForAttachment(release(), user(), "attachment-1", false))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(transportFailure);
    }

    @Test
    void shouldNotTranslateSW360Exception_whenParsingAttachment() throws TException {
        // given -- unlike the other two methods, getLicenseInfoForAttachment has no
        // SW360Exception catch block, so a backend rejection is not mapped to HTTP 400
        SW360Exception backendRejection = new SW360Exception("Attachment not found");
        when(licenseInfoClient.getLicenseInfoForAttachment(release(), "attachment-1", true, user()))
                .thenThrow(backendRejection);

        // when / then
        assertThatThrownBy(() -> licenseInfoService
                .getLicenseInfoForAttachment(release(), user(), "attachment-1", true))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(backendRejection)
                .isNotInstanceOf(BadRequestClientException.class);
    }

    // =========================================================================
    //  Fixtures -- Thrift structs implement equals(), so fresh instances with the
    //  same content match both the stubbed arguments and the verifications.
    // =========================================================================

    private static Project project() {
        return new Project().setId("project-1").setName("Installer").setVersion("1.0");
    }

    private static Release release() {
        return new Release().setId("release-1").setName("Apache Commons").setVersion("3.2.1");
    }

    private static User user() {
        return new User().setId("user-1").setEmail("test@sw360.org");
    }

    private static Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds() {
        return Map.of("release-1", Map.of("attachment-1", true));
    }

    private static Map<String, Set<LicenseNameWithText>> excludedLicenses() {
        return Map.of("attachment-1", Set.of(new LicenseNameWithText().setLicenseName("GPL-2.0")));
    }
}
