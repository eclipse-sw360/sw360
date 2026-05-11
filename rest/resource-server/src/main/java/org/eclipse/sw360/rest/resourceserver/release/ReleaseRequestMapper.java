/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.release;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReleaseRequestMapper {

    private static final ImmutableMap<Release._Fields, String[]> BACKWARD_COMPATIBLE_FIELDS =
            ImmutableMap.<Release._Fields, String[]>builder()
                    .put(Release._Fields.SOURCE_CODE_DOWNLOADURL, new String[]{"downloadurl", "sourceCodeDownloadurl"})
                    .build();

    private final Sw360AttachmentService attachmentService;
    private final com.fasterxml.jackson.databind.Module sw360Module;

    public Release toRelease(Map<String, Object> requestBody) {
        Map<String, Object> requestCopy = new HashMap<>(requestBody);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        Set<Attachment> attachments = attachmentService.getAttachmentsFromRequest(requestCopy.get("attachments"), mapper);
        if (requestCopy.get("attachments") != null) {
            requestCopy.remove("attachments");
        }

        Release release = mapper.convertValue(requestCopy, Release.class);
        if (attachments != null) {
            release.setAttachments(attachments);
        }

        BACKWARD_COMPATIBLE_FIELDS.forEach((field, aliases) -> {
            String oldFieldName = aliases[0];
            String newFieldName = aliases[1];
            if (!requestCopy.containsKey(newFieldName) && requestCopy.containsKey(oldFieldName)) {
                release.setFieldValue(field, CommonUtils.nullToEmptyString(requestCopy.get(oldFieldName)));
            }
        });

        return release;
    }

    public Release normalizeCreateRequest(Release release) throws URISyntaxException {
        if (release.isSetComponentId()) {
            release.setComponentId(extractId(release.getComponentId()));
        }
        if (release.isSetVendorId()) {
            release.setVendorId(extractId(release.getVendorId()));
        }
        if (release.getMainLicenseIds() != null) {
            Set<String> normalizedMainLicenseIds = new HashSet<>();
            for (String licenseUri : release.getMainLicenseIds()) {
                normalizedMainLicenseIds.add(extractId(licenseUri));
            }
            release.setMainLicenseIds(normalizedMainLicenseIds);
        }
        return release;
    }

    private String extractId(String uriString) throws URISyntaxException {
        URI uri = new URI(uriString);
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
