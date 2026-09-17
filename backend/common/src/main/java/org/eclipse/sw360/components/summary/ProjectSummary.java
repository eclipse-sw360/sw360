/*
 * Copyright Siemens AG, 2014-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.services.projects.Project;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ProjectSummary extends DocumentSummary<Project> {

    @Override
    protected Project summary(SummaryType type, Project document) {
        Project copy = new Project();

        switch (type) {
            case LINKED_PROJECT_ACCESSIBLE:
                setFieldsForAccessibleLinkedProject(document, copy);
                break;
            case SUMMARY:
                setSummaryFields(document, copy);
                break;
            default:
                setDefaultFields(document, copy);
                break;
        }

        return copy;
    }

    protected static void setSummaryFields(Project document, Project copy) {
        // Full copy of every non-null field on the POJO (semantic equivalent of
        // the old "copy all thrift fields" branch).
        copy.setId(document.getId());
        copy.setRevision(document.getRevision());
        copy.setType(document.getType());
        copy.setName(document.getName());
        copy.setDescription(document.getDescription());
        copy.setVersion(document.getVersion());
        copy.setDomain(document.getDomain());
        copy.setAttachments(document.getAttachments());
        copy.setCreatedOn(document.getCreatedOn());
        copy.setBusinessUnit(document.getBusinessUnit());
        copy.setState(document.getState());
        copy.setProjectType(document.getProjectType());
        copy.setTag(document.getTag());
        copy.setClearingState(document.getClearingState());
        copy.setCreatedBy(document.getCreatedBy());
        copy.setProjectResponsible(document.getProjectResponsible());
        copy.setLeadArchitect(document.getLeadArchitect());
        copy.setModerators(document.getModerators());
        copy.setContributors(document.getContributors());
        copy.setVisbility(document.getVisbility());
        copy.setRoles(document.getRoles());
        copy.setSecurityResponsibles(document.getSecurityResponsibles());
        copy.setProjectOwner(document.getProjectOwner());
        copy.setOwnerAccountingUnit(document.getOwnerAccountingUnit());
        copy.setOwnerGroup(document.getOwnerGroup());
        copy.setOwnerCountry(document.getOwnerCountry());
        copy.setLinkedProjects(document.getLinkedProjects());
        copy.setReleaseIdToUsage(document.getReleaseIdToUsage());
        copy.setPackageIds(document.getPackageIds());
        copy.setClearingTeam(document.getClearingTeam());
        copy.setPreevaluationDeadline(document.getPreevaluationDeadline());
        copy.setSystemTestStart(document.getSystemTestStart());
        copy.setSystemTestEnd(document.getSystemTestEnd());
        copy.setDeliveryStart(document.getDeliveryStart());
        copy.setPhaseOutSince(document.getPhaseOutSince());
        copy.setEnableSvm(document.getEnableSvm());
        copy.setExternalIds(document.getExternalIds());
        copy.setAdditionalData(document.getAdditionalData());
        copy.setConsiderReleasesFromExternalList(document.getConsiderReleasesFromExternalList());
        copy.setLicenseInfoHeaderText(document.getLicenseInfoHeaderText());
        copy.setEnableVulnerabilitiesDisplay(document.getEnableVulnerabilitiesDisplay());
        copy.setObligationsText(document.getObligationsText());
        copy.setClearingSummary(document.getClearingSummary());
        copy.setSpecialRisksOSS(document.getSpecialRisksOSS());
        copy.setGeneralRisks3rdParty(document.getGeneralRisks3rdParty());
        copy.setSpecialRisks3rdParty(document.getSpecialRisks3rdParty());
        copy.setDeliveryChannels(document.getDeliveryChannels());
        copy.setRemarksAdditionalRequirements(document.getRemarksAdditionalRequirements());
        copy.setDocumentState(document.getDocumentState());
        copy.setClearingRequestId(document.getClearingRequestId());
        copy.setReleaseClearingStateSummary(document.getReleaseClearingStateSummary());
        copy.setLinkedObligationId(document.getLinkedObligationId());
        copy.setPermissions(document.getPermissions());
        copy.setExternalUrls(document.getExternalUrls());
        copy.setVendor(document.getVendor());
        copy.setVendorId(document.getVendorId());
        copy.setModifiedBy(document.getModifiedBy());
        copy.setModifiedOn(document.getModifiedOn());
        copy.setReleaseRelationNetwork(document.getReleaseRelationNetwork());
    }

    protected static void setDefaultFields(Project document, Project copy) {
        copy.setId(document.getId());
        copy.setName(document.getName());
        copy.setDescription(document.getDescription());
        copy.setVersion(document.getVersion());
        copy.setClearingTeam(document.getClearingTeam());
    }

    protected static void setFieldsForAccessibleLinkedProject(Project document, Project copy) {
        copy.setId(document.getId());
        copy.setName(document.getName());
        copy.setDescription(document.getDescription());
        copy.setVersion(document.getVersion());
        copy.setClearingTeam(document.getClearingTeam());
        copy.setBusinessUnit(document.getBusinessUnit());
        copy.setProjectResponsible(document.getProjectResponsible());
    }
}
