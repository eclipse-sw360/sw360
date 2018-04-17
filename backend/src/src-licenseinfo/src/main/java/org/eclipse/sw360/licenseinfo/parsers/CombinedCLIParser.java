/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.parsers;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;

/**
 * Class for extracting copyright and license information from a simple XML file
 * @author: alex.borodin@evosoft.com
 */
public class CombinedCLIParser extends AbstractCLIParser{

    private static final Logger log = Logger.getLogger(CombinedCLIParser.class);
    private static final String COPYRIGHTS_XPATH = "/CombinedCLI/Copyright";
    private static final String LICENSES_XPATH = "/CombinedCLI/License";
    private static final String COPYRIGHT_CONTENT_ELEMENT_NAME = "Content";
    private static final String EXTERNAL_ID_ATTRIBUTE_NAME = "srcComponent";
    private static final String COMBINED_CLI_ROOT_ELEMENT_NAME = "CombinedCLI";
    private static final String COMBINED_CLI_ROOT_ELEMENT_NAMESPACE = null;

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    public static final String EXTERNAL_ID_CORRELATION_KEY = "combined.cli.parser.external.id.correlation.key";

    private ComponentDatabaseHandler componentDatabaseHandler;

    public CombinedCLIParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider, ComponentDatabaseHandler componentDatabaseHandler) {
        super(attachmentConnector, attachmentContentProvider);
        this.componentDatabaseHandler = componentDatabaseHandler;
    }

    String getCorrelationKey(){
        Properties props = CommonUtils.loadProperties(CombinedCLIParser.class, PROPERTIES_FILE_PATH);
        String releaseExternalIdCorrelationKey = props.getProperty(EXTERNAL_ID_CORRELATION_KEY);
        if (isNullOrEmpty(releaseExternalIdCorrelationKey)){
            log.warn("Property combined.cli.parser.external.id.correlation.key is not set. Combined CLI parsing will not be able to load names of referenced releases");
        }
        return releaseExternalIdCorrelationKey;
    }

    @Override
    public <T> boolean isApplicableTo(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        return attachmentContent.getFilename().endsWith(XML_FILE_EXTENSION) && hasCombinedCLIRootElement(attachmentContent, user, context);
    }

    private <T> boolean hasCombinedCLIRootElement(AttachmentContent content, User user, T context) throws TException {
        return hasThisXMLRootElement(content, COMBINED_CLI_ROOT_ELEMENT_NAMESPACE, COMBINED_CLI_ROOT_ELEMENT_NAME, user, context);
    }

    @Override
    public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context) throws TException {
        AttachmentContent attachmentContent = attachmentContentProvider.getAttachmentContent(attachment);
        InputStream attachmentStream = null;
        List<LicenseInfoParsingResult> parsingResults = new ArrayList<>();
        Map<String, Release> releasesByExternalId = prepareReleasesByExternalId(getCorrelationKey());

        try {
            attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, context);
            Document doc = getDocument(attachmentStream);

            Map<String, Set<String>> copyrightSetsByExternalId = getCopyrightSetsByExternalIdsMap(doc);
            Map<String, Set<LicenseNameWithText>> licenseNamesWithTextsByExternalId = getLicenseNamesWithTextsByExternalIdsMap(doc);

            Set<String> allExternalIds = Sets.union(copyrightSetsByExternalId.keySet(), licenseNamesWithTextsByExternalId.keySet());
            allExternalIds.forEach(extId -> {
                LicenseInfoParsingResult parsingResult = getLicenseInfoParsingResultForExternalId(attachmentContent, releasesByExternalId, copyrightSetsByExternalId, licenseNamesWithTextsByExternalId, extId);
                parsingResults.add(parsingResult);
            });
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException | SW360Exception e) {
            log.error(e);
            parsingResults.add(new LicenseInfoParsingResult()
                    .setStatus(LicenseInfoRequestStatus.FAILURE)
                    .setMessage("Error while parsing combined CLI file: " + e.toString()));
        } finally {
            closeQuietly(attachmentStream, log);
        }
        return parsingResults;
    }

    private Map<String, Set<LicenseNameWithText>> getLicenseNamesWithTextsByExternalIdsMap(Document doc) throws XPathExpressionException {
        NodeList licenseNodes = getNodeListByXpath(doc, LICENSES_XPATH);
        return nodeListToLicenseNamesWithTextsSetsByExternalId(licenseNodes, EXTERNAL_ID_ATTRIBUTE_NAME);
    }

    private Map<String, Set<String>> getCopyrightSetsByExternalIdsMap(Document doc) throws XPathExpressionException {
        NodeList copyrightNodes = getNodeListByXpath(doc, COPYRIGHTS_XPATH);
        return nodeListToStringSetsByExternalId(copyrightNodes, EXTERNAL_ID_ATTRIBUTE_NAME, COPYRIGHT_CONTENT_ELEMENT_NAME);
    }

    @NotNull
    private LicenseInfoParsingResult getLicenseInfoParsingResultForExternalId(AttachmentContent attachmentContent, Map<String, Release> releasesByExternalId, Map<String, Set<String>> copyrightSetsByExternalId, Map<String, Set<LicenseNameWithText>> licenseNamesWithTextsByExternalId, String extId) {
        LicenseInfo licenseInfo = new LicenseInfo().setFilenames(Arrays.asList(attachmentContent.getFilename()));
        licenseInfo.setCopyrights(copyrightSetsByExternalId.get(extId));
        licenseInfo.setLicenseNamesWithTexts(licenseNamesWithTextsByExternalId.get(extId));
        LicenseInfoParsingResult parsingResult = new LicenseInfoParsingResult().setLicenseInfo(licenseInfo);
        Release release = releasesByExternalId.get(extId);
        if (release != null) {
            parsingResult.setVendor(release.isSetVendor() ? release.getVendor().getShortname() : "");
            parsingResult.setName(release.getName());
            parsingResult.setVersion(release.getVersion());
        } else {
            parsingResult.setName("No info found for external component ID " + extId);
        }
        parsingResult.setStatus(LicenseInfoRequestStatus.SUCCESS);
        return parsingResult;
    }

    private Map<String, Release> prepareReleasesByExternalId(String correlationKey) {
        Map<String, Release> idMap = componentDatabaseHandler.getAllReleasesIdMap();
        Map<String, Release> releasesByExternalId = idMap.values().stream()
                .filter(r -> r.getExternalIds() != null && r.getExternalIds().containsKey(correlationKey))
                .collect(Collectors.toMap(r -> r.getExternalIds().get(correlationKey), r -> r, (r1, r2) -> {
                    log.warn(String.format("Duplicate externalId in releases %s and %s", SW360Utils.printFullname(r1), SW360Utils.printFullname(r2)));
                    return r1;
                }));
        return releasesByExternalId;
    }

    private Map<String, Set<String>> nodeListToStringSetsByExternalId(NodeList nodes, String externalIdAttributeName, String contentElementName){
        Map<String, Set<String>> result = Maps.newHashMap();
        for (int i = 0; i < nodes.getLength(); i++){
            Optional<Node> externalIdOptional = findNamedAttribute(nodes.item(i), externalIdAttributeName);
            String externalId = externalIdOptional.map(Node::getNodeValue).orElse(null);
            String contentText = findNamedSubelement(nodes.item(i), contentElementName)
                    .map(AbstractCLIParser::normalizeEscapedXhtml)
                    .orElse(null);
            if (!result.containsKey(externalId)){
                result.put(externalId, Sets.newHashSet());
            }
            result.get(externalId).add(contentText);
        }
        return result;
    }

    private Map<String, Set<LicenseNameWithText>> nodeListToLicenseNamesWithTextsSetsByExternalId(NodeList nodes, String externalIdAttributeName){
        Map<String, Set<LicenseNameWithText>> result = Maps.newHashMap();
        for (int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i);
            Optional<Node> externalIdOptional = findNamedAttribute(node, externalIdAttributeName);
            String externalId = externalIdOptional.map(Node::getNodeValue).orElse(null);

            LicenseNameWithText licenseNameWithText = getLicenseNameWithTextFromLicenseNode(node);

            if (!result.containsKey(externalId)) {
                result.put(externalId, Sets.newHashSet());
            }
            result.get(externalId).add(licenseNameWithText);

        }
        return result;
    }
}
