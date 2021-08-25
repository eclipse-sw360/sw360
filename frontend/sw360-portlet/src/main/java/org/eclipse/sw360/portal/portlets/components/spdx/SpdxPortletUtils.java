/*
 * Copyright . Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.portlets.components.spdx;

import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.net.URLConnection.guessContentTypeFromName;
import static java.net.URLConnection.guessContentTypeFromStream;

/**
 * SPDX portlet implementation
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public abstract class SpdxPortletUtils {

    private SpdxPortletUtils() {
        // Utility class with only static functions
    }


    public static RequestStatus deleteSPDXDocument(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.SPDX_DOCUMENT_ID);
        if (id != null) {
            try {
                String deleteCommentEncoded = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
                User user = UserCacheHolder.getUserFromRequest(request);
                if(deleteCommentEncoded != null) {
                    String deleteComment = new String(Base64.getDecoder().decode(deleteCommentEncoded));
                    user.setCommentMadeDuringModerationRequest(deleteComment);
                }
                SPDXDocumentService.Iface client = new ThriftClients().makeSPDXClient();
                return client.deleteSPDXDocument(id, UserCacheHolder.getUserFromRequest(request));

            } catch (TException e) {
                log.error("Could not delete SPDX Document from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus deleteSpdxDocumentCreationInfo(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.SPDX_DOCUMENT_CREATION_INFO_ID);
        if (id != null) {
            try {
                String deleteCommentEncoded = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
                User user = UserCacheHolder.getUserFromRequest(request);
                if(deleteCommentEncoded != null) {
                    String deleteComment = new String(Base64.getDecoder().decode(deleteCommentEncoded));
                    user.setCommentMadeDuringModerationRequest(deleteComment);
                }
                DocumentCreationInformationService.Iface client = new ThriftClients().makeSPDXDocumentInfoClient();
                return client.deleteDocumentCreationInformation(id, UserCacheHolder.getUserFromRequest(request));

            } catch (TException e) {
                log.error("Could not delete Spdx Document Creation Info from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus deleteSpdxPackageInfo(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.SPDX_PACKAGE_INFO_ID);
        if (id != null) {
            try {
                String deleteCommentEncoded = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
                User user = UserCacheHolder.getUserFromRequest(request);
                if(deleteCommentEncoded != null) {
                    String deleteComment = new String(Base64.getDecoder().decode(deleteCommentEncoded));
                    user.setCommentMadeDuringModerationRequest(deleteComment);
                }
                PackageInformationService.Iface client = new ThriftClients().makeSPDXPackageInfoClient();
                return client.deletePackageInformation(id, UserCacheHolder.getUserFromRequest(request));

            } catch (TException e) {
                log.error("Could not delete Spdx Package Info from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static void updateSPDXDocumentFromRequest(PortletRequest request, SPDXDocument spdxDocument) {
        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
            switch (field) {
                case SNIPPETS:
                    spdxDocument.setFieldValue(field, getSnippetInfoFromRequest(request));
                    break;
                case RELATIONSHIPS:
                    spdxDocument.setFieldValue(field, getRelationshipsFromRequest(request));
                    break;
                case ANNOTATIONS:
                    spdxDocument.setFieldValue(field, getAnnotationsFromRequest(request));
                    break;
                default:
                    setFieldValue(request, spdxDocument, field); 
            }
        }
    }

    private static void setFieldValue(PortletRequest request, SPDXDocument spdxDocument, SPDXDocument._Fields field) {
        PortletUtils.setFieldValue(request, spdxDocument, field, SPDXDocument.metaDataMap.get(field), "");
    }

    // private static Set<SnippetInformation> getSnippetInfoFromRequest(SPDXDocument._Fields field, PortletRequest request) {
    //     Set<SnippetInformation> snippetInfos = new HashSet<>();
    //     String[] ids = request
    //             .getParameterValues(SPDXDocument._Fields.SNIPPETS.toString());

       

            
    //         for (int i = 0; i< ids.length; i++) {
    //             SnippetInformation snippetInfo = new SnippetInformation();
    //             PortletUtils.setFieldValue(request, snippetInfo, field, SnippetInformation.metaDataMap.get(field), "");
    //         }
        

    //     return snippetInfos;
    // }

    private static SnippetInformation getSnippetInfoFromRequest(PortletRequest request) {
        SnippetInformation snippetInfo = new SnippetInformation();
        for (SnippetInformation._Fields field : SnippetInformation._Fields.values()) {
            PortletUtils.setFieldValue(request, snippetInfo, field, SnippetInformation.metaDataMap.get(field), "");
        }

        return snippetInfo;
    }

    private static RelationshipsBetweenSPDXElements getRelationshipsFromRequest(PortletRequest request) {
        RelationshipsBetweenSPDXElements relationship = new RelationshipsBetweenSPDXElements();
        for (RelationshipsBetweenSPDXElements._Fields field : RelationshipsBetweenSPDXElements._Fields.values()) {
            PortletUtils.setFieldValue(request, relationship, field, RelationshipsBetweenSPDXElements.metaDataMap.get(field), "");
        }

        return relationship;
    }

    private static Annotations getAnnotationsFromRequest(PortletRequest request) {
        Annotations annotations= new Annotations();
        for (Annotations._Fields field : Annotations._Fields.values()) {
            PortletUtils.setFieldValue(request, annotations, field, Annotations.metaDataMap.get(field), "");
        }

        return annotations;
    }

    
}
