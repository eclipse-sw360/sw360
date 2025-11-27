/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.cyclonedx.CycloneDxBOMImporter;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Assert;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftValidate;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Class for accessing the CouchDB for Packages.
 *
 * @author: abdul.kapti@siemens-healthineers.com
 */
@Component
public class PackageDatabaseHandler extends AttachmentAwareDatabaseHandler {

    @Autowired
    private AttachmentConnector attachmentConnector;
    @Autowired
    private ComponentDatabaseHandler componentDatabaseHandler;
    private final PackageRepository packageRepository;
    private final ProjectRepository projectRepository;
    private final DatabaseHandlerUtil databaseHandlerUtil;

    private static final Logger log = LogManager.getLogger(CycloneDxBOMImporter.class);

    private static final ImmutableList<Package._Fields> listOfStringFieldsInPackageToTrim = ImmutableList.of(
            Package._Fields.NAME, Package._Fields.VERSION, Package._Fields.VCS, Package._Fields.DESCRIPTION,
            Package._Fields.HOMEPAGE_URL, Package._Fields.PURL, Package._Fields.HASH);

    @Autowired
    public PackageDatabaseHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db,
            @Qualifier("CLOUDANT_DB_CONNECTOR_CHANGELOGS") DatabaseConnectorCloudant changeLogsDb,
            AttachmentDatabaseHandler attachmentDatabaseHandler
    ) {
        super(attachmentDatabaseHandler);

        // Create the repositories
        packageRepository = new PackageRepository(db);
        projectRepository = new ProjectRepository(db);

        this.databaseHandlerUtil = new DatabaseHandlerUtil(changeLogsDb);
    }

    public Package getPackageById(String id) throws SW360Exception {
        Package pkg = packageRepository.get(id);
        assertNotNull(pkg, "Invalid Package Id");
        return pkg;
    }

    public List<Package> getPackageByIds(Set<String> ids) throws SW360Exception {
        List<Package> packages = packageRepository.get(ids);
        SW360Assert.assertEquals(ids.size(), CommonUtils.nullToEmptyList(packages).size(), "At least one package id was invalid!" );
        return packages;
    }

    public List<Package> getPackageWithReleaseByPackageIds(Set<String> ids) throws SW360Exception {
        List<Package> packages = packageRepository.get(ids);
        Set<String> releaseIds = packages.stream().filter(pkg -> CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId()))
                .map(Package::getReleaseId).collect(Collectors.toSet());
        if (CommonUtils.isNotEmpty(releaseIds)) {
            List<Release> releases = componentDatabaseHandler.getReleasesByIds(releaseIds);
            Map<String, Release> releaseIdToReleaseMap = releases.stream()
                    .map(rel -> new AbstractMap.SimpleEntry<>(rel.getId(), rel))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldVal, newVal) -> newVal));
            packages.forEach(pkg -> {
                if (CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())) {
                    pkg.setRelease(releaseIdToReleaseMap.get(pkg.getReleaseId()));
                }
            });
        }
        return packages;
    }

    public Set<Package> getPackagesByReleaseId(String id) {
        Set<Package> packages = Sets.newHashSet();
        packages.addAll(packageRepository.getPackagesByReleaseId(id));
        return packages;
    }

    public Set<Package> getPackagesByReleaseIds(Set<String> ids) {
        Set<Package> packages = Sets.newHashSet();
        for (String id : ids) {
            packages.addAll(packageRepository.getPackagesByReleaseId(id));
        }
        return packages;
    }

    public List<Package> getAllPackages() {
        return packageRepository.getAll();
    }

    public List<Package> getAllOrphanPackages() {
        return packageRepository.getOrphanPackage();
    }

    public List<Package> searchPackages(PackageSearchHandler searchHandler, String searchText) {
        return searchHandler.searchPackages(searchText);
    }

    public List<Package> searchPackagesWithFilter(String searchText, PackageSearchHandler searchHandler, Map<String,Set<String>> subQueryRestrictions) {
        return searchHandler.searchPackagesWithRestrictions(searchText, subQueryRestrictions);
    }

    public int getTotalPackageCount() {
        return packageRepository.getDocumentCount();
    }

    public List<Package> searchOrphanPackages(PackageSearchHandler searchHandler, String searchText) {
        List<Package> packages = searchPackages(searchHandler, searchText);
        Predicate<Package> orphanReleaseFilter = pkg -> CommonUtils.isNullEmptyOrWhitespace(pkg.getReleaseId());
        return packages.stream().filter(orphanReleaseFilter).collect(Collectors.toList());
    }

    private void preparePackage(Package pkg) throws SW360Exception {
        // Prepare package for database
        ThriftValidate.preparePackage(pkg);
    }

    public AddDocumentRequestSummary addPackage(Package pkg, User user) throws SW360Exception {
        removeLeadingTrailingWhitespace(pkg);
        String name = pkg.getName();
        String version = pkg.getVersion();

        if (name == null || name.isEmpty() || version == null || version.isEmpty()) {
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.NAMINGERROR);
        }

        if (CommonUtils.isNullEmptyOrWhitespace(ThriftEnumUtils.enumToString(pkg.getPackageType())))  {
            log.error(String.format("Invalid Package Type for package: '%s'", SW360Utils.printName(pkg)));
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT).setMessage("Invalid Pacakge Type!");
        }

        try {
            PackageURL purl = new PackageURL(pkg.getPurl());
            pkg.setPackageManager(PackageManager.valueOf(purl.getType().toUpperCase()));
        } catch (MalformedPackageURLException e) {
            log.error(String.format("Invalid PURL for package: '%s'", SW360Utils.printName(pkg)), e);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT).setMessage("Invalid Pacakge URL!");
        } catch (IllegalArgumentException e) {
            log.error(String.format("Invalid Package Manager for package: '%s'", SW360Utils.printName(pkg)), e);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT).setMessage("Invalid Pacakge Manager!");
        }

        // Prepare package for database
        preparePackage(pkg);
        List<Package> duplicatePackagesByPurl = getPackageByPurl(pkg.getPurl());

        if (!duplicatePackagesByPurl.isEmpty()) {
            final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                    .setMessage(SW360Constants.DUPLICATE_PACKAGE_BY_PURL);
            if(duplicatePackagesByPurl.size() == 1){
                addDocumentRequestSummary.setId(duplicatePackagesByPurl.get(0).getId());
            }
            return addDocumentRequestSummary;
        }else{
            List<Package> duplicatePackages = getPackageByNameAndVersion(name, version);
            if(!duplicatePackages.isEmpty()){
                final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                        .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
                if(duplicatePackages.size() == 1) {
                    addDocumentRequestSummary.setId(duplicatePackages.get(0).getId());
                }
                return addDocumentRequestSummary;
            }
        }

        pkg.setCreatedBy(user.getEmail());
        pkg.setCreatedOn(SW360Utils.getCreatedOn());
        AddDocumentRequestSummary summary = new AddDocumentRequestSummary()
                .setRequestStatus(AddDocumentRequestStatus.FAILURE);

        // Ensure that release exists
        if (CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())) {
            try {
                Release release = componentDatabaseHandler.getRelease(pkg.getReleaseId(), user);
                // create new package
                boolean updateStatus = packageRepository.add(pkg);
                // Update the underlying release
                if (updateStatus) {
                    release.addToPackageIds(pkg.getId());
                    componentDatabaseHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true);
                    summary.setMessage("Package successfully created with linked release");
                } else {
                    log.error(String.format("Failed to add package: %s ", SW360Utils.printName(pkg)));
                    return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE).setMessage("Failed to add package!");
                }
            } catch (SW360Exception e) {
                log.error(String.format("Invalid release id %s while adding package %s ", pkg.getReleaseId(), SW360Utils.printName(pkg)), e);
                return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT).setMessage("Invalid Release Id");
            }
        } else {
            // create new orphan package (without linked releases)
            packageRepository.add(pkg);
            summary.setMessage(String.format("An Orphan Package with id: <%s> successfully created (without linked release)", pkg.getId()));
        }
        databaseHandlerUtil.addChangeLogs(pkg, null, user.getEmail(), Operation.CREATE, attachmentConnector, Lists.newArrayList(), null, null);
        return summary.setId(pkg.getId()).setRequestStatus(AddDocumentRequestStatus.SUCCESS);
    }

    public RequestStatus updatePackage(Package updatedPkg, User user) throws SW360Exception {
        removeLeadingTrailingWhitespace(updatedPkg);
        String packageId = updatedPkg.getId();
        String name = updatedPkg.getName();
        String version = updatedPkg.getVersion();
        if (CommonUtils.isNullEmptyOrWhitespace(name) || CommonUtils.isNullEmptyOrWhitespace(version)) {
            return RequestStatus.NAMINGERROR;
        }

        if (!SW360Utils.isWriteAccessUser(updatedPkg.getCreatedBy(), user, SW360Utils.readConfig(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER))) {
            log.error(String.format("User %s does not have write access to package: %s ", user.getEmail(), packageId));
            return RequestStatus.ACCESS_DENIED;
        }

        if (CommonUtils.isNullEmptyOrWhitespace(ThriftEnumUtils.enumToString(updatedPkg.getPackageType())))  {
            log.error(String.format("Invalid Package Type for package: '%s'", packageId));
            return RequestStatus.INVALID_INPUT;
        }

        if (CommonUtils.isNotNullEmptyOrWhitespace(updatedPkg.getReleaseId()) &&
                DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(updatedPkg.getReleaseId()), packageRepository)) {
            log.error(String.format("Invalid linked release id %s for package: %s ", updatedPkg.getReleaseId(), packageId));
            return RequestStatus.INVALID_INPUT;
        }

        try {
            PackageURL purl = new PackageURL(updatedPkg.getPurl());
            updatedPkg.setPackageManager(PackageManager.valueOf(purl.getType().toUpperCase()));
        } catch (MalformedPackageURLException e) {
            log.error(String.format("Invalid PURL for package: %s", packageId), e);
            return RequestStatus.INVALID_INPUT;
        } catch (IllegalArgumentException e) {
            log.error(String.format("Invalid Package Manager for package: %s", packageId), e);
            return RequestStatus.INVALID_INPUT;
        }

        // Prepare package for database
        preparePackage(updatedPkg);
        // Get actual document for members that should not change
        Package actualPkg = packageRepository.get(packageId);
        if (actualPkg == null) {
            throw fail(404, String.format("Could not fetch package from database! id = %s", packageId));
        }

        if (changeWouldResultInDuplicate(actualPkg, updatedPkg)) {
            return RequestStatus.DUPLICATE;
        }

        copyImmutableFields(updatedPkg, actualPkg);
        updateModifiedFields(updatedPkg, user.getEmail());

        String actualReleaseId = CommonUtils.nullToEmptyString(actualPkg.getReleaseId());
        String newReleaseId = CommonUtils.nullToEmptyString(updatedPkg.getReleaseId());
        // update linked release if there is a change in linked release.
        if (!newReleaseId.equals(actualReleaseId)) {
            // Ensure that release exists
            try {
                // once we are sure that release exists, update the package
                DocumentResult resp = packageRepository.updateWithResponse(updatedPkg);
                if (CommonUtils.isNotNullEmptyOrWhitespace(actualReleaseId)) {
                    Release actualRelease = componentDatabaseHandler.getRelease(actualReleaseId, user);
                    Set<String> packageIds = CommonUtils.nullToEmptySet(actualRelease.getPackageIds());
                    // Update the previously linked release, if it contains package id
                    if (resp.isOk()) {
                        if (packageIds.contains(packageId)) {
                            packageIds.remove(packageId);
                            actualRelease.setPackageIds(packageIds);
                            componentDatabaseHandler.updateRelease(actualRelease, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true);
                        } else {
                            log.info(String.format("Linked pacakgeId: %s is not present in release: %s", packageId, actualReleaseId));
                        }
                    } else {
                        log.error(String.format("Failed to update package: %s with id: %s", SW360Utils.printName(updatedPkg), packageId));
                        return RequestStatus.FAILURE;
                    }
                }
                if (CommonUtils.isNotNullEmptyOrWhitespace(newReleaseId)) {
                    Release newRelease = componentDatabaseHandler.getRelease(newReleaseId, user);
                    Set<String> packageIds = CommonUtils.nullToEmptySet(newRelease.getPackageIds());
                    // Update the newly linked release, if it does not contain package id
                    if (resp.isOk()) {
                        if (!packageIds.contains(packageId)) {
                            newRelease.addToPackageIds(packageId);
                            componentDatabaseHandler.updateRelease(newRelease, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true);
                        } else {
                            log.info(String.format("Linked pacakgeId: %s is already present in release: %s", packageId, newReleaseId));
                        }
                    } else {
                        log.error(String.format("Failed to update package: %s with id: %s", SW360Utils.printName(updatedPkg), packageId));
                        return RequestStatus.FAILURE;
                    }
                }

            } catch (SW360Exception e) {
                log.error(String.format("Invalid release id %s while adding package %s ", newReleaseId, SW360Utils.printName(updatedPkg)), e);
                return RequestStatus.INVALID_INPUT;
            }
        } else {
            // update orphan package (without linked releases)
            packageRepository.update(updatedPkg);
        }
        databaseHandlerUtil.addChangeLogs(updatedPkg, actualPkg, user.getEmail(), Operation.UPDATE, attachmentConnector, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus deletePackage(String id, User user) throws SW360Exception {
        Package pkg = packageRepository.get(id);
        assertNotNull(pkg);

        if (checkIfInUse(id)) {
            return RequestStatus.IN_USE;
        }

        if (!SW360Utils.isWriteAccessUser(pkg.getCreatedBy(), user, SW360Utils.readConfig(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER))) {
            log.error(String.format("User %s does not have write access to package: %s ", user.getEmail(), pkg.getId()));
            return RequestStatus.ACCESS_DENIED;
        }

        RequestStatus status = cleanupPackageDependentFieldsInRelease(pkg, user);
        if (RequestStatus.SUCCESS.equals(status)) {
            packageRepository.remove(pkg);
            databaseHandlerUtil.addChangeLogs(null, pkg, user.getEmail(), Operation.DELETE, attachmentConnector,
                    Lists.newArrayList(), null, null);
            return status;
        }
        return status;
    }

    private boolean checkIfInUse(String packageId) {
        return projectRepository.getCountByPackageId(packageId) > 0;
    }

    private RequestStatus cleanupPackageDependentFieldsInRelease(Package pkg, User user) throws SW360Exception {
        String releaseId = pkg.getReleaseId();
        if (CommonUtils.isNotNullEmptyOrWhitespace(releaseId)) {
            Release release = componentDatabaseHandler.getRelease(releaseId, user);
            Set<String> packageIds = release.getPackageIds();
            if (CommonUtils.isNotEmpty(packageIds) && packageIds.contains(pkg.getId())) {
                packageIds.remove(pkg.getId());
                release.setPackageIds(packageIds);
                return componentDatabaseHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true);
            }
        }
        return RequestStatus.SUCCESS;
    }

    private boolean changeWouldResultInDuplicate(Package before, Package after) {
        if (before.getName().equalsIgnoreCase(after.getName())
                && before.getVersion().equalsIgnoreCase(after.getVersion())) {
            return false;
        }
        List<Package> duplicates = getPackageByNameAndVersion(after.getName(), after.getVersion());
        return !duplicates.isEmpty();
    }

    public List<Package> searchByName(String name) {
        return packageRepository.searchByName(name);
    }

    public List<Package> searchByVersion(String version) {
        return packageRepository.searchByVersion(version);
    }

    public List<Package> searchByPackageManager(String packageManager) {
        return packageRepository.searchByPackageManager(packageManager);
    }

    public List<Package> getPackageByNameAndVersion(String pkgName, String pkgVersion) {
        if (isNullEmptyOrWhitespace(pkgName)) {
            return Collections.emptyList();
        }
        return packageRepository.searchByNameAndVersion(pkgName, pkgVersion, true);
    }

    public List<Package> getPackageByPurl(String purl) {
        if (isNullEmptyOrWhitespace(purl)) {
            return Collections.emptyList();
        }
        return packageRepository.searchByPurl(purl, true);
    }

    private void copyImmutableFields(Package destination, Package source) {
        ThriftUtils.copyField(source, destination, Package._Fields.CREATED_ON);
        ThriftUtils.copyField(source, destination, Package._Fields.CREATED_BY);
    }

    private void removeLeadingTrailingWhitespace(Package pkg) {
        DatabaseHandlerUtil.trimStringFields(pkg, listOfStringFieldsInPackageToTrim);
        pkg.setLicenseIds(DatabaseHandlerUtil.trimSetOfString(pkg.getLicenseIds()));
    }

    public Map<PaginationData, List<Package>> getPackagesWithPagination(PaginationData pageData) {
          return packageRepository.getPackagesWithPagination(pageData);
    }
}
