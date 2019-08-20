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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DisplayDownloadAttachmentBundleTest {

    @Test
    public void testThatAttachmentsIsNotSetToNull() throws Exception {
        DisplayDownloadAttachmentBundle displayDownloadAttachmentBundle = new DisplayDownloadAttachmentBundle();
        displayDownloadAttachmentBundle.setAttachments(null);
        assertNotNull(displayDownloadAttachmentBundle.attachments);
    }

    @Test
    public void testThatTextsAreEscaped() throws Exception {
        DisplayDownloadAttachmentBundle displayDownloadAttachmentBundle = new DisplayDownloadAttachmentBundle();
        displayDownloadAttachmentBundle.setName("Html <>&' entities");
        assertEquals("Download Html &lt;&gt;&amp;&#39; entities", displayDownloadAttachmentBundle.getTitleText());
    }
}
