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
package org.eclipse.sw360.datahandler.db.spdx.db;

import com.cloudant.client.api.CloudantClient;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentRepository;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdxpackageinfo.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing the CouchDB database
 *
 * @author hieu1.phamvan@toshiba.co.jp
 */
public class SpdxPackageInfoDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxPackageInfoDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;

    private final SpdxPackageInfoRepository PackageInfoRepository;
    private final SpdxDocumentRepository SPDXDocumentRepository;

    public SpdxPackageInfoDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(httpClient, dbName);

        // Create the repositories
        PackageInfoRepository = new SpdxPackageInfoRepository(db);
        SPDXDocumentRepository = new SpdxDocumentRepository(db);

        // Create the moderator
    }

    public List<PackageInformation> getPackageInformationSummary(User user) {
        List<PackageInformation> packageInfos = PackageInfoRepository.getPackageInformationSummary();
        return packageInfos;
    }

    public PackageInformation getPackageInformationById(String id, User user) throws SW360Exception {
        PackageInformation packageInfo = PackageInfoRepository.get(id);
        return packageInfo;
    }

    public AddDocumentRequestSummary addPackageInformation(PackageInformation packageInfo, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary();
        // if (makePermission(packageInfo, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return requestSummary.setRequestStatus(AddDocumentRequestStatus.SENT_TO_MODERATOR);
        // }
        PackageInfoRepository.add(packageInfo);
        String packageInfoId = packageInfo.getId();
        String spdxDocumentId = packageInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        Set<String> spdxPackageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        spdxPackageInfoIds.add(packageInfoId);
        spdxDocument.setSpdxPackageInfoIds(spdxPackageInfoIds);
        SPDXDocumentRepository.update(spdxDocument);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(packageInfoId);
    }

    public AddDocumentRequestSummary addPackageInformations(Set<PackageInformation> packageInfos, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary();
        // if (makePermission(packageInfos, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return requestSummary.setRequestStatus(AddDocumentRequestStatus.SENT_TO_MODERATOR);
        // }
        String spdxDocumentId = packageInfos.iterator().next().getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        Set<String> packageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        for (PackageInformation packageInfo : packageInfos) {
            PackageInfoRepository.add(packageInfo);
            packageInfoIds.add(packageInfo.getId());
        }
        spdxDocument.setSpdxPackageInfoIds(packageInfoIds);
        SPDXDocumentRepository.update(spdxDocument);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(spdxDocumentId);
    }

    public RequestStatus updatePackageInformation(PackageInformation packageInfo, User user) throws SW360Exception {
        // if (makePermission(packageInfo, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return RequestStatus.SENT_TO_MODERATOR;
        // }
        PackageInformation actual = PackageInfoRepository.get(packageInfo.getId());
        assertNotNull(actual, "Could not find SPDX Package Information to update!");
        PackageInfoRepository.update(packageInfo);
        return RequestStatus.SUCCESS;
    }

    public RequestSummary updatePackageInformations(Set<PackageInformation> packageInfos, User user) throws SW360Exception {
        // if (makePermission(packageInfos, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return new RequestSummary().setRequestStatus(RequestStatus.SENT_TO_MODERATOR);
        // }
        for (PackageInformation packageInfo : packageInfos) {
            PackageInformation actual = PackageInfoRepository.get(packageInfo.getId());
            assertNotNull(actual, "Could not find SPDX Package Information to update!");
            PackageInfoRepository.update(packageInfo);
        }
        return new RequestSummary().setRequestStatus(RequestStatus.SUCCESS);
    }

    public RequestStatus deletePackageInformation(String id, User user) throws SW360Exception {
        PackageInformation packageInfo = PackageInfoRepository.get(id);
        assertNotNull(packageInfo, "Could not find SPDX Package Information to delete!");
        // if (makePermission(packageInfo, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return RequestStatus.SENT_TO_MODERATOR;
        // }
        PackageInfoRepository.remove(packageInfo);
        String spdxDocumentId = packageInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        Set<String> packageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        packageInfoIds.remove(id);
        spdxDocument.setSpdxPackageInfoIds(packageInfoIds);
        SPDXDocumentRepository.update(spdxDocument);
        return RequestStatus.SUCCESS;
    }

}