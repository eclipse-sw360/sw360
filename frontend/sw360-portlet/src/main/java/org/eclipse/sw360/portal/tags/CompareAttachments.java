/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.apache.thrift.meta_data.FieldMetaData;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the fields that have changed as described by additions and deletions
 *
 * @author Daniele.Fognini@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class CompareAttachments extends ContextAwareTag {
    public static final List<Attachment._Fields> RELEVANT_FIELDS = FluentIterable
            .from(copyOf(Attachment._Fields.values()))
            .filter(CompareAttachments::isFieldRelevant)
            .toList();

    private Set<Attachment> actual;
    private Set<Attachment> additions;
    private Set<Attachment> deletions;
    private String tableClasses = "";
    private String idPrefix = "";
    private String contextType;
    private String contextId;

    public void setActual(Set<Attachment> actual) {
        this.actual = nullToEmptySet(actual);
    }

    public void setAdditions(Set<Attachment> additions) {
        this.additions = nullToEmptySet(additions);
    }

    public void setDeletions(Set<Attachment> deletions) {
        this.deletions = nullToEmptySet(deletions);
    }

    public void setTableClasses(String tableClasses) {
        this.tableClasses = tableClasses;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public int doStartTag() throws JspException {

        JspWriter jspWriter = pageContext.getOut();
        StringBuilder display = new StringBuilder();

        try {
            renderAttachments(jspWriter, actual, additions, deletions, contextType, contextId);

            String renderString = display.toString();

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-info\">No changes in attachments</div>";
            }

            jspWriter.print(renderString);
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void renderAttachments(JspWriter jspWriter,
                                   Set<Attachment> currentAttachments,
                                   Set<Attachment> addedAttachments ,
                                   Set<Attachment> deletedAttachments,
                                   String contextType, String contextId) throws JspException, IOException {

        Map<String, Attachment> currentAttachmentsById = getAttachmentsById(currentAttachments);
        Map<String, Attachment> addedAttachmentsById = getAttachmentsById(addedAttachments);
        Map<String, Attachment> deletedAttachmentsById = getAttachmentsById(deletedAttachments);

        Set<String> currentAttachmentIds = currentAttachmentsById.keySet();
        Set<String> addedAttachmentIds = addedAttachmentsById.keySet();
        Set<String> deletedAttachmentIds = deletedAttachmentsById.keySet();
        Set<String> commonAttachmentIds = Sets.intersection(deletedAttachmentIds, addedAttachmentIds);

        addedAttachmentIds = Sets.difference(addedAttachmentIds, commonAttachmentIds);
        deletedAttachmentIds = Sets.difference(deletedAttachmentIds, commonAttachmentIds);
        deletedAttachmentIds = Sets.intersection(deletedAttachmentIds, currentAttachmentIds);//remove what was deleted already in the database

        renderAttachmentList(jspWriter, currentAttachmentsById, deletedAttachmentIds, "Deleted", contextType, contextId);
        renderAttachmentList(jspWriter, addedAttachmentsById, addedAttachmentIds, "Added", contextType, contextId);
        renderAttachmentComparison(jspWriter, currentAttachmentsById, deletedAttachmentsById, addedAttachmentsById, commonAttachmentIds);
    }

    private void renderAttachmentList(JspWriter jspWriter,
                                      Map<String, Attachment> allAttachments,
                                      Set<String> attachmentIds, String msg,
                                      String contextType, String contextId) throws JspException, IOException {
        if (attachmentIds.isEmpty()) return;
        jspWriter.write(String.format("<table class=\"%s\" id=\"%s%s\" >", tableClasses, idPrefix, msg));

        renderAttachmentRowHeader(jspWriter, msg);
        for (String attachmentId : attachmentIds) {
            renderAttachmentRow(jspWriter, allAttachments.get(attachmentId), contextType, contextId);
        }

        jspWriter.write("</table>");
    }

    private void renderAttachmentRowHeader(JspWriter jspWriter, String msg) throws IOException {
        jspWriter.write(String.format("<thead><tr><th colspan=\"%d\"> %s Attachments: </th></tr><tr>", RELEVANT_FIELDS.size(), msg));
        for (Attachment._Fields field : RELEVANT_FIELDS) {
            jspWriter.write(String.format("<th>%s</th>", field.getFieldName()));
        }
        jspWriter.write("</tr></thead>");
    }

    private void renderAttachmentRow(JspWriter jspWriter, Attachment attachment, String contextType, String contextId) throws JspException, IOException {
        jspWriter.write("<tr>");
        for (Attachment._Fields field : RELEVANT_FIELDS) {
            FieldMetaData fieldMetaData = Attachment.metaDataMap.get(field);
            Object fieldValue = attachment.getFieldValue(field);
            if(field.equals(Attachment._Fields.FILENAME)){
                jspWriter.append(String.format("<td>%s", getDisplayString(fieldMetaData.valueMetaData.type, fieldValue)));
                jspWriter.write("<br/>");
                addDownloadLink(pageContext, jspWriter, attachment.getFilename(), attachment.getAttachmentContentId(), contextType, contextId);
                jspWriter.append("</td>");
            } else {
                jspWriter.append(String.format("<td>%s</td>", getDisplayString(fieldMetaData.valueMetaData.type, fieldValue)));
            }
        }
        jspWriter.append("</tr>");
    }

    private void renderAttachmentComparison(JspWriter jspWriter,
                                            Map<String, Attachment> currentAttachmentsById,
                                            Map<String, Attachment> deletedAttachmentsById,
                                            Map<String, Attachment> addedAttachmentsById,
                                            Set<String> commonAttachmentIds) throws IOException {
        if (commonAttachmentIds.isEmpty()) return;

        StringBuilder candidate = new StringBuilder();
        for (String commonAttachmentId : commonAttachmentIds) {
            renderCompareAttachment(candidate,
                    currentAttachmentsById.get(commonAttachmentId),
                    deletedAttachmentsById.get(commonAttachmentId),
                    addedAttachmentsById.get(commonAttachmentId));
        }
        String changedAttachmentTable = candidate.toString();
        if (!changedAttachmentTable.isEmpty()) {
            jspWriter.write("<h4>Changed Attachments</h4>");
            jspWriter.write(changedAttachmentTable);
        }
    }

    private void renderCompareAttachment(StringBuilder display, Attachment old, Attachment deleted, Attachment added) {

        if (old.equals(added)) return;
        display.append(String.format("<table class=\"%s\" id=\"%schanges%s\" >", tableClasses, idPrefix, old.getAttachmentContentId()));
        display.append(String.format("<thead><tr><th colspan=\"4\"> Changes for Attachment %s </th></tr>", old.getFilename()));
        display.append(String.format("<tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL));

        for (Attachment._Fields field : RELEVANT_FIELDS) {

            FieldMetaData fieldMetaData = Attachment.metaDataMap.get(field);
            displaySimpleFieldOrSet(display, old, added, deleted, field,
                    fieldMetaData, Release._Fields.ATTACHMENTS.getFieldName());
        }
        display.append("</tbody></table>");
    }


    private static Map<String, Attachment> getAttachmentsById(Set<Attachment> currentAttachments) {
        return Maps.uniqueIndex(currentAttachments, Attachment::getAttachmentContentId);
    }

    private static boolean isFieldRelevant(Attachment._Fields field) {
        switch (field) {
            //ignored Fields
            case ATTACHMENT_CONTENT_ID:
                return false;
            case UPLOAD_HISTORY:
                return false;
            default:
                return true;
        }
    }
}
