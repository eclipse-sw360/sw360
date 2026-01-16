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

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.exporter.VendorExporter;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.common.collect.ImmutableSet;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareVendor;

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

    public Vendor getByID(String id) throws TException {
        return repository.get(id);
    }

    public List<Vendor> getAllVendors() throws TException {
        return repository.getAll();
    }

    public Map<PaginationData, List<Vendor>> getAllVendors(PaginationData pageData) throws TException {
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
            log.error("Error creating the vendor: " + e.why);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE).setMessage(e.why);
        }
        try {
            repository.add(vendor);
        } catch (SW360Exception e) {
            log.error("Error adding vendor", e);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE).setMessage(e.why);
        }
        return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(vendor.getId());
    }

    // always perform for case insensitive check
    private boolean isDuplicate(Vendor vendor) {
        List<Vendor> duplicates = repository.searchByFullname(vendor.getFullname().toLowerCase());
        return duplicates.size() > 0;
    }

    public RequestStatus deleteVendor(String id, User user) throws SW360Exception {
        Vendor vendor = repository.get(id);
        assertNotNull(vendor);

        if (makePermission(vendor, user).isActionAllowed(RequestedAction.DELETE)) {
            repository.remove(id);
            return RequestStatus.SUCCESS;
        } else {
            log.error("User is not allowed to delete!");
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus updateVendor(Vendor vendor, User user) throws SW360Exception {
        Vendor actual = repository.get(vendor.getId());
        assertNotNull(actual);
        trimVendorFields(vendor);
        trimVendorFields(actual);
        if (isChangeResultInDuplicate(actual, vendor)) {
            return RequestStatus.DUPLICATE;
        } else if (makePermission(vendor, user).isActionAllowed(RequestedAction.WRITE)) {
            try {
                prepareVendor(vendor);
            } catch (SW360Exception e) {
                log.error("Error updating the vendor: " + e.why);
                return RequestStatus.FAILURE;
            }
            repository.update(vendor);
            return RequestStatus.SUCCESS;
        } else {
            log.error("User is not allowed to update!");
            return RequestStatus.FAILURE;
        }
    }

    private void trimVendorFields(Vendor vendor) {
        vendor.setUrl(CommonUtils.nullToEmptyString(vendor.getUrl()).trim());
        vendor.setFullname(CommonUtils.nullToEmptyString(vendor.getFullname()).trim());
        vendor.setShortname(CommonUtils.nullToEmptyString(vendor.getShortname()).trim());
    }

    private boolean isChangeResultInDuplicate(Vendor before, Vendor after) {
        if (CommonUtils.nullToEmptyString(before.getFullname()).equals(after.getFullname()) ||
                CommonUtils.nullToEmptyString(before.getUrl()).equals(after.getUrl())) {
            // not duplicated, because fullname and url is same
            return false;
        }
        return isDuplicate(after);
    }

    public void fillVendor(Release release){
        repository.fillVendor(release);
    }

    public RequestStatus mergeVendors(String mergeTargetId, String mergeSourceId, Vendor mergeSelection, User user) throws TException {
        Vendor mergeTarget = getByID(mergeTargetId);
        Vendor mergeSource = getByID(mergeSourceId);

        if (!makePermission(mergeTarget, user).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(mergeSource, user).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(mergeSource, user).isActionAllowed(RequestedAction.DELETE)) {
            return RequestStatus.ACCESS_DENIED;
        }

        if (isVendorUnderModeration(mergeTargetId) ||
                isVendorUnderModeration(mergeSourceId)){
            return RequestStatus.IN_USE;
        }

        try {
            RequestSummary summary;

            // First merge everything into the new compontent which is mergable in one step (attachments, plain fields)
            mergePlainFields(mergeSelection, mergeTarget);
            RequestStatus status = updateVendor(mergeTarget, user);
            if(status != RequestStatus.SUCCESS) {
                return status;
            }

            // now update the vendor in relating documents
            summary = updateComponents(mergeTarget, mergeSource, user);
            if(summary.getRequestStatus() != RequestStatus.SUCCESS) {
                log.error("Cannot update [" + (summary.getTotalElements() - summary.getTotalAffectedElements())
                    + "] of [" + summary.getTotalElements() + "] components: " + summary.getMessage());
                return summary.getRequestStatus();
            }

            summary = updateReleases(mergeTarget, mergeSource, user);
            if(summary.getRequestStatus() != RequestStatus.SUCCESS) {
                log.error("Cannot update [" + (summary.getTotalElements() - summary.getTotalAffectedElements())
                    + "] of [" + summary.getTotalElements() + "] releases: " + summary.getMessage());
                return summary.getRequestStatus();
            }

            // Finally we can delete the source vendor
            return deleteVendor(mergeSourceId, user);
        } catch(Exception e) {
            log.error("Cannot merge vendor [" + mergeSource.getId() + "] into [" + mergeTarget.getId() + "].", e);
            return RequestStatus.FAILURE;
        }
    }

    private void mergePlainFields(Vendor mergeSelection, Vendor mergeTarget) {
        ThriftUtils.copyFields(mergeSelection, mergeTarget, ImmutableSet.<Vendor._Fields>builder()
                .add(Vendor._Fields.FULLNAME)
                .add(Vendor._Fields.SHORTNAME)
                .add(Vendor._Fields.URL)
                .build()
        );
    }

    private RequestSummary updateComponents(Vendor mergeTarget, Vendor mergeSource, User user) throws TException {
        ComponentService.Iface componentsClient = new ThriftClients().makeComponentClient();

        Set<Component> components = componentsClient.getComponentsByDefaultVendorId(mergeSource.getId());
        components.stream().forEach(component -> {
            component.setDefaultVendorId(mergeTarget.getId());
        });

        return componentsClient.updateComponents(components, user);
    }

    private RequestSummary updateReleases(Vendor mergeTarget, Vendor mergeSource, User user) throws TException {
        ComponentService.Iface componentsClient = new ThriftClients().makeComponentClient();

        Set<String> componentIds = new HashSet<>();
        Set<Release> releases = componentsClient.getReleasesByVendorId(mergeSource.getId());
        releases.stream().forEach(release -> {
            componentIds.add(release.getComponentId());
            release.setVendorId(mergeTarget.getId());
        });

        RequestSummary result = componentsClient.updateReleasesDirectly(releases, user);
        if(result.getRequestStatus() != RequestStatus.SUCCESS) {
            return result;
        }

        // update computed fields of affected components
        for(String componentId : componentIds) {
            componentsClient.recomputeReleaseDependentFields(componentId, user);
        }

        return result;
    }

    private boolean isVendorUnderModeration(String vendorId) throws TException {
        ModerationService.Iface moderationClient = new ThriftClients().makeModerationClient();
        List<ModerationRequest> sourceModerationRequests = moderationClient.getModerationRequestByDocumentId(vendorId);
        return sourceModerationRequests.stream().anyMatch(CommonUtils::isInProgressOrPending);
    }

    public ByteBuffer getVendorReportDataStream(List<Vendor> vendorList) throws TException{
        try {
            InputStream stream = new VendorExporter().makeExcelExport(vendorList);
            return ByteBuffer.wrap(IOUtils.toByteArray(stream));
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

}
