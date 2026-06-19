/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.projects;

import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ProjectConverter {

    private ProjectConverter() {}

    public static Project fromThrift(org.eclipse.sw360.datahandler.thrift.projects.Project thrift) {
        if (thrift == null) {
            return null;
        }
        Project pojo = new Project();
        if (thrift.isSetAdditionalData()) {
            pojo.setAdditionalData(thrift.getAdditionalData());
        }
        if (thrift.isSetAttachments()) {
            pojo.setAttachments(ThriftCollectionConverter.mapSet(thrift.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.fromThrift(e)));
        }
        if (thrift.isSetBusinessUnit()) {
            pojo.setBusinessUnit(thrift.getBusinessUnit());
        }
        if (thrift.isSetClearingRequestId()) {
            pojo.setClearingRequestId(thrift.getClearingRequestId());
        }
        if (thrift.isSetClearingState()) {
            pojo.setClearingState(EnumConverter.fromThrift(thrift.getClearingState(), org.eclipse.sw360.datahandler.services.projects.ProjectClearingState.class));
        }
        if (thrift.isSetClearingSummary()) {
            pojo.setClearingSummary(thrift.getClearingSummary());
        }
        if (thrift.isSetClearingTeam()) {
            pojo.setClearingTeam(thrift.getClearingTeam());
        }
        if (thrift.isSetConsiderReleasesFromExternalList()) {
            pojo.setConsiderReleasesFromExternalList(thrift.isConsiderReleasesFromExternalList());
        }
        if (thrift.isSetContributors()) {
            pojo.setContributors(ThriftCollectionConverter.mapSet(thrift.getContributors(), e -> e));
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetDeliveryChannels()) {
            pojo.setDeliveryChannels(thrift.getDeliveryChannels());
        }
        if (thrift.isSetDeliveryStart()) {
            pojo.setDeliveryStart(thrift.getDeliveryStart());
        }
        if (thrift.isSetDescription()) {
            pojo.setDescription(thrift.getDescription());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetDomain()) {
            pojo.setDomain(thrift.getDomain());
        }
        if (thrift.isSetEnableSvm()) {
            pojo.setEnableSvm(thrift.isEnableSvm());
        }
        if (thrift.isSetEnableVulnerabilitiesDisplay()) {
            pojo.setEnableVulnerabilitiesDisplay(thrift.isEnableVulnerabilitiesDisplay());
        }
        if (thrift.isSetExternalIds()) {
            pojo.setExternalIds(thrift.getExternalIds());
        }
        if (thrift.isSetExternalUrls()) {
            pojo.setExternalUrls(thrift.getExternalUrls());
        }
        if (thrift.isSetGeneralRisks3rdParty()) {
            pojo.setGeneralRisks3rdParty(thrift.getGeneralRisks3rdParty());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLeadArchitect()) {
            pojo.setLeadArchitect(thrift.getLeadArchitect());
        }
        if (thrift.isSetLicenseInfoHeaderText()) {
            pojo.setLicenseInfoHeaderText(thrift.getLicenseInfoHeaderText());
        }
        if (thrift.isSetLinkedObligationId()) {
            pojo.setLinkedObligationId(thrift.getLinkedObligationId());
        }
        if (thrift.isSetLinkedProjects()) {
            pojo.setLinkedProjects(ThriftCollectionConverter.mapMap(thrift.getLinkedProjects(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.projects.ProjectProjectRelationshipConverter.fromThrift(mapValue)));
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(ThriftCollectionConverter.mapSet(thrift.getModerators(), e -> e));
        }
        if (thrift.isSetModifiedBy()) {
            pojo.setModifiedBy(thrift.getModifiedBy());
        }
        if (thrift.isSetModifiedOn()) {
            pojo.setModifiedOn(thrift.getModifiedOn());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetObligationsText()) {
            pojo.setObligationsText(thrift.getObligationsText());
        }
        if (thrift.isSetOwnerAccountingUnit()) {
            pojo.setOwnerAccountingUnit(thrift.getOwnerAccountingUnit());
        }
        if (thrift.isSetOwnerCountry()) {
            pojo.setOwnerCountry(thrift.getOwnerCountry());
        }
        if (thrift.isSetOwnerGroup()) {
            pojo.setOwnerGroup(thrift.getOwnerGroup());
        }
        if (thrift.isSetPackageIds()) {
            pojo.setPackageIds(ThriftCollectionConverter.mapMap(thrift.getPackageIds(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.common.ProjectPackageRelationshipConverter.fromThrift(mapValue)));
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetPhaseOutSince()) {
            pojo.setPhaseOutSince(thrift.getPhaseOutSince());
        }
        if (thrift.isSetPreevaluationDeadline()) {
            pojo.setPreevaluationDeadline(thrift.getPreevaluationDeadline());
        }
        if (thrift.isSetProjectOwner()) {
            pojo.setProjectOwner(thrift.getProjectOwner());
        }
        if (thrift.isSetProjectResponsible()) {
            pojo.setProjectResponsible(thrift.getProjectResponsible());
        }
        if (thrift.isSetProjectType()) {
            pojo.setProjectType(EnumConverter.fromThrift(thrift.getProjectType(), org.eclipse.sw360.datahandler.services.projects.ProjectType.class));
        }
        if (thrift.isSetReleaseClearingStateSummary()) {
            pojo.setReleaseClearingStateSummary(org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStateSummaryConverter.fromThrift(thrift.getReleaseClearingStateSummary()));
        }
        if (thrift.isSetReleaseIdToUsage()) {
            pojo.setReleaseIdToUsage(ThriftCollectionConverter.mapMap(thrift.getReleaseIdToUsage(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter.fromThrift(mapValue)));
        }
        if (thrift.isSetReleaseRelationNetwork()) {
            pojo.setReleaseRelationNetwork(thrift.getReleaseRelationNetwork());
        }
        if (thrift.isSetRemarksAdditionalRequirements()) {
            pojo.setRemarksAdditionalRequirements(thrift.getRemarksAdditionalRequirements());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetRoles()) {
            pojo.setRoles(ThriftCollectionConverter.mapMap(thrift.getRoles(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (thrift.isSetSecurityResponsibles()) {
            pojo.setSecurityResponsibles(ThriftCollectionConverter.mapSet(thrift.getSecurityResponsibles(), e -> e));
        }
        if (thrift.isSetSpecialRisks3rdParty()) {
            pojo.setSpecialRisks3rdParty(thrift.getSpecialRisks3rdParty());
        }
        if (thrift.isSetSpecialRisksOSS()) {
            pojo.setSpecialRisksOSS(thrift.getSpecialRisksOSS());
        }
        if (thrift.isSetState()) {
            pojo.setState(EnumConverter.fromThrift(thrift.getState(), org.eclipse.sw360.datahandler.services.projects.ProjectState.class));
        }
        if (thrift.isSetSystemTestEnd()) {
            pojo.setSystemTestEnd(thrift.getSystemTestEnd());
        }
        if (thrift.isSetSystemTestStart()) {
            pojo.setSystemTestStart(thrift.getSystemTestStart());
        }
        if (thrift.isSetTag()) {
            pojo.setTag(thrift.getTag());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.fromThrift(thrift.getVendor()));
        }
        if (thrift.isSetVendorId()) {
            pojo.setVendorId(thrift.getVendorId());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        if (thrift.isSetVisbility()) {
            pojo.setVisbility(EnumConverter.fromThrift(thrift.getVisbility(), org.eclipse.sw360.datahandler.services.common.Visibility.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.Project toThrift(Project pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.Project thrift = new org.eclipse.sw360.datahandler.thrift.projects.Project();
        if (pojo.getAdditionalData() != null) {
            thrift.setAdditionalData(pojo.getAdditionalData());
        }
        if (pojo.getAttachments() != null) {
            thrift.setAttachments(ThriftCollectionConverter.mapSet(pojo.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.toThrift(e)));
        }
        if (pojo.getBusinessUnit() != null) {
            thrift.setBusinessUnit(pojo.getBusinessUnit());
        }
        if (pojo.getClearingRequestId() != null) {
            thrift.setClearingRequestId(pojo.getClearingRequestId());
        }
        if (pojo.getClearingState() != null) {
            thrift.setClearingState(EnumConverter.toThrift(pojo.getClearingState(), org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState.class));
        }
        if (pojo.getClearingSummary() != null) {
            thrift.setClearingSummary(pojo.getClearingSummary());
        }
        if (pojo.getClearingTeam() != null) {
            thrift.setClearingTeam(pojo.getClearingTeam());
        }
        if (pojo.getConsiderReleasesFromExternalList() != null) {
            thrift.setConsiderReleasesFromExternalList(pojo.getConsiderReleasesFromExternalList());
        }
        if (pojo.getContributors() != null) {
            thrift.setContributors(ThriftCollectionConverter.mapSet(pojo.getContributors(), e -> e));
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getDeliveryChannels() != null) {
            thrift.setDeliveryChannels(pojo.getDeliveryChannels());
        }
        if (pojo.getDeliveryStart() != null) {
            thrift.setDeliveryStart(pojo.getDeliveryStart());
        }
        if (pojo.getDescription() != null) {
            thrift.setDescription(pojo.getDescription());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getDomain() != null) {
            thrift.setDomain(pojo.getDomain());
        }
        if (pojo.getEnableSvm() != null) {
            thrift.setEnableSvm(pojo.getEnableSvm());
        }
        if (pojo.getEnableVulnerabilitiesDisplay() != null) {
            thrift.setEnableVulnerabilitiesDisplay(pojo.getEnableVulnerabilitiesDisplay());
        }
        if (pojo.getExternalIds() != null) {
            thrift.setExternalIds(pojo.getExternalIds());
        }
        if (pojo.getExternalUrls() != null) {
            thrift.setExternalUrls(pojo.getExternalUrls());
        }
        if (pojo.getGeneralRisks3rdParty() != null) {
            thrift.setGeneralRisks3rdParty(pojo.getGeneralRisks3rdParty());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLeadArchitect() != null) {
            thrift.setLeadArchitect(pojo.getLeadArchitect());
        }
        if (pojo.getLicenseInfoHeaderText() != null) {
            thrift.setLicenseInfoHeaderText(pojo.getLicenseInfoHeaderText());
        }
        if (pojo.getLinkedObligationId() != null) {
            thrift.setLinkedObligationId(pojo.getLinkedObligationId());
        }
        if (pojo.getLinkedProjects() != null) {
            thrift.setLinkedProjects(ThriftCollectionConverter.mapMap(pojo.getLinkedProjects(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.projects.ProjectProjectRelationshipConverter.toThrift(mapValue)));
        }
        if (pojo.getModerators() != null) {
            thrift.setModerators(ThriftCollectionConverter.mapSet(pojo.getModerators(), e -> e));
        }
        if (pojo.getModifiedBy() != null) {
            thrift.setModifiedBy(pojo.getModifiedBy());
        }
        if (pojo.getModifiedOn() != null) {
            thrift.setModifiedOn(pojo.getModifiedOn());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getObligationsText() != null) {
            thrift.setObligationsText(pojo.getObligationsText());
        }
        if (pojo.getOwnerAccountingUnit() != null) {
            thrift.setOwnerAccountingUnit(pojo.getOwnerAccountingUnit());
        }
        if (pojo.getOwnerCountry() != null) {
            thrift.setOwnerCountry(pojo.getOwnerCountry());
        }
        if (pojo.getOwnerGroup() != null) {
            thrift.setOwnerGroup(pojo.getOwnerGroup());
        }
        if (pojo.getPackageIds() != null) {
            thrift.setPackageIds(ThriftCollectionConverter.mapMap(pojo.getPackageIds(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.common.ProjectPackageRelationshipConverter.toThrift(mapValue)));
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getPhaseOutSince() != null) {
            thrift.setPhaseOutSince(pojo.getPhaseOutSince());
        }
        if (pojo.getPreevaluationDeadline() != null) {
            thrift.setPreevaluationDeadline(pojo.getPreevaluationDeadline());
        }
        if (pojo.getProjectOwner() != null) {
            thrift.setProjectOwner(pojo.getProjectOwner());
        }
        if (pojo.getProjectResponsible() != null) {
            thrift.setProjectResponsible(pojo.getProjectResponsible());
        }
        if (pojo.getProjectType() != null) {
            thrift.setProjectType(EnumConverter.toThrift(pojo.getProjectType(), org.eclipse.sw360.datahandler.thrift.projects.ProjectType.class));
        }
        if (pojo.getReleaseClearingStateSummary() != null) {
            thrift.setReleaseClearingStateSummary(org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStateSummaryConverter.toThrift(pojo.getReleaseClearingStateSummary()));
        }
        if (pojo.getReleaseIdToUsage() != null) {
            thrift.setReleaseIdToUsage(ThriftCollectionConverter.mapMap(pojo.getReleaseIdToUsage(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter.toThrift(mapValue)));
        }
        if (pojo.getReleaseRelationNetwork() != null) {
            thrift.setReleaseRelationNetwork(pojo.getReleaseRelationNetwork());
        }
        if (pojo.getRemarksAdditionalRequirements() != null) {
            thrift.setRemarksAdditionalRequirements(pojo.getRemarksAdditionalRequirements());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getRoles() != null) {
            thrift.setRoles(ThriftCollectionConverter.mapMap(pojo.getRoles(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (pojo.getSecurityResponsibles() != null) {
            thrift.setSecurityResponsibles(ThriftCollectionConverter.mapSet(pojo.getSecurityResponsibles(), e -> e));
        }
        if (pojo.getSpecialRisks3rdParty() != null) {
            thrift.setSpecialRisks3rdParty(pojo.getSpecialRisks3rdParty());
        }
        if (pojo.getSpecialRisksOSS() != null) {
            thrift.setSpecialRisksOSS(pojo.getSpecialRisksOSS());
        }
        if (pojo.getState() != null) {
            thrift.setState(EnumConverter.toThrift(pojo.getState(), org.eclipse.sw360.datahandler.thrift.projects.ProjectState.class));
        }
        if (pojo.getSystemTestEnd() != null) {
            thrift.setSystemTestEnd(pojo.getSystemTestEnd());
        }
        if (pojo.getSystemTestStart() != null) {
            thrift.setSystemTestStart(pojo.getSystemTestStart());
        }
        if (pojo.getTag() != null) {
            thrift.setTag(pojo.getTag());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.toThrift(pojo.getVendor()));
        }
        if (pojo.getVendorId() != null) {
            thrift.setVendorId(pojo.getVendorId());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        if (pojo.getVisbility() != null) {
            thrift.setVisbility(EnumConverter.toThrift(pojo.getVisbility(), org.eclipse.sw360.datahandler.thrift.Visibility.class));
        }
        return thrift;
    }
}
