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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;

/**
 * Class for extracting copyright and license information from a simple XML file
 * @author: alex.borodin@evosoft.com
 */
@Component
public class CLIParser extends AbstractCLIParser {

    private static final Logger log = LogManager.getLogger(CLIParser.class);
    private static final String COPYRIGHTS_CONTENT_XPATH = "/ComponentLicenseInformation/Copyright";
    private static final String COPYRIGHTS_XPATH = "/ComponentLicenseInformation/Copyright/Content";
    private static final String LICENSES_XPATH = "/ComponentLicenseInformation/License";
    private static final String OBLIGATIONS_XPATH = "/ComponentLicenseInformation/Obligation";
    private static final String ASSESSMENT_SUMMARY_XPATH = "/ComponentLicenseInformation/AssessmentSummary";
    private static final String CLI_ROOT_ELEMENT_NAME = "ComponentLicenseInformation";
    private static final String CLI_ROOT_XPATH = "/ComponentLicenseInformation";
    private static final String CLI_ROOT_ELEMENT_NAMESPACE = null;

    @Override
    public <T> boolean isApplicableTo(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        return attachmentContent.getFilename().endsWith(XML_FILE_EXTENSION) && hasCLIRootElement(attachmentContent, user, context);
    }

    private <T> boolean hasCLIRootElement(AttachmentContent content, User user, T context) throws TException {
        return hasThisXMLRootElement(content, CLI_ROOT_ELEMENT_NAMESPACE, CLI_ROOT_ELEMENT_NAME, user, context);
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context,
            boolean includeFilesHash) throws TException {
        return getLicenseInfosDelegated(attachment, user, context, includeFilesHash);
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context)
            throws TException {
        return getLicenseInfosDelegated(attachment, user, context, false);
    }

