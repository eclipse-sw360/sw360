/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags.links;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayDownloadAttachmentFileTest {

    @Test
    public void testThatTextsAreEscaped() throws Exception {
        DisplayDownloadAttachmentFile displayDownloadAttachment = new DisplayDownloadAttachmentFile();
        displayDownloadAttachment.setAttachment(createAttachment("Html <>&' entities"));
        assertEquals("Download Html &lt;&gt;&amp;&#39; entities", displayDownloadAttachment.getTitleText());
    }
    
    private Attachment createAttachment(String filename) {
        Attachment attachment = new Attachment();
        attachment.setFilename(filename);
        return attachment;
    }
}
