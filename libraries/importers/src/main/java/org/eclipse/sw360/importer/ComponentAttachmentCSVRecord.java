/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.importer;

import org.eclipse.sw360.commonIO.SampleOptions;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

/**
 *
 * @author johannes.najjar@tngtech.com
 */
public class ComponentAttachmentCSVRecord extends ComponentAwareCSVRecord{

    private final String attachmentContentId;
    private final String filename;
    private final String attachmentType;
    private final String comment;
    private final String createdOn;

    public String getCreatedBy() {
        return createdBy;
    }

    private final String createdBy;

    public ComponentAttachmentCSVRecord(String componentName, String releaseName, String releaseVersion,
                                        String attachmentContentId, String filename, String attachmentType,
                                        String comment, String createdOn, String createdBy) {
        super(componentName, releaseName, releaseVersion);
        this.attachmentContentId = attachmentContentId;
        this.filename = filename;
        this.attachmentType = attachmentType;
        this.comment = comment;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
    }

    public boolean isForComponent() {
        return !isNullOrEmpty(componentName) && isNullOrEmpty(releaseName) && isNullOrEmpty(releaseVersion);
    }

    public boolean isForRelease() {
        return !isNullOrEmpty(releaseName) && !isNullOrEmpty(releaseVersion);
    }

    public boolean isSaveableAttachment(){
        return !isNullOrEmpty(getCreatedBy()) && isSetAttachment();
    }

    public boolean isSetAttachment() {
        return !isNullOrEmpty(attachmentContentId) && !isNullOrEmpty(filename);
    }

    public String getReleaseIdentifier() {
        return SW360Utils.getVersionedName(releaseName, releaseVersion);
    }

    public String getComponentName() {
        return componentName;
    }

    @Override
    public Iterable<String> getCSVIterable() {

        final ArrayList<String> elements = new ArrayList<>();

        elements.add(componentName);
        elements.add(releaseName);
        elements.add(releaseVersion);
        elements.add(attachmentContentId);
        elements.add(filename);
        elements.add(attachmentType);
        elements.add(comment);
        elements.add(createdOn);
        elements.add(createdBy);

        return elements;
    }

    public static Iterable<String> getCSVHeaderIterable(){
         final ArrayList<String> elements = new ArrayList<>();

         elements.add("componentName");
         elements.add("releaseName");
         elements.add("releaseVersion");
         elements.add("attachmentContentId");
         elements.add("filename");
         elements.add("attachmentType");
         elements.add("comment");
         elements.add("createdOn");
         elements.add("createdBy");

         return elements;
     }

     public static Iterable<String> getSampleInputIterable(){
        final ArrayList<String> elements = new ArrayList<>();

        elements.add("componentName");
        elements.add("releaseName");
        elements.add(SampleOptions.VERSION_OPTION);
        elements.add(SampleOptions.ID_OPTION);
        elements.add(SampleOptions.FILE_OPTION);
        elements.add(SampleOptions.ATTACHMENT_TYPE_OPTIONS);
        elements.add("comment");
        elements.add(SampleOptions.DATE_OPTION);
        elements.add(SampleOptions.URL_OPTION);

         return elements;
     }

    public Attachment getAttachment() {
        final Attachment attachment = new Attachment()
                                             .setAttachmentContentId(attachmentContentId)
                                             .setFilename(filename);

        if(!isNullOrEmpty(attachmentType)) {
            final AttachmentType attachmentTypeCandidate = ThriftEnumUtils.stringToEnum(attachmentType, AttachmentType.class);
            if(attachmentTypeCandidate!=null)
              attachment.setAttachmentType(attachmentTypeCandidate);
        }

        attachment.setCreatedComment(nullToEmpty(comment))
                  .setCreatedOn(nullToEmpty(createdOn))
                  .setCreatedBy(nullToEmpty(createdBy));

        return attachment;
    }

    public static ComponentAttachmentCSVRecordBuilder builder(){
        return new ComponentAttachmentCSVRecordBuilder();
    }

    public static ComponentAttachmentCSVRecordBuilder builder(CSVRecord in){
        return new ComponentAttachmentCSVRecordBuilder(in);
    }
}
