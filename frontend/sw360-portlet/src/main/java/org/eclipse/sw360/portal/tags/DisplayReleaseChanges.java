/*
 * Copyright Siemens AG, 2016, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.protocol.TType;
import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.tags.urlutils.LinkedReleaseRenderer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.HashMap;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;
import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the fields that have changed in the project
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayReleaseChanges extends UserAwareTag {
    private Release actual;
    private Release additions;
    private Release deletions;
    private String tableClasses = "";
    private String idPrefix = "";

    public void setActual(Release actual) {
        this.actual = actual;
    }

    public void setAdditions(Release additions) {
        this.additions = additions;
    }

    public void setDeletions(Release deletions) {
        this.deletions = deletions;
    }

    public void setTableClasses(String tableClasses) {
        this.tableClasses = tableClasses;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public int doStartTag() throws JspException {

        JspWriter jspWriter = pageContext.getOut();

        StringBuilder display = new StringBuilder();
        String namespace = getNamespace();

        if (additions == null || deletions == null) {
            return SKIP_BODY;
        }

        try {
            for (Release._Fields field : Release._Fields.values()) {
                switch (field) {
                    //ignored Fields
                    case ID:
                    case REVISION:
                    case TYPE:
                    case CREATED_BY:
                    case CREATED_ON:
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case COMPONENT_ID:
                    case VENDOR_ID:
                    case CLEARING_TEAM_TO_FOSSOLOGY_STATUS:
                        //Taken care of externally or in extra tables
                    case ATTACHMENTS:
                    case RELEASE_ID_TO_RELATIONSHIP:
                    case CLEARING_INFORMATION:
                    case COTS_DETAILS:
                    case ECC_INFORMATION:
                        break;
                    default:
                        FieldMetaData fieldMetaData = Release.metaDataMap.get(field);
                        displaySimpleFieldOrSet(display, actual, additions, deletions, field, fieldMetaData, "");
                }
            }

            String renderString = display.toString();

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-danger\">No changes in basic fields.</div>";
            } else {
                renderString = String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                        + "<thead><tr><th colspan=\"4\"> Changes for Basic fields</th></tr>"
                        + String.format("<tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                        FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL)
                        + renderString + "</tbody></table>";
            }

            StringBuilder releaseRelationshipDisplay = new StringBuilder();
            User user = getUserFromContext("Cannot render release changes without logged in user in request");
            renderReleaseIdToRelationship(releaseRelationshipDisplay, user);

            String clearingInformationDisplay = renderClearingInformation();
            String eccInformationDisplay = renderEccInformation();
            String cotsDetailDisplay = renderCOTSDetails();

            jspWriter.print(renderString + releaseRelationshipDisplay.toString() + clearingInformationDisplay + eccInformationDisplay + cotsDetailDisplay);
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void renderReleaseIdToRelationship(StringBuilder display, User user) {

        if (ensureSomethingTodoAndNoNull(Release._Fields.RELEASE_ID_TO_RELATIONSHIP)) {

            Set<String> changedReleaseIds = Sets.intersection(
                    additions.getReleaseIdToRelationship().keySet(),
                    deletions.getReleaseIdToRelationship().keySet());
            Set<String> releaseIdsInDb = nullToEmptyMap(actual.getReleaseIdToRelationship()).keySet();
            //keep only releases that are still in the database
            changedReleaseIds = Sets.intersection(changedReleaseIds, releaseIdsInDb);

            Set<String> removedReleaseIds = Sets.difference(deletions.getReleaseIdToRelationship().keySet(), changedReleaseIds);
            removedReleaseIds = Sets.intersection(removedReleaseIds, releaseIdsInDb);

            Set<String> addedReleaseIds = Sets.difference(additions.getReleaseIdToRelationship().keySet(), changedReleaseIds);

            display.append("<h3> Changes in linked releases </h3>");
            LinkedReleaseRenderer renderer = new LinkedReleaseRenderer(display, tableClasses, idPrefix, user);
            renderer.renderReleaseLinkList(display, deletions.getReleaseIdToRelationship(), removedReleaseIds, "Removed Release Links");
            renderer.renderReleaseLinkList(display, additions.getReleaseIdToRelationship(), addedReleaseIds, "Added Release Links");
            renderer.renderReleaseLinkListCompare(
                    display,
                    actual.getReleaseIdToRelationship(),
                    deletions.getReleaseIdToRelationship(),
                    additions.getReleaseIdToRelationship(),
                    changedReleaseIds);
        }
    }

    private boolean ensureSomethingTodoAndNoNull(Release._Fields field) {
        if (!deletions.isSet(field) && !additions.isSet(field)) {
            return false;
        }
        if(Release.metaDataMap.get(field).valueMetaData.type == TType.MAP) {
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new HashMap<>());
            }
            if (!additions.isSetReleaseIdToRelationship()) {
                additions.setFieldValue(field, new HashMap<>());
            }
        } else if (field == Release._Fields.CLEARING_INFORMATION){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new ClearingInformation());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new ClearingInformation());
            }
        } else if (field == Release._Fields.ECC_INFORMATION){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, newDefaultEccInformation());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, newDefaultEccInformation());
            }
        } else if (field == Release._Fields.COTS_DETAILS){
            if (!deletions.isSet(field)) {
                deletions.setFieldValue(field, new COTSDetails());
            }
            if (!additions.isSet(field)) {
                additions.setFieldValue(field, new COTSDetails());
            }
        }
        return true;
    }

    private String renderClearingInformation() {
        if (!ensureSomethingTodoAndNoNull(Release._Fields.CLEARING_INFORMATION)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(Release._Fields.CLEARING_INFORMATION)){
            actual.clearingInformation = new ClearingInformation();
        }
        for (ClearingInformation._Fields field : ClearingInformation._Fields.values()) {
            FieldMetaData fieldMetaData = ClearingInformation.metaDataMap.get(field);
            displaySimpleFieldOrSet(
                    display,
                    actual.getClearingInformation(),
                    additions.getClearingInformation(),
                    deletions.getClearingInformation(),
                    field, fieldMetaData, "");
        }
        return "<h3> Changes in Clearing Information </h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL)
                + display.toString() + "</tbody></table>";


    }

    private String renderEccInformation() {
        if (!ensureSomethingTodoAndNoNull(Release._Fields.ECC_INFORMATION)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(Release._Fields.ECC_INFORMATION)){
            actual.eccInformation = newDefaultEccInformation();
        }
        for (EccInformation._Fields field : EccInformation._Fields.values()) {
            FieldMetaData fieldMetaData = EccInformation.metaDataMap.get(field);
            displaySimpleFieldOrSet(
                    display,
                    actual.getEccInformation(),
                    additions.getEccInformation(),
                    deletions.getEccInformation(),
                    field, fieldMetaData, "");
        }
        return "<h3> Changes in ECC Information </h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL)
                + display.toString() + "</tbody></table>";


    }

    private String renderCOTSDetails() {
        if (!ensureSomethingTodoAndNoNull(Release._Fields.COTS_DETAILS)) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        if (! actual.isSet(Release._Fields.COTS_DETAILS)){
            actual.cotsDetails = new COTSDetails();
        }
        for (COTSDetails._Fields field : COTSDetails._Fields.values()) {
            FieldMetaData fieldMetaData = COTSDetails.metaDataMap.get(field);
            displaySimpleFieldOrSet(
                    display,
                    actual.getCotsDetails(),
                    additions.getCotsDetails(),
                    deletions.getCotsDetails(),
                    field, fieldMetaData, "");
        }
        return "<h3> Changes in COTS Details </h3>"
                + String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                + String.format("<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL)
                + display.toString() + "</tbody></table>";


    }
}
