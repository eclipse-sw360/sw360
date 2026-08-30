package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ReleaseRequestMapper {

    private static final ImmutableMap<Release._Fields, String[]> mapOfBackwardCompatible_Field_OldFieldNames_NewFieldNames = ImmutableMap.<Release._Fields, String[]>builder()
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
}
