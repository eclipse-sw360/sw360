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
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.thrift.TException;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Todo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.math.BigInteger;
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
    private static final String DOCX_TEMPLATE_REPORT_FILE = "/templateReport.docx";
    private static final String DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String DOCX_OUTPUT_TYPE = "docx";
    public static final String UNKNOWN_LICENSE = "Unknown";

    public DocxGenerator(OutputFormatVariant outputFormatVariant, String description) {
        super(DOCX_OUTPUT_TYPE, description, true, DOCX_MIME_TYPE, outputFormatVariant);
    }

    @Override
    public byte[] generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, Project project, Collection<ObligationParsingResult> obligationResults) throws SW360Exception {
        String licenseInfoHeaderText = project.getLicenseInfoHeaderText();

        ByteArrayOutputStream docxOutputStream = new ByteArrayOutputStream();
        Optional<byte[]> docxTemplateFile;
        XWPFDocument xwpfDocument;
        try {
            switch (getOutputVariant()) {
                case DISCLOSURE:
                    docxTemplateFile = CommonUtils.loadResource(DocxGenerator.class, DOCX_TEMPLATE_FILE);
                    xwpfDocument = new XWPFDocument(new ByteArrayInputStream(docxTemplateFile.get()));
                    if (docxTemplateFile.isPresent()) {
                        fillDisclosureDocument(
                            xwpfDocument,
                            projectLicenseInfoResults,
                            project,
                            licenseInfoHeaderText,
                            false
                            );
                    } else {
                        throw new SW360Exception("Could not load the template for xwpf document: " + DOCX_TEMPLATE_FILE);
                    }
                    break;
                case REPORT:
                    docxTemplateFile = CommonUtils.loadResource(DocxGenerator.class, DOCX_TEMPLATE_REPORT_FILE);
                    xwpfDocument = new XWPFDocument(new ByteArrayInputStream(docxTemplateFile.get()));
                    if (docxTemplateFile.isPresent()) {
                        fillReportDocument(
                            xwpfDocument,
                            projectLicenseInfoResults,
                            project,
                            licenseInfoHeaderText,
                            true,
                            obligationResults
                        );
                    } else {
                        throw new SW360Exception("Could not load the template for xwpf document: " + DOCX_TEMPLATE_REPORT_FILE);
                    }
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
    }

    private void fillDisclosureDocument(
        XWPFDocument document,
        Collection<LicenseInfoParsingResult> projectLicenseInfoResults,
        Project project,
        String licenseInfoHeaderText,
        boolean includeObligations) throws XmlException, TException {

            String projectName = project.getName();
            String projectVersion = project.getVersion();

            replaceText(document, "$license-info-header", licenseInfoHeaderText);
            replaceText(document, "$project-name", projectName);
            replaceText(document, "$project-version", projectVersion);

            fillReleaseBulletList(document, projectLicenseInfoResults);
            fillReleaseDetailList(document, projectLicenseInfoResults, includeObligations);
            fillLicenseList(document, projectLicenseInfoResults);
        }

    private void fillReportDocument(
        XWPFDocument document,
        Collection<LicenseInfoParsingResult> projectLicenseInfoResults,
        Project project,
        String licenseInfoHeaderText,
        boolean includeObligations,
        Collection<ObligationParsingResult> obligationResults) throws XmlException, TException {

            String projectName = project.getName();
            String projectVersion = project.getVersion();
            String obligationsText = project.getObligationsText();
            String clearingSummaryText = project.getClearingSummary();
            String specialRisksOSSText = project.getSpecialRisksOSS();
            String generalRisks3rdPartyText = project.getGeneralRisks3rdParty();
            String specialRisks3rdPartyText = project.getSpecialRisks3rdParty();
            String deliveryChannelsText = project.getDeliveryChannels();
            String remarksAdditionalRequirementsText = project.getRemarksAdditionalRequirements();
            String projectDescription = project.getDescription();

            fillOverview3rdPartyComponentTable(document, projectLicenseInfoResults);
            fillOwnerGroup(document, project);
            fillAttendeesTable(document, project);

            replaceText(document, "$license-info-header", licenseInfoHeaderText);
            replaceText(document, "$project-name", projectName);
            replaceText(document, "$project-version", projectVersion);
            replaceText(document, "$obligations-text", obligationsText);
            replaceText(document, "$clearing-summary-text", clearingSummaryText);
            replaceText(document, "$special-risks-oss-addition-text", specialRisksOSSText);
            replaceText(document, "$general-risks-3rd-party-text", generalRisks3rdPartyText);
            replaceText(document, "$special-risks-3rd-party-text", specialRisks3rdPartyText);
            replaceText(document, "$delivery-channels-text", deliveryChannelsText);
            replaceText(document, "$remarks-additional-requirements-text", remarksAdditionalRequirementsText);
            replaceText(document, "$product-description", projectDescription);


            fillSpecialOSSRisksTable(document, project, obligationResults);

            // because of the impossible API component subsections must be the last thing in the docx file
            // the rest of the sections must be generated after this
            writeComponentSubsections(document, projectLicenseInfoResults, obligationResults);

    }

    private void fillOwnerGroup(XWPFDocument document, Project project) throws XmlException, TException {
        String businessUnit = "";
        if(project.isSetBusinessUnit()) {
            businessUnit = project.getBusinessUnit();
        }
        replaceText(document, "$owner-group", businessUnit);
    }

    private void fillAttendeesTable(XWPFDocument document, Project project) throws XmlException, TException {
        XWPFTable table = document.getTables().get(0);

        int currentRow = 6;

        UserService.Iface userClient = new ThriftClients().makeUserClient();

        if(project.isSetProjectOwner() && !project.getProjectOwner().isEmpty()) {
            User owner = userClient.getByEmail(project.getProjectOwner());
            if(owner != null) {
                XWPFTableRow row = table.insertNewTableRow(currentRow++);
                row.addNewTableCell().setText(owner.getEmail());
                row.addNewTableCell().setText(owner.getDepartment());
                row.addNewTableCell().setText("Owner");
            }
        }

        if(project.isSetRoles()) {
            for(Map.Entry<String,Set<String> > rolRelationship : project.getRoles().entrySet()) {

                String rol = rolRelationship.getKey();
                Set<String> emails = rolRelationship.getValue();
                for(String email : emails) {
                    if(email.isEmpty()) {
                        continue;
                    }

                    User user = userClient.getByEmail(email);

                    XWPFTableRow row = table.insertNewTableRow(currentRow++);
                    String name = email;
                    if(user != null && user.isSetFullname()) {
                        name = user.getFullname();
                    }
                    String department = "N.A.";
                    if(user != null) {
                        name = user.getDepartment();
                    }

                    row.addNewTableCell().setText(name);
                    row.addNewTableCell().setText(department);
                    row.addNewTableCell().setText(rol);
                }
            }
        }
    }

    private class ObligationKey {
        public final String topic;
        public final String text;
        public ObligationKey(String topic, String text) {
            this.topic = topic;
            this.text = text;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ObligationKey)) return false;
            ObligationKey key = (ObligationKey) o;
            return topic == key.topic && text == key.text;
        }
        @Override
        public int hashCode() {
            return (topic == null ? 0 : topic.hashCode()) ^ (text == null ? 0 : text.hashCode());
        }
    }

    private void fillSpecialOSSRisksTable(XWPFDocument document, Project project, Collection<ObligationParsingResult> obligationResults) throws XmlException, TException {
        XWPFTable table = document.getTables().get(1);

        Map<ObligationKey,ArrayList<String>> collatedObligations = new HashMap<ObligationKey,ArrayList<String>>();
        for(ObligationParsingResult result : obligationResults) {
            if(result.getStatus() != ObligationInfoRequestStatus.SUCCESS) {
                continue;
            }
            for(Obligation obligation : result.getObligations()) {
                ObligationKey key = new ObligationKey(obligation.getTopic(), obligation.getText());
                if(!collatedObligations.containsKey(key)) {
                    collatedObligations.put(key, new ArrayList<String>());
                }
                ArrayList<String> licenses = collatedObligations.get(key);
                for(String license : obligation.getLicenseIDs()) {
                    if(!licenses.contains(license)) {
                        licenses.add(license);
                    }
                }
            }
        }
        int currentRow = 1;
        for( ObligationKey key :  collatedObligations.keySet()) {
            ArrayList<String> licenses = collatedObligations.get(key);
            XWPFTableRow row = table.insertNewTableRow(currentRow++);
            String licensesString = String.join(" ", licenses);
            row.addNewTableCell().setText(key.topic);
            row.addNewTableCell().setText(licensesString);
            row.addNewTableCell().setText(key.text);
        }
    }

    private void fillOverview3rdPartyComponentTable(XWPFDocument document, Collection<LicenseInfoParsingResult> projectLicenseInfoResults) throws XmlException {
        XWPFTable table = document.getTables().get(2);

        int currentRow = 1;
        for(LicenseInfoParsingResult result : projectLicenseInfoResults) {
            if(result.getStatus() != LicenseInfoRequestStatus.SUCCESS) {
                continue;
            }

            XWPFTableRow row = table.insertNewTableRow(currentRow++);
            LicenseInfo licenseInfo = result.getLicenseInfo();
            row.addNewTableCell().setText(result.getName());
            row.addNewTableCell().setText(result.getVersion());
            row.addNewTableCell().setText(licenseInfo.getSha1Hash());
            row.addNewTableCell().setText(licenseInfo.getComponentName());

            String globalLicense = "";
            for(LicenseNameWithText l : licenseInfo.getLicenseNamesWithTexts()) {
                if(l != null && "global".equals(l.getType())) {
                    globalLicense = l.getLicenseName();
                    break;
                }
            }

            row.addNewTableCell().setText(result.getComponentType());
            row.addNewTableCell().setText(globalLicense);
        }
    }

    private static Optional<ObligationParsingResult> obligationsForRelease(Release release, Collection<ObligationParsingResult> obligationResults) {
        return obligationResults.stream().filter(opr -> opr.getRelease() == release).findFirst();
    }

    private void writeComponentSubsections(XWPFDocument document, Collection<LicenseInfoParsingResult> projectLicenseInfoResults, Collection<ObligationParsingResult> obligationResults) throws XmlException {

        for(LicenseInfoParsingResult result : projectLicenseInfoResults) {

            XWPFParagraph title = document.createParagraph();
            title.setStyle(STYLE_HEADING_3);
            title.setNumID(new BigInteger("2"));
            XWPFRun titleRun = title.createRun();
            titleRun.setText(result.getVendor() + " " + result.getName());

            XWPFParagraph description = document.createParagraph();
            XWPFRun descriptionRun = description.createRun();

            LicenseInfo licenseInfo = result.getLicenseInfo();
            String globalLicense = UNKNOWN_LICENSE;
            for(LicenseNameWithText l : licenseInfo.getLicenseNamesWithTexts()) {
                if("global".equals(l.getType())) {
                    globalLicense = l.getLicenseName();
                    break;
                }
            }

            descriptionRun.setText("The component is licensed under " + globalLicense + ".");

            if(result.isSetRelease()) {
                Optional<ObligationParsingResult> obligationsResultOp = obligationsForRelease(result.getRelease(), obligationResults);

                if(!obligationsResultOp.isPresent()) {
                    continue;
                }

                ObligationParsingResult obligationsResult = obligationsResultOp.get();

                if(!obligationsResult.isSetObligations()) {
                    continue;
                }

                int currentRow = 0;
                Collection<Obligation> obligations = obligationsResult.getObligations();
                XWPFTable table = document.createTable();
                for(Obligation o :  obligations) {
                    XWPFTableRow row = table.insertNewTableRow(currentRow++);
                    String licensesString = String.join(" ", o.getLicenseIDs());
                    row.addNewTableCell().setText(o.getTopic());
                    row.addNewTableCell().setText(licensesString);
                    row.addNewTableCell().setText(o.getText());
                }
            }
        }
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
