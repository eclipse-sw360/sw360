/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils;

import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.changelogs.ChangeLogsConverter;
import org.eclipse.sw360.common.utils.converter.changelogs.ChangedFieldsConverter;
import org.eclipse.sw360.common.utils.converter.changelogs.ReferenceDocDataConverter;
import org.eclipse.sw360.common.utils.converter.common.ConfigContainerConverter;
import org.eclipse.sw360.common.utils.converter.common.ConfigForConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.SW360ExceptionConverter;
import org.eclipse.sw360.common.utils.converter.cvesearch.UpdateTypeConverter;
import org.eclipse.sw360.common.utils.converter.cvesearch.VulnerabilityUpdateStatusConverter;
import org.eclipse.sw360.common.utils.converter.vendors.VendorConverter;
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.services.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.services.cvesearch.UpdateType;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;

/**
 * Facade for Thrift conversion during incremental service-api migration.
 * Prefer the typed converters under {@code org.eclipse.sw360.common.utils.converter}.
 */
public final class ThriftConverter {

    private ThriftConverter() {}

    // ---- Shared ----

    public static SW360Exception fromThriftException(org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
        return SW360ExceptionConverter.fromThrift(e);
    }

    public static RequestStatus fromThriftRequestStatus(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        return RequestStatusConverter.fromThrift(thrift);
    }

    public static org.eclipse.sw360.datahandler.thrift.RequestStatus toThriftRequestStatus(RequestStatus pojo) {
        return RequestStatusConverter.toThrift(pojo);
    }

    public static org.eclipse.sw360.datahandler.thrift.ConfigFor toThriftConfigFor(ConfigFor pojo) {
        return ConfigForConverter.toThrift(pojo);
    }

    public static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPaginationData(PaginationData pojo) {
        return PaginationDataConverter.toThrift(pojo);
    }

    public static PaginationData fromThriftPaginationData(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        return PaginationDataConverter.fromThrift(thrift);
    }

    public static org.eclipse.sw360.datahandler.thrift.ConfigContainer toThriftConfigContainer(ConfigContainer pojo) {
        return ConfigContainerConverter.toThrift(pojo);
    }

    public static AddDocumentRequestSummary fromThriftAddDocumentRequestSummary(
            org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary thrift) {
        return AddDocumentRequestSummaryConverter.fromThrift(thrift);
    }

    // ---- Vendors ----

    public static Vendor fromThriftVendor(org.eclipse.sw360.datahandler.thrift.vendors.Vendor thrift) {
        return VendorConverter.fromThrift(thrift);
    }

    public static org.eclipse.sw360.datahandler.thrift.vendors.Vendor toThriftVendor(Vendor pojo) {
        return VendorConverter.toThrift(pojo);
    }

    // ---- Changelogs ----

    public static ChangedFields fromThriftChangedFields(
            org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields thrift) {
        return ChangedFieldsConverter.fromThrift(thrift);
    }

    public static ReferenceDocData fromThriftReferenceDocData(
            org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData thrift) {
        return ReferenceDocDataConverter.fromThrift(thrift);
    }

    public static ChangeLogs fromThriftChangeLogs(org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs thrift) {
        return ChangeLogsConverter.fromThrift(thrift);
    }

    // ---- CveSearch ----

    public static UpdateType fromThriftUpdateType(org.eclipse.sw360.datahandler.thrift.cvesearch.UpdateType thrift) {
        return UpdateTypeConverter.fromThrift(thrift);
    }

    public static org.eclipse.sw360.datahandler.thrift.cvesearch.UpdateType toThriftUpdateType(UpdateType pojo) {
        return UpdateTypeConverter.toThrift(pojo);
    }

    public static VulnerabilityUpdateStatus fromThriftVulnerabilityUpdateStatus(
            org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus thrift) {
        return VulnerabilityUpdateStatusConverter.fromThrift(thrift);
    }

    public static org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus toThriftVulnerabilityUpdateStatus(
            VulnerabilityUpdateStatus pojo) {
        return VulnerabilityUpdateStatusConverter.toThrift(pojo);
    }
}
