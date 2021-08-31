/*
 * Copyright . Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.portlets.components.spdx;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.liferay.portal.kernel.xml.Element;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * SPDX portlet implementation
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public abstract class SpdxPortlet {

    private SpdxPortlet() {
        // Utility class with only static functions
    }

    private static final Logger log = LogManager.getLogger(SpdxPortlet.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static SPDXDocument parseSPDXDocumentFromRequest(String jsonData) {
        SPDXDocument spdx = new SPDXDocument();
        if (jsonData == null) {
            return null;
        }
        try {
            spdx = mapper.readValue(jsonData, SPDXDocument.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return spdx;
    }

    private static DocumentCreationInformation parseDocumentCreationInfoFromRequest(String jsonData) {
        DocumentCreationInformation documentCreationInfo = new DocumentCreationInformation();
        if (jsonData == null) {
            return null;
        }
        try {
            documentCreationInfo = mapper.readValue(jsonData, DocumentCreationInformation.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return documentCreationInfo;
    }

    private static Set<PackageInformation> parsePackageInfosFromRequest(String jsonData) {
        Set<PackageInformation> packageInfos = new HashSet<>();
        if (jsonData == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser;
        try {
            parser = factory.createParser(jsonData);
            JsonNode packageInfosJsonNode = mapper.readTree(parser);
            packageInfosJsonNode.forEach(packageInfoJson -> {
                PackageInformation packageInfo = new PackageInformation();
                try {
                    packageInfo = mapper.readValue(packageInfoJson.toString(), PackageInformation.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                packageInfos.add(packageInfo);
            });
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return packageInfos;
    }

    public static void updateSPDX(ActionRequest request, ActionResponse response, User user, String releaseId) throws TException {
        String spdxDocumentData = request.getParameter(SPDXDocument._Fields.TYPE.toString());
        String documentCreationInfoData = request.getParameter(SPDXDocument._Fields.TYPE.toString());
        String packageInfoData = request.getParameter(SPDXDocument._Fields.TYPE.toString());
        String spdxDocumentId = "";
        //
        // spdxDocumentData = "{ \"id\":\"7e5a5481c8c643dc8d030805a09738c5\", \"revision\":\"21-7225d4699bba8297997276fa85598019\", \"snippets\": [ { \"SPDXID\": \"SPDXRef-Snippet\", \"snippetFromFile\": \"./src/org/spdx/parser/DOAPProject.java\", \"snippetRanges\": [ { \"rangeType\": \"LINE\", \"startPointer\": \"5\", \"endPointer\": \"23\", \"reference\": \"./src/org/spdx/parser/DOAPProject.java\" }, { \"rangeType\": \"BYTE\", \"startPointer\": \"310\", \"endPointer\": \"420\", \"reference\": \"./src/org/spdx/parser/DOAPProject.java\" } ], \"licenseConcluded\": \"GPL-2.0\", \"licenseInfoInSnippets\": [ \"GPL-2.0\" ], \"licenseComments\": \"The concluded license was taken from package xyz, from which the snippet was copied into the current file. The concluded license information was found in the COPYING.txt file in package xyz.\", \"copyrightText\": \"Copyright 2008-2010 John Smith\", \"comment\": \"This snippet was identified as significant and highlighted in this Apache-2.0 file, when a commercial scanner identified it as being derived from file foo.c in package xyz which is licensed under GPL-2.0.\", \"name\": \"from linux kernel\", \"snippetAttributionText\": \"AAAAAAAAA\" }, { \"SPDXID\": \"SPDXRef-sdasdSnippet\", \"snippetFromFile\": \"./src/org/spdx/parser/DOAPdsadasroject.java\", \"snippetRanges\": [ { \"rangeType\": \"LINE\", \"startPointer\": \"dsadas5\", \"endPointer\": \"2dasda3\", \"reference\": \"./src/org/spdx/parser/DdasdasdOAPProject.java\" }, { \"rangeType\": \"BYTE\", \"startPointer\": \"310\", \"endPointer\": \"420\", \"reference\": \"./src/org/spdx/pdasdarser/DOAPProject.java\" } ], \"licenseConcluded\": \"GPL-2.0\", \"licenseInfoInSnippets\": [ \"GPL-2.0\" ], \"licenseComments\": \"The concluded licensedasdas was taken from package xyz, from which the snippet was copied into the current file. The concluded license information was found in the COPYING.txt file in package xyz.\", \"copyrightText\": \"Copyright 2008-2010 John Smith\", \"comment\": \"This snippet was identified adasdas significant and highlighted in this Apache-2.0 file, when a commercial scanner identified it as being derived from file foo.c in package xyz which is licensed under GPL-2.0.\", \"name\": \"from linux kernel\", \"snippetAttributionText\": \"AAAAdasdaAAAAA\" } ], \"relationships\": [ { \"spdxElementId\": \"SPDXRef-File\", \"relationshipType\": \"relationshipType_describes\", \"relatedSpdxElement\": \"./package/foo.c\", \"relationshipComment\": \"AAAAAÂÂÂAÂAA\" }, { \"spdxElementId\": \"SPDXRef-Package\", \"relationshipType\": \"relationshipType_contains\", \"relatedSpdxElement\": \"glibc\" }, { \"spdxElementId\": \"SPDXRef-Package\", \"relationshipType\": \"relationshipType_describes\", \"relatedSpdxElement\": \"glibc\" } ], \"annotations\": [ { \"annotator\": \"Person: Jane Doe ()\", \"annotationDate\": \"2010-01-29T18:30:22Z\", \"annotationType\": \"OTHER\", \"annotationComment\": \"Document level annotation\", \"spdxIdRef\": \"spdxIdRef\" }, { \"annotator\": \"Person: Jane ddsdsDoe ()\", \"annotationDate\": \"2011-01-29T18:30:22Z\", \"annotationType\": \"OTHER\", \"annotationComment\": \"Document sdsdlevel annotation\", \"spdxIdRef\": \"sdsdpdxRef\" } ], \"otherLicensingInformationDetecteds\": [ { \"licenseId\": \"Person: Jane Doe ()\", \"extractedText\": \"2010-01-29T18:30:22Z\", \"licenseName\": \"OTHER1\", \"licenseCrossRefs\": [ \"Document level annotation\", \"AAAAAAA\" ], \"licenseComment\": \"spdxRef\" }, { \"licenseId\": \"Person:dsadasd Jane Doe ()\", \"extractedText\": \"2010-01-2sdasda9T18:30:22Z\", \"licenseName\": \"OTHER\", \"licenseCrossRefs\": [ \"Document level annosdsadasdtation\", \"BBBBBBBBBBB\" ], \"licenseComment\": \"spdxdsdRef\" } ] }";
        // documentCreationInfoData = "{  \"id\":\"789057d9076b4fc28312dfe0fe380849\", \"revision\":\"1-6b3dac6fe192685c49b34df0f9e071ef\", \"spdxVersion\": \"SPDX-2.0\", \"SPDXID\": \"SPDX-2.0\", \"dataLicense\": \"CC0-1.0\", \"name\": \"SPDX-Tools-v2.0\", \"documentNamespace\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\", \"externalDocumentRefs\": [ { \"externalDocumentId\": \"DocumentRef\", \"checksum\": { \"algorithm\": \"checksumAlgorithm_sha1\", \"checksumValue\": \"d6a770ba38583ed4bb4525bd96e50461655d2759\" }, \"spdxDocument\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaa\" }, { \"externalDocumentId\": \"DocumentRgsdgsdgef\", \"checksum\": { \"algorithm\": \"checksumAlgoritsdgsdghm_sha1\", \"checksumValue\": \"d6a770ba38583edsdgsdg4bb4525bd96e50461655d2759\" }, \"spdxDocument\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\" } ], \"licenseListVersion\": \"1.1997\", \"creator\": [ { \"type\": \"Organization\", \"value\": \"ExampleCodeInspect\" }, { \"type\": \"Tool\", \"value\": \"LicenseFind-1.0\" }, { \"type\": \"Person\", \"value\": \"Jane Doe\" } ], \"created\": \"2010-01-29T18:30:22Z\", \"creatorComment\": \"This package has been shipped in source and binary form. The binaries were created with gcc 4.5.1 and expect to link to compatible system run time libraries.\", \"documentComment\": \"This document was created using SPDX 2.0 using licenses from the web sites.\", \"createdBy\": \"admin@sw360.org\" }";
        // packageInfoData = "[ { \"id\":\"6f158d9149b04b33868111cb4e757b5e\", \"revision\":\"1-4e1314347046e6b57f7b77cce175345c\", \"name\": \"glibc\", \"SPDXID\": \"SPDXRef-Package\", \"versionInfo\": \"2.11.1\", \"packageFileName\": \"glibc-2.11.1.tar.gz\", \"supplier\": \"Person: Jane Doe (jane.doe@example.com)\", \"originator\": \"Organization: ExampleCodeInspect (contact@example.com)\", \"downloadLocation\": \"http://ftp.gnu.org/gnu/glibc/glibc-ports-2.15.tar.gz\", \"filesAnalyzed\": true, \"packageVerificationCode\": { \"excludedFiles\": [ \"excludes: ./package.spdx\", \"AAAAAAAAAAAAAAAAAAAAAAAAA\", \"SSSSSSSSSSSSSSSSSSSSSSSSS\" ], \"value\": \"d6a770ba38583ed4bb4525bd96e50461655d2758\" }, \"checksums\": [ { \"algorithm\": \"2222222222222222\", \"checksumValue\": \"111111111111111\" }, { \"algorithm\": \"2222222222222\", \"checksumValue\": \"2222222222222\" } ], \"homepage\": \"http://ftp.gnu.org/gnu/glibc\", \"sourceInfo\": \"uses glibc-2_11-branch from git://sourceware.org/git/glibc.git.\", \"licenseConcluded\": \"ewqewqeeeeee\", \"licenseInfoFromFiles\": [ \"GPL-2.0\", \"LicenseRef-2\", \"LicenseRef-1\" ], \"licenseDeclared\": \"(LicenseRef-3 AND LGPL-2.0)\", \"licenseComments\": \"The license for this project changed with the release of version x.y.  The version of the project included here post-dates the license change.\", \"copyrightText\": \"Copyright 2008-2010 John Smith a\", \"summary\": \"GNU C library.\", \"description\": \"The GNU C Library defines functions that are specified by the ISO C standard, as well as additional features specific to POSIX and other derivatives of the Unix operating system, and extensions specific to GNU systems.\", \"externalRefs\": [ { \"referenceCategory\": \"referenceCategory_other\", \"referenceLocator\": \"acmecorp/acmenator/4.1.3-alpha\", \"referenceType\": \"http://spdx.org/spdxdocs/spdx-example-444504E0-4F89-41D3-9A0C-0305E82C3301#LocationRef-acmeforge\", \"comment\": \"This is the external ref for Acme\" }, { \"referenceCategory\": \"referenceCategory_security\", \"referenceLocator\": \"cpe:2.3:a:pivotal_software:spring_framework:4.1.0:*:*:*:*:*:*:*\", \"referenceType\": \"cpe23Type\" } ], \"attributionText\": [], \"annotations\": [ { \"annotator\": \"Person: Package Commenter\", \"annotationDate\": \"2011-01-29T18:30:22Z\", \"annotationType\": \"OTHER\", \"annotationComment\": \"Package level annotation\" }, { \"annotator\": \"Person: Packdsdsdsge Commenter\", \"annotationDate\": \"2011-11-29T18:30:22Z\", \"annotationType\": \"OTHER\", \"annotationComment\": \"Package levedsadasdaddasl annotation\" } ]} ]";
        //
        if (!isNullOrEmpty(spdxDocumentData)) {
            SPDXDocument spdx = parseSPDXDocumentFromRequest(spdxDocumentData);
            SPDXDocumentService.Iface spdxClient = new ThriftClients().makeSPDXClient();
            if (spdx != null) {
                if (isNullOrEmpty(spdx.getReleaseId()) && ! isNullOrEmpty(releaseId)) {
                    spdx.setReleaseId(releaseId);
                }
                if (isNullOrEmpty(spdx.getId())) {
                    spdxDocumentId = spdxClient.addSPDXDocument(spdx, user).getId();
                } else {
                    spdxClient.updateSPDXDocument(spdx, user);
                    spdxDocumentId = spdx.getId();
                }
            }
        }
        if (!isNullOrEmpty(documentCreationInfoData)) {
            DocumentCreationInformation document = parseDocumentCreationInfoFromRequest(documentCreationInfoData);
            if (isNullOrEmpty(document.getSpdxDocumentId())) {
                document.setSpdxDocumentId(spdxDocumentId);
            }
            if (document != null) {
                DocumentCreationInformationService.Iface documentClient = new ThriftClients().makeSPDXDocumentInfoClient();
                if (isNullOrEmpty(document.getId())) {
                    documentClient.addDocumentCreationInformation(document, user);
                } else {
                    documentClient.updateDocumentCreationInformation(document, user);
                }
            }
        }
        if (!isNullOrEmpty(packageInfoData)) {
            Set<PackageInformation> packageInfos = parsePackageInfosFromRequest(packageInfoData);
            if (packageInfos != null) {
                PackageInformationService.Iface packageClient = new ThriftClients().makeSPDXPackageInfoClient();
                for (PackageInformation packageInfo : packageInfos) {
                    if (isNullOrEmpty(packageInfo.getSpdxDocumentId())) {
                       packageInfo.setSpdxDocumentId(spdxDocumentId);
                    }
                    if (isNullOrEmpty(packageInfo.getId())) {
                        packageClient.addPackageInformation(packageInfo, user);
                    } else {
                        packageClient.updatePackageInformation(packageInfo, user);
                    }
                }
            }
        }
    }

}
