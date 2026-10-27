/*
 * Copyright Siemens AG, 2026.
 * Copyright Sandip Mandal<sandipmandal02.sm@gmail.com>, 2026.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReleaseRequestMapper {
    private static final int MAX_BATCH_SUMMARY_IDS = 200;

    static final ImmutableMap<Release._Fields, String[]> mapOfBackwardCompatible_Field_OldFieldNames_NewFieldNames = ImmutableMap.<Release._Fields, String[]>builder()
            .put(Release._Fields.SOURCE_CODE_DOWNLOADURL, new String[] { "downloadurl", "sourceCodeDownloadurl" })
            .build();
    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @NonNull
    private final Sw360AttachmentService attachmentService;

    public ReleaseRequestMapper(@NonNull Module sw360Module, @NonNull Sw360AttachmentService attachmentService) {
        this.sw360Module = sw360Module;
        this.attachmentService = attachmentService;
    }

    Release setBackwardCompatibleFieldsInRelease(Map<String, Object> reqBodyMap) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        Set<Attachment> attachments = attachmentService.getAttachmentsFromRequest(reqBodyMap.get("attachments"), mapper);
        if (null != reqBodyMap.get("attachments")) {
            reqBodyMap.remove("attachments");
        }
        // Not a Release field - exclude from conversion.
        reqBodyMap.remove("comment");
        Release release = mapper.convertValue(reqBodyMap, Release.class);
        if (null != attachments) {
            release.setAttachments(attachments);
        }

        mapOfBackwardCompatible_Field_OldFieldNames_NewFieldNames.entrySet().stream().forEach(entry -> {
            Release._Fields field = entry.getKey();
            String oldFieldName = entry.getValue()[0];
            String newFieldName = entry.getValue()[1];
            if (!reqBodyMap.containsKey(newFieldName) && reqBodyMap.containsKey(oldFieldName)) {
                release.setFieldValue(field, CommonUtils.nullToEmptyString(reqBodyMap.get(oldFieldName)));
            }
        });

        return release;
    }

    String extractModerationComment(Map<String, Object> reqBodyMap) {
        Object comment = reqBodyMap.get("comment");
        if (comment == null) {
            return null;
        }
        String commentStr = comment.toString().trim();
        return commentStr.isEmpty() ? null : commentStr;
    }

    List<String> extractReleaseBatchSummaryIds(Map<String, Object> reqBodyMap) {
        if (reqBodyMap == null || !reqBodyMap.containsKey("ids")) {
            throw new BadRequestClientException("The request body must contain an 'ids' array.");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.registerModule(sw360Module);
            return mapper.convertValue(reqBodyMap.get("ids"), new TypeReference<List<String>>() {});
        } catch (IllegalArgumentException e) {
            throw new BadRequestClientException("The 'ids' field must be an array of release IDs.");
        }
    }

     LinkedHashSet<String> normalizeReleaseBatchSummaryIds(List<String> releaseIds) {
        if (releaseIds == null) {
            throw new BadRequestClientException("The 'ids' field must be an array of release IDs.");
        }

        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();
        for (String releaseId : releaseIds) {
            if (StringUtils.isBlank(releaseId)) {
                throw new BadRequestClientException("The 'ids' field must not contain blank values.");
            }
            normalizedIds.add(releaseId);
        }

        if (normalizedIds.size() > MAX_BATCH_SUMMARY_IDS) {
            throw new BadRequestClientException("A maximum of " + MAX_BATCH_SUMMARY_IDS + " unique release IDs is allowed.");
        }

        return normalizedIds;
    }
}
