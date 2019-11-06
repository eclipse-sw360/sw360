/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;

/**
 * Abstract class with common helper methods for CLIParser and CombinedCLIParser
 *
 * @author: alex.borodin@evosoft.com
 */
public abstract class AbstractCLIParser extends LicenseInfoParser {
    private static final String LICENSE_CONTENT_ELEMENT_NAME = "Content";
    private static final String LICENSE_ACKNOWLEDGEMENTS_ELEMENT_NAME = "Acknowledgements";
    protected static final String XML_FILE_EXTENSION = ".xml";
    private static final String LICENSENAME_ATTRIBUTE_NAME = "name";
    private static final String SPDX_IDENTIFIER_ATTRIBUTE_NAME = "spdxidentifier";
    private static final String TYPE_ATTRIBUTE_NAME = "type";
    private static final String LICENSE_NAME_UNKNOWN = "License name unknown";
    private static final String TYPE_UNKNOWN = "Type unknown";
    private static final String OBLIGATION_TEXT_UNKNOWN = "Obligation text unknown";
    private static final Logger log = Logger.getLogger(CLIParser.class);
    private static final String SPDX_IDENTIFIER_UNKNOWN = "SPDX identifier unknown";
    private static final String OBLIGATION_TOPIC_UNKNOWN = "Obligation topic unknown";

    private static final String OBLIGATION_TOPIC_ELEMENT_NAME = "Topic";
    private static final String OBLIGATION_TEXT_ELEMENT_NAME = "Text";
    private static final String OBLIGATION_LICENSE_ELEMENT_NAME = "Licenses";

    public AbstractCLIParser(AttachmentConnector attachmentConnector, AttachmentContentProvider attachmentContentProvider) {
        super(attachmentConnector, attachmentContentProvider);
    }

    @Override
    public List<String> getApplicableFileExtensions() {
        return Collections.singletonList(XML_FILE_EXTENSION);
    }

    protected static String normalizeEscapedXhtml(Node node) {
        return StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeXml(node.getTextContent().trim()));
    }

    protected static String normalizeSpace(Node node) {
        return StringUtils.normalizeSpace(node.getTextContent());
    }

    protected Optional<Node> findNamedAttribute(Node node, String name) {
        NamedNodeMap childNodes = node.getAttributes();
        return Optional.ofNullable(childNodes.getNamedItem(name));
    }

    protected Optional<Node> findNamedSubelement(Node node, String name) {
        NodeList childNodes = node.getChildNodes();
        return streamFromNodeList(childNodes).filter(n -> n.getNodeName().equals(name)).findFirst();
    }

    private Stream<Node> streamFromNodeList(NodeList nodes) {
        Iterator<Node> iter = new NodeListIterator(nodes);
        Iterable<Node> iterable = () -> iter;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    protected <T> boolean hasThisXMLRootElement(AttachmentContent content, String rootElementNamespace, String rootElementName, User user, T context) throws TException {
        XMLInputFactory xmlif = XMLInputFactory.newFactory();
        XMLStreamReader xmlStreamReader = null;
        InputStream attachmentStream = null;
        try {
            attachmentStream = attachmentConnector.getAttachmentStream(content, user, context);
            xmlStreamReader = xmlif.createXMLStreamReader(attachmentStream);

            //skip to first element
            while (xmlStreamReader.hasNext() && xmlStreamReader.next() != XMLStreamConstants.START_ELEMENT) ;
            xmlStreamReader.require(XMLStreamConstants.START_ELEMENT, rootElementNamespace, rootElementName);
            return true;
        } catch (XMLStreamException | SW360Exception e) {
            return false;
        } finally {
            if (null != xmlStreamReader) {
                try {
                    xmlStreamReader.close();
                } catch (XMLStreamException e) {
                    // ignore it
                }
            }
            closeQuietly(attachmentStream, log);
        }
    }

    protected Document getDocument(InputStream attachmentStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(attachmentStream);
    }

    protected NodeList getNodeListByXpath(Document doc, String xpathString) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression xpathExpression = xpath.compile(xpathString);
        return (NodeList) xpathExpression.evaluate(doc, XPathConstants.NODESET);
    }

    protected Set<String> nodeListToStringSet(NodeList nodes) {
        Set<String> strings = Sets.newHashSet();
        for (int i = 0; i < nodes.getLength(); i++) {
            strings.add(normalizeEscapedXhtml(nodes.item(i)));
        }
        return strings;
    }

    protected LicenseNameWithText getLicenseNameWithTextFromLicenseNode(Node node) {
        return new LicenseNameWithText()
                .setLicenseText(findNamedSubelement(node, LICENSE_CONTENT_ELEMENT_NAME)
                        .map(AbstractCLIParser::normalizeEscapedXhtml)
                        .orElse(null))
                .setAcknowledgements(findNamedSubelement(node, LICENSE_ACKNOWLEDGEMENTS_ELEMENT_NAME)
                        .map(AbstractCLIParser::normalizeEscapedXhtml)
                        .orElse(null))
                .setLicenseName(findNamedAttribute(node, LICENSENAME_ATTRIBUTE_NAME)
                        .map(Node::getNodeValue)
                        .orElse(LICENSE_NAME_UNKNOWN))
                .setLicenseSpdxId(findNamedAttribute(node, SPDX_IDENTIFIER_ATTRIBUTE_NAME)
                        .map(AbstractCLIParser::normalizeSpace)
                        .orElse(SPDX_IDENTIFIER_UNKNOWN))
                .setType(findNamedAttribute(node, TYPE_ATTRIBUTE_NAME)
                        .map(Node::getNodeValue)
                        .orElse(TYPE_UNKNOWN));
    }

    protected Obligation getObligationFromObligationNode(Node node) {

        return new Obligation()
                .setTopic(findNamedSubelement(node, OBLIGATION_TOPIC_ELEMENT_NAME)
                    .map(Node::getFirstChild)
                    .map(AbstractCLIParser::normalizeSpace)
                    .orElse(OBLIGATION_TOPIC_UNKNOWN))
                .setText(findNamedSubelement(node, OBLIGATION_TEXT_ELEMENT_NAME)
                    .map(Node::getFirstChild)
                    .map(Node::getTextContent)
                    .orElse(OBLIGATION_TEXT_UNKNOWN))
                .setLicenseIDs(findNamedSubelement(node, OBLIGATION_LICENSE_ELEMENT_NAME)
                    .map((Node n) -> {
                        List<String> strings = new ArrayList<String>();
                        NodeListIterator it = new NodeListIterator(n.getChildNodes());
                        while (it.hasNext()) {
                            Node child = it.next();
                            if(child.getFirstChild() != null) {
                                strings.add(normalizeSpace(child));
                            }
                        }
                        return strings;
                    })
                    .orElse(new ArrayList<String>()));
    }

    protected class NodeListIterator implements Iterator<Node> {
        private final NodeList nodes;
        private int i;

        public NodeListIterator(NodeList nodes) {
            this.nodes = nodes;
            this.i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < nodes.getLength();
        }

        @Override
        public Node next() {
            if (hasNext()) {
                i++;
                return nodes.item(i - 1);
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
