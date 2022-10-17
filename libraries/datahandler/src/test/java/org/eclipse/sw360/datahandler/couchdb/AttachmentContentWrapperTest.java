/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.couchdb;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

public class AttachmentContentWrapperTest extends DocumentWrapperTest<AttachmentContentWrapper, AttachmentContent, AttachmentContent._Fields> {

    public void testUpdateNonMetadataTouchesAllFields() throws Exception {
        AttachmentContent source;
        source = new AttachmentContent();
        source.setFilename("a");
        source.setType("b");
        source.setContentType("v");
        source.setPartsCount("1");
        source.setRemoteUrl("uskt"); //TODO this is not required !

        AttachmentContentWrapper attachmentContentWrapper = new AttachmentContentWrapper();
        attachmentContentWrapper.updateNonMetadata(source);

        assertTFields(source, attachmentContentWrapper, AttachmentContentWrapper.class, AttachmentContent._Fields.class);
    }
}
