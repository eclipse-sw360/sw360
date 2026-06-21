/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 * With modifications by Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxpackageinfo;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.spdx.packageinfo.SpdxPackageInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.Cloudant;

import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

@Service
public class PackageInformationHandler {

    private final SpdxPackageInfoDatabaseHandler handler;

    public PackageInformationHandler() throws MalformedURLException {
        handler = new SpdxPackageInfoDatabaseHandler(
                DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
    }

    PackageInformationHandler(Cloudant client, String dbName) throws MalformedURLException {
        handler = new SpdxPackageInfoDatabaseHandler(client, dbName);
    }

    public List<PackageInformation> getPackageInformationSummary(User user) {
        try {
            assertUser(user);
            return handler.getPackageInformationSummary(user).stream()
                    .map(ThriftConverter::fromThriftPackageInformation)
                    .toList();
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public PackageInformation getPackageInformationById(String id, User user) {
        try {
            assertNotEmpty(id);
            assertUser(user);
            return ThriftConverter.fromThriftPackageInformation(handler.getPackageInformationById(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public PackageInformation getPackageInformationForEdit(String id, User user) {
        try {
            assertNotEmpty(id);
            assertUser(user);
            return ThriftConverter.fromThriftPackageInformation(handler.getPackageInformationForEdit(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public AddDocumentRequestSummary addPackageInformation(PackageInformation packageInformation, User user) {
        try {
            assertNotNull(packageInformation);
            assertUser(user);
            return ThriftConverter.fromThriftAddDocumentRequestSummary(handler.addPackageInformation(
                    ThriftConverter.toThriftPackageInformation(packageInformation), user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public AddDocumentRequestSummary addPackageInformations(Set<PackageInformation> packageInformations, User user) {
        try {
            assertUser(user);
            Set<org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation> thriftSet =
                    packageInformations.stream()
                            .map(ThriftConverter::toThriftPackageInformation)
                            .collect(Collectors.toSet());
            return ThriftConverter.fromThriftAddDocumentRequestSummary(
                    handler.addPackageInformations(thriftSet, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updatePackageInformation(PackageInformation packageInformation, User user) {
        try {
            assertNotNull(packageInformation);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.updatePackageInformation(
                    ThriftConverter.toThriftPackageInformation(packageInformation), user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestSummary updatePackageInformations(Set<PackageInformation> packageInformations, User user) {
        try {
            assertUser(user);
            Set<org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation> thriftSet =
                    packageInformations.stream()
                            .map(ThriftConverter::toThriftPackageInformation)
                            .collect(Collectors.toSet());
            return ThriftConverter.fromThriftRequestSummary(handler.updatePackageInformations(thriftSet, user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updatePackageInfomationFromModerationRequest(
            ModerationUpdate<PackageInformation> update, User user) {
        try {
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.updatePackageInfomationFromModerationRequest(
                    ThriftConverter.toThriftPackageInformation(update.getAdditions()),
                    ThriftConverter.toThriftPackageInformation(update.getDeletions()),
                    user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus deletePackageInformation(String id, User user) {
        try {
            assertId(id);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.deletePackageInformation(id, user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }
}
