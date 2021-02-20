/*
 * Copyright Bosch Software Innovations GmbH, 2016-2017.
 * Copyright Siemens AG, 2016-2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
    protected static final String FILETYPE_SPDX_EXTENSION = ".rdf";

    private static final Logger log = LogManager.getLogger(SPDXParser.class);

    public SPDXParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider) {
        super(attachmentConnector, attachmentContentProvider);
    }

    @Override
    public List<String> getApplicableFileExtensions() {
        return Collections.singletonList(FILETYPE_SPDX_EXTENSION);
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context)
            throws TException {
        return Collections.singletonList(getLicenseInfo(attachment, true, user, context));
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfosIncludeConcludedLicense(Attachment attachment,
            boolean includeConcludedLicense, User user, T context) throws TException {
        return Collections.singletonList(getLicenseInfo(attachment, includeConcludedLicense, user, context));
    }

    public <T> LicenseInfoParsingResult getLicenseInfo(Attachment attachment, boolean includeConcludedLicense, User user,
            T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);

        final Optional<Document> spdxDocument = openAsSpdx(attachmentContent, user, context);
        if(! spdxDocument.isPresent()){
            return new LicenseInfoParsingResult()
                    .setStatus(LicenseInfoRequestStatus.FAILURE);
        }

        return getLicenseInfoFromSpdx(attachmentContent, includeConcludedLicense, spdxDocument.get());
    }

    protected String getUriOfAttachment(AttachmentContent attachmentContent) throws URISyntaxException {
        String filename = attachmentContent.getFilename();
        String filePath = "///" + new File(filename).getAbsoluteFile().toString().replace('\\', '/');
        return new URI("file", filePath, null).toString();
    }

    protected <T> Optional<Document> openAsSpdx(AttachmentContent attachmentContent, User user, T context) throws SW360Exception {
        try (InputStream attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context)) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(attachmentStream);
            doc.getDocumentElement().normalize();
            return Optional.ofNullable(doc);
        } catch (ParserConfigurationException e) {
            String msg = "Unable to parse SPDX for attachment=" + attachmentContent.getFilename() + " with id="
                    + attachmentContent.getId() + "\nThe message was: " + e.getMessage();
            log.info(msg, e);
            return Optional.empty();
        } catch (IOException | TException e) {
            String msg = "failed to read attachment=" + attachmentContent.getFilename() + " with id="
                    + attachmentContent.getId();
            log.error(msg, e);
            throw new SW360Exception(msg);
        } catch (SAXException e) {
            String msg = "failed to parse attachment=" + attachmentContent.getFilename() + " with id="
                    + attachmentContent.getId();
            throw new SW360Exception(msg);
        }
    }
}
