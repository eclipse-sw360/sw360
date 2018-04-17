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

import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author daniele.fognini@tngtech.com
 */
public class AttachmentContentDownloader {
    /**
     * download an incomplete AttachmentContent from its URL
     *
     * @todo setup DI and move timeout to a member
     */
    public InputStream download(AttachmentContent attachmentContent, Duration timeout) throws IOException {
        int millisTimeout = ((Number) timeout.toMillis()).intValue();

        URL remoteURL = new URL(attachmentContent.getRemoteUrl());
        URLConnection urlConnection = remoteURL.openConnection();

        urlConnection.setConnectTimeout(millisTimeout);
        urlConnection.setReadTimeout(millisTimeout);

        InputStream downloadStream = urlConnection.getInputStream();
        return new BufferedInputStream(downloadStream);
    }
}
