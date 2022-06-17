/*
 * Copyright Siemens AG, 2016-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import java.util.ArrayList;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus;
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

    public <T> List<LicenseInfoParsingResult> getLicenseInfosIncludeConcludedLicense(Attachment attachment,
            boolean includeConcludedLicense, User user, T context) throws TException {
        return new ArrayList<LicenseInfoParsingResult>();
    }

    public <T> ObligationParsingResult getObligations(Attachment attachment, User user, T context) throws TException {
        return new ObligationParsingResult().setStatus(ObligationInfoRequestStatus.NO_APPLICABLE_SOURCE);
    }
}
