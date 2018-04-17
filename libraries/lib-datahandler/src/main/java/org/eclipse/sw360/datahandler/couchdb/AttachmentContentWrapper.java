/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

public class AttachmentContentWrapper extends DocumentWrapper<AttachmentContent> {

    @JsonProperty("issetBitfield")
    private byte __isset_bitfield = 0; //TODO set it

    /**
     * must have a copy of all fields in @see Attachment
     */
    public String id; // optional
    public String revision; // optional
    public String type; // optional
    public boolean onlyRemote; // required
    public String remoteUrl; // optional
    public String filename; // required
    public String contentType; // required
    public String partsCount; // optional


    @Override
    public void updateNonMetadata(AttachmentContent source) {
        filename = source.getFilename();
        type = source.getType();
        contentType = source.getContentType();
        remoteUrl = source.getRemoteUrl();
        partsCount = source.getPartsCount();
        remoteUrl = source.getRemoteUrl();
        onlyRemote = source.isOnlyRemote();
    }
}
