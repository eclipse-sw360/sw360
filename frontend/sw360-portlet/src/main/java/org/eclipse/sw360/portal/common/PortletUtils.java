/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;

import com.google.common.collect.Sets;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.meta_data.FieldMetaData;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.cvesearch.UpdateType;
import org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;

/**
 * Portlet helpers
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class PortletUtils {

    private static final Logger LOGGER = Logger.getLogger(PortletUtils.class);

    private PortletUtils() {
        // Utility class with only static functions
    }

    public static ComponentType getComponentTypefromString(String enumNumber) {
        return ComponentType.findByValue(parseInt(enumNumber));
    }

    public static ClearingState getClearingStatefromString(String enumNumber) {
        return ClearingState.findByValue(parseInt(enumNumber));
    }

    public static RepositoryType getRepositoryTypefromString(String enumNumber) {
        return RepositoryType.findByValue(parseInt(enumNumber));
    }

    public static MainlineState getMainlineStatefromString(String enumNumber) {
        return MainlineState.findByValue(parseInt(enumNumber));
    }

    public static ModerationState getModerationStatusfromString(String enumNumber) {
        return ModerationState.findByValue(parseInt(enumNumber));
    }

    public static AttachmentType getAttachmentTypefromString(String enumNumber) {
        return AttachmentType.findByValue(parseInt(enumNumber));
    }
    public static CheckStatus getCheckStatusfromString(String enumNumber) {
        return CheckStatus.findByValue(parseInt(enumNumber));
    }

    public static ProjectState getProjectStateFromString(String enumNumber) {
        return ProjectState.findByValue(parseInt(enumNumber));
    }

    public static ProjectClearingState getProjectClearingStateFromString(String enumNumber) {
        return ProjectClearingState.findByValue(parseInt(enumNumber));
    }

    public static ProjectType getProjectTypeFromString(String enumNumber) {
        return ProjectType.findByValue(parseInt(enumNumber));
    }
    public static Visibility getVisibilityFromString(String enumNumber) {
        return  Visibility.findByValue(parseInt(enumNumber));
    }

    public static UserGroup getUserGroupFromString(String enumNumber) {
        return  UserGroup.findByValue(parseInt(enumNumber));
    }
    public static ECCStatus getEccStatusFromString(String enumNumber) {
        return  ECCStatus.findByValue(parseInt(enumNumber));
    }

    public static <U extends TFieldIdEnum, T extends TBase<T, U>> void setFieldValue(PortletRequest request, T instance, U field, FieldMetaData fieldMetaData, String prefix) {

        String value = request.getParameter(prefix + field.toString());
        switch (fieldMetaData.valueMetaData.type) {

            case org.apache.thrift.protocol.TType.SET:
                instance.setFieldValue(field, CommonUtils.splitToSet(nullToEmptyString(value)));
                break;
            case org.apache.thrift.protocol.TType.ENUM:
                if (!"".equals(nullToEmptyString(value))){
                    instance.setFieldValue(field, enumFromString(value, field));
                }
                break;
            case org.apache.thrift.protocol.TType.I32:
                if (!"".equals(nullToEmptyString(value))){
                    instance.setFieldValue(field, Integer.parseInt(value));
                }
                break;
            case org.apache.thrift.protocol.TType.BOOL:
                instance.setFieldValue(field, !"".equals(nullToEmptyString(value)));
                break;
            default:
                if (value != null) {
                    instance.setFieldValue(field, value);
                }
        }
    }

    private static <U extends TFieldIdEnum> Object enumFromString(String value, U field) {
        if (field == Release._Fields.CLEARING_STATE)
            return getClearingStatefromString(value);
        else if (field == Component._Fields.COMPONENT_TYPE)
            return getComponentTypefromString(value);
        else if (field == Repository._Fields.REPOSITORYTYPE)
            return getRepositoryTypefromString(value);
        else if (field == Release._Fields.MAINLINE_STATE)
            return getMainlineStatefromString(value);
        else if (field == Project._Fields.STATE)
            return getProjectStateFromString(value);
        else if (field == Project._Fields.CLEARING_STATE)
            return getProjectClearingStateFromString(value);
        else if (field == Project._Fields.PROJECT_TYPE)
            return getProjectTypeFromString(value);
        else if (field == Project._Fields.VISBILITY)
            return getVisibilityFromString(value);
        else if (field == User._Fields.USER_GROUP)
            return getUserGroupFromString(value);
        else if (field == EccInformation._Fields.ECC_STATUS)
            return getEccStatusFromString(value);
        else {
            LOGGER.error("Missing case in enumFromString, unknown field was " + field.toString());
            return null;
        }
    }

    /**
     * Returns a set of updated attachments from the given request.
     *
     * This function will also take a set of existing attachments that will be
     * updated according to the request. A new set with the updated attachments will
     * be returned.
     *
     * If some of the attachments in the given set are not present in the request,
     * they will not be part of the returned set.
     *
     * Note: the given set will not be changed. However, the containing attachments
     * are changed during update.
     *
     * @param request
     *            request to parse for attachments
     * @param documentAttachments
     *            existing attachments
     *
     * @return set of updated attachments present in the request
     */
    public static Set<Attachment> updateAttachmentsFromRequest(PortletRequest request, Set<Attachment> documentAttachments) {
        Set<Attachment> attachments = Sets.newHashSet();

        User user = UserCacheHolder.getUserFromRequest(request);
        String[] ids = request
                .getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.ATTACHMENT_CONTENT_ID.toString());
        String[] fileNames = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.FILENAME.toString());
        String[] types = request
                .getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.ATTACHMENT_TYPE.toString());
        String[] createdComments = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CREATED_COMMENT.toString());
        String[] checkStatuses = request
                .getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CHECK_STATUS.toString());
        String[] checkedComments = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CHECKED_COMMENT.toString());

        if (ids == null || ids.length == 0) {
            LOGGER.info("No ids transmitted. All attachments will be deleted.");
            return attachments;
        } else if (CommonUtils.oneIsNull(fileNames, types, createdComments, checkStatuses, checkedComments)) {
            LOGGER.error("Invalid request content. One of the attachment parameters is null. No attachments will be saved or deleted.");
            return documentAttachments;
        } else if (!CommonUtils.allHaveSameLength(ids, fileNames, types, createdComments, checkStatuses, checkedComments)) {
            LOGGER.error("Not all of the attachment parameter arrays have the same length! No attachments will be saved or deleted.");
            return documentAttachments;
        }

        Map<String, Attachment> documentAttachmentMap = CommonUtils.nullToEmptySet(documentAttachments).stream()
                .collect(Collectors.toMap(Attachment::getAttachmentContentId, Function.identity()));
        for (int i = 0; i < ids.length; ++i) {
            String id = ids[i];
            Attachment attachment = documentAttachmentMap.get(id);

            if (attachment == null) {
                // the sha1 checksum is not computed here, but in the backend, when updating the
                // component in the database
                attachment = CommonUtils.getNewAttachment(user, id, fileNames[i]);
                documentAttachmentMap.put(attachment.getAttachmentContentId(), attachment);
            }

            // Filename is not overwritten. Unknown reason.
            attachment.setAttachmentType(getAttachmentTypefromString(types[i]));
            attachment.setCreatedComment(createdComments[i]);

            if (getCheckStatusfromString(checkStatuses[i]) != CheckStatus.NOTCHECKED) {
                if (attachment.checkStatus != getCheckStatusfromString(checkStatuses[i])
                        || !checkedComments[i].equals(attachment.checkedComment)) {
                    attachment.setCheckedOn(SW360Utils.getCreatedOn());
                    attachment.setCheckedBy(UserCacheHolder.getUserFromRequest(request).getEmail());
                    attachment.setCheckedTeam(UserCacheHolder.getUserFromRequest(request).getDepartment());
                    attachment.setCheckedComment(checkedComments[i]);
                }
            } else {
                attachment.setCheckedOn(null);
                attachment.setCheckedBy(null);
                attachment.setCheckedTeam(null);
                attachment.setCheckedComment("");
            }
            attachment.setCheckStatus(getCheckStatusfromString(checkStatuses[i]));

            // add attachments to list of added/modified attachments. This way deleted
            // attachments are automatically not in the set
            attachments.add(attachment);
        }

        return attachments;
    }


    public static Release cloneRelease(String emailFromRequest, Release release) {

        Release newRelease = release.deepCopy();

        //new DB object
        newRelease.unsetId();
        newRelease.unsetRevision();

        //new Owner
        newRelease.setCreatedBy(emailFromRequest);
        newRelease.setCreatedOn(SW360Utils.getCreatedOn());

        //release specifics
        newRelease.unsetCpeid();
        newRelease.unsetAttachments();
        newRelease.unsetClearingInformation();

        return newRelease;
    }

    public static Project cloneProject(String emailFromRequest, String department, Project project) {

        Project newProject = project.deepCopy();

        //new DB object
        newProject.unsetId();
        newProject.unsetRevision();

        //new Owner
        newProject.setCreatedBy(emailFromRequest);
        newProject.setCreatedOn(SW360Utils.getCreatedOn());
        newProject.setBusinessUnit(department);

        //project specifics
        newProject.unsetAttachments();
        newProject.setClearingState(ProjectClearingState.OPEN);

        return newProject;
    }

    public static JSONObject importStatusToJSON(VulnerabilityUpdateStatus updateStatus) {
        JSONObject responseData = JSONFactoryUtil.createJSONObject();
        if(! updateStatus.isSetStatusToVulnerabilityIds() || ! updateStatus.isSetRequestStatus()){
            responseData.put(PortalConstants.REQUEST_STATUS, PortalConstants.RESPONSE__IMPORT_GENERAL_FAILURE);
            return responseData;
        }
        if (updateStatus.getRequestStatus().equals(RequestStatus.FAILURE)
                && nullToEmptyList(updateStatus.getStatusToVulnerabilityIds().get(UpdateType.FAILED)).size() == 0) {
            responseData.put(PortalConstants.REQUEST_STATUS, PortalConstants.RESPONSE__IMPORT_GENERAL_FAILURE);
            return responseData;
        }
        responseData.put(PortalConstants.REQUEST_STATUS, updateStatus.getRequestStatus().toString());
        JSONArray jsonFailedIds = JSONFactoryUtil.createJSONArray();
        JSONArray jsonNewIds = JSONFactoryUtil.createJSONArray();
        JSONArray jsonUpdatedIds = JSONFactoryUtil.createJSONArray();

        updateStatus.getStatusToVulnerabilityIds().get(UpdateType.FAILED).forEach(jsonFailedIds::put);
        updateStatus.getStatusToVulnerabilityIds().get(UpdateType.NEW).forEach(jsonNewIds::put);
        updateStatus.getStatusToVulnerabilityIds().get(UpdateType.UPDATED).forEach(jsonUpdatedIds::put);

        responseData.put(PortalConstants.UPDATE_VULNERABILITIES__FAILED_IDS, jsonFailedIds);
        responseData.put(PortalConstants.UPDATE_VULNERABILITIES__NEW_IDS, jsonNewIds);
        responseData.put(PortalConstants.UPDATE_VULNERABILITIES__UPDATED_IDS, jsonUpdatedIds);
        return responseData;
    }

    public static VerificationState getVerificationState(VulnerabilityDTO vul){
        if(vul.isSetReleaseVulnerabilityRelation()){
            if(vul.getReleaseVulnerabilityRelation().isSetVerificationStateInfo()){
                List<VerificationStateInfo> history = vul.getReleaseVulnerabilityRelation().getVerificationStateInfo();
                if(history.size()>0) {
                    return history.get(history.size() - 1).getVerificationState();
                }
            }
        }
        return VerificationState.NOT_CHECKED;
    }

    public static Map<String,Set<String>> getCustomMapFromRequest(PortletRequest request) {
        return getCustomMapFromRequest(request, PortalConstants.CUSTOM_MAP_KEY, PortalConstants.CUSTOM_MAP_VALUE);
    }

    public static Map<String,Set<String>> getCustomMapFromRequest(PortletRequest request, String mapKey, String mapValue) {
        Map<String, Set<String>> customMap = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        List<String> keyAndValueParameterIds = Collections.list(parameterNames).stream()
                .filter(p -> p.startsWith(mapKey))
                .map(s -> s.replace(mapKey, ""))
                .collect(Collectors.toList());
        for(String parameterId : keyAndValueParameterIds) {
            String key = request.getParameter(mapKey +parameterId);
            if(isNullEmptyOrWhitespace(key)){
                LOGGER.error("Empty map key found");
            } else {
                String value = request.getParameter(mapValue + parameterId);
                if(!customMap.containsKey(key)){
                    customMap.put(key, new HashSet<>());
                }
                customMap.get(key).add(value);
            }
        }
        return customMap;
    }

    public static Map<String,String> getMapFromRequest(PortletRequest request, String key, String value) {
        Map<String, Set<String>> customMap = getCustomMapFromRequest(request, key, value);
        return customMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .findFirst()
                                .orElse("")));
    }

    public static Map<String,String> getExternalIdMapFromRequest(PortletRequest request) {
        return getMapFromRequest(request, PortalConstants.EXTERNAL_ID_KEY, PortalConstants.EXTERNAL_ID_VALUE);
    }

    public static Map<String,String> getAdditionalDataMapFromRequest(PortletRequest request) {
        return getMapFromRequest(request, PortalConstants.ADDITIONAL_DATA_KEY, PortalConstants.ADDITIONAL_DATA_VALUE);
    }
}
