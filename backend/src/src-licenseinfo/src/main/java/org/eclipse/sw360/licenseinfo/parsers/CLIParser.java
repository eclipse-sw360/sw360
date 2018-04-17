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

import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;

/**
 * Class for extracting copyright and license information from a simple XML file
 * @author: alex.borodin@evosoft.com
 */
public class CLIParser extends AbstractCLIParser {

    private static final Logger log = Logger.getLogger(CLIParser.class);
    private static final String COPYRIGHTS_XPATH = "/ComponentLicenseInformation/Copyright/Content";
    private static final String LICENSES_XPATH = "/ComponentLicenseInformation/License";
    private static final String CLI_ROOT_ELEMENT_NAME = "ComponentLicenseInformation";
    private static final String CLI_ROOT_ELEMENT_NAMESPACE = null;

    public CLIParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider) {
        super(attachmentConnector, attachmentContentProvider);
    }

    @Override
    public <T> boolean isApplicableTo(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        return attachmentContent.getFilename().endsWith(XML_FILE_EXTENSION) && hasCLIRootElement(attachmentContent, user, context);
    }

    private <T> boolean hasCLIRootElement(AttachmentContent content, User user, T context) throws TException {
        return hasThisXMLRootElement(content, CLI_ROOT_ELEMENT_NAMESPACE, CLI_ROOT_ELEMENT_NAME, user, context);
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        LicenseInfo licenseInfo = new LicenseInfo().setFilenames(Arrays.asList(attachmentContent.getFilename()));
        LicenseInfoParsingResult result = new LicenseInfoParsingResult().setLicenseInfo(licenseInfo);
        InputStream attachmentStream = null;

        try {
            attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context);
            Document doc = getDocument(attachmentStream);

            Set<String> copyrights = getCopyrights(doc);
            licenseInfo.setCopyrights(copyrights);

            Set<LicenseNameWithText> licenseNamesWithTexts = getLicenseNameWithTexts(doc);
            licenseInfo.setLicenseNamesWithTexts(licenseNamesWithTexts);

            result.setStatus(LicenseInfoRequestStatus.SUCCESS);
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException | SW360Exception e) {
            log.error(e);
            result.setStatus(LicenseInfoRequestStatus.FAILURE).setMessage("Error while parsing CLI file: " + e.toString());
        } finally {
            closeQuietly(attachmentStream, log);
        }
        return Collections.singletonList(result);
    }

    private Set<LicenseNameWithText> getLicenseNameWithTexts(Document doc) throws XPathExpressionException {
        NodeList licenseNodes = getNodeListByXpath(doc, LICENSES_XPATH);
        return nodeListToLicenseNamesWithTextsSet(licenseNodes);
    }

    private Set<String> getCopyrights(Document doc) throws XPathExpressionException {
        NodeList copyrightNodes = getNodeListByXpath(doc, COPYRIGHTS_XPATH);
        return nodeListToStringSet(copyrightNodes);
    }

    private Set<LicenseNameWithText> nodeListToLicenseNamesWithTextsSet(NodeList nodes){
        Set<LicenseNameWithText> licenseNamesWithTexts= Sets.newHashSet();
        for (int i = 0; i < nodes.getLength(); i++){
            licenseNamesWithTexts.add(getLicenseNameWithTextFromLicenseNode(nodes.item(i)));
        }
        return licenseNamesWithTexts;
    }
}
