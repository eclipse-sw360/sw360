/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags.links;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayDownloadApprovedClearingReportTest {
    
    @Test
    public void testThatImageDependsOnStatus() {
        Attachment attachment = createAttachment("test.file");

        DisplayDownloadApprovedClearingReport displayDownloadAttachment = new DisplayDownloadApprovedClearingReport();
        displayDownloadAttachment.setAttachment(attachment);

        assertEquals(DisplayDownloadAbstract.DOWNLOAD_IMAGE_ENABLED, displayDownloadAttachment.getImage());
    }

    @Test
    public void testTitleTextWithMinimalInformation() {
        Attachment attachment = createAttachment("test.file");

        DisplayDownloadApprovedClearingReport displayDownloadAttachment = new DisplayDownloadApprovedClearingReport();
        displayDownloadAttachment.setAttachment(attachment);

        assertEquals("Filename: test.file&#010;Status: APPROVED by  on &#010;Comment: &#010;Created:  on ",
                displayDownloadAttachment.getTitleText());
    }

    @Test
    public void testThatFieldsAreEscapedForTitle() {
        Attachment attachment = createAttachment("F<>&'\"F");
        attachment.setCheckedBy("AB<>&'\"AB");
        attachment.setCheckedOn("AO<>&'\"AO");
        attachment.setCheckedComment("CC<>&'\"CC");
        attachment.setCreatedBy("CB<>&'\"CB");
        attachment.setCreatedOn("CO<>&'\"CO");
        attachment.setCheckStatus(CheckStatus.ACCEPTED);

        DisplayDownloadApprovedClearingReport displayDownloadAttachment = new DisplayDownloadApprovedClearingReport();
        displayDownloadAttachment.setAttachment(attachment);

        assertEquals(
                "Filename: F&lt;&gt;&amp;&#39;&quot;F&#010;Status: APPROVED by AB&lt;&gt;&amp;&#39;&quot;AB on AO&lt;&gt;&amp;&#39;&quot;AO&#010;Comment: CC&lt;&gt;&amp;&#39;&quot;CC&#010;Created: CB&lt;&gt;&amp;&#39;&quot;CB on CO&lt;&gt;&amp;&#39;&quot;CO",
                displayDownloadAttachment.getTitleText());
    }

    private Attachment createAttachment(String filename) {
        Attachment attachment = new Attachment();
        attachment.setFilename(filename);
        return attachment;
    }
}
