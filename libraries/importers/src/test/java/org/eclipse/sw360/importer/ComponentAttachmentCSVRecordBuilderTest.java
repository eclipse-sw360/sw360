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

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author johannes.najjar@tngtech.com
 */
public class ComponentAttachmentCSVRecordBuilderTest {

    @Test
    public void testFillComponent() throws Exception {
        final String componentName = "myCompo";

        final Component component = new Component();
        component.setName(componentName);

        final ComponentAttachmentCSVRecordBuilder componentAttachmentCSVRecordBuilder = new ComponentAttachmentCSVRecordBuilder();
        componentAttachmentCSVRecordBuilder.fill(component);

        final ComponentAttachmentCSVRecord filledRecord = componentAttachmentCSVRecordBuilder.build();

        assertThat(filledRecord.getComponentName(), is(componentName));
    }

    @Test
    public void testFillRelease() throws Exception {
        final String releaseName =  "myRelease";
        final String releaseVersion =  "1.862b";

        final Release release = new Release();
        release.setName(releaseName).setVersion(releaseVersion);

        final ComponentAttachmentCSVRecordBuilder componentAttachmentCSVRecordBuilder =  new ComponentAttachmentCSVRecordBuilder();
        componentAttachmentCSVRecordBuilder.fill(release);

        final ComponentAttachmentCSVRecord filledRecord = componentAttachmentCSVRecordBuilder.build();

        assertThat(filledRecord.getReleaseIdentifier(), is(SW360Utils.getVersionedName(releaseName, releaseVersion)));
    }

    @Test
    public void testFillAttachment() throws Exception {
        final String attachmentContentID =  "asda823123123";
        final String fileName = "My.tar.gz";
        final String comment = "blabla";
        final AttachmentType attachmentType = AttachmentType.CLEARING_REPORT;
        final String createdBy = "Me";
        final String createdOn =  "Now";


        final Attachment attachment = new Attachment();
        attachment.setFilename(fileName)
                  .setAttachmentContentId(attachmentContentID)
                .setCreatedComment(comment)
                .setAttachmentType(attachmentType)
                .setCreatedBy(createdBy)
                .setCreatedOn(createdOn);


        final ComponentAttachmentCSVRecordBuilder componentAttachmentCSVRecordBuilder =  new ComponentAttachmentCSVRecordBuilder();
        componentAttachmentCSVRecordBuilder.fill(attachment);

        final ComponentAttachmentCSVRecord filledRecord = componentAttachmentCSVRecordBuilder.build();

        assertThat(filledRecord.getAttachment(), is(attachment));


    }
}