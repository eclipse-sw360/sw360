/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.outputGenerators;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.thrift.TException;
import org.apache.xmlbeans.XmlException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Todo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.licenseinfo.outputGenerators.DocxUtils.*;

public class DocxGenerator extends OutputGenerator<byte[]> {

    private static final String UNKNOWN_LICENSE_NAME = "Unknown license name";
    private static final String UNKNOWN_FILE_NAME = "Unknown file name";
    private static final String TODO_DEFAULT_TEXT = "todo not determined so far.";

    private static final String DOCX_TEMPLATE_FILE = "/templateFrontpageContent.docx";
    private static final String DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String DOCX_OUTPUT_TYPE = "docx";

    public DocxGenerator(OutputFormatVariant outputFormatVariant, String description) {
        super(DOCX_OUTPUT_TYPE, description, true, DOCX_MIME_TYPE, outputFormatVariant);
    }

    @Override
    public byte[] generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, String projectName, String projectVersion, String licenseInfoHeaderText) throws SW360Exception {
        ByteArrayOutputStream docxOutputStream = new ByteArrayOutputStream();
        Optional<byte[]> docxTemplateFile = CommonUtils.loadResource(DocxGenerator.class, DOCX_TEMPLATE_FILE);
        if (docxTemplateFile.isPresent()) {
            try {
                XWPFDocument xwpfDocument = new XWPFDocument(new ByteArrayInputStream(docxTemplateFile.get()));
                switch (getOutputVariant()) {
                    case DISCLOSURE:
                        fillDocument(xwpfDocument, projectLicenseInfoResults, projectName, projectVersion, licenseInfoHeaderText, false);
                        break;
                    case REPORT:
                        fillDocument(xwpfDocument, projectLicenseInfoResults, projectName, projectVersion, licenseInfoHeaderText, true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown generator variant type: " + getOutputVariant());
                }
                xwpfDocument.write(docxOutputStream);
                docxOutputStream.close();
            } catch (XmlException e) {
                throw new SW360Exception("Got XmlException while generating docx document: " + e.getMessage());
            } catch (IOException e) {
                throw new SW360Exception("Got IOException when generating docx document: " + e.getMessage());
            } catch (TException e) {
                throw new SW360Exception("Error reading sw360 licenses: " + e.getMessage());
            }
            return docxOutputStream.toByteArray();
        } else {
            throw new SW360Exception("Could not load the template for xwpf document: " + DOCX_TEMPLATE_FILE);
        }
    }

    private void fillDocument(XWPFDocument document, Collection<LicenseInfoParsingResult> projectLicenseInfoResults,
                              String projectName, String projectVersion, String licenseInfoHeaderText, boolean includeObligations) throws XmlException, TException {
        replaceText(document, "$license-info-header", licenseInfoHeaderText);
        replaceText(document, "$project-name", projectName);
        replaceText(document, "$project-version", projectVersion);
        fillReleaseBulletList(document, projectLicenseInfoResults);
        fillReleaseDetailList(document, projectLicenseInfoResults, includeObligations);
        fillLicenseList(document, projectLicenseInfoResults);
    }

    private void fillReleaseBulletList(XWPFDocument document, Collection<LicenseInfoParsingResult> projectLicenseInfoResults) throws XmlException {
        List<String> releaseList = new ArrayList<>();
        for (LicenseInfoParsingResult result : projectLicenseInfoResults) {
            releaseList.add(getComponentLongName(result));
        }
        addBulletList(document, releaseList, true);
        addPageBreak(document);
    }

    private void fillReleaseDetailList(XWPFDocument document, Collection<LicenseInfoParsingResult> projectLicenseInfoResults, boolean includeObligations) throws TException {
        addFormattedText(document.createParagraph().createRun(), "Detailed Releases Information", FONT_SIZE + 2, true);
        setText(document.createParagraph().createRun(), "Please note the following license conditions and copyright " +
                "notices applicable to Open Source Software and/or other components (or parts thereof):");
        addNewLines(document, 0);

        for (LicenseInfoParsingResult parsingResult : projectLicenseInfoResults) {
            addReleaseTitle(document, parsingResult);
            if (parsingResult.getStatus() == LicenseInfoRequestStatus.SUCCESS) {
                addCopyrights(document, parsingResult);
                addLicenses(document, parsingResult, includeObligations);
            } else {
                XWPFRun errorRun = document.createParagraph().createRun();
                String errorText = nullToEmptyString(parsingResult.getMessage());
                String filename = getFilename(parsingResult);
                addFormattedText(errorRun, String.format("Error reading license information: %s", errorText), FONT_SIZE, false, ALERT_COLOR);
                addFormattedText(errorRun, String.format("Source file: %s", filename), FONT_SIZE, false, ALERT_COLOR);
            }
            addNewLines(document, 1);
        }
        addPageBreak(document);
    }

    private void addReleaseTitle(XWPFDocument document, LicenseInfoParsingResult parsingResult) {
        String releaseTitle = getComponentLongName(parsingResult);
        XWPFParagraph releaseTitleParagraph = document.createParagraph();
        releaseTitleParagraph.setStyle(STYLE_HEADING);
        addBookmark(releaseTitleParagraph, releaseTitle, releaseTitle);
        addNewLines(document, 0);
    }

    private void addCopyrights(XWPFDocument document, LicenseInfoParsingResult parsingResult) {
        XWPFRun copyrightTitleRun = document.createParagraph().createRun();
        addFormattedText(copyrightTitleRun, "Copyrights", FONT_SIZE, true);
        for (String copyright : getReleaseCopyrights(parsingResult)) {
            XWPFParagraph copyPara = document.createParagraph();
            copyPara.setSpacingAfter(0);
            setText(copyPara.createRun(), copyright);
        }
    }

    private void addLicenses(XWPFDocument document, LicenseInfoParsingResult parsingResult, boolean includeObligations) throws TException {
        XWPFRun licensesTitleRun = document.createParagraph().createRun();
        addNewLines(licensesTitleRun, 1);
        addFormattedText(licensesTitleRun, "Licenses", FONT_SIZE, true);
        for (String licenseName : getReleasesLicenses(parsingResult)) {
            XWPFParagraph licensePara = document.createParagraph();
            licensePara.setSpacingAfter(0);
            addBookmarkHyperLink(licensePara, licenseName, licenseName);
            if (includeObligations) {
                addLicenseObligations(document, licenseName);
            }
        }
    }

    private void addLicenseObligations(XWPFDocument document, String spdxLicense) throws TException {
        List<License> sw360Licenses = getLicenses();
        XWPFRun todoTitleRun = document.createParagraph().createRun();
        addNewLines(todoTitleRun, 0);
        Set<String> todos = getTodosFromLicenses(spdxLicense, sw360Licenses);
        addFormattedText(todoTitleRun, "Obligations for license " + spdxLicense + ":", FONT_SIZE, true);
        for (String todo : todos) {
            XWPFParagraph copyPara = document.createParagraph();
            copyPara.setSpacingAfter(0);
            XWPFRun todoRun = copyPara.createRun();
            setText(todoRun, todo);
            addNewLines(todoRun, 1);
        }
    }

    private Set<String> getReleaseCopyrights(LicenseInfoParsingResult licenseInfoParsingResult) {
        Set<String> copyrights = Collections.emptySet();
        if (licenseInfoParsingResult.isSetLicenseInfo()) {
            LicenseInfo licenseInfo = licenseInfoParsingResult.getLicenseInfo();
            if (licenseInfo.isSetCopyrights()) {
                copyrights = licenseInfo.getCopyrights();
            }
        }
        return copyrights;
    }

    private Set<String> getReleasesLicenses(LicenseInfoParsingResult licenseInfoParsingResult) {
        Set<String> licenses = new HashSet<>();
        if (licenseInfoParsingResult.isSetLicenseInfo()) {
            LicenseInfo licenseInfo = licenseInfoParsingResult.getLicenseInfo();
            if (licenseInfo.isSetLicenseNamesWithTexts()) {
                for (LicenseNameWithText licenseNameWithText : licenseInfo.getLicenseNamesWithTexts()) {
                    licenses.add(licenseNameWithText.isSetLicenseName()
                            ? licenseNameWithText.getLicenseName()
                            : UNKNOWN_LICENSE_NAME);
                }
            }
        }
        return licenses;
    }

    private String getFilename(LicenseInfoParsingResult licenseInfoParsingResult) {
        return Optional.ofNullable(licenseInfoParsingResult.getLicenseInfo())
                .map(LicenseInfo::getFilenames)
                .flatMap(l -> l.stream().findFirst())
                .orElse(UNKNOWN_FILE_NAME);
    }

    private static Set<String> getTodosFromLicenses(String spdxLicense, List<License> licenses) {
        Set<String> todos = new HashSet<>();
        if (spdxLicense != null && !spdxLicense.isEmpty() && licenses != null) {
            for (License license : licenses) {
                if (spdxLicense.equalsIgnoreCase(license.getId())) {
                    for (Todo todo : license.getTodos()) {
                        todos.add(todo.getText());
                    }
                }
            }
            if (todos.isEmpty()) {
                todos.add(TODO_DEFAULT_TEXT);
            }
        }
        return todos;
    }

    private static List<License> getLicenses() throws TException {
        LicenseService.Iface licenseClient = new ThriftClients().makeLicenseClient();
        return licenseClient.getLicenses();
    }

    private void fillLicenseList(XWPFDocument document, Collection<LicenseInfoParsingResult> projectLicenseInfoResults) {
        List<LicenseNameWithText> licenseNameWithTexts = OutputGenerator.getSortedLicenseNameWithTexts(projectLicenseInfoResults);
        XWPFRun licenseHeaderRun = document.createParagraph().createRun();
        addFormattedText(licenseHeaderRun, "License texts", FONT_SIZE + 2, true);
        addNewLines(document, 0);

        for (LicenseNameWithText licenseNameWithText : licenseNameWithTexts) {
            XWPFParagraph licenseParagraph = document.createParagraph();
            licenseParagraph.setStyle(STYLE_HEADING);
            String licenseName = licenseNameWithText.isSetLicenseName() ? licenseNameWithText.getLicenseName() : UNKNOWN_LICENSE_NAME;
            addBookmark(licenseParagraph, licenseName, licenseName);
            addNewLines(document, 0);
            setText(document.createParagraph().createRun(), nullToEmptyString(licenseNameWithText.getLicenseText()));
            addNewLines(document, 1);
        }
    }
}
