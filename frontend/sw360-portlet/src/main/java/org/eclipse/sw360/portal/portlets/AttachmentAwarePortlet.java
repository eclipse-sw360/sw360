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
package org.eclipse.sw360.portal.portlets;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.Maps;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.AttachmentPortletUtils;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.portal.common.PortalConstants.ATTACHMENTS;

/**
 * Attachment portlet implementation
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public abstract class AttachmentAwarePortlet extends Sw360Portlet {
    private static final Map<String, String> ATTACHMENT_TYPE_MAP = Maps.newHashMap();
    private static final Map<String, String> CHECK_STATUS_MAP = Maps.newHashMap();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static class AttachmentSerializer extends StdSerializer<Attachment> {
        private static final TSerializer JSON_SERIALIZER = new TSerializer(new TSimpleJSONProtocol.Factory());

        public AttachmentSerializer(Class<Attachment> clazz) {
            super(clazz);
        }

        @Override
        public void serialize(Attachment attachment, JsonGenerator jsonGenerator, SerializerProvider provider)
                throws IOException, JsonGenerationException {
            try {
                jsonGenerator.writeRawValue(JSON_SERIALIZER.toString(attachment));
            } catch (TException exception) {
                throw new JsonGenerationException(exception);
            }
        }
    }

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Attachment.class, new AttachmentSerializer(Attachment.class));
        OBJECT_MAPPER.registerModule(module);

        for (AttachmentType attachmentType : AttachmentType.values()) {
            ATTACHMENT_TYPE_MAP.put(String.valueOf(attachmentType.getValue()), ThriftEnumUtils.enumToString(attachmentType));
        }

        for (CheckStatus checkStatus : CheckStatus.values()) {
            CHECK_STATUS_MAP.put(String.valueOf(checkStatus.getValue()), ThriftEnumUtils.enumToString(checkStatus));
        }
    }


    protected final AttachmentPortletUtils attachmentPortletUtils;
    protected Map< String, Map< String, Set<String>>> uploadHistoryPerUserEmailAndDocumentId;

    protected AttachmentAwarePortlet() {
        this(new ThriftClients());
    }

    public AttachmentAwarePortlet(ThriftClients thriftClients) {
        this(thriftClients, new AttachmentPortletUtils(thriftClients));
    }

    public AttachmentAwarePortlet(ThriftClients thriftClients, AttachmentPortletUtils attachmentPortletUtils) {
        super(thriftClients);
        this.attachmentPortletUtils = attachmentPortletUtils;
        uploadHistoryPerUserEmailAndDocumentId = new HashMap<>();
    }


    public static void setAttachmentsInRequest(PortletRequest request, Set<Attachment> attachments) {
        request.setAttribute(ATTACHMENTS, CommonUtils.nullToEmptySet(attachments));
    }

    private String getDocumentType(ResourceRequest request) {
        return request.getParameter(PortalConstants.DOCUMENT_TYPE);
    }

    protected abstract Set<Attachment> getAttachments(String documentId, String documentType, User user);

    protected void dealWithAttachments(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException {
        if (PortalConstants.ATTACHMENT_DOWNLOAD.equals(action)) {
            attachmentPortletUtils.serveFile(request, response);
        } else if (PortalConstants.ATTACHMENT_LIST.equals(action)) {
            serveAttachmentSet(request, response);
        } else if (PortalConstants.ATTACHMENT_LINK_TO.equals(action)) {
            doGetAttachmentForDisplay(request, response);
        } else if (PortalConstants.ATTACHMENT_RESERVE_ID.equals(action)) {
            serveNewAttachmentId(request, response);
        } else if (PortalConstants.ATTACHMENT_UPLOAD.equals(action)) {
            if ("POST".equals(request.getMethod())) {
                // POST is for actual upload
                storeUploadedAttachmentIdInHistory(request);
                if (!attachmentPortletUtils.uploadAttachmentPart(request, "file")) {
                    response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
                }
            } else {
                // GET is to check if it already exists: 200 or 204 return code
                if (!attachmentPortletUtils.checkAttachmentExistsFromRequest(request)) {
                    response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "204");
                }
            }
        } else if (PortalConstants.ATTACHMENT_CANCEL.equals(action)) {
            RequestStatus status = attachmentPortletUtils.cancelUpload(request);
            renderRequestStatus(request, response, status);
        }
    }

    private void doGetAttachmentForDisplay(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        final String attachmentId = request.getParameter(PortalConstants.ATTACHMENT_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);

        Attachment attachment = attachmentPortletUtils.getAttachmentForDisplay(user, attachmentId);
        if (attachment == null) {
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        } else {
            writeJSON(request, response, OBJECT_MAPPER.writeValueAsString(attachment));
        }
    }

    private void serveAttachmentSet(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        final String documentType = getDocumentType(request);
        final String documentId = request.getParameter(PortalConstants.DOCUMENT_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);

        // this is the raw attachment data
        List<Attachment> attachments = getAttachments(documentId, documentType, user).stream()
                .sorted(Comparator.comparing(Attachment::getFilename)).collect(Collectors.toList());

        Map<String, Object> data = Maps.newHashMap();
        data.put("data", attachments);
        data.put("attachmentTypes", ATTACHMENT_TYPE_MAP);
        data.put("checkStatuses", CHECK_STATUS_MAP);
        writeJSON(request, response, OBJECT_MAPPER.writeValueAsString(data));
    }

    private void serveNewAttachmentId(ResourceRequest request, ResourceResponse response) throws IOException {
        final AttachmentContent attachment = attachmentPortletUtils.createAttachmentContent(request);
        if (attachment == null) {
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
        } else {
            final String attachmentId = attachment.getId();
            response.getWriter().write(attachmentId);
        }
    }

    private void storeUploadedAttachmentIdInHistory(ResourceRequest request){
        String documentId = request.getParameter(PortalConstants.DOCUMENT_ID);
        String userEmail = UserCacheHolder.getUserFromRequest(request).getEmail();
        String attachmentId = request.getParameter("resumableIdentifier");

        if(!uploadHistoryPerUserEmailAndDocumentId.containsKey(userEmail)){
            Map<String,Set<String>> documentIdsToAttachmentIds = new HashMap<>();
            documentIdsToAttachmentIds.put(documentId,new HashSet<>());
            uploadHistoryPerUserEmailAndDocumentId.put(userEmail, documentIdsToAttachmentIds);
        } else if (!uploadHistoryPerUserEmailAndDocumentId.get(userEmail).containsKey(documentId)){
            uploadHistoryPerUserEmailAndDocumentId.get(userEmail).put(documentId,new HashSet<>());
        }

        Set<String> attachmentIdsForDocument = uploadHistoryPerUserEmailAndDocumentId.get(userEmail).get(documentId);
        attachmentIdsForDocument.add(attachmentId);
    }

    public void cleanUploadHistory(String userEmail, String documentId){
        if(uploadHistoryPerUserEmailAndDocumentId.containsKey(userEmail)) {
            if (uploadHistoryPerUserEmailAndDocumentId.get(userEmail).containsKey(documentId)) {
                uploadHistoryPerUserEmailAndDocumentId.get(userEmail).remove(documentId);
            }
        }
    }

    public void deleteUnneededAttachments(String userEmail, String documentId){
        if(uploadHistoryPerUserEmailAndDocumentId.containsKey(userEmail)) {
            Set<String> uploadedAttachmentIds = nullToEmptySet(uploadHistoryPerUserEmailAndDocumentId.get(userEmail).get(documentId));
            attachmentPortletUtils.deleteAttachments(uploadedAttachmentIds);
            cleanUploadHistory(userEmail,documentId);
        }
    }

    protected boolean isAttachmentAwareAction(String action) {
        return action.startsWith(PortalConstants.ATTACHMENT_PREFIX);
    }

    @Override
    protected boolean isGenericAction(String action) {
        return super.isGenericAction(action) || isAttachmentAwareAction(action);
    }

    @Override
    protected void dealWithGenericAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException {
        if (super.isGenericAction(action)) {
            super.dealWithGenericAction(request, response, action);
        } else {
            dealWithAttachments(request, response, action);
        }
    }

    @UsedAsLiferayAction
    public void attachmentDeleteOnCancel(ActionRequest request, ActionResponse response){
        String userEmail = UserCacheHolder.getUserFromRequest(request).getEmail();
        if(uploadHistoryPerUserEmailAndDocumentId.containsKey(userEmail)) {
            String documentId = request.getParameter(PortalConstants.DOCUMENT_ID);
            Set<String> uploadedAttachmentIds = nullToEmptySet(uploadHistoryPerUserEmailAndDocumentId.get(userEmail).get(documentId));
            attachmentPortletUtils.deleteAttachments(uploadedAttachmentIds);
            cleanUploadHistory(userEmail, documentId);
        }
    }
}
