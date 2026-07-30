/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.packages;

import java.util.List;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.datahandler.services.packages.PackageSearchFilterRequest;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the packages backend service.
 *
 * Types are service-api POJOs. See {@link PackageServiceRestClient} and {@link PackageClients}.
 */
public interface PackageClient {

    AddDocumentRequestSummary createPackage(Package pkg, User user);

    RequestStatus updatePackage(Package pkg, User user);

    RequestStatus deletePackage(String packageId, User user);

    Package getPackageById(String id);

    List<Package> getAllPackages();

    List<Package> searchByFilter(PackageSearchFilterRequest request);

    List<Package> searchByName(String name);

    List<Package> searchByPackageManager(String pkgManager);

    List<Package> searchByVersion(String version);

    List<Package> searchByPurl(String purl);

    int getTotalPackagesCount();

    List<Package> refineSearch(PackageSearchFilterRequest request, User user);

    Set<Package> getLinkedPackagesForRelease(String releaseId);

    List<Package> getPackagesWithReleaseByIds(Set<String> packageIds);
}
