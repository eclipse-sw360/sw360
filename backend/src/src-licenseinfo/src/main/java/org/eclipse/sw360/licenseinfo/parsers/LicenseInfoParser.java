/*
 * Copyright Siemens AG, 2016-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.parsers;

import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.List;

/**
 * @author: alex.borodin@evosoft.com
 */
public abstract class LicenseInfoParser {
    protected final AttachmentConnector attachmentConnector;
    protected AttachmentContentProvider attachmentContentProvider;

    protected LicenseInfoParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider){
        this.attachmentConnector = attachmentConnector;
        this.attachmentContentProvider = attachmentContentProvider;
    }

    public abstract List<String> getApplicableFileExtensions();

    public <T> boolean isApplicableTo(Attachment attachmentContent, User user, T context) throws TException {
        List<String> applicableFileExtensions = getApplicableFileExtensions();
        if(applicableFileExtensions.size() == 0){
            return true;
        }
        String lowerFileName = attachmentContent.getFilename().toLowerCase();
        return applicableFileExtensions.stream()
                .anyMatch(extension -> lowerFileName.endsWith(extension.toLowerCase()));
    }

    public abstract <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context) throws TException;
}
