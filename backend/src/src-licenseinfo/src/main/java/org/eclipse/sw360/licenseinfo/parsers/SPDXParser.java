/*
 * Copyright Bosch Software Innovations GmbH, 2016-2017.
 * Copyright Siemens AG, 2016-2017.
 * Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.model.SpdxDocument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.eclipse.sw360.licenseinfo.parsers.SPDXParserTools.getLicenseInfoFromSpdx;

/**
 * @author: alex.borodin@evosoft.com
 * @author: maximilian.huber@tngtech.com
 */
public class SPDXParser extends LicenseInfoParser {
    protected static final String FILETYPE_SPDX_INTERNAL = "RDF/XML";
    protected static final String FILETYPE_SPDX_EXTENSION = ".rdf";

    private static final Logger log = Logger.getLogger(SPDXParser.class);

    public SPDXParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider) {
        super(attachmentConnector, attachmentContentProvider);
    }

    @Override
    public List<String> getApplicableFileExtensions() {
        return Collections.singletonList(FILETYPE_SPDX_EXTENSION);
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context) throws TException {
        return Collections.singletonList(getLicenseInfo(attachment, user, context));
    }

    public <T> LicenseInfoParsingResult getLicenseInfo(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);

        final Optional<SpdxDocument> spdxDocument = openAsSpdx(attachmentContent, user, context);
        if(! spdxDocument.isPresent()){
            return new LicenseInfoParsingResult()
                    .setStatus(LicenseInfoRequestStatus.FAILURE);
        }

        return getLicenseInfoFromSpdx(attachmentContent, spdxDocument.get());
    }

    protected String getUriOfAttachment(AttachmentContent attachmentContent) throws URISyntaxException {
        String filename = attachmentContent.getFilename();
        String filePath = "///" + new File(filename).getAbsoluteFile().toString().replace('\\', '/');
        return new URI("file", filePath, null).toString();
    }

    protected <T> Optional<SpdxDocument> openAsSpdx(AttachmentContent attachmentContent, User user, T context) throws SW360Exception {
        try (InputStream attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context)) {
            return Optional.ofNullable(SPDXDocumentFactory.createSpdxDocument(attachmentStream,
                    getUriOfAttachment(attachmentContent),
                    FILETYPE_SPDX_INTERNAL));
        } catch (InvalidSPDXAnalysisException e) {
            String msg = "Unable to parse SPDX for attachment=" + attachmentContent.getFilename() + " with id=" + attachmentContent.getId()+
                    "\nThe message was: " + e.getMessage();
            log.info(msg);
            return Optional.empty();
        } catch (URISyntaxException e) {
            String msg = "Invalid URI syntax for attachment=" + attachmentContent.getFilename() + " with id=" + attachmentContent.getId();
            log.error(msg, e);
            throw new SW360Exception(msg);
        } catch (IOException | TException e) {
            String msg = "failed to read attachment=" + attachmentContent.getFilename() + " with id=" + attachmentContent.getId();
            log.error(msg, e);
            throw new SW360Exception(msg);
        }
    }
}
