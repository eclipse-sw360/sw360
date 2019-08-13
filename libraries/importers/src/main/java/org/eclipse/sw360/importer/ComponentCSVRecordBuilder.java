/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.importer;

import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.apache.commons.csv.CSVRecord;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
/**
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentCSVRecordBuilder extends  CustomizedCSVRecordBuilder<ComponentCSVRecord>{

    private String componentName;
    private String componentDescription;
    private String componentCreatedOn;
    private String componentType;
    private String componentCreatedBy;
    private String componentSubscribers;
    private String categories;
    private String softwarePlatforms;
    private String componentHomePage;
    private String componentMailingList;
    private String componentWiki;
    private String componentBlog;
    private String componentWikipedia;
    private String componentOpenHub;
    private String releaseName;
    private String releaseVersion;
    private String releaseDate;
    private String CPEId;
    private String releaseCreatedOn;
    private String releaseCreatedBy;
    private String releaseRepositoryURL;
    private String releaseRepositoryType;
    private String releaseMainlineState;
    private String releaseClearingState;
    private String releaseContributors;
    private String releaseModerators;
    private String releaseSubscribers;
    private String releaseLanguages;
    private String releaseOperatingSystems;
    private String releaseMainLicenseNames;
    private String releaseDownloadURL;
    private String vendorName;
    private String vendorShortname;
    private String vendorUrl;

    private String eccStatus;
    private String eccAL;
    private String eccECCN;
    private String eccMaterialIndexNumber;
    private String eccComment;
    private String eccAssessorContactPerson;
    private String eccAssessorDepartment;
    private String eccAssessmentDate;

    private String cIExternalSupplierID;
    private String cIAdditionalInfo;
    private String cIEvaluated;
    private String cIProcStart;
    private String cIRequestId;
    private String cIClearingTeam;
    private String cIRequestorPerson;

    private String cIScanned;

    private Boolean cIBinariesOriginalFromCommunity;
    private Boolean cIBinariesSelfMade;
    private Boolean cIComponentLicenseInformation;
    private Boolean cISourceCodeDelivery;
    private Boolean cISourceCodeOriginalFromCommunity;
    private Boolean cISourceCodeToolMade;
    private Boolean cISourceCodeSelfMade;
    private Boolean cISourceCodeCotsAvailable;
    private Boolean cIScreenshotOfWebSite;

    private Boolean cIFinalizedLicenseScanReport;
    private Boolean cILicenseScanReportResult;
    private Boolean cILegalEvaluation;
    private Boolean cILicenseAgreement;
    private Boolean cIComponentClearingReport;
    private String cIClearingStandard;
    private Boolean cIReadmeOssAvailable;

        private String cIComment;

    private Integer cICountOfSecurityVn;
    private String cIExternalUrl;
    private Boolean cIEndOfLifeReached;

    @Override
    public ComponentCSVRecord build() {
        return new ComponentCSVRecord(componentName, componentDescription, componentCreatedOn,
                componentType, componentCreatedBy, componentSubscribers,
                categories, softwarePlatforms, componentHomePage,
                componentMailingList, componentWiki, componentBlog, componentWikipedia,
                componentOpenHub,
                releaseName, releaseVersion, releaseDate, CPEId,
                releaseCreatedOn, releaseCreatedBy, releaseRepositoryURL,
                releaseRepositoryType, releaseMainlineState, releaseClearingState,
                releaseContributors, releaseModerators, releaseSubscribers,
                releaseLanguages, releaseOperatingSystems, releaseMainLicenseNames,
                releaseDownloadURL, vendorName, vendorShortname, vendorUrl, cIExternalSupplierID, cIAdditionalInfo, cIEvaluated,
                cIProcStart, cIRequestId, cIScanned, cIClearingStandard,
                cIComment, cIExternalUrl, cIBinariesOriginalFromCommunity,
                cIBinariesSelfMade, cIComponentLicenseInformation,
                cISourceCodeDelivery, cISourceCodeOriginalFromCommunity,
                cISourceCodeToolMade, cISourceCodeSelfMade, cIScreenshotOfWebSite,
                cIFinalizedLicenseScanReport, cILicenseScanReportResult, cILegalEvaluation,
                cILicenseAgreement, cIComponentClearingReport,
                cICountOfSecurityVn, eccStatus, eccAL, eccECCN, eccMaterialIndexNumber, eccComment,
                eccAssessorContactPerson, eccAssessorDepartment, eccAssessmentDate);
    }

    ComponentCSVRecordBuilder() {
        componentName = null;
        componentDescription = null;
        componentCreatedOn = null;
        componentType = null;
        componentCreatedBy = null;
        componentSubscribers = null;
        categories = null;
        softwarePlatforms = null;
        componentHomePage = null;
        componentMailingList = null;
        componentWiki = null;
        componentBlog = null;
        componentWikipedia = null;
        componentOpenHub = null;
        releaseName = null;
        releaseVersion = null;
        releaseDate = null;
        CPEId = null;
        releaseCreatedOn = null;
        releaseCreatedBy = null;
        releaseRepositoryURL = null;
        releaseRepositoryType = null;
        releaseMainlineState = null;
        releaseClearingState = null;
        releaseContributors = null;
        releaseModerators = null;
        releaseSubscribers = null;
        releaseLanguages = null;
        releaseOperatingSystems = null;
        releaseMainLicenseNames = null;
        releaseDownloadURL = null;
        vendorName = null;
        vendorShortname = null;
        vendorUrl = null;
        cIExternalSupplierID = null;
        cIAdditionalInfo = null;
        cIEvaluated = null;
        cIProcStart = null;
        cIRequestId = null;
        cIScanned = null;
        cIClearingStandard = null;
        cIComment = null;
        cIExternalUrl = null;
        cIBinariesOriginalFromCommunity = null;
        cIBinariesSelfMade = null;
        cIComponentLicenseInformation = null;
        cISourceCodeDelivery = null;
        cISourceCodeOriginalFromCommunity = null;
        cISourceCodeToolMade = null;
        cISourceCodeSelfMade = null;
        cIScreenshotOfWebSite = null;
        cIFinalizedLicenseScanReport = null;
        cILicenseScanReportResult = null;
        cILegalEvaluation = null;
        cILicenseAgreement = null;
        cIComponentClearingReport = null;
        cICountOfSecurityVn = null;
        eccStatus = null;
        eccAL = null;
        eccECCN = null;
        eccMaterialIndexNumber = null;
        eccComment = null;
        eccAssessorContactPerson = null;
        eccAssessorDepartment = null;
        eccAssessmentDate = null;
    }

    ComponentCSVRecordBuilder(CSVRecord record) {

        int i = 0;

        //String
        componentName = record.get(i++);
        componentDescription = record.get(i++);
        componentCreatedOn = record.get(i++);
        componentType = record.get(i++);
        componentCreatedBy = record.get(i++);
        componentSubscribers = record.get(i++);
        categories = record.get(i++);
        softwarePlatforms = record.get(i++);
        componentHomePage = record.get(i++);
        componentMailingList = record.get(i++);
        componentWiki = record.get(i++);
        componentBlog = record.get(i++);
        componentWikipedia = record.get(i++);
        componentOpenHub = record.get(i++);
        releaseName = record.get(i++);
        releaseVersion = record.get(i++);
        releaseDate = record.get(i++);
        CPEId = record.get(i++);
        releaseCreatedOn = record.get(i++);
        releaseCreatedBy = record.get(i++);
        releaseRepositoryURL = record.get(i++);
        releaseRepositoryType = record.get(i++);
        releaseMainlineState = record.get(i++);
        releaseClearingState = record.get(i++);
        releaseContributors = record.get(i++);
        releaseModerators = record.get(i++);
        releaseSubscribers = record.get(i++);
        releaseLanguages = record.get(i++);
        releaseOperatingSystems = record.get(i++);
        releaseMainLicenseNames = record.get(i++);
        releaseDownloadURL = record.get(i++);
        vendorName = record.get(i++);
        vendorShortname = record.get(i++);
        vendorUrl = record.get(i++);
        cIExternalSupplierID = record.get(i++);
        cIAdditionalInfo = record.get(i++);
        cIEvaluated = record.get(i++);
        cIProcStart = record.get(i++);
        cIRequestId = record.get(i++);
        cIScanned = record.get(i++);
        cIClearingStandard = record.get(i++);
        cIComment = record.get(i++);
        cIExternalUrl = record.get(i++);

        // Booleans
        cIBinariesOriginalFromCommunity = getBoolOrNull(record.get(i++));
        cIBinariesSelfMade = getBoolOrNull(record.get(i++));
        cIComponentLicenseInformation = getBoolOrNull(record.get(i++));
        cISourceCodeDelivery = getBoolOrNull(record.get(i++));
        cISourceCodeOriginalFromCommunity = getBoolOrNull(record.get(i++));
        cISourceCodeToolMade = getBoolOrNull(record.get(i++));
        cISourceCodeSelfMade = getBoolOrNull(record.get(i++));
        cIScreenshotOfWebSite = getBoolOrNull(record.get(i++));
        cIFinalizedLicenseScanReport = getBoolOrNull(record.get(i++));
        cILicenseScanReportResult = getBoolOrNull(record.get(i++));
        cILegalEvaluation = getBoolOrNull(record.get(i++));
        cILicenseAgreement = getBoolOrNull(record.get(i++));
        cIComponentClearingReport = getBoolOrNull(record.get(i++));

        // Int
        cICountOfSecurityVn = getIntegerOrNull(record.get(i++));

        eccStatus = record.get(i++);
        eccAL = record.get(i++);
        eccECCN = record.get(i++);
        eccMaterialIndexNumber = record.get(i++);
        eccComment = record.get(i++);
        eccAssessorContactPerson = record.get(i++);
        eccAssessorDepartment = record.get(i++);
        eccAssessmentDate = record.get(i);

        // Default copies:
        releaseCreatedBy = alternative(releaseCreatedBy, componentCreatedBy);
        componentCreatedBy = alternative(componentCreatedBy, releaseCreatedBy);

        releaseCreatedOn = alternative(releaseCreatedOn, componentCreatedOn);
        componentCreatedOn = alternative(componentCreatedOn, releaseCreatedOn);


    }

    //Aggregate Setters
    public ComponentCSVRecordBuilder fill(Component component) {

        setComponentName(component.getName());
        setComponentDescription(component.getDescription());
        setComponentCreatedOn(component.getCreatedOn());
        setComponentCreatedBy(component.getCreatedBy());
        setComponentHomePage(component.getHomepage());
        setComponentMailingList(component.getMailinglist());
        setComponentBlog(component.getBlog());
        setComponentWiki(component.getWiki());

        setComponentType(getEnumStringOrNull(component.getComponentType()));

        setComponentSubscribers(joinStrings(component.getSubscribers()));
        setCategories(joinStrings(component.getCategories()));
        setSoftwarePlatforms(joinStrings(component.getSoftwarePlatforms()));

        return this;
    }

    public ComponentCSVRecordBuilder fill(Vendor vendor) {
        setVendorName(vendor.getFullname());
        setVendorShortname(vendor.getShortname());
        setVendorUrl(vendor.getUrl());

        return this;
    }

    public ComponentCSVRecordBuilder fill(ClearingInformation cI) {
        setcIExternalSupplierID(cI.getExternalSupplierID());
        setcIAdditionalInfo(cI.getAdditionalRequestInfo());
        setcIEvaluated(cI.getEvaluated());
        setcIProcStart(cI.getProcStart());
        setcIRequestId(cI.getRequestID());
        setcIScanned(cI.getScanned());
        setcIClearingStandard(cI.getClearingStandard());
        setcIComment(cI.getComment());
        setcIExternalUrl(cI.getExternalUrl());

        if(cI.isSetBinariesOriginalFromCommunity()) setcIBinariesOriginalFromCommunity(cI.binariesOriginalFromCommunity);
        if(cI.isSetBinariesSelfMade()) setcIBinariesSelfMade(cI.binariesSelfMade);
        if(cI.isSetComponentLicenseInformation()) setcIComponentLicenseInformation(cI.componentLicenseInformation);
        if(cI.isSetSourceCodeDelivery()) setcISourceCodeDelivery(cI.sourceCodeDelivery);
        if(cI.isSetSourceCodeOriginalFromCommunity()) setcISourceCodeOriginalFromCommunity(cI.sourceCodeOriginalFromCommunity);
        if(cI.isSetSourceCodeToolMade()) setcISourceCodeToolMade(cI.sourceCodeToolMade);
        if(cI.isSetSourceCodeSelfMade()) setcISourceCodeSelfMade(cI.sourceCodeSelfMade);
        if(cI.isSetScreenshotOfWebSite()) setcIScreenshotOfWebSite(cI.screenshotOfWebSite);
        if(cI.isSetFinalizedLicenseScanReport()) setcIFinalizedLicenseScanReport(cI.finalizedLicenseScanReport);
        if(cI.isSetLicenseScanReportResult()) setcILicenseScanReportResult(cI.licenseScanReportResult);
        if(cI.isSetLegalEvaluation()) setcILegalEvaluation(cI.legalEvaluation);
        if(cI.isSetLicenseAgreement()) setcILicenseAgreement(cI.licenseAgreement);
        if(cI.isSetComponentClearingReport()) setcIComponentClearingReport(cI.componentClearingReport);

        if(cI.isSetCountOfSecurityVn()) setcICountOfSecurityVn(cI.countOfSecurityVn);

        return this;
    }

    public ComponentCSVRecordBuilder fill(EccInformation eccInfo) {
        setEccStatus(getEnumStringOrNull(eccInfo.getEccStatus()));
        setEccAL(eccInfo.getAL());
        setEccECCN(eccInfo.getECCN());
        setEccMaterialIndexNumber(eccInfo.getMaterialIndexNumber());
        setEccComment(eccInfo.getEccComment());
        setEccAssessorContactPerson(eccInfo.getAssessorContactPerson());
        setEccAssessorDepartment(eccInfo.getAssessorDepartment());
        setEccAssessmentDate(eccInfo.getAssessmentDate());
        return this;
    }

    public ComponentCSVRecordBuilder fill(Repository repository) {
        setReleaseRepositoryType(getEnumStringOrNull(repository.getRepositorytype()));
        setReleaseRepositoryURL(repository.getUrl());
        return this;
    }

    public ComponentCSVRecordBuilder fill(Release release) {
        if (release.isSetVendor())
            fill(release.getVendor());

        if (release.isSetClearingInformation())
            fill(release.getClearingInformation());

        if (release.isSetEccInformation())
            fill(release.getEccInformation());

        if (release.isSetRepository())
            fill(release.getRepository());

        setReleaseName(release.getName());
        setReleaseVersion(release.getVersion());
        setCPEId(release.getCpeid());
        setReleaseDate(release.getReleaseDate());
        setReleaseCreatedOn(release.getCreatedOn());
        setReleaseCreatedBy(release.getCreatedBy());
        setReleaseDownloadURL(release.getDownloadurl());

        setReleaseMainlineState(getEnumStringOrNull(release.getMainlineState()));
        setReleaseClearingState(getEnumStringOrNull(release.getClearingState()));

        setReleaseContributors(joinStrings(release.getContributors()));
        setReleaseModerators(joinStrings(release.getModerators()));
        setReleaseSubscribers(joinStrings(release.getSubscribers()));
        setReleaseLanguages(joinStrings(release.getLanguages()));
        setReleaseOperatingSystems(joinStrings(release.getOperatingSystems()));
        setReleaseMainLicenseNames(joinStrings(release.getMainLicenseIds()));

        return this;
    }

    //Setters
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setComponentDescription(String componentDescription) {
        this.componentDescription = componentDescription;
    }

    public void setComponentCreatedOn(String componentCreatedOn) {
        this.componentCreatedOn = componentCreatedOn;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public void setComponentCreatedBy(String componentCreatedBy) {
        this.componentCreatedBy = componentCreatedBy;
    }

    public void setComponentSubscribers(String componentSubscribers) {
        this.componentSubscribers = componentSubscribers;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setSoftwarePlatforms(String softwarePlatforms) {
        this.softwarePlatforms = softwarePlatforms;
    }

    public void setComponentHomePage(String componentHomePage) {
        this.componentHomePage = componentHomePage;
    }

    public void setComponentMailingList(String componentMailingList) {
        this.componentMailingList = componentMailingList;
    }

    public void setComponentWiki(String componentWiki) {
        this.componentWiki = componentWiki;
    }

    public void setComponentBlog(String componentBlog) {
        this.componentBlog = componentBlog;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setCPEId(String CPEId) {
        this.CPEId = CPEId;
    }

    public void setReleaseCreatedOn(String releaseCreatedOn) {
        this.releaseCreatedOn = releaseCreatedOn;
    }

    public void setReleaseCreatedBy(String releaseCreatedBy) {
        this.releaseCreatedBy = releaseCreatedBy;
    }

    public void setReleaseRepositoryURL(String releaseRepositoryURL) {
        this.releaseRepositoryURL = releaseRepositoryURL;
    }

    public void setReleaseRepositoryType(String releaseRepositoryType) {
        this.releaseRepositoryType = releaseRepositoryType;
    }

    public void setReleaseMainlineState(String releaseMainlineState) {
        this.releaseMainlineState = releaseMainlineState;
    }

    public void setReleaseClearingState(String releaseClearingState) {
        this.releaseClearingState = releaseClearingState;
    }

    public void setReleaseContributors(String releaseContributors) {
        this.releaseContributors = releaseContributors;
    }

    public void setReleaseModerators(String releaseModerators) {
        this.releaseModerators = releaseModerators;
    }

    public void setReleaseSubscribers(String releaseSubscribers) {
        this.releaseSubscribers = releaseSubscribers;
    }

    public void setReleaseLanguages(String releaseLanguages) {
        this.releaseLanguages = releaseLanguages;
    }

    public void setReleaseOperatingSystems(String releaseOperatingSystems) {
        this.releaseOperatingSystems = releaseOperatingSystems;
    }

    public void setReleaseMainLicenseNames(String releaseMainLicenseNames) {
        this.releaseMainLicenseNames = releaseMainLicenseNames;
    }

    public void setReleaseDownloadURL(String releaseDownloadURL) {
        this.releaseDownloadURL = releaseDownloadURL;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setVendorShortname(String vendorShortname) {
        this.vendorShortname = vendorShortname;
    }

    public void setVendorUrl(String vendorUrl) {
        this.vendorUrl = vendorUrl;
    }

    public void setcIExternalSupplierID(String cIExternalSupplierID) {
        this.cIExternalSupplierID = cIExternalSupplierID;
    }

    public void setcIAdditionalInfo(String cIAdditionalInfo) {
        this.cIAdditionalInfo = cIAdditionalInfo;
    }

    public void setcIEvaluated(String cIEvaluated) {
        this.cIEvaluated = cIEvaluated;
    }

    public void setcIProcStart(String cIProcStart) {
        this.cIProcStart = cIProcStart;
    }

    public void setcIRequestId(String cIRequestId) {
        this.cIRequestId = cIRequestId;
    }

    public void setcIScanned(String cIScanned) {
        this.cIScanned = cIScanned;
    }

    public void setcIClearingStandard(String cIClearingStandard) {
        this.cIClearingStandard = cIClearingStandard;
    }

    public void setcIComment(String cIComment) {
        this.cIComment = cIComment;
    }

    public void setcIExternalUrl(String cIExternalUrl) {
        this.cIExternalUrl = cIExternalUrl;
    }

    public void setcIBinariesOriginalFromCommunity(Boolean cIBinariesOriginalFromCommunity) {
        this.cIBinariesOriginalFromCommunity = cIBinariesOriginalFromCommunity;
    }

    public void setcIBinariesSelfMade(Boolean cIBinariesSelfMade) {
        this.cIBinariesSelfMade = cIBinariesSelfMade;
    }

    public void setcIComponentLicenseInformation(Boolean cIComponentLicenseInformation) {
        this.cIComponentLicenseInformation = cIComponentLicenseInformation;
    }

    public void setcISourceCodeDelivery(Boolean cISourceCodeDelivery) {
        this.cISourceCodeDelivery = cISourceCodeDelivery;
    }

    public void setcISourceCodeOriginalFromCommunity(Boolean cISourceCodeOriginalFromCommunity) {
        this.cISourceCodeOriginalFromCommunity = cISourceCodeOriginalFromCommunity;
    }

    public void setcISourceCodeToolMade(Boolean cISourceCodeToolMade) {
        this.cISourceCodeToolMade = cISourceCodeToolMade;
    }

    public void setcISourceCodeSelfMade(Boolean cISourceCodeSelfMade) {
        this.cISourceCodeSelfMade = cISourceCodeSelfMade;
    }

    public void setcIScreenshotOfWebSite(Boolean cIScreenshotOfWebSite) {
        this.cIScreenshotOfWebSite = cIScreenshotOfWebSite;
    }

    public void setcIFinalizedLicenseScanReport(Boolean cIFinalizedLicenseScanReport) {
        this.cIFinalizedLicenseScanReport = cIFinalizedLicenseScanReport;
    }

    public void setcILicenseScanReportResult(Boolean cILicenseScanReportResult) {
        this.cILicenseScanReportResult = cILicenseScanReportResult;
    }

    public void setcILegalEvaluation(Boolean cILegalEvaluation) {
        this.cILegalEvaluation = cILegalEvaluation;
    }

    public void setcILicenseAgreement(Boolean cILicenseAgreement) {
        this.cILicenseAgreement = cILicenseAgreement;
    }

    public void setcIComponentClearingReport(Boolean cIComponentClearingReport) {
        this.cIComponentClearingReport = cIComponentClearingReport;
    }

    public void setcICountOfSecurityVn(Integer cICountOfSecurityVn) {
        this.cICountOfSecurityVn = cICountOfSecurityVn;
    }

    public void setEccStatus(String eccStatus) {
        this.eccStatus = eccStatus;
    }

    public void setEccAL(String eccAL) {
        this.eccAL = eccAL;
    }

    public void setEccECCN(String eccECCN) {
        this.eccECCN = eccECCN;
    }

    public void setEccMaterialIndexNumber(String eccMaterialIndexNumber) {
        this.eccMaterialIndexNumber = eccMaterialIndexNumber;
    }

    public void setEccComment(String eccComment) {
        this.eccComment = eccComment;
    }

    public void setEccAssessorContactPerson(String eccAssessorContactPerson) {
        this.eccAssessorContactPerson = eccAssessorContactPerson;
    }

    public void setEccAssessorDepartment(String eccAssessorDepartment) {
        this.eccAssessorDepartment = eccAssessorDepartment;
    }

    public void setEccAssessmentDate(String eccAssessmentDate) {
        this.eccAssessmentDate = eccAssessmentDate;
    }
}
