/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Narrow access surface used by {@link LicsExporter}, {@code LicsImporter} and
 * {@code TypeMappings} for license-archive import/export.
 *
 * <p>It exposes only the operations those utilities actually need, so a caller
 * can supply either an in-process handler or a REST-backed client without
 * depending on the full Thrift {@code LicenseService.Iface}. This replaces the
 * previous reflective {@code java.lang.reflect.Proxy} bridge in the backend
 * license handler with a compile-time typed contract; behaviour is unchanged.
 */
public interface LicenseImportExportGateway {

    List<License> getLicenses() throws TException;

    List<License> addOrOverwriteLicenses(List<License> licenses, User user) throws TException;

    List<LicenseType> getLicenseTypes() throws TException;

    List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) throws TException;

    List<Obligation> getObligations() throws TException;

    List<Obligation> addListOfObligations(List<Obligation> obligations, User user) throws TException;

    List<CustomProperties> getCustomProperties(String documentType) throws TException;

    RequestStatus updateCustomProperties(CustomProperties customProperties, User user) throws TException;
}
