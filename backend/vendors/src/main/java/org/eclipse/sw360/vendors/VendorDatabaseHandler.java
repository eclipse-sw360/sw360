/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vendors;

import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_VENDOR;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.vendors.VendorConverter;
import org.eclipse.sw360.components.ComponentHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.exporter.VendorExporter;

import com.google.common.base.Strings;
import com.ibm.cloud.cloudant.v1.Cloudant;

public class VendorDatabaseHandler {
    private static final Logger log = LogManager.getLogger(VendorDatabaseHandler.class);
    private final VendorRepository repository;

    public VendorDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        repository = new VendorRepository(db);
    }

    public VendorDatabaseHandler(DatabaseConnectorCloudant db) throws MalformedURLException {
        repository = new VendorRepository(db);
    }

    public Vendor getByID(String id) {
        return repository.get(id);
    }

    public List<Vendor> getAllVendors() {
        return repository.getAll();
    }

    public Map<PaginationData, List<Vendor>> getAllVendors(PaginationData pageData) {
        return repository.getVendorsWithPagination(pageData);
    }

    public AddDocumentRequestSummary addVendor(Vendor vendor) {
        trimVendorFields(vendor);
        if (isDuplicate(vendor)) {
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
        }
        try {
            prepareVendor(vendor);
        } catch (SW360Exception e) {
            log.error("Error creating the vendor: {}", e.getWhy());
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE)
                    .setMessage(e.getWhy());
        }
        try {
            repository.add(vendor);
        } catch (SW360Exception e) {
            log.error("Error adding vendor", e);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE)
                    .setMessage(e.getWhy());
        }
        return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(vendor.getId());
    }

    private boolean isDuplicate(Vendor vendor) {
        List<Vendor> duplicates = repository.searchByFullname(vendor.getFullname().toLowerCase());
        return !duplicates.isEmpty();
    }

    public RequestStatus deleteVendor(String id, User user) {
        Vendor vendor = repository.get(id);
        if (vendor == null) {
            throw new SW360Exception("Invalid null input!");
        }

        if (makePermission(VendorConverter.toThrift(vendor), user).isActionAllowed(RequestedAction.DELETE)) {
            repository.remove(id);
            return RequestStatus.SUCCESS;
        }
        log.error("User is not allowed to delete!");
        return RequestStatus.FAILURE;
    }

    public RequestStatus updateVendor(Vendor vendor, User user) {
        Vendor actual = repository.get(vendor.getId());
        if (actual == null) {
            throw new SW360Exception("Invalid null input!");
        }
        trimVendorFields(vendor);
        trimVendorFields(actual);
        if (isChangeResultInDuplicate(actual, vendor)) {
            return RequestStatus.DUPLICATE;
        } else if (makePermission(VendorConverter.toThrift(vendor), user).isActionAllowed(RequestedAction.WRITE)) {
            try {
                prepareVendor(vendor);
            } catch (SW360Exception e) {
                log.error("Error updating the vendor: {}", e.getWhy());
                return RequestStatus.FAILURE;
            }
            repository.update(vendor);
            return RequestStatus.SUCCESS;
        }
        log.error("User is not allowed to update!");
        return RequestStatus.FAILURE;
    }

    private void trimVendorFields(Vendor vendor) {
        vendor.setUrl(CommonUtils.nullToEmptyString(vendor.getUrl()).trim());
        vendor.setFullname(CommonUtils.nullToEmptyString(vendor.getFullname()).trim());
        vendor.setShortname(CommonUtils.nullToEmptyString(vendor.getShortname()).trim());
    }

    private boolean isChangeResultInDuplicate(Vendor before, Vendor after) {
        if (CommonUtils.nullToEmptyString(before.getFullname()).equals(after.getFullname())
                || CommonUtils.nullToEmptyString(before.getUrl()).equals(after.getUrl())) {
            return false;
        }
        return isDuplicate(after);
    }

    public void fillVendor(Release release) {
        repository.fillVendor(release);
    }

    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor mergeSelection, User user)
            throws TException {
        Vendor mergeTarget = getByID(mergeTargetId);
        Vendor mergeSource = getByID(mergeSourceId);

        if (!makePermission(VendorConverter.toThrift(mergeTarget), user).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(VendorConverter.toThrift(mergeSource), user).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(VendorConverter.toThrift(mergeSource), user)
                        .isActionAllowed(RequestedAction.DELETE)) {
            return RequestStatus.ACCESS_DENIED;
        }

        if (isVendorUnderModeration(mergeTargetId) || isVendorUnderModeration(mergeSourceId)) {
            return RequestStatus.IN_USE;
        }

        try {
            RequestSummary summary;

            mergePlainFields(mergeSelection, mergeTarget);
            RequestStatus status = updateVendor(mergeTarget, user);
            if (status != RequestStatus.SUCCESS) {
                return status;
            }

            summary = updateComponents(mergeTarget, mergeSource, user);
            if (summary.getRequestStatus() != RequestStatus.SUCCESS) {
                log.error("Cannot update [" + failedCount(summary)
                        + "] of [" + nullToZero(summary.getTotalElements()) + "] components: " + summary.getMessage());
                return summary.getRequestStatus() == null ? RequestStatus.FAILURE : summary.getRequestStatus();
            }

            summary = updateReleases(mergeTarget, mergeSource, user);
            if (summary.getRequestStatus() != RequestStatus.SUCCESS) {
                log.error("Cannot update [" + failedCount(summary)
                        + "] of [" + nullToZero(summary.getTotalElements()) + "] releases: " + summary.getMessage());
                return summary.getRequestStatus() == null ? RequestStatus.FAILURE : summary.getRequestStatus();
            }

            return deleteVendor(mergeSourceId, user);
        } catch (Exception e) {
            log.error("Cannot merge vendor [" + mergeSource.getId() + "] into [" + mergeTarget.getId() + "].", e);
            return RequestStatus.FAILURE;
        }
    }

    private void mergePlainFields(Vendor mergeSelection, Vendor mergeTarget) {
        if (mergeSelection.getFullname() != null) {
            mergeTarget.setFullname(mergeSelection.getFullname());
        }
        if (mergeSelection.getShortname() != null) {
            mergeTarget.setShortname(mergeSelection.getShortname());
        }
        if (mergeSelection.getUrl() != null) {
            mergeTarget.setUrl(mergeSelection.getUrl());
        }
    }

    private RequestSummary updateComponents(Vendor mergeTarget, Vendor mergeSource, User user) throws TException {
        ComponentHandler componentsHandler = newComponentHandler();

        Set<Component> components = componentsHandler.getComponentsByDefaultVendorId(mergeSource.getId());
        components.forEach(component -> component.setDefaultVendorId(mergeTarget.getId()));

        return componentsHandler.updateComponents(components, user);
    }

    private RequestSummary updateReleases(Vendor mergeTarget, Vendor mergeSource, User user) throws TException {
        ComponentHandler componentsHandler = newComponentHandler();

        Set<String> componentIds = new HashSet<>();
        Set<Release> releases = componentsHandler.getReleasesByVendorId(mergeSource.getId());
        releases.forEach(release -> {
            componentIds.add(release.getComponentId());
            release.setVendorId(mergeTarget.getId());
        });

        RequestSummary result = componentsHandler.updateReleasesDirectly(releases, user);
        if (result.getRequestStatus() != RequestStatus.SUCCESS) {
            return result;
        }

        for (String componentId : componentIds) {
            componentsHandler.recomputeReleaseDependentFields(componentId, user);
        }

        return result;
    }

    private static ComponentHandler newComponentHandler() {
        try {
            return new ComponentHandler();
        } catch (IOException e) {
            throw new SW360Exception("Error creating ComponentHandler: " + e.getMessage(), e);
        }
    }

    private static int failedCount(RequestSummary summary) {
        return nullToZero(summary.getTotalElements()) - nullToZero(summary.getTotalAffectedElements());
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isVendorUnderModeration(String vendorId) {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> sourceModerationRequests =
                ModerationClients.get().getModerationRequestByDocumentId(vendorId);
        return sourceModerationRequests.stream().anyMatch(CommonUtils::isInProgressOrPending);
    }

    public byte[] getVendorReportDataStream(List<Vendor> vendorList) {
        try {
            InputStream stream = new VendorExporter().makeExcelExport(vendorList);
            return IOUtils.toByteArray(stream);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    private static void prepareVendor(Vendor vendor) {
        if (Strings.isNullOrEmpty(vendor.getShortname())) {
            throw new SW360Exception("vendor short name cannot be empty!");
        }
        if (Strings.isNullOrEmpty(vendor.getFullname())) {
            throw new SW360Exception("vendor full name cannot be empty!");
        }
        try {
            org.eclipse.sw360.datahandler.common.SW360Assert.assertValidUrl(vendor.getUrl());
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw new SW360Exception(e.getWhy() != null ? e.getWhy() : e.getMessage(), e);
        }
        vendor.setType(TYPE_VENDOR);
    }
}
