/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.AdsClearingAssessment;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.AdsInformation;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.AdsReleaseReference;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ADSParser extends LicenseInfoParser {

    private static final Logger log = LogManager.getLogger(ADSParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String[] ADS_CANDIDATE_FIELDS = {"candidate release", "candidateRelease", "candidate_release", "candidate"};
    private static final String[] ADS_BASE_FIELDS = {"base release", "baseRelease", "base_release", "base"};
    private static final String[] ADS_ASSESSMENT_FIELDS = {"clearing assessment", "clearingAssessment", "clearing_assessment", "assessment"};
    private static final String[] ADS_LICENSE_CHANGE_FIELDS = {"files_with_license_changes", "licenseChanges", "license_changes"};
    private static final String[] ADS_COPYRIGHT_CHANGE_FIELDS = {"files_with_copyright_changes", "copyrightChanges", "copyright_changes"};
    private static final String[] ADS_DELETED_FILE_FIELDS = {"deleted_files", "deletedFiles", "deleted_files"};
    private static final String[] ADS_RENAMED_FILE_FIELDS = {"renamed_files", "renamedFiles", "renamed_files"};
    private static final String[] ADS_CANDIDATE_RELEASE_NAME_FIELDS = {"release_name", "releaseName", "release_name", "candidate_release_name", "name"};
    private static final String[] ADS_CANDIDATE_RELEASE_ID_FIELDS = {"release_id", "releaseId", "release_id", "candidate_release_id", "id"};
    private static final String[] ADS_CANDIDATE_RELEASE_VERSION_FIELDS = {"version"};
    private static final String[] ADS_CANDIDATE_RELEASE_CHANGED_FILES_FIELDS = {"changed files", "changedFilesCount", "changed_files_count", "changed_files", "changed files count"};
    private static final String[] ADS_BASE_RELEASE_NAME_FIELDS = {"base_release_name", "baseReleaseName", "base_release_name", "base release name", "releaseName", "release_name", "name"};
    private static final String[] ADS_BASE_RELEASE_ID_FIELDS = {"base_release_id", "baseReleaseId", "base_release_id", "base release id", "releaseId", "release_id", "id"};
    private static final String[] ADS_BASE_RELEASE_VERSION_FIELDS = {"base_release_version", "baseReleaseVersion", "base_release_version", "base release version", "version"};
    private static final String[] ADS_CLEARING_REQUIRED_FIELDS = {"clearingRequired", "clearing_required"};
    private static final String[] ADS_LICENSE_CHANGES_COUNT_FIELDS = {"licenseChangesCount", "license_changes_count", "number_of_files_with_license_changes"};
    private static final String[] ADS_COPYRIGHT_CHANGES_COUNT_FIELDS = {"copyrightChangesCount", "copyright_changes_count", "number_of_files_with_copyright_changes"};
    private static final String[] ADS_DELETED_FILES_COUNT_FIELDS = {"deletedFilesCount", "deleted_files_count", "number_of_deleted_files"};
    private static final String[] ADS_RENAMED_FILES_COUNT_FIELDS = {"renamedFilesCount", "renamed_files_count", "number_of_renamed_files"};
    private static final String[] ADS_CLX_AUTO_UPDATE_REQUIRED_FIELDS = {"clxAutoUpdateRequired", "clx_auto_update_required", "CLX_file_auto-update_required"};

    public ADSParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider) {
        super(attachmentConnector, attachmentContentProvider);
    }

    @Override
    public List<String> getApplicableFileExtensions() {
        return Collections.emptyList();
    }

    @Override
    public <T> boolean isApplicableTo(Attachment attachment, User user, T context) throws TException {
        return AttachmentType.ADS_JSON.equals(attachment.getAttachmentType());
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context) {
        return Collections.emptyList();
    }

    public <T> AdsInformation getAdsInformation(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        try (InputStream attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context)) {
            String adsJson = sanitizeAdsJson(new String(attachmentStream.readAllBytes(), StandardCharsets.UTF_8));
            JsonNode root = OBJECT_MAPPER.readTree(adsJson);
            return buildAdsInformation(root, attachment);
        } catch (IOException e) {
            log.error("Failed to parse ADS JSON attachment {}", attachmentContent.getFilename(), e);
            throw new SW360Exception("Failed to parse ADS JSON attachment").setErrorCode(400);
        }
    }

    @Override
    public <T> ObligationParsingResult getObligations(Attachment attachment, User user, T context) {
        return new ObligationParsingResult().setStatus(ObligationInfoRequestStatus.NO_APPLICABLE_SOURCE);
    }

    private String sanitizeAdsJson(String adsJson) {
        if (adsJson == null) {
            return null;
        }
        return adsJson.replace('\u00A0', ' ').replace("\uFEFF", "");
    }

    private AdsInformation buildAdsInformation(JsonNode root, Attachment attachment) {
        AdsReleaseReference candidateRelease = extractReleaseReference(
                root, ADS_CANDIDATE_FIELDS, ADS_CANDIDATE_RELEASE_NAME_FIELDS, ADS_CANDIDATE_RELEASE_ID_FIELDS,
                ADS_CANDIDATE_RELEASE_VERSION_FIELDS, ADS_CANDIDATE_RELEASE_CHANGED_FILES_FIELDS);
        AdsReleaseReference baseRelease = extractReleaseReference(
                root, ADS_BASE_FIELDS, ADS_BASE_RELEASE_NAME_FIELDS, ADS_BASE_RELEASE_ID_FIELDS,
                ADS_BASE_RELEASE_VERSION_FIELDS, ADS_CANDIDATE_RELEASE_CHANGED_FILES_FIELDS);

        JsonNode clearingAssessmentNode = findObjectNode(root, ADS_ASSESSMENT_FIELDS);
        List<Map<String, String>> licenseChanges = extractSectionListWithFallback(clearingAssessmentNode, root, ADS_LICENSE_CHANGE_FIELDS);
        List<Map<String, String>> copyrightChanges = extractSectionListWithFallback(clearingAssessmentNode, root, ADS_COPYRIGHT_CHANGE_FIELDS);
        List<Map<String, String>> deletedFiles = extractSectionListWithFallback(clearingAssessmentNode, root, ADS_DELETED_FILE_FIELDS);
        List<Map<String, String>> renamedFiles = extractSectionListWithFallback(clearingAssessmentNode, root, ADS_RENAMED_FILE_FIELDS);

        AdsClearingAssessment clearingAssessment = new AdsClearingAssessment();
        if (!clearingAssessment.isSetLicenseChangesCount()) {
            Integer count = readInteger(clearingAssessmentNode, ADS_LICENSE_CHANGES_COUNT_FIELDS);
            if (count == null) {
                count = readInteger(root, ADS_LICENSE_CHANGES_COUNT_FIELDS);
            }
            clearingAssessment.setLicenseChangesCount(count != null ? count : licenseChanges.size());
        }
        if (!clearingAssessment.isSetCopyrightChangesCount()) {
            Integer count = readInteger(clearingAssessmentNode, ADS_COPYRIGHT_CHANGES_COUNT_FIELDS);
            if (count == null) {
                count = readInteger(root, ADS_COPYRIGHT_CHANGES_COUNT_FIELDS);
            }
            clearingAssessment.setCopyrightChangesCount(count != null ? count : copyrightChanges.size());
        }
        if (!clearingAssessment.isSetDeletedFilesCount()) {
            Integer count = readInteger(clearingAssessmentNode, ADS_DELETED_FILES_COUNT_FIELDS);
            if (count == null) {
                count = readInteger(root, ADS_DELETED_FILES_COUNT_FIELDS);
            }
            clearingAssessment.setDeletedFilesCount(count != null ? count : deletedFiles.size());
        }
        if (!clearingAssessment.isSetRenamedFilesCount()) {
            Integer count = readInteger(clearingAssessmentNode, ADS_RENAMED_FILES_COUNT_FIELDS);
            if (count == null) {
                count = readInteger(root, ADS_RENAMED_FILES_COUNT_FIELDS);
            }
            clearingAssessment.setRenamedFilesCount(count != null ? count : renamedFiles.size());
        }
        if (!clearingAssessment.isSetClearingRequired()) {
            Boolean value = readBoolean(clearingAssessmentNode, ADS_CLEARING_REQUIRED_FIELDS);
            if (value == null) {
                value = readBoolean(root, ADS_CLEARING_REQUIRED_FIELDS);
            }
            if (value != null) {
                clearingAssessment.setClearingRequired(value);
            }
        }
        if (!clearingAssessment.isSetClxAutoUpdateRequired()) {
            Boolean value = readBoolean(clearingAssessmentNode, ADS_CLX_AUTO_UPDATE_REQUIRED_FIELDS);
            if (value == null) {
                value = readBoolean(root, ADS_CLX_AUTO_UPDATE_REQUIRED_FIELDS);
            }
            if (value != null) {
                clearingAssessment.setClxAutoUpdateRequired(value);
            }
        }

        return new AdsInformation()
                .setAttachmentContentId(attachment.getAttachmentContentId())
                .setAttachmentFilename(attachment.getFilename())
                .setCandidateRelease(candidateRelease)
                .setBaseRelease(baseRelease)
                .setClearingAssessment(clearingAssessment)
                .setLicenseChanges(licenseChanges)
                .setCopyrightChanges(copyrightChanges)
                .setDeletedFiles(deletedFiles)
                .setRenamedFiles(renamedFiles);
    }

    private AdsReleaseReference extractReleaseReference(
            JsonNode root,
            String[] sectionFieldNames,
            String[] nameFieldNames,
            String[] idFieldNames,
            String[] versionFieldNames,
            String[] changedFilesFieldNames) {
        JsonNode referenceNode = findObjectNode(root, sectionFieldNames);
        if (referenceNode == null) {
            return null;
        }

        JsonNode releaseNode = referenceNode.has("release") && referenceNode.get("release").isObject()
                ? referenceNode.get("release")
                : referenceNode;

        AdsReleaseReference releaseReference = new AdsReleaseReference()
                .setReleaseId(readString(releaseNode, idFieldNames))
                .setReleaseName(readString(releaseNode, nameFieldNames))
                .setVersion(readString(releaseNode, versionFieldNames));

        Integer changedFilesCount = readInteger(releaseNode, changedFilesFieldNames);
        if (changedFilesCount != null) {
            releaseReference.setChangedFilesCount(changedFilesCount);
        }

        return releaseReference;
    }

    private List<Map<String, String>> extractSectionListWithFallback(JsonNode preferredRoot, JsonNode fallbackRoot, String... fieldNames) {
        List<Map<String, String>> preferred = extractSectionList(preferredRoot, fieldNames);
        if (!preferred.isEmpty() || fallbackRoot == null || fallbackRoot == preferredRoot) {
            return preferred;
        }
        return extractSectionList(fallbackRoot, fieldNames);
    }

    private List<Map<String, String>> extractSectionList(JsonNode root, String... fieldNames) {
        JsonNode sectionNode = findArrayNode(root, fieldNames);
        if (sectionNode == null) {
            return Collections.emptyList();
        }

        List<Map<String, String>> sectionEntries = new ArrayList<>();
        for (JsonNode entry : sectionNode) {
            if (entry == null || entry.isNull()) {
                continue;
            }
            if (entry.isObject()) {
                sectionEntries.add(OBJECT_MAPPER.convertValue(entry, new TypeReference<LinkedHashMap<String, String>>() {}));
            } else {
                Map<String, String> entryMap = new LinkedHashMap<>();
                entryMap.put("value", entry.asText());
                sectionEntries.add(entryMap);
            }
        }
        return sectionEntries;
    }

    private JsonNode findObjectNode(JsonNode root, String... fieldNames) {
        if (root == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (root.has(fieldName) && root.get(fieldName).isObject()) {
                return root.get(fieldName);
            }
        }
        return null;
    }

    private JsonNode findArrayNode(JsonNode root, String... fieldNames) {
        if (root == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (root.has(fieldName)) {
                JsonNode fieldNode = root.get(fieldName);
                if (fieldNode != null && fieldNode.isArray()) {
                    return fieldNode;
                }
                if (fieldNode != null && fieldNode.isObject()) {
                    for (String childField : List.of("items", "entries", "values", "changes")) {
                        if (fieldNode.has(childField) && fieldNode.get(childField).isArray()) {
                            return fieldNode.get(childField);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String readString(JsonNode node, String... fieldNames) {
        JsonNode valueNode = findValueNode(node, fieldNames);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.asText();
    }

    private Integer readInteger(JsonNode node, String... fieldNames) {
        JsonNode valueNode = findValueNode(node, fieldNames);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isNumber()) {
            return valueNode.intValue();
        }
        String text = valueNode.asText();
        if (Strings.isNullOrEmpty(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean readBoolean(JsonNode node, String... fieldNames) {
        JsonNode valueNode = findValueNode(node, fieldNames);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isBoolean()) {
            return valueNode.booleanValue();
        }
        String text = valueNode.asText();
        if (Strings.isNullOrEmpty(text)) {
            return null;
        }
        if ("yes".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text) || "1".equals(text)) {
            return Boolean.TRUE;
        }
        if ("no".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text) || "0".equals(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private JsonNode findValueNode(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return node.get(fieldName);
            }
        }
        return null;
    }

}
