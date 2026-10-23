/*
 * Copyright Kavya Popat, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licenseinfo;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Sw360LicenseInfoServiceTest {

    @Mock
    private LicenseInfoService.Client licenseInfoClient;

    private MockedStatic<ThriftClients> mockedThriftClients;
    private Sw360LicenseInfoService licenseInfoService;

    @BeforeEach
    void setUp() {
        mockedThriftClients = mockStatic(ThriftClients.class);
        mockedThriftClients.when(() -> ThriftClients.makeLicenseInfoClient())
                .thenReturn(licenseInfoClient);
        licenseInfoService = new Sw360LicenseInfoService();
    }

    @AfterEach
    void tearDown() {
        mockedThriftClients.close();
    }

    // =========================================================================
    // getOutputFormatInfoForGeneratorClass
    // =========================================================================

    @Test
    void getOutputFormatInfoForGeneratorClass_returnsOutputFormatInfo_whenClientSucceeds() throws TException {
        // Given
        String generatorClassName = "DocxGenerator";
        OutputFormatInfo expected = new OutputFormatInfo();
        expected.setGeneratorClassName(generatorClassName);
        expected.setFileExtension(".docx");
        expected.setDescription("Microsoft Word Document");
        expected.setIsOutputBinary(true);
        expected.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        expected.setVariant(OutputFormatVariant.REPORT);

        when(licenseInfoClient.getOutputFormatInfoForGeneratorClass(generatorClassName)).thenReturn(expected);

        // When
        OutputFormatInfo result = licenseInfoService.getOutputFormatInfoForGeneratorClass(generatorClassName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getGeneratorClassName()).isEqualTo(generatorClassName);
        assertThat(result.getFileExtension()).isEqualTo(".docx");
        verify(licenseInfoClient).getOutputFormatInfoForGeneratorClass(generatorClassName);
    }

    @Test
    void getOutputFormatInfoForGeneratorClass_wrapsSW360Exception_asBadRequestClientException() throws TException {
        // Given
        String generatorClassName = "InvalidGenerator";
        SW360Exception thriftException = new SW360Exception();
        thriftException.setWhy("Generator class not found: " + generatorClassName);
        thriftException.setErrorCode(4);

        when(licenseInfoClient.getOutputFormatInfoForGeneratorClass(generatorClassName)).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getOutputFormatInfoForGeneratorClass(generatorClassName))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining("Generator class not found")
                .hasCause(thriftException);
        verify(licenseInfoClient).getOutputFormatInfoForGeneratorClass(generatorClassName);
    }

    @Test
    void getOutputFormatInfoForGeneratorClass_wrapsTException_asRuntimeException() throws TException {
        // Given
        String generatorClassName = "DocxGenerator";
        TException thriftException = new TException("Thrift connection failed");

        when(licenseInfoClient.getOutputFormatInfoForGeneratorClass(generatorClassName)).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getOutputFormatInfoForGeneratorClass(generatorClassName))
                .isInstanceOf(RuntimeException.class)
                .hasCause(thriftException);
        verify(licenseInfoClient).getOutputFormatInfoForGeneratorClass(generatorClassName);
    }

    // =========================================================================
    // getLicenseInfoFile
    // =========================================================================

    @Test
    void getLicenseInfoFile_returnsLicenseInfoFile_whenExcludeReleaseVersionTrue() throws TException {
        // Given
        Project project = new Project();
        project.setId("project-123");
        project.setName("Test Project");

        User user = new User();
        user.setEmail("test@example.com");

        String generatorClassNameWithVariant = "DocxGenerator-REPORT";
        Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = Collections.emptyMap();
        Map<String, Set<LicenseNameWithText>> excludedLicenses = Collections.emptyMap();
        String externalIds = "";
        String fileName = "license-info.docx";
        boolean excludeReleaseVersion = true;

        LicenseInfoFile expected = new LicenseInfoFile();
        OutputFormatInfo outputFormatInfo = new OutputFormatInfo();
        outputFormatInfo.setGeneratorClassName("DocxGenerator");
        expected.setOutputFormatInfo(outputFormatInfo);
        expected.setGeneratedOutput(ByteBuffer.wrap("test content".getBytes()));

        when(licenseInfoClient.getLicenseInfoFileWithoutReleaseVersion(
                eq(project),
                eq(user),
                eq(generatorClassNameWithVariant),
                eq(selectedReleaseAndAttachmentIds),
                eq(excludedLicenses),
                eq(externalIds),
                eq(fileName),
                eq(excludeReleaseVersion)
        )).thenReturn(expected);

        // When
        LicenseInfoFile result = licenseInfoService.getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOutputFormatInfo().getGeneratorClassName()).isEqualTo("DocxGenerator");
        verify(licenseInfoClient).getLicenseInfoFileWithoutReleaseVersion(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        );
        verify(licenseInfoClient, never()).getLicenseInfoFile(
                any(Project.class), any(User.class), anyString(),
                anyMap(), anyMap(), anyString(), anyString()
        );
    }

    @Test
    void getLicenseInfoFile_returnsLicenseInfoFile_whenExcludeReleaseVersionFalse() throws TException {
        // Given
        Project project = new Project();
        project.setId("project-123");

        User user = new User();
        user.setEmail("test@example.com");

        String generatorClassNameWithVariant = "DocxGenerator-REPORT";
        Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = Collections.emptyMap();
        Map<String, Set<LicenseNameWithText>> excludedLicenses = Collections.emptyMap();
        String externalIds = "";
        String fileName = "license-info.docx";
        boolean excludeReleaseVersion = false;

        LicenseInfoFile expected = new LicenseInfoFile();
        OutputFormatInfo outputFormatInfo = new OutputFormatInfo();
        outputFormatInfo.setGeneratorClassName("DocxGenerator");
        expected.setOutputFormatInfo(outputFormatInfo);
        expected.setGeneratedOutput(ByteBuffer.wrap("test content".getBytes()));

        when(licenseInfoClient.getLicenseInfoFile(
                eq(project),
                eq(user),
                eq(generatorClassNameWithVariant),
                eq(selectedReleaseAndAttachmentIds),
                eq(excludedLicenses),
                eq(externalIds),
                eq(fileName)
        )).thenReturn(expected);

        // When
        LicenseInfoFile result = licenseInfoService.getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOutputFormatInfo().getGeneratorClassName()).isEqualTo("DocxGenerator");
        verify(licenseInfoClient).getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName
        );
        verify(licenseInfoClient, never()).getLicenseInfoFileWithoutReleaseVersion(
                any(Project.class), any(User.class), anyString(),
                anyMap(), anyMap(), anyString(), anyString(), anyBoolean()
        );
    }

    @Test
    void getLicenseInfoFile_wrapsSW360Exception_asBadRequestClientException_whenExcludeReleaseVersionTrue() throws TException {
        // Given
        Project project = new Project();
        project.setId("project-123");
        User user = new User();
        String generatorClassNameWithVariant = "DocxGenerator-REPORT";
        Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = Collections.emptyMap();
        Map<String, Set<LicenseNameWithText>> excludedLicenses = Collections.emptyMap();
        String externalIds = "";
        String fileName = "license-info.docx";
        boolean excludeReleaseVersion = true;

        SW360Exception thriftException = new SW360Exception();
        thriftException.setWhy("Failed to generate license info file");
        thriftException.setErrorCode(1);

        when(licenseInfoClient.getLicenseInfoFileWithoutReleaseVersion(
                eq(project),
                eq(user),
                eq(generatorClassNameWithVariant),
                eq(selectedReleaseAndAttachmentIds),
                eq(excludedLicenses),
                eq(externalIds),
                eq(fileName),
                eq(excludeReleaseVersion)
        )).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        ))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining("Failed to generate license info file")
                .hasCause(thriftException);
        verify(licenseInfoClient, never()).getLicenseInfoFile(
                any(Project.class), any(User.class), anyString(),
                anyMap(), anyMap(), anyString(), anyString()
        );
    }

    @Test
    void getLicenseInfoFile_wrapsSW360Exception_asBadRequestClientException_whenExcludeReleaseVersionFalse() throws TException {
        // Given
        Project project = new Project();
        project.setId("project-123");
        User user = new User();
        String generatorClassNameWithVariant = "DocxGenerator-REPORT";
        Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = Collections.emptyMap();
        Map<String, Set<LicenseNameWithText>> excludedLicenses = Collections.emptyMap();
        String externalIds = "";
        String fileName = "license-info.docx";
        boolean excludeReleaseVersion = false;

        SW360Exception thriftException = new SW360Exception();
        thriftException.setWhy("Failed to generate license info file");
        thriftException.setErrorCode(1);

        when(licenseInfoClient.getLicenseInfoFile(
                eq(project),
                eq(user),
                eq(generatorClassNameWithVariant),
                eq(selectedReleaseAndAttachmentIds),
                eq(excludedLicenses),
                eq(externalIds),
                eq(fileName)
        )).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        ))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining("Failed to generate license info file")
                .hasCause(thriftException);
        verify(licenseInfoClient, never()).getLicenseInfoFileWithoutReleaseVersion(
                any(Project.class), any(User.class), anyString(),
                anyMap(), anyMap(), anyString(), anyString(), anyBoolean()
        );
    }

    @Test
    void getLicenseInfoFile_wrapsTException_asRuntimeException_whenExcludeReleaseVersionTrue() throws TException {
        // Given
        Project project = new Project();
        project.setId("project-123");
        User user = new User();
        String generatorClassNameWithVariant = "DocxGenerator-REPORT";
        Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = Collections.emptyMap();
        Map<String, Set<LicenseNameWithText>> excludedLicenses = Collections.emptyMap();
        String externalIds = "";
        String fileName = "license-info.docx";
        boolean excludeReleaseVersion = true;

        TException thriftException = new TException("Thrift transport error");

        when(licenseInfoClient.getLicenseInfoFileWithoutReleaseVersion(
                eq(project),
                eq(user),
                eq(generatorClassNameWithVariant),
                eq(selectedReleaseAndAttachmentIds),
                eq(excludedLicenses),
                eq(externalIds),
                eq(fileName),
                eq(excludeReleaseVersion)
        )).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        ))
                .isInstanceOf(RuntimeException.class)
                .hasCause(thriftException);
        verify(licenseInfoClient, never()).getLicenseInfoFile(
                any(Project.class), any(User.class), anyString(),
                anyMap(), anyMap(), anyString(), anyString()
        );
    }

    @Test
    void getLicenseInfoFile_wrapsTException_asRuntimeException_whenExcludeReleaseVersionFalse() throws TException {
        // Given
        Project project = new Project();
        project.setId("project-123");
        User user = new User();
        String generatorClassNameWithVariant = "DocxGenerator-REPORT";
        Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = Collections.emptyMap();
        Map<String, Set<LicenseNameWithText>> excludedLicenses = Collections.emptyMap();
        String externalIds = "";
        String fileName = "license-info.docx";
        boolean excludeReleaseVersion = false;

        TException thriftException = new TException("Thrift transport error");

        when(licenseInfoClient.getLicenseInfoFile(
                eq(project),
                eq(user),
                eq(generatorClassNameWithVariant),
                eq(selectedReleaseAndAttachmentIds),
                eq(excludedLicenses),
                eq(externalIds),
                eq(fileName)
        )).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoFile(
                project, user, generatorClassNameWithVariant,
                selectedReleaseAndAttachmentIds, excludedLicenses,
                externalIds, fileName, excludeReleaseVersion
        ))
                .isInstanceOf(RuntimeException.class)
                .hasCause(thriftException);
        verify(licenseInfoClient, never()).getLicenseInfoFileWithoutReleaseVersion(
                any(Project.class), any(User.class), anyString(),
                anyMap(), anyMap(), anyString(), anyString(), anyBoolean()
        );
    }

    // =========================================================================
    // getLicenseInfoForAttachment
    // =========================================================================

    @Test
    void getLicenseInfoForAttachment_returnsParsingResults_whenClientSucceeds() throws TException {
        // Given
        Release release = new Release();
        release.setId("release-123");
        release.setName("Test Release");
        release.setVersion("1.0.0");

        User user = new User();
        user.setEmail("test@example.com");

        String attachmentContentId = "attachment-456";
        boolean includeConcludedLicense = true;

        LicenseInfoParsingResult result1 = new LicenseInfoParsingResult();
        result1.setStatus(LicenseInfoRequestStatus.SUCCESS);
        result1.setMessage("Parsed successfully");

        List<LicenseInfoParsingResult> expected = List.of(result1);

        when(licenseInfoClient.getLicenseInfoForAttachment(
                eq(release),
                eq(attachmentContentId),
                eq(includeConcludedLicense),
                eq(user)
        )).thenReturn(expected);

        // When
        List<LicenseInfoParsingResult> result = licenseInfoService.getLicenseInfoForAttachment(
                release, user, attachmentContentId, includeConcludedLicense
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(LicenseInfoRequestStatus.SUCCESS);
        verify(licenseInfoClient).getLicenseInfoForAttachment(release, attachmentContentId, includeConcludedLicense, user);
    }

    @Test
    void getLicenseInfoForAttachment_returnsEmptyList_whenClientReturnsEmptyList() throws TException {
        // Given
        Release release = new Release();
        release.setId("release-123");
        User user = new User();
        String attachmentContentId = "attachment-456";
        boolean includeConcludedLicense = false;

        when(licenseInfoClient.getLicenseInfoForAttachment(
                eq(release),
                eq(attachmentContentId),
                eq(includeConcludedLicense),
                eq(user)
        )).thenReturn(Collections.emptyList());

        // When
        List<LicenseInfoParsingResult> result = licenseInfoService.getLicenseInfoForAttachment(
                release, user, attachmentContentId, includeConcludedLicense
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(licenseInfoClient).getLicenseInfoForAttachment(release, attachmentContentId, includeConcludedLicense, user);
    }

    @Test
    void getLicenseInfoForAttachment_wrapsTException_asRuntimeException() throws TException {
        // Given
        Release release = new Release();
        release.setId("release-123");
        User user = new User();
        String attachmentContentId = "attachment-456";
        boolean includeConcludedLicense = true;

        TException thriftException = new TException("Thrift connection failed");

        when(licenseInfoClient.getLicenseInfoForAttachment(
                eq(release),
                eq(attachmentContentId),
                eq(includeConcludedLicense),
                eq(user)
        )).thenThrow(thriftException);

        // When / Then
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoForAttachment(
                release, user, attachmentContentId, includeConcludedLicense
        ))
                .isInstanceOf(RuntimeException.class)
                .hasCause(thriftException);
    }

    @Test
    void getLicenseInfoForAttachment_wrapsSW360Exception_asRuntimeException() throws TException {
        // Given
        Release release = new Release();
        release.setId("release-123");
        User user = new User();
        String attachmentContentId = "attachment-456";
        boolean includeConcludedLicense = true;

        SW360Exception thriftException = new SW360Exception();
        thriftException.setWhy("License info parsing failed");
        thriftException.setErrorCode(2);

        when(licenseInfoClient.getLicenseInfoForAttachment(
                eq(release),
                eq(attachmentContentId),
                eq(includeConcludedLicense),
                eq(user)
        )).thenThrow(thriftException);

        // When / Then
        // SW360Exception extends TException in Thrift-generated code, so it is caught by catch(TException)
        // and wrapped in RuntimeException
        assertThatThrownBy(() -> licenseInfoService.getLicenseInfoForAttachment(
                release, user, attachmentContentId, includeConcludedLicense
        ))
                .isInstanceOf(RuntimeException.class)
                .hasCause(thriftException);
    }
}