    @Override
    public <T> ObligationParsingResult getObligations(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        ObligationParsingResult result = new ObligationParsingResult();

        InputStream attachmentStream = null;

        try {
            attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context);
            Document doc = getDocument(attachmentStream);
            result.setSha1Hash(getSha1Hash(doc));
            result.setObligationsAtProject(getObligations(doc));
            result.setAttachmentContentId(attachment.getAttachmentContentId());
            result.setStatus(ObligationInfoRequestStatus.SUCCESS);
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException | SW360Exception e) {
            log.error(e);
            result.setStatus(ObligationInfoRequestStatus.FAILURE).setMessage("Error while parsing CLI file: " + e.toString());
        } finally {
            closeQuietly(attachmentStream, log);
        }
        return result;
    }

    private <T> List<LicenseInfoParsingResult> getLicenseInfosDelegated(Attachment attachment, User user, T context, boolean includeFilesHash) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        LicenseInfo licenseInfo = new LicenseInfo().setFilenames(Arrays.asList(attachmentContent.getFilename()));
        LicenseInfoParsingResult result = new LicenseInfoParsingResult().setLicenseInfo(licenseInfo);
        InputStream attachmentStream = null;

        try {
            attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context);
            Document doc = getDocument(attachmentStream);

            if (includeFilesHash) {
                Map<String, Set<String>> copyrightsWithFileHash = getCopyrightsWithFileHash(doc);
                licenseInfo.setCopyrights(copyrightsWithFileHash.keySet());
                licenseInfo.setCopyrightsWithFilesHash(copyrightsWithFileHash);
            } else {
                Set<String> copyrights = getCopyrights(doc);
                licenseInfo.setCopyrights(copyrights);
            }

            licenseInfo.setLicenseNamesWithTexts(getLicenseNameWithTexts(doc, includeFilesHash));
            licenseInfo.setAssessmentSummary(getAssessmentSummary(doc));

            licenseInfo.setSha1Hash(getSha1Hash(doc));
            licenseInfo.setComponentName(getComponent(doc));

            result.setAttachmentContentId(attachment.getAttachmentContentId());
            result.setStatus(LicenseInfoRequestStatus.SUCCESS);
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException | SW360Exception e) {
            log.error(e);
            result.setStatus(LicenseInfoRequestStatus.FAILURE).setMessage("Error while parsing CLI file: " + e.toString());
        } finally {
            closeQuietly(attachmentStream, log);
        }
        return Collections.singletonList(result);
    }
    private List<ObligationAtProject> getObligations(Document doc) throws XPathExpressionException {
        NodeList obligationNodes = getNodeListByXpath(doc, OBLIGATIONS_XPATH);
        return nodeListToObligationList(obligationNodes);
    }

    private Set<LicenseNameWithText> getLicenseNameWithTexts(Document doc, boolean includeFilesHash) throws XPathExpressionException {
        NodeList licenseNodes = getNodeListByXpath(doc, LICENSES_XPATH);
        return nodeListToLicenseNamesWithTextsSet(licenseNodes, includeFilesHash);
    }

    private Map<String, String> getAssessmentSummary(Document doc) throws XPathExpressionException {
        NodeList assessmentSummaryList = getNodeListByXpath(doc, ASSESSMENT_SUMMARY_XPATH);
        Map<String, String> assessmentSummaryMap = new HashMap<String, String>();
        if (assessmentSummaryList.getLength() == 1) {
            Node node = assessmentSummaryList.item(0);
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                String nodeName = childNodes.item(i).getNodeName();
                String textContent = childNodes.item(i).getTextContent();
                assessmentSummaryMap.put(nodeName, textContent);
            }
        } else {
            log.error("AssessmentSummary not found in CLI!");
        }
        return assessmentSummaryMap;
    }

    private Set<String> getCopyrights(Document doc) throws XPathExpressionException {
        NodeList copyrightNodes = getNodeListByXpath(doc, COPYRIGHTS_XPATH);
        return nodeListToStringSet(copyrightNodes);
    }

    private Map<String, Set<String>> getCopyrightsWithFileHash(Document doc) throws XPathExpressionException {
        NodeList copyrightNodes = getNodeListByXpath(doc, COPYRIGHTS_CONTENT_XPATH);

        Map<String, Set<String>> copyrightMap = new HashMap<String, Set<String>>();
        for (int i = 0; i < copyrightNodes.getLength(); i++) {
            Map<String, Set<String>> copyrightWithFileHash = getCopyrightWithFileHash(copyrightNodes.item(i));
            copyrightMap.putAll(copyrightWithFileHash);
        }
        return copyrightMap;
    }

    protected Map<String, Set<String>> getCopyrightWithFileHash(Node node) {
        Set<String> filesHash = new HashSet<String>();
        String sourceFilesHash = findNamedSubelement(node, SOURCE_FILES_HASH_ELEMENT_NAME)
                .map(AbstractCLIParser::normalizeEscapedXhtml).orElse(null);
        if (CommonUtils.isNotNullEmptyOrWhitespace(sourceFilesHash)) {
            filesHash.addAll(Arrays.asList(sourceFilesHash.split("\\n")));
        }

        String content = findNamedSubelement(node, LICENSE_CONTENT_ELEMENT_NAME)
                .map(AbstractCLIParser::normalizeEscapedXhtml).orElse(null);
        Map<String, Set<String>> copyrightWithFileHash = new HashMap<String, Set<String>>();
        copyrightWithFileHash.put(content, filesHash);
        return copyrightWithFileHash;
    }

    private String getSha1Hash(Document doc) throws XPathExpressionException {
        NodeList copyrightNodes = getNodeListByXpath(doc, CLI_ROOT_XPATH);

        if(copyrightNodes.getLength() < 1) {
            return "";
        }

        String result = findNamedAttribute(copyrightNodes.item(0), "componentSHA1")
                        .map(Node::getNodeValue)
                        .orElse("");
        return result;
    }

    private String getComponent(Document doc) throws XPathExpressionException {
        NodeList copyrightNodes = getNodeListByXpath(doc, CLI_ROOT_XPATH);

        if(copyrightNodes.getLength() < 1) {
            return "";
        }

        String result = findNamedAttribute(copyrightNodes.item(0), "component")
                        .map(Node::getNodeValue)
                        .orElse("");
        return result;
    }

    private Set<LicenseNameWithText> nodeListToLicenseNamesWithTextsSet(NodeList nodes, boolean includeFilesHash) {
        Set<LicenseNameWithText> licenseNamesWithTexts = Sets.newHashSet();
        for (int i = 0; i < nodes.getLength(); i++) {
            licenseNamesWithTexts.add(includeFilesHash ? getLicenseNameWithTextFromLicenseNodeAndFileHash(nodes.item(i))
                    : getLicenseNameWithTextFromLicenseNode(nodes.item(i)));
        }
        return licenseNamesWithTexts;
    }

    private List<ObligationAtProject> nodeListToObligationList(NodeList nodes) {
        List<ObligationAtProject> obligations = Lists.newArrayList();
        for (int i = 0; i < nodes.getLength(); i++) {
            obligations.add(getObligationFromObligationNode(nodes.item(i)));
        }
        return obligations;
    }
}
