/*
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Copyright Siemens AG, 2018-2019
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.apache.jena.ext.xerces.util.XMLChar;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import org.apache.jena.rdf.model.impl.Util;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

public class SPDXParserTools {
    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String PROPERTY_KEY_USE_LICENSE_INFO_FROM_FILES = "licenseinfo.spdxparser.use-license-info-from-files";
    private static final boolean USE_LICENSE_INFO_FROM_FILES;

    private static final String XML_LITERAL = "^^http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";
    private static final String LICENSE_REF_PREFIX = "LicenseRef-";
    private static final String RELATIONSHIP_TYPE_DESCRIBES = "relationshipType_describes";

    // Namespace
    private static final String SPDX_NAMESPACE = "http://spdx.org/rdf/terms#";
    private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    // RDF Properties
    private static final String RDF_ABOUT = "about";
    private static final String RDF_RESOURCE = "resource";
    private static final String RDF_NODEID = "nodeID";

    // SPDX Class
    private static final String SPDX_FILE = "File";
    private static final String SPDX_LICENSE = "License";
    private static final String SPDX_PACKAGE = "Package";
    private static final String SPDX_EXTRACTED_LICENSING_INFO = "ExtractedLicensingInfo";
    private static final String SPDX_CONJUNCTIVE_LICENSE_SET = "ConjunctiveLicenseSet";
    private static final String SPDX_DISJUNCTIVE_LICENSE_SET = "DisjunctiveLicenseSet";

    private static final String SPDX_LICENSE_CONCLUDED = "licenseConcluded";
    private static final String SPDX_MEMBER = "member";
    private static final String SPDX_NAME = "name";
    private static final String SPDX_LICENSE_ID = "licenseId";
    private static final String SPDX_LICENSE_TEXT = "licenseText";
    private static final String SPDX_EXTRACTED_TEXT = "extractedText";
    private static final String SPDX_LICENSE_INFO_IN_FILE = "licenseInfoInFile";
    private static final String SPDX_LICENSE_INFO_FROM_FILES = "licenseInfoFromFiles";
    private static final String SDPX_LICENSE_INFO_IN_SNIPPETS = "licenseInfoInSnippet";
    private static final String SPDX_SNIPPET_FROM_FILE = "snippetFromFile";
    private static final String SPDX_LICENSE_DECLARED = "licenseDeclared";
    private static final String SPDX_COPYRIGHT_TEXT = "copyrightText";
    private static final String SPDX_RELATIONSHIP_TYPE = "relationshipType";
    private static final String SPDX_RELATED_SPDX_ELEMENT = "relatedSpdxElement";
    private static final String SPDX_LICENSE_NAME_VERSION_1 = "licenseName"; // old property name (pre 1.2 spec)
    private static final String SPDX_REFERENCES_FILE = "referencesFile";
    private static final String SPDX_HAS_FILE = "hasFile";
    private static final String SPDX_DESCRIBES_PACKAGE = "describesPackage"; // old property name (pre 1.2 spec)

    // Store spdx:License and spdx:ExtractedLicensingInfo index by their URI
    private static HashMap<String, LicenseNameWithText> uriLicenseMap = new HashMap<String, LicenseNameWithText>();

    // Store spdx:referencesFile index by their nodeID
    private static HashMap<String, Element> nodeIDFileMap = new HashMap<String, Element>();

    static {
        Properties properties = CommonUtils.loadProperties(SPDXParserTools.class, PROPERTIES_FILE_PATH);
        USE_LICENSE_INFO_FROM_FILES = Boolean
                .valueOf(properties.getOrDefault(PROPERTY_KEY_USE_LICENSE_INFO_FROM_FILES, "true").toString());
    }

    // Make NodeList be iterable
    private static Iterable<Node> iterable(final NodeList nodeList) {
        return () -> new Iterator<Node>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            @Override
            public Node next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return nodeList.item(index++);
            }
        };
    }

    /*
     * Get localName. Examples: http://spdx.org/rdf/terms#noassertion: localName is
     * noassertion. http://spdx.org/licenses/LGPL-2.1: localName is LGPL-2.1.
     * spdx:Package: localName is Package.
     */
    private static String getLocalName(String label) {
        for (int i = 0; i < label.length(); i++) {
            // label contain invalid NCName
            if (!XMLChar.isNCName(label.charAt(i))) {
                if (Util.splitNamespaceXML(label) == label.length()) {
                    try {
                        label = URLDecoder.decode(label, StandardCharsets.UTF_8.name());
                        URL url = new URL(label);
                        if (null != url.getRef()) {
                            return url.getRef();
                        } else {
                            String path = url.getPath();
                            return path.substring(path.lastIndexOf('/') + 1);
                        }
                    } catch (UnsupportedEncodingException | MalformedURLException e) {
                      return "";
                    }
                }
                return label.substring(Util.splitNamespaceXML(label));
            }
        }
        return label;
    }

    /*
     * Get node name without its namespace
     */
    private static String getNodeName(Node n) {
        return n != null ? getLocalName(n.getNodeName()) : "";
    }

    /*
     * Get localName in attribute rdf:about or rdf:resource
     */
    private static String getResourceLocalName(Node n) {
        if (n instanceof Element) {
            Element e = (Element) n;
            String[] attrs = { RDF_ABOUT, RDF_RESOURCE };
            for (String attr : attrs) {
                String label = e.getAttributeNS(RDF_NAMESPACE, attr);
                if (!isNullEmptyOrWhitespace(label)) {
                    return getLocalName(label);
                }
            }
        }
        return "";
    }

    /*
     * Find all nodes (in all levels) with 'localName' inside node 'parent'.
     */
    private static Node[] findMultipleSpdxNodes(Node parent, String localName) {
        NodeList childNodes = null;
        if (parent instanceof Element) {
            Element e = (Element) parent;
            childNodes = e.getElementsByTagNameNS(SPDX_NAMESPACE, localName);

            // In ealier version (eg. v1.1), spdx document doesn't have namespace.
            if (childNodes.getLength() == 0) {
                childNodes = e.getElementsByTagName(localName);
            }
        } else if (parent instanceof Document) {
            Document doc = (Document) parent;
            childNodes = doc.getElementsByTagNameNS(SPDX_NAMESPACE, localName);

            // In ealier version (eg. v1.1), spdx document doesn't have namespace.
            if (childNodes.getLength() == 0) {
                childNodes = doc.getElementsByTagName(localName);
            }
        }

        if (childNodes == null) {
            return new Node[] {};
        }

        // Convert NodeList to Array
        int noChilds = childNodes.getLength();
        Node[] children = new Node[noChilds];
        for (int i = 0; i < noChilds; i++) {
            children[i] = childNodes.item(i);
        }

        return children;
    }

    /*
     * Find all childs with 'localName' in node 'parent'.
     */
    private static Node[] findMultipleSpdxChilds(Node parent, String localName) {
        List<Node> childList = new ArrayList<>();
        NodeList childs = parent.getChildNodes();
        for (Node child : iterable(childs)) {
            if (!isNullEmptyOrWhitespace(child.getLocalName()) && child.getLocalName().equals(localName)) {
                childList.add(child);
            }
        }

        // Convert List to Array
        return (Node[]) childList.toArray(new Node[0]);
    }

    /*
     * Find content of the first node that matches query.
     */
    private static String findFirstSpdxNodeContent(Node parent, String localName) {
        Node[] childNodes = findMultipleSpdxNodes(parent, localName);
        return childNodes.length > 0 ? childNodes[0].getTextContent() : "";
    }

    /*
     * Find content of the first child that matches query.
     */
    private static String findFirstSpdxChildContent(Node parent, String localName) {
        Node[] childNodes = findMultipleSpdxChilds(parent, localName);
        return childNodes.length > 0 ? childNodes[0].getTextContent() : "";
    }

    /*
     * Get license name of spdx:ExtractedLicensingInfo.
     */
    private static String getExtractedLicenseName(Node extractedLicenseInfo) {
        String[] tagNames = { SPDX_NAME, SPDX_LICENSE_NAME_VERSION_1, SPDX_LICENSE_ID };
        for (String tagName : tagNames) {
            String name = findFirstSpdxNodeContent(extractedLicenseInfo, tagName);
            if (!isNullEmptyOrWhitespace(name)) {
                return name.replace(LICENSE_REF_PREFIX, "");
            }
        }
        return "";
    }

    /*
     * Get content of spdx:extractedText.
     */
    private static String getExtractedText(Node extractedLicenseInfo) {
        return findFirstSpdxNodeContent(extractedLicenseInfo, SPDX_EXTRACTED_TEXT);
    }

    /*
     * Get content of spdx:licenseText.
     */
    private static String getLicenseText(Node license) {
        String licenseText = findFirstSpdxNodeContent(license, SPDX_LICENSE_TEXT);
        if (!isNullEmptyOrWhitespace(licenseText) && licenseText.endsWith(XML_LITERAL)) {
            licenseText = licenseText.substring(0, licenseText.length() - XML_LITERAL.length());
        }
        return licenseText;
    }

    /*
     * Get spdx:member nodes.
     */
    private static Node[] getMembers(Node licenseSet) {
        return findMultipleSpdxChilds(licenseSet, SPDX_MEMBER);
    }

    /*
     * Get license ID in spdx:licenseId.
     */
    private static String getLicenseId(Node spdxLicenseInfo) {
        return findFirstSpdxNodeContent(spdxLicenseInfo, SPDX_LICENSE_ID).replace(LICENSE_REF_PREFIX, "");
    }

    /*
     * Get content of spdx:copyrightText.
     */
    private static String getCopyrightText(Node spdxItem) {
        return findFirstSpdxChildContent(spdxItem, SPDX_COPYRIGHT_TEXT);
    }

    /*
     * Get spdx:licenseConcluded node.
     */
    private static Node getLicenseConcluded(Node spdxItem) {
        Node[] licenseConcludedNodes = findMultipleSpdxChilds(spdxItem, SPDX_LICENSE_CONCLUDED);
        return licenseConcludedNodes.length > 0 ? licenseConcludedNodes[0] : null;
    }

    /*
     * Get license info from File. This info is in spdx:licenseInfoInFile
     * spdx:licenseInfoInSnippet or spdx:licenseInfoFromFiles.
     */
    private static Node[] getLicenseInfoFromFiles(Node spdxItem) {
        String licenseInfoNodeName = SPDX_LICENSE_INFO_FROM_FILES;
        switch (getNodeName(spdxItem)) {
            case SPDX_FILE:
                licenseInfoNodeName = SPDX_LICENSE_INFO_IN_FILE;
                break;
            case SPDX_SNIPPET_FROM_FILE:
                licenseInfoNodeName = SDPX_LICENSE_INFO_IN_SNIPPETS;
                break;
            default:
                break;
        }

        return findMultipleSpdxChilds(spdxItem, licenseInfoNodeName);
    }

    /*
     * Get spdx:licenseDeclared in spdx:Package.
     */
    private static Node getLicenseDeclared(Node spdxPackage) {
        Node[] licenseDeclareds = findMultipleSpdxChilds(spdxPackage, SPDX_LICENSE_DECLARED);
        return licenseDeclareds.length > 0 ? licenseDeclareds[0] : null;
    }

    /*
     * Get all licenses with text in spdx:ExtractedLicensingInfo and spdx:License
     * with their uri.
     */
    private static HashMap<String, LicenseNameWithText> getLicenseTextFromMetadata(Document doc) {
        HashMap<String, LicenseNameWithText> uriLicenseMap = new HashMap<String, LicenseNameWithText>();
        String[] licenseTags = { SPDX_EXTRACTED_LICENSING_INFO, SPDX_LICENSE };

        for (String licenseTag : licenseTags) {
            Node[] licenses = findMultipleSpdxNodes(doc, licenseTag);
            for (Node license : licenses) {
                if (license instanceof Element) {
                    Element e = (Element) license;
                    String uri = e.getAttributeNS(RDF_NAMESPACE, RDF_ABOUT);

                    // In earlier version, rdf:nodeID is used instead of rdf:about
                    if (isNullEmptyOrWhitespace(uri))
                        uri = e.getAttributeNS(RDF_NAMESPACE, RDF_NODEID);

                    if (isNullEmptyOrWhitespace(uri))
                        continue;

                    String licenseName = extractLicenseName(e);
                    String licenseID = getLicenseId(e);
                    String licenseText = getExtractedText(e);
                    if (isNullEmptyOrWhitespace(licenseText))
                        licenseText = getLicenseText(e);
                    uriLicenseMap.put(uri, new LicenseNameWithText().setLicenseName(licenseName)
                            .setLicenseText(licenseText).setLicenseSpdxId(licenseID));
                }
            }
        }

        return uriLicenseMap;
    }

    /*
     * Get all spdx:File in spdx:referencesFile. In ealier version, File is not in
     * Package, but referencesFile.
     */
    private static HashMap<String, Element> getFileFromMetadata(Document doc) {
        HashMap<String, Element> nodeIDFileMap = new HashMap<String, Element>();
        Node[] referencesFiles = findMultipleSpdxNodes(doc, SPDX_REFERENCES_FILE);
        for (Node referencesFile : referencesFiles) {
            Node[] spdxFiles = findMultipleSpdxNodes(referencesFile, SPDX_FILE);
            for (Node spdxFile : spdxFiles) {
                if (spdxFile instanceof Element) {
                    Element e = (Element) spdxFile;
                    String uri = e.getAttributeNS(RDF_NAMESPACE, RDF_NODEID);
                    if (!isNullEmptyOrWhitespace(uri)) {
                        nodeIDFileMap.put(uri, e);
                    }
                }
            }
        }
        return nodeIDFileMap;
    }

    /*
     * Find license in uriLicenseMap.
     */
    private static LicenseNameWithText findInURILicenseMap(Node node) {
        if (node instanceof Element) {
            Element e = (Element) node;
            String uri = e.getAttributeNS(RDF_NAMESPACE, RDF_RESOURCE);

            // In earlier version (ex. v1.2), license is indexed by nodeID
            if (isNullEmptyOrWhitespace(uri))
                uri = e.getAttributeNS(RDF_NAMESPACE, RDF_NODEID);

            if (!isNullEmptyOrWhitespace(uri) && uriLicenseMap.containsKey(uri))
                return uriLicenseMap.get(uri);
        }

        return null;
    }

    /*
     * Find file in nodeIDFileMap.
     */
    private static Node findInNodeIDFileMap(Node node) {
        if (node instanceof Element) {
            Element e = (Element) node;
            String uri = e.getAttributeNS(RDF_NAMESPACE, RDF_NODEID);
            if (!isNullEmptyOrWhitespace(uri) && nodeIDFileMap.containsKey(uri))
                return nodeIDFileMap.get(uri);
        }

        return null;
    }

    /*
     * Get spdx:File in spdx:Package.
     */
    private static Node[] getFiles(Node spdxPackage) {
        Node[] filesInPackage = findMultipleSpdxNodes(spdxPackage, SPDX_FILE);

        // In earlier version, spdx:File is stored in spdx:referencesFile,
        // and spdx:Package contains spdx:hasFile map by nodeID.
        List<Node> fileList = new ArrayList<>();
        Node[] hasFiles = findMultipleSpdxNodes(spdxPackage, SPDX_HAS_FILE);
        for (Node hasFile : hasFiles) {
            Node file = findInNodeIDFileMap(hasFile);
            if (file != null)
                fileList.add(file);

        }

        Node[] spdxFiles = new Node[fileList.size() + filesInPackage.length];
        int i = 0;
        for (Node spdxFile : fileList) {
            spdxFiles[i] = spdxFile;
            i++;
        }
        for (Node spdxFile : filesInPackage) {
            spdxFiles[i] = spdxFile;
            i++;
        }

        return spdxFiles;
    }

    /*
     * Get all Elements that are child of all spdx:redlatedSpdxElement that have
     * relationshipType_describes with document.
     */
    private static List<Node> getDocumentDescribes(Document doc) {
        Node[] relTypeNodes = findMultipleSpdxNodes(doc, SPDX_RELATIONSHIP_TYPE);
        List<Node> retval = new ArrayList<>();
        for (Node relTypeNode : relTypeNodes) {
            String relType = getResourceLocalName(relTypeNode);
            if (relType.equals(RELATIONSHIP_TYPE_DESCRIBES)) {
                Node parentNode = relTypeNode.getParentNode();
                Node[] relatedElementNodes = findMultipleSpdxNodes(parentNode, SPDX_RELATED_SPDX_ELEMENT);
                for (Node relatedElementNode : relatedElementNodes) {
                    NodeList packageNodes = relatedElementNode.getChildNodes();
                    for (Node child : iterable(packageNodes)) {
                        retval.add(child);
                    }
                }
            }
        }

        // In ealier version, spdx used spdx:describesPackage instead
        Node[] describesPackages = findMultipleSpdxNodes(doc, SPDX_DESCRIBES_PACKAGE);
        for (Node describesPackage : describesPackages) {
            NodeList packageNodes = describesPackage.getChildNodes();
            for (Node child : iterable(packageNodes)) {
                retval.add(child);
            }
        }

        return retval;
    }

    private static String extractLicenseName(Node license) {
        switch (getNodeName(license)) {
            case SPDX_EXTRACTED_LICENSING_INFO:
                return getExtractedLicenseName(license);
            default:
                LicenseNameWithText lwt = findInURILicenseMap(license);
                if (lwt != null) {
                    return lwt.getLicenseName();
                }

                return getResourceLocalName(license).replace(LICENSE_REF_PREFIX, "");
        }
    }

    private static Stream<LicenseNameWithText> getAllLicenseTextsFromInfo(Node spdxLicenseInfo) {
        switch (getNodeName(spdxLicenseInfo)) {
            case SPDX_CONJUNCTIVE_LICENSE_SET:
            case SPDX_DISJUNCTIVE_LICENSE_SET:
                return Arrays.stream(getMembers(spdxLicenseInfo)).flatMap(SPDXParserTools::getAllLicenseTextsFromInfo);
            case SPDX_EXTRACTED_LICENSING_INFO:
                return Stream.of(new LicenseNameWithText().setLicenseName(extractLicenseName(spdxLicenseInfo))
                        .setLicenseText(getExtractedText(spdxLicenseInfo))
                        .setLicenseSpdxId(getLicenseId(spdxLicenseInfo)));
            case SPDX_LICENSE:
                return Stream.of(new LicenseNameWithText().setLicenseName(extractLicenseName(spdxLicenseInfo))
                        .setLicenseText(getLicenseText(spdxLicenseInfo))
                        .setLicenseSpdxId(getLicenseId(spdxLicenseInfo)));
            case SPDX_LICENSE_CONCLUDED:
            case SPDX_MEMBER:
                if (spdxLicenseInfo.hasChildNodes()) {
                    NodeList childNodes = spdxLicenseInfo.getChildNodes();
                    return IntStream.range(0, childNodes.getLength()).mapToObj(childNodes::item)
                            .flatMap(SPDXParserTools::getAllLicenseTextsFromInfo);
                }
            default:
                LicenseNameWithText lwt = findInURILicenseMap(spdxLicenseInfo);
                if (lwt != null) {
                    return Stream.of(lwt);
                }

                String licenseName = extractLicenseName(spdxLicenseInfo);
                if (!isNullEmptyOrWhitespace(licenseName)) {
                    return Stream.of(new LicenseNameWithText().setLicenseName(licenseName).setLicenseText(""));
                }
        }
        return Stream.empty();
    }

    private static Set<LicenseNameWithText> getAllLicenseTexts(Node spdxItem, boolean useLicenseInfoFromFiles,
            boolean includeConcludedLicense) {
        Set<LicenseNameWithText> licenseTexts = new HashSet<>();
        if (includeConcludedLicense) {
            licenseTexts = getAllLicenseTextsFromInfo(getLicenseConcluded(spdxItem))
                    .collect(Collectors.toCollection(HashSet::new));
        }

        if (useLicenseInfoFromFiles) {
            licenseTexts.addAll(Arrays.stream(getLicenseInfoFromFiles(spdxItem))
                    .flatMap(SPDXParserTools::getAllLicenseTextsFromInfo)
                    .collect(Collectors.toCollection(HashSet::new)));
        }

        if (getNodeName(spdxItem).equals(SPDX_PACKAGE)) {
            licenseTexts.addAll(getAllLicenseTextsFromInfo(getLicenseDeclared(spdxItem))
                    .collect(Collectors.toCollection(HashSet::new)));

            for (Node spdxFile : getFiles(spdxItem)) {
                licenseTexts.addAll(getAllLicenseTexts(spdxFile, useLicenseInfoFromFiles, includeConcludedLicense));
            }
        }

        return licenseTexts;
    }

    private static Set<String> getAllConcludedLicenseIds(Node spdxLicenseInfo) {
        Set<String> result = Sets.newHashSet();

        switch (getNodeName(spdxLicenseInfo)) {
            case SPDX_CONJUNCTIVE_LICENSE_SET:
            case SPDX_DISJUNCTIVE_LICENSE_SET:
                result.addAll(Arrays.stream(getMembers(spdxLicenseInfo))
                        .flatMap(setMember -> SPDXParserTools.getAllConcludedLicenseIds(setMember).stream())
                        .collect(Collectors.toSet()));
                break;
            case SPDX_LICENSE_CONCLUDED:
            case SPDX_MEMBER:
                if (spdxLicenseInfo.hasChildNodes()) {
                    NodeList childNodes = spdxLicenseInfo.getChildNodes();
                    for (Node child : iterable(childNodes)) {
                        result.addAll(getAllConcludedLicenseIds(child));
                    }
                    break;
                }
            default:
                String licenseId = getLicenseId(spdxLicenseInfo);
                if (!isNullEmptyOrWhitespace(licenseId)) {
                    result.add(licenseId);
                } else {
                    LicenseNameWithText lwt = findInURILicenseMap(spdxLicenseInfo);
                    if (lwt != null)
                        result.add(lwt.getLicenseSpdxId());
                    else {
                        licenseId = getResourceLocalName(spdxLicenseInfo);
                        if (!isNullEmptyOrWhitespace(licenseId))
                            result.add(licenseId.replace(LICENSE_REF_PREFIX, ""));
                    }
                }
                break;
        }

        return result;
    }

    private static Stream<String> getAllCopyrights(Node spdxItem) {
        Stream<String> copyrights = Stream.empty();
        String copyrightText = getCopyrightText(spdxItem).trim();
        if (!isNullEmptyOrWhitespace(copyrightText)) {
            copyrights = Stream.of(copyrightText);
        }

        if (getNodeName(spdxItem).equals(SPDX_PACKAGE)) {
            copyrights = Stream.concat(copyrights,
                    Arrays.stream(getFiles(spdxItem)).flatMap(spdxFile -> getAllCopyrights(spdxFile)));
        }
        return copyrights;
    }

    protected static LicenseInfoParsingResult getLicenseInfoFromSpdx(AttachmentContent attachmentContent,
            boolean includeConcludedLicense, Document doc) {
        LicenseInfo licenseInfo = new LicenseInfo().setFilenames(Arrays.asList(attachmentContent.getFilename()));
        licenseInfo.setLicenseNamesWithTexts(new HashSet<>());
        licenseInfo.setCopyrights(new HashSet<>());
        Set<String> concludedLicenseIds = Sets.newHashSet();

        uriLicenseMap = getLicenseTextFromMetadata(doc);
        nodeIDFileMap = getFileFromMetadata(doc);

        for (Node spdxItem : getDocumentDescribes(doc)) {
            licenseInfo.getLicenseNamesWithTexts()
                    .addAll(getAllLicenseTexts(spdxItem, USE_LICENSE_INFO_FROM_FILES, includeConcludedLicense));
            licenseInfo.getCopyrights().addAll(getAllCopyrights(spdxItem).collect(Collectors.toSet()));
            if (getNodeName(spdxItem).equals(SPDX_PACKAGE)) {
                concludedLicenseIds.addAll(getAllConcludedLicenseIds(getLicenseConcluded(spdxItem)));
            }
        }
        licenseInfo.setConcludedLicenseIds(concludedLicenseIds);

        return new LicenseInfoParsingResult().setLicenseInfo(licenseInfo).setStatus(LicenseInfoRequestStatus.SUCCESS);
    }
}
