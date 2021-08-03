/*
 * Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxpackageinfo;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.spdxpackageinfo.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.spdxpackageinfo.db.*;

import com.cloudant.client.api.CloudantClient;

import org.apache.thrift.TException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * Implementation of the Thrift service
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class PackageInformationHandler implements PackageInformationService.Iface {

    SpdxPackageInfoDatabaseHandler handler;

    PackageInformationHandler() throws MalformedURLException {
        handler = new SpdxPackageInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
    }

    PackageInformationHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        handler = new SpdxPackageInfoDatabaseHandler(httpClient, dbName);
    }

    @Override
    public List<PackageInformation> getPackageInformationSummary(User user) throws TException {
        return handler.getPackageInformationSummary(user);
    }

    @Override
    public PackageInformation getPackageInformationById(String id, User user) throws TException {
        return handler.getPackageInformationById(id, user);
    }

    @Override
    public AddDocumentRequestSummary addPackageInformation(PackageInformation packageInformation, User user) throws TException {
        return handler.addPackageInformation(packageInformation, user);
    }

    @Override
    public AddDocumentRequestSummary addPackageInformations(Set<PackageInformation> packageInformations, User user) throws TException {
        return handler.addPackageInformations(packageInformations, user);
    }

    @Override
    public RequestStatus updatePackageInformation(PackageInformation packageInformation, User user) throws TException {
        return handler.updatePackageInformation(packageInformation, user);
    }

    @Override
    public RequestSummary updatePackageInformations(Set<PackageInformation> packageInformations, User user) throws TException {
        return handler.updatePackageInformations(packageInformations, user);
    }

    @Override
    public RequestStatus deletePackageInformation(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deletePackageInformation(id, user);
    }

}