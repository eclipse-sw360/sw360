/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxpackageinfo;

import org.eclipse.sw360.datahandler.db.spdx.packageinfo.*;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

@Component
public class PackageInformationHandler implements PackageInformationService.Iface {

    @Autowired
    SpdxPackageInfoDatabaseHandler handler;

    @Override
    public List<PackageInformation> getPackageInformationSummary(User user) throws TException {
        assertUser(user);
        return handler.getPackageInformationSummary(user);
    }

    @Override
    public PackageInformation getPackageInformationById(String id, User user) throws TException {
        assertNotEmpty(id);
        assertUser(user);
        return handler.getPackageInformationById(id, user);
    }

    @Override
    public PackageInformation getPackageInformationForEdit(String id, User user) throws TException {
        assertNotEmpty(id);
        assertUser(user);
        return handler.getPackageInformationForEdit(id, user);
    }

    @Override
    public AddDocumentRequestSummary addPackageInformation(PackageInformation packageInformation, User user) throws TException {
        assertNotNull(packageInformation);
        assertUser(user);
        return handler.addPackageInformation(packageInformation, user);
    }

    @Override
    public AddDocumentRequestSummary addPackageInformations(Set<PackageInformation> packageInformations, User user) throws TException {
        return handler.addPackageInformations(packageInformations, user);
    }

    @Override
    public RequestStatus updatePackageInformation(PackageInformation packageInformation, User user) throws TException {
        assertNotNull(packageInformation);
        assertUser(user);
        return handler.updatePackageInformation(packageInformation, user);
    }

    @Override
    public RequestSummary updatePackageInformations(Set<PackageInformation> packageInformations, User user) throws TException {
        assertUser(user);
        return handler.updatePackageInformations(packageInformations, user);
    }

    @Override
    public RequestStatus updatePackageInfomationFromModerationRequest(PackageInformation packageInfoAdditions, PackageInformation packageInfoDeletions, User user) throws TException {
        assertUser(user);
        return handler.updatePackageInfomationFromModerationRequest(packageInfoAdditions, packageInfoDeletions, user);
    }

    @Override
    public RequestStatus deletePackageInformation(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deletePackageInformation(id, user);
    }

}
