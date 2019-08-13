/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.importer;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.sw360.commonIO.SampleOptions;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isValidUrl;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;

/**
 * @author daniele.fognini@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentCSVRecord extends ComponentAwareCSVRecord {
//    private final String componentName;
    private final String componentDescription;
    private final String componentCreatedOn;
    private final String componentType;
    private final String componentCreatedBy;
    private final String componentSubscribers;
    private final String categories;
    private final String softwarePlatforms;
    private final String componentHomePage;
    private final String componentMailingList;
    private final String componentWiki;
    private final String componentBlog;
    private final String componentWikipedia;
    private final String componentOpenHub;
// TODO get rid of these picknick left overs (see also below)
//    private final String releaseName;
//    private final String releaseVersion;
    private final String releaseDate;
    private final String CPEId;
    private final String releaseCreatedOn;
    private final String releaseCreatedBy;
    private final String releaseRepositoryURL;
    private final String releaseRepositoryType;
    private final String releaseMainlineState;
    private final String releaseClearingState;
    private final String releaseContributors;
    private final String releaseModerators;
    private final String releaseSubscribers;
    private final String releaseLanguages;
    private final String releaseOperatingSystems;
    private final String releaseMainLicenseIds;
    private final String releaseDownloadURL;
    private final String vendorName;
    private final String vendorShortname;
    private final String vendorUrl;
    private final String cIExternalSupplierID;
    private final String cIAdditionalInfo;
    private final String cIEvaluated;
    private final String cIProcStart;
    private final String cIRequestId;
    private final String cIScanned;
    private final String cIClearingStandard;
    private final String cIComment;
    private final String cIExternalUrl;
    private final Boolean cIBinariesOriginalFromCommunity;
    private final Boolean cIBinariesSelfMade;
    private final Boolean cIComponentLicenseInformation;
    private final Boolean cISourceCodeDelivery;
    private final Boolean cISourceCodeOriginalFromCommunity;
    private final Boolean cISourceCodeToolMade;
    private final Boolean cISourceCodeSelfMade;
    private final Boolean cIScreenshotOfWebSite;
    private final Boolean cIFinalizedLicenseScanReport;
    private final Boolean cILicenseScanReportResult;
    private final Boolean cILegalEvaluation;
    private final Boolean cILicenseAgreement;
    private final Boolean cIComponentClearingReport;
    private final Integer cICountOfSecurityVn;
    private final String eccStatus;
    private final String eccAL;
    private final String eccECCN;
    private final String eccMaterialIndexNumber;
    private final String eccComment;
    private final String eccAssessorContactPerson;
    private final String eccAssessorDepartment;
    private final String eccAssessmentDate;

    ComponentCSVRecord(String componentName, String componentDescription, String componentCreatedOn,
                       String componentType, String componentCreatedBy, String componentSubscribers,
                       String categories, String softwarePlatforms, String componentHomePage,
                       String componentMailingList, String componentWiki, String componentBlog,
                       String componentWikipedia, String componentOpenHub,
                       String releaseName, String releaseVersion, String releaseDate, String CPEId,
                       String releaseCreatedOn, String releaseCreatedBy, String releaseRepositoryURL,
                       String releaseRepositoryType, String releaseMainlineState, String releaseClearingState,
                       String releaseContributors, String releaseModerators, String releaseSubscribers,
                       String releaseLanguages, String releaseOperatingSystems, String releaseMainLicenseIds,
                       String releaseDownloadURL, String vendorName, String vendorShortname, String vendorUrl,
                       String cIExternalSupplierID, String cIAdditionalInfo, String cIEvaluated,
                       String cIProcStart, String cIRequestId, String cIScanned, String cIClearingStandard,
                       String cIComment, String cIExternalUrl, Boolean cIBinariesOriginalFromCommunity,
                       Boolean cIBinariesSelfMade, Boolean cIComponentLicenseInformation,
                       Boolean cISourceCodeDelivery, Boolean cISourceCodeOriginalFromCommunity,
                       Boolean cISourceCodeToolMade, Boolean cISourceCodeSelfMade, Boolean cIScreenshotOfWebSite,
                       Boolean cIFinalizedLicenseScanReport, Boolean cILicenseScanReportResult, Boolean cILegalEvaluation,
                       Boolean cILicenseAgreement, Boolean cIComponentClearingReport,
                       Integer cICountOfSecurityVn, String eccStatus, String eccAL, String eccECCN,
                       String eccMaterialIndexNumber, String eccComment, String eccAssessorContactPerson,
                       String eccAssessorDepartment, String eccAssessmentDate) {
//        this.componentName = componentName;
//        this.releaseName = releaseName;
//        this.releaseVersion = releaseVersion;

        super(componentName,releaseName,releaseVersion);

        this.componentDescription = componentDescription;
        this.componentCreatedOn = componentCreatedOn;
        this.componentType = componentType;
        this.componentCreatedBy = componentCreatedBy;
        this.componentSubscribers = componentSubscribers;
        this.categories = categories;
        this.softwarePlatforms = softwarePlatforms;
        this.componentHomePage = componentHomePage;
        this.componentMailingList = componentMailingList;
        this.componentWiki = componentWiki;
        this.componentBlog = componentBlog;
        this.componentWikipedia = componentWikipedia;
        this.componentOpenHub = componentOpenHub;
        this.releaseDate = releaseDate;
        this.CPEId = CPEId;
        this.releaseCreatedOn = releaseCreatedOn;
        this.releaseCreatedBy = releaseCreatedBy;
        this.releaseRepositoryURL = releaseRepositoryURL;
        this.releaseRepositoryType = releaseRepositoryType;
        this.releaseMainlineState = releaseMainlineState;
        this.releaseClearingState = releaseClearingState;
        this.releaseContributors = releaseContributors;
        this.releaseModerators = releaseModerators;
        this.releaseSubscribers = releaseSubscribers;
        this.releaseLanguages = releaseLanguages;
        this.releaseOperatingSystems = releaseOperatingSystems;
        this.releaseMainLicenseIds = releaseMainLicenseIds;
        this.releaseDownloadURL = releaseDownloadURL;
        this.vendorName = vendorName;
        this.vendorShortname = vendorShortname;
        this.vendorUrl = vendorUrl;
        this.cIExternalSupplierID = cIExternalSupplierID;
        this.cIAdditionalInfo = cIAdditionalInfo;
        this.cIEvaluated = cIEvaluated;
        this.cIProcStart = cIProcStart;
        this.cIRequestId = cIRequestId;
        this.cIScanned = cIScanned;
        this.cIClearingStandard = cIClearingStandard;
        this.cIComment = cIComment;
        this.cIExternalUrl = cIExternalUrl;
        this.cIBinariesOriginalFromCommunity = cIBinariesOriginalFromCommunity;
        this.cIBinariesSelfMade = cIBinariesSelfMade;
        this.cIComponentLicenseInformation = cIComponentLicenseInformation;
        this.cISourceCodeDelivery = cISourceCodeDelivery;
        this.cISourceCodeOriginalFromCommunity = cISourceCodeOriginalFromCommunity;
        this.cISourceCodeToolMade = cISourceCodeToolMade;
        this.cISourceCodeSelfMade = cISourceCodeSelfMade;
        this.cIScreenshotOfWebSite = cIScreenshotOfWebSite;
        this.cIFinalizedLicenseScanReport = cIFinalizedLicenseScanReport;
        this.cILicenseScanReportResult = cILicenseScanReportResult;
        this.cILegalEvaluation = cILegalEvaluation;
        this.cILicenseAgreement = cILicenseAgreement;
        this.cIComponentClearingReport = cIComponentClearingReport;
        this.cICountOfSecurityVn = cICountOfSecurityVn;
        this.eccStatus = eccStatus;
        this.eccAL = eccAL;
        this.eccECCN = eccECCN;
        this.eccMaterialIndexNumber = eccMaterialIndexNumber;
        this.eccComment = eccComment;
        this.eccAssessorContactPerson = eccAssessorContactPerson;
        this.eccAssessorDepartment = eccAssessorDepartment;
        this.eccAssessmentDate = eccAssessmentDate;
    }

    //Helpers
    public boolean isSetVendor() {
        return !isNullOrEmpty(vendorName) && !isNullOrEmpty(vendorUrl);
    }

    public boolean isSetComponent() {
        return !isNullOrEmpty(componentName);
    }

    public boolean isSetRelease() {
        return isSetComponent() && !isNullOrEmpty(releaseVersion);
    }

    public boolean isSetRepository() {
        return isSetRelease() && !isNullOrEmpty(releaseRepositoryURL);
    }

    public boolean isSetAttachmentContent() {return isValidUrl(releaseDownloadURL);}

    private boolean isSetClearingInformation() {
        if (!isNullOrEmpty(cIExternalSupplierID)) {
            return true;
        }
        if (!isNullOrEmpty(cIAdditionalInfo)) {
            return true;
        }
        if (!isNullOrEmpty(cIEvaluated)) {
            return true;
        }
        if (!isNullOrEmpty(cIProcStart)) {
            return true;
        }
        if (!isNullOrEmpty(cIRequestId)) {
            return true;
        }
        if (!isNullOrEmpty(cIScanned)) {
            return true;
        }
        if (!isNullOrEmpty(cIClearingStandard)) {
            return true;
        }
        if (!isNullOrEmpty(cIComment)) {
            return true;
        }
        if (!isNullOrEmpty(cIExternalUrl)) {
            return true;
        }

        if (cIBinariesOriginalFromCommunity != null) {
            return true;
        }
        if (cIBinariesSelfMade != null) {
            return true;
        }
        if (cIComponentLicenseInformation != null) {
            return true;
        }
        if (cISourceCodeDelivery != null) {
            return true;
        }
        if (cISourceCodeOriginalFromCommunity != null) {
            return true;
        }
        if (cISourceCodeToolMade != null) {
            return true;
        }
        if (cISourceCodeSelfMade != null) {
            return true;
        }
        if (cIScreenshotOfWebSite != null) {
            return true;
        }
        if (cIFinalizedLicenseScanReport != null) {
            return true;
        }
        if (cILicenseScanReportResult != null) {
            return true;
        }
        if (cILegalEvaluation != null) {
            return true;
        }
        if (cILicenseAgreement != null) {
            return true;
        }
        if (cIComponentClearingReport != null) {
            return true;
        }
        if (cICountOfSecurityVn != null) {
            return true;
        }

        return false;
    }

    private boolean isSetEccInformation() {
        if (!isNullOrEmpty(eccStatus)) {
            return true;
        }
        if (!isNullOrEmpty(eccAL)) {
            return true;
        }
        if (!isNullOrEmpty(eccECCN)) {
            return true;
        }
        if (!isNullOrEmpty(eccMaterialIndexNumber)) {
            return true;
        }
        if (!isNullOrEmpty(eccComment)) {
            return true;
        }
        if (!isNullOrEmpty(eccAssessorContactPerson)) {
            return true;
        }
        if (!isNullOrEmpty(eccAssessorDepartment)) {
            return true;
        }
        if (!isNullOrEmpty(eccAssessmentDate)) {
            return true;
        }
        return false;
    }

    public Vendor getVendor() {
        final Vendor vendor = new Vendor();

        vendor.setFullname(vendorName);

        if (!isNullOrEmpty(vendorShortname)) {
            vendor.setShortname(vendorShortname);
        } else {
            vendor.setShortname(vendorName);
        }
        if (!isNullOrEmpty(vendorUrl)) {
            vendor.setUrl(vendorUrl);
        }

        return vendor;
    }

    public Component getComponent() {
        Component component = new Component().setName(componentName);

        if (!isNullOrEmpty(componentCreatedBy)) {
            component.setCreatedBy(componentCreatedBy);
        }
        if (!isNullOrEmpty(componentCreatedOn)) {
            component.setCreatedOn(componentCreatedOn);
        }
        if (!isNullOrEmpty(componentDescription)) {
            component.setDescription(componentDescription);
        }
        if (!isNullOrEmpty(componentHomePage)) {
            component.setHomepage(componentHomePage);
        }
        if (!isNullOrEmpty(componentMailingList)) {
            component.setMailinglist(componentMailingList);
        }
        if (!isNullOrEmpty(componentWiki)) {
            component.setWiki(componentWiki);
        }
        if (!isNullOrEmpty(componentBlog)) {
            component.setBlog(componentBlog);
        }
        if (!isNullOrEmpty(componentWikipedia)) {
            component.setWikipedia(componentWikipedia);
        }
        if (!isNullOrEmpty(componentOpenHub)) {
            component.setOpenHub(componentOpenHub);
        }

        final ComponentType componentType = ThriftEnumUtils.stringToEnum(this.componentType, ComponentType.class);
        if (componentType != null) {
            component.setComponentType(componentType);
        }

        if (!isNullOrEmpty(categories)) {
            component.setCategories(CommonUtils.splitToSet(categories));
        }
        if (!isNullOrEmpty(componentSubscribers)) {
            component.setSubscribers(CommonUtils.splitToSet(componentSubscribers));
        }
        if (!isNullOrEmpty(softwarePlatforms)) {
            component.setSoftwarePlatforms(CommonUtils.splitToSet(softwarePlatforms));
        }

        return component;
    }

    public Release getRelease(String vendorId, String componentId, List<AttachmentContent> attachments) {
        Release release = new Release();
        //required
        release.setName(releaseName).setVersion(releaseVersion).setComponentId(componentId);


        //optionals
        if (!isNullOrEmpty(vendorId)) {
            release.setVendorId(vendorId);
        }

        if (!isNullOrEmpty(CPEId)) {
            release.setCpeid(CPEId);
        }
        if (!isNullOrEmpty(releaseDate)) {
            release.setReleaseDate(releaseDate);
        }
        if (!isNullOrEmpty(releaseCreatedOn)) {
            release.setCreatedOn(releaseCreatedOn);
        }
        if (!isNullOrEmpty(releaseCreatedBy)) {
            release.setCreatedBy(releaseCreatedBy);
        }
        if (CommonUtils.isValidUrl(releaseDownloadURL)) {
            release.setDownloadurl(releaseDownloadURL);
        }

        if (isSetRepository() ) {
            release.setRepository(getRepository());
        }

        if (!isNullOrEmpty(releaseMainlineState)) {
            final MainlineState mainlineState = ThriftEnumUtils.stringToEnum(releaseMainlineState, MainlineState.class);
            if (mainlineState != null) release.setMainlineState(mainlineState);
        }

        if (!isNullOrEmpty(releaseClearingState)) {
            final ClearingState clearingState = ThriftEnumUtils.stringToEnum(releaseClearingState, ClearingState.class);
            if (clearingState != null) release.setClearingState(clearingState);
        }


        if (!isNullOrEmpty(releaseContributors)) {
            release.setContributors(CommonUtils.splitToSet(releaseContributors));
        }
        if (!isNullOrEmpty(releaseModerators)) {
            release.setModerators(CommonUtils.splitToSet(releaseModerators));
        }
        if (!isNullOrEmpty(releaseSubscribers)) {
            release.setSubscribers(CommonUtils.splitToSet(releaseSubscribers));
        }
        if (!isNullOrEmpty(releaseLanguages)) {
            release.setLanguages(CommonUtils.splitToSet(releaseLanguages));
        }
        if (!isNullOrEmpty(releaseOperatingSystems)) {
            release.setOperatingSystems(CommonUtils.splitToSet(releaseOperatingSystems));
        }
        if (!isNullOrEmpty(releaseMainLicenseIds)) {
            release.setMainLicenseIds(CommonUtils.splitToSet(releaseMainLicenseIds));
        }

        if (isSetClearingInformation()) {
            release.setClearingInformation(getClearingInformation());
        }
        if (isSetEccInformation()) {
            release.setEccInformation(getEccInformation());
        }

        //TODO: There should be only one SOURCE per Release
        if(attachments!=null) {
            for (AttachmentContent attachmentContent : attachments) {
                String attachmentContentId = attachmentContent.getId();
                release.addToAttachments(
                        new Attachment()
                                .setAttachmentContentId(attachmentContentId)
                                .setCreatedOn(SW360Utils.getCreatedOn())
                                .setCreatedBy(releaseCreatedBy)
                                .setAttachmentType(AttachmentType.SOURCE)
                                .setFilename(attachmentContent.getFilename())
                );
            }
        }
        return release;
    }




    public Repository getRepository() {
        if(releaseRepositoryURL == null) return null;

        final Repository repository = new Repository();
        repository.setUrl(releaseRepositoryURL);

        if (!isNullOrEmpty(releaseRepositoryType)) {
            final RepositoryType repositoryType = ThriftEnumUtils.stringToEnum(releaseRepositoryType, RepositoryType.class);
            if (repositoryType != null)
                repository.setRepositorytype(repositoryType);
        }
        return repository;
    }

    public ClearingInformation getClearingInformation() {
        ClearingInformation clearingInformation = new ClearingInformation();

        if (!isNullOrEmpty(cIExternalSupplierID)) {
            clearingInformation.setExternalSupplierID(cIExternalSupplierID);
        }
        if (!isNullOrEmpty(cIAdditionalInfo)) {
            clearingInformation.setAdditionalRequestInfo(cIAdditionalInfo);
        }
        if (!isNullOrEmpty(cIEvaluated)) {
            clearingInformation.setEvaluated(cIEvaluated);
        }
        if (!isNullOrEmpty(cIProcStart)) {
            clearingInformation.setProcStart(cIProcStart);
        }
        if (!isNullOrEmpty(cIRequestId)) {
            clearingInformation.setRequestID(cIRequestId);
        }
        if (!isNullOrEmpty(cIScanned)) {
            clearingInformation.setScanned(cIScanned);
        }
        if (!isNullOrEmpty(cIClearingStandard)) {
            clearingInformation.setClearingStandard(cIClearingStandard);
        }
        if (!isNullOrEmpty(cIComment)) {
            clearingInformation.setComment(cIComment);
        }
        if (!isNullOrEmpty(cIExternalUrl)) {
            clearingInformation.setExternalUrl(cIExternalUrl);
        }

        if (cIBinariesOriginalFromCommunity != null) {
            clearingInformation.setBinariesOriginalFromCommunity(cIBinariesOriginalFromCommunity);
        }
        if (cIBinariesSelfMade != null) {
            clearingInformation.setBinariesSelfMade(cIBinariesSelfMade);
        }
        if (cIComponentLicenseInformation != null) {
            clearingInformation.setComponentLicenseInformation(cIComponentLicenseInformation);
        }
        if (cISourceCodeDelivery != null) {
            clearingInformation.setSourceCodeDelivery(cISourceCodeDelivery);
        }
        if (cISourceCodeOriginalFromCommunity != null) {
            clearingInformation.setSourceCodeOriginalFromCommunity(cISourceCodeOriginalFromCommunity);
        }
        if (cISourceCodeToolMade != null) {
            clearingInformation.setSourceCodeToolMade(cISourceCodeToolMade);
        }
        if (cISourceCodeSelfMade != null) {
            clearingInformation.setSourceCodeSelfMade(cISourceCodeSelfMade);
        }
        if (cIScreenshotOfWebSite != null) {
            clearingInformation.setScreenshotOfWebSite(cIScreenshotOfWebSite);
        }
        if (cIFinalizedLicenseScanReport != null) {
            clearingInformation.setFinalizedLicenseScanReport(cIFinalizedLicenseScanReport);
        }
        if (cILicenseScanReportResult != null) {
            clearingInformation.setLicenseScanReportResult(cILicenseScanReportResult);
        }
        if (cILegalEvaluation != null) {
            clearingInformation.setLegalEvaluation(cILegalEvaluation);
        }
        if (cILicenseAgreement != null) {
            clearingInformation.setLicenseAgreement(cILicenseAgreement);
        }
        if (cIComponentClearingReport != null) {
            clearingInformation.setComponentClearingReport(cIComponentClearingReport);
        }

        if (cICountOfSecurityVn != null) {
            clearingInformation.setCountOfSecurityVn(cICountOfSecurityVn);
        }

        return clearingInformation;
    }

    public EccInformation getEccInformation() {
        EccInformation eccInformation = newDefaultEccInformation();

        if (!isNullOrEmpty(eccStatus)) {
            final ECCStatus eccs = ThriftEnumUtils.stringToEnum(eccStatus, ECCStatus.class);
            eccInformation.setEccStatus(eccs);
        }
        if (!isNullOrEmpty(eccAL)) {
            eccInformation.setAL(eccAL);
        }
        if (!isNullOrEmpty(eccECCN)) {
            eccInformation.setECCN(eccECCN);
        }
        if (!isNullOrEmpty(eccMaterialIndexNumber)) {
            eccInformation.setMaterialIndexNumber(eccMaterialIndexNumber);
        }
        if (!isNullOrEmpty(eccComment)) {
            eccInformation.setEccComment(eccComment);
        }
        if (!isNullOrEmpty(eccAssessorContactPerson)) {
            eccInformation.setAssessorContactPerson(eccAssessorContactPerson);
        }
        if (!isNullOrEmpty(eccAssessorDepartment)) {
            eccInformation.setAssessorDepartment(eccAssessorDepartment);
        }
        if (!isNullOrEmpty(eccAssessmentDate)) {
            eccInformation.setAssessmentDate(eccAssessmentDate);
        }
        return eccInformation;
    }


    public List<AttachmentContent> getAttachmentContents() {
        List<AttachmentContent> attachments = new ArrayList<>();
        if (CommonUtils.isValidUrl(releaseDownloadURL)) {
            String urlFileName = CommonUtils.getTargetNameOfUrl(releaseDownloadURL);
            String fileName = prefixFileNameIfNecessary(urlFileName);

            attachments.add(new AttachmentContent()
                    .setFilename(fileName)
                    .setRemoteUrl(releaseDownloadURL)
                    .setOnlyRemote(true));
        }
        return attachments;
    }

    private String prefixFileNameIfNecessary(String urlFileName) {
        StringBuilder fileName = new StringBuilder();
        if (!urlFileName.contains(componentName)) {
            fileName.append(componentName);
            fileName.append("-");
        }
        if (!urlFileName.contains(releaseVersion)) {
            fileName.append(releaseVersion);
            fileName.append("-");
        }
        fileName.append(urlFileName);
        return fileName.toString();
    }

    public String getVendorName() {
        return vendorName;
    }

    //Number of field dependent "autogenerated" methods
    @Override
    public Iterable<String> getCSVIterable() {
        List<String> elements = new ArrayList<>();
        elements.add(nullToEmptyString(componentName));
        elements.add(nullToEmptyString(componentDescription));
        elements.add(nullToEmptyString(componentCreatedOn));
        elements.add(nullToEmptyString(componentType));
        elements.add(nullToEmptyString(componentCreatedBy));
        elements.add(nullToEmptyString(componentSubscribers));
        elements.add(nullToEmptyString(categories));
        elements.add(nullToEmptyString(softwarePlatforms));
        elements.add(nullToEmptyString(componentHomePage));
        elements.add(nullToEmptyString(componentMailingList));
        elements.add(nullToEmptyString(componentWiki));
        elements.add(nullToEmptyString(componentBlog));
        elements.add(nullToEmptyString(componentWikipedia));
        elements.add(nullToEmptyString(componentOpenHub));
        elements.add(nullToEmptyString(releaseName));
        elements.add(nullToEmptyString(releaseVersion));
        elements.add(nullToEmptyString(releaseDate));
        elements.add(nullToEmptyString(CPEId));
        elements.add(nullToEmptyString(releaseCreatedOn));
        elements.add(nullToEmptyString(releaseCreatedBy));
        elements.add(nullToEmptyString(releaseRepositoryURL));
        elements.add(nullToEmptyString(releaseRepositoryType));
        elements.add(nullToEmptyString(releaseMainlineState));
        elements.add(nullToEmptyString(releaseClearingState));
        elements.add(nullToEmptyString(releaseContributors));
        elements.add(nullToEmptyString(releaseModerators));
        elements.add(nullToEmptyString(releaseSubscribers));
        elements.add(nullToEmptyString(releaseLanguages));
        elements.add(nullToEmptyString(releaseOperatingSystems));
        elements.add(nullToEmptyString(releaseMainLicenseIds));
        elements.add(nullToEmptyString(releaseDownloadURL));
        elements.add(nullToEmptyString(vendorName));
        elements.add(nullToEmptyString(vendorShortname));
        elements.add(nullToEmptyString(vendorUrl));
        elements.add(nullToEmptyString(cIExternalSupplierID));
        elements.add(nullToEmptyString(cIAdditionalInfo));
        elements.add(nullToEmptyString(cIEvaluated));
        elements.add(nullToEmptyString(cIProcStart));
        elements.add(nullToEmptyString(cIRequestId));
        elements.add(nullToEmptyString(cIScanned));
        elements.add(nullToEmptyString(cIClearingStandard));
        elements.add(nullToEmptyString(cIComment));
        elements.add(nullToEmptyString(cIExternalUrl));
        elements.add(nullToEmptyString(cIBinariesOriginalFromCommunity));
        elements.add(nullToEmptyString(cIBinariesSelfMade));
        elements.add(nullToEmptyString(cIComponentLicenseInformation));
        elements.add(nullToEmptyString(cISourceCodeDelivery));
        elements.add(nullToEmptyString(cISourceCodeOriginalFromCommunity));
        elements.add(nullToEmptyString(cISourceCodeToolMade));
        elements.add(nullToEmptyString(cISourceCodeSelfMade));
        elements.add(nullToEmptyString(cIScreenshotOfWebSite));
        elements.add(nullToEmptyString(cIFinalizedLicenseScanReport));
        elements.add(nullToEmptyString(cILicenseScanReportResult));
        elements.add(nullToEmptyString(cILegalEvaluation));
        elements.add(nullToEmptyString(cILicenseAgreement));
        elements.add(nullToEmptyString(cIComponentClearingReport));
        elements.add(nullToEmptyString(cICountOfSecurityVn));
        elements.add(nullToEmptyString(eccStatus));
        elements.add(nullToEmptyString(eccAL));
        elements.add(nullToEmptyString(eccECCN));
        elements.add(nullToEmptyString(eccMaterialIndexNumber));
        elements.add(nullToEmptyString(eccComment));
        elements.add(nullToEmptyString(eccAssessorContactPerson));
        elements.add(nullToEmptyString(eccAssessorDepartment));
        elements.add(nullToEmptyString(eccAssessmentDate));

        return elements;
    }

    public static Iterable<String> getSampleInputIterable() {

        List<String> elements = new ArrayList<>();
        elements.add("componentName");
        elements.add("componentDescription");
        elements.add(SampleOptions.DATE_OPTION);
        elements.add(SampleOptions.COMPONENT_TYPE_OPTIONS);
        elements.add(SampleOptions.USER_OPTION);
        elements.add(SampleOptions.USER_LIST_OPTION);
        elements.add(SampleOptions.CATEGORIES_OPTION);
        elements.add(SampleOptions.SOFTWARE_PLATFORMS_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add(SampleOptions.URL_OPTION);

        elements.add("releaseName");
        elements.add(SampleOptions.VERSION_OPTION);
        elements.add(SampleOptions.DATE_OPTION);
        elements.add(SampleOptions.CPE_OPTION);
        elements.add(SampleOptions.USER_OPTION);
        elements.add(SampleOptions.USER_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add(SampleOptions.REPOSITORY_TYPE_OPTIONS);
        elements.add(SampleOptions.MAINLINE_STATE_OPTIONS);
        elements.add(SampleOptions.CLEARING_STATE_OPTIONS);
        elements.add(SampleOptions.USER_LIST_OPTION);
        elements.add(SampleOptions.USER_LIST_OPTION);
        elements.add(SampleOptions.USER_LIST_OPTION);
        elements.add(SampleOptions.PROGRAMMING_LANGUAGES_OPTION);
        elements.add(SampleOptions.OPERATING_SYSTEMS_OPTION);
        elements.add(SampleOptions.LICENSE_LIST_OPTION);
        elements.add(SampleOptions.URL_OPTION);
        elements.add("vendorName");
        elements.add("vendorShortname");
        elements.add(SampleOptions.URL_OPTION);
        elements.add("cIExternalSupplierID");
        elements.add("cIAdditionalInfo");
        elements.add(SampleOptions.DATE_OPTION);
        elements.add(SampleOptions.DATE_OPTION);
        elements.add("cIRequestId");
        elements.add("cIScanned");
        elements.add("which generation of tool used");
        elements.add("cIComment");
        elements.add(SampleOptions.URL_OPTION);

        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);
        elements.add(SampleOptions.BOOL_OPTION);

        elements.add(SampleOptions.NUMBER_OPTION);

        elements.add("ECC Status");
        elements.add("Ausfuhrliste");
        elements.add("European control classification number");
        elements.add("eccMaterialIndexNumber");
        elements.add("eccComment");
        elements.add(SampleOptions.USER_OPTION);
        elements.add("eccAssessorDepartment");
        elements.add(SampleOptions.DATE_OPTION);

        return elements;
    }

    public static Iterable<String> getCSVHeaderIterable() {
        List<String> elements = new ArrayList<>();
        elements.add("componentName");
        elements.add("componentDescription");
        elements.add("componentCreatedOn");
        elements.add("componentType");
        elements.add("componentCreatedBy");
        elements.add("componentSubscribers");
        elements.add("categories");
        elements.add("softwarePlatforms");
        elements.add("componentHomePage");
        elements.add("componentMailingList");
        elements.add("componentWiki");
        elements.add("componentBlog");
        elements.add("componentWikipedia");
        elements.add("componentOpenHub");
        elements.add("releaseName");
        elements.add("releaseVersion");
        elements.add("releaseDate");
        elements.add("CPEId");
        elements.add("releaseCreatedOn");
        elements.add("releaseCreatedBy");
        elements.add("releaseRepositoryURL");
        elements.add("releaseRepositoryType");
        elements.add("releaseMainlineState");
        elements.add("releaseClearingState");
        elements.add("releaseContributors");
        elements.add("releaseModerators");
        elements.add("releaseSubscribers");
        elements.add("releaseLanguages");
        elements.add("releaseOperatingSystems");
        elements.add("releaseMainLicenseIds");
        elements.add("releaseDownloadURL");
        elements.add("vendorName");
        elements.add("vendorShortname");
        elements.add("vendorUrl");
        elements.add("cIExternalSupplierID");
        elements.add("cIAdditionalInfo");
        elements.add("cIEvaluated");
        elements.add("cIProcStart");
        elements.add("cIRequestId");
        elements.add("cIScanned");
        elements.add("cIClearingStandard");
        elements.add("cIComment");
        elements.add("cIExternalUrl");
        elements.add("cIBinariesOriginalFromCommunity");
        elements.add("cIBinariesSelfMade");
        elements.add("cIComponentLicenseInformation");
        elements.add("cISourceCodeDelivery");
        elements.add("cISourceCodeOriginalFromCommunity");
        elements.add("cISourceCodeToolMade");
        elements.add("cISourceCodeSelfMade");
        elements.add("cIScreenshotOfWebSite");
        elements.add("cIFinalizedLicenseScanReport");
        elements.add("cILicenseScanReportResult");
        elements.add("cILegalEvaluation");
        elements.add("cILicenseAgreement");
        elements.add("cIComponentClearingReport");
        elements.add("cICountOfSecurityVn");
        elements.add("eccStatus");
        elements.add("eccAL");
        elements.add("eccECCN");
        elements.add("eccMaterialIndexNumber");
        elements.add("eccComment");
        elements.add("eccAssessorContactPerson");
        elements.add("eccAssessorDepartment");
        elements.add("eccAssessmentDate");

        return elements;
    }

    public static ComponentCSVRecordBuilder builder() {
        return new ComponentCSVRecordBuilder();
    }

    public static ComponentCSVRecordBuilder builder(CSVRecord in) {
        return new ComponentCSVRecordBuilder(in);
    }

    @Override
    public String toString() {
        return "ComponentCSVRecord{" +
                "componentName='" + componentName + '\'' +
                ", componentDescription='" + componentDescription + '\'' +
                ", componentCreatedOn='" + componentCreatedOn + '\'' +
                ", componentType='" + componentType + '\'' +
                ", componentCreatedBy='" + componentCreatedBy + '\'' +
                ", componentSubscribers='" + componentSubscribers + '\'' +
                ", categories='" + categories + '\'' +
                ", softwarePlatforms='" + softwarePlatforms + '\'' +
                ", componentHomePage='" + componentHomePage + '\'' +
                ", componentMailingList='" + componentMailingList + '\'' +
                ", componentWiki='" + componentWiki + '\'' +
                ", componentBlog='" + componentBlog + '\'' +
                ", componentWikipedia='" + componentWikipedia + '\'' +
                ", componentOpenHub='" + componentOpenHub + '\'' +
                ", releaseName='" + releaseName + '\'' +
                ", releaseVersion='" + releaseVersion + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", CPEId='" + CPEId + '\'' +
                ", releaseCreatedOn='" + releaseCreatedOn + '\'' +
                ", releaseCreatedBy='" + releaseCreatedBy + '\'' +
                ", releaseRepositoryURL='" + releaseRepositoryURL + '\'' +
                ", releaseRepositoryType='" + releaseRepositoryType + '\'' +
                ", releaseMainlineState='" + releaseMainlineState + '\'' +
                ", releaseClearingState='" + releaseClearingState + '\'' +
                ", releaseContributors='" + releaseContributors + '\'' +
                ", releaseModerators='" + releaseModerators + '\'' +
                ", releaseSubscribers='" + releaseSubscribers + '\'' +
                ", releaseLanguages='" + releaseLanguages + '\'' +
                ", releaseOperatingSystems='" + releaseOperatingSystems + '\'' +
                ", releaseMainLicenseIds='" + releaseMainLicenseIds + '\'' +
                ", releaseDownloadURL='" + releaseDownloadURL + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", vendorShortname='" + vendorShortname + '\'' +
                ", vendorUrl='" + vendorUrl + '\'' +
                ", cIExternalSupplierID='" + cIExternalSupplierID + '\'' +
                ", cIAdditionalInfo='" + cIAdditionalInfo + '\'' +
                ", cIEvaluated='" + cIEvaluated + '\'' +
                ", cIProcStart='" + cIProcStart + '\'' +
                ", cIRequestId='" + cIRequestId + '\'' +
                ", cIScanned='" + cIScanned + '\'' +
                ", cIClearingStandard='" + cIClearingStandard + '\'' +
                ", cIComment='" + cIComment + '\'' +
                ", cIExternalUrl='" + cIExternalUrl + '\'' +
                ", cIBinariesOriginalFromCommunity=" + cIBinariesOriginalFromCommunity +
                ", cIBinariesSelfMade=" + cIBinariesSelfMade +
                ", cIComponentLicenseInformation=" + cIComponentLicenseInformation +
                ", cISourceCodeDelivery=" + cISourceCodeDelivery +
                ", cISourceCodeOriginalFromCommunity=" + cISourceCodeOriginalFromCommunity +
                ", cISourceCodeToolMade=" + cISourceCodeToolMade +
                ", cISourceCodeSelfMade=" + cISourceCodeSelfMade +
                ", cIScreenshotOfWebSite=" + cIScreenshotOfWebSite +
                ", cIFinalizedLicenseScanReport=" + cIFinalizedLicenseScanReport +
                ", cILicenseScanReportResult=" + cILicenseScanReportResult +
                ", cILegalEvaluation=" + cILegalEvaluation +
                ", cILicenseAgreement=" + cILicenseAgreement +
                ", cIComponentClearingReport=" + cIComponentClearingReport +
                ", cICountOfSecurityVn=" + cICountOfSecurityVn +
                ", eccStatus='" + eccStatus + '\'' +
                ", eccAL='" + eccAL + '\'' +
                ", eccECCN='" + eccECCN + '\'' +
                ", eccMaterialIndexNumber='" + eccMaterialIndexNumber + '\'' +
                ", eccComment='" + eccComment + '\'' +
                ", eccAssessorContactPerson='" + eccAssessorContactPerson + '\'' +
                ", eccAssessorDepartment='" + eccAssessorDepartment + '\'' +
                ", eccAssessmentDate='" + eccAssessmentDate + '\'' +
                '}';
    }
}
