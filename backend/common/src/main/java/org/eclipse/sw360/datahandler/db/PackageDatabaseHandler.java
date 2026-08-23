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
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_PACKAGE;

import java.net.MalformedURLException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.packages.PackageConverter;
import org.eclipse.sw360.cyclonedx.CycloneDxBOMImporter;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.services.changelogs.Operation;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.datahandler.services.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;

/**
 * Class for accessing the CouchDB for Packages (service-api POJO storage).
 */
public class PackageDatabaseHandler extends AttachmentAwareDatabaseHandler {

    private final AttachmentConnector attachmentConnector;
    private final PackageRepository packageRepository;
    private final ProjectRepository projectRepository;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final DatabaseHandlerUtil databaseHandlerUtil;

    private static final Logger log = LogManager.getLogger(CycloneDxBOMImporter.class);

    public PackageDatabaseHandler(Cloudant client, String dbName, String attachmentDbName, String changeLogsDbName,
            AttachmentDatabaseHandler attachmentDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler)
            throws MalformedURLException {

        super(attachmentDatabaseHandler);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);

        packageRepository = new PackageRepository(db);
        projectRepository = new ProjectRepository(db);

        attachmentConnector = new AttachmentConnector(client, attachmentDbName, Duration.durationOf(30, TimeUnit.SECONDS));

        this.componentDatabaseHandler = componentDatabaseHandler;
        DatabaseConnectorCloudant changeLogsDb = new DatabaseConnectorCloudant(client, changeLogsDbName);
        this.databaseHandlerUtil = new DatabaseHandlerUtil(changeLogsDb);
    }

    public PackageDatabaseHandler(Cloudant client, String dbName, String changeLogsDbName, String attachmentDbName)
            throws MalformedURLException {

        this(client, dbName, attachmentDbName, changeLogsDbName,
                new AttachmentDatabaseHandler(client, dbName, attachmentDbName),
                new ComponentDatabaseHandler(client, dbName, changeLogsDbName, attachmentDbName));
    }

    public Package getPackageById(String id) {
        Package pkg = packageRepository.get(id);
        if (pkg == null) {
            throw new SW360Exception("Invalid Package Id");
        }
        return pkg;
    }

    public List<Package> getPackageByIds(Set<String> ids) {
        List<Package> packages = packageRepository.get(ids);
        if (ids.size() != CommonUtils.nullToEmptyList(packages).size()) {
            throw new SW360Exception("At least one package id was invalid!");
        }
        return packages;
    }

    public List<Package> getPackageWithReleaseByPackageIds(Set<String> ids) {
        List<Package> packages = packageRepository.get(ids);
        Set<String> releaseIds = packages.stream()
                .filter(pkg -> CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId()))
                .map(Package::getReleaseId)
                .collect(Collectors.toSet());
        if (CommonUtils.isNotEmpty(releaseIds)) {
            List<Release> releases = componentDatabaseHandler.getReleasesByIds(releaseIds);
            Map<String, Release> releaseIdToReleaseMap = releases.stream()
                    .map(rel -> new AbstractMap.SimpleEntry<>(rel.getId(), rel))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldVal, newVal) -> newVal));
            packages.forEach(pkg -> {
                if (CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())) {
                    pkg.setRelease(ReleaseConverter.fromThrift(releaseIdToReleaseMap.get(pkg.getReleaseId())));
                }
            });
        }
        return packages;
    }

    public Set<Package> getPackagesByReleaseId(String id) {
        return Sets.newHashSet(packageRepository.getPackagesByReleaseId(id));
    }

    public Set<Package> getPackagesByReleaseIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return Sets.newHashSet(packageRepository.getPackagesByReleaseIds(ids));
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

    public List<Package> searchPackagesWithFilter(String searchText, PackageSearchHandler searchHandler,
            Map<String, Set<String>> subQueryRestrictions) {
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

    private void preparePackage(Package pkg) {
        if (CommonUtils.isNullEmptyOrWhitespace(pkg.getName())) {
            throw new SW360Exception("package name cannot be empty!");
        }
        if (CommonUtils.isNullEmptyOrWhitespace(pkg.getVersion())) {
            throw new SW360Exception("package version cannot be empty!");
        }
        if (CommonUtils.isNullEmptyOrWhitespace(pkg.getPurl())) {
            throw new SW360Exception("package purl cannot be empty!");
        }
        if (pkg.getPackageManager() == null) {
            throw new SW360Exception("package manager cannot be empty!");
        }
        if (pkg.getPackageType() == null) {
            throw new SW360Exception("package type cannot be empty!");
        }
        pkg.setRelease(null);
        pkg.setType(TYPE_PACKAGE);
    }

    public AddDocumentRequestSummary addPackage(Package pkg, User user) {
        removeLeadingTrailingWhitespace(pkg);
        String name = pkg.getName();
        String version = pkg.getVersion();

        if (name == null || name.isEmpty() || version == null || version.isEmpty()) {
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.NAMINGERROR);
        }

        if (pkg.getPackageType() == null) {
            log.error("Invalid Package Type for package: '{}'", printName(pkg));
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT)
                    .setMessage("Invalid Pacakge Type!");
        }

        try {
            PackageURL purl = new PackageURL(pkg.getPurl());
            pkg.setPackageManager(PackageManager.valueOf(purl.getType().toUpperCase()));
        } catch (MalformedPackageURLException e) {
            log.error(String.format("Invalid PURL for package: '%s'", printName(pkg)), e);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT)
                    .setMessage("Invalid Pacakge URL!");
        } catch (IllegalArgumentException e) {
            log.error(String.format("Invalid Package Manager for package: '%s'", printName(pkg)), e);
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT)
                    .setMessage("Invalid Pacakge Manager!");
        }

        preparePackage(pkg);
        List<Package> duplicatePackagesByPurl = getPackageByPurl(pkg.getPurl());

        if (!duplicatePackagesByPurl.isEmpty()) {
            final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                    .setMessage(org.eclipse.sw360.datahandler.common.SW360Constants.DUPLICATE_PACKAGE_BY_PURL);
            if (duplicatePackagesByPurl.size() == 1) {
                addDocumentRequestSummary.setId(duplicatePackagesByPurl.get(0).getId());
            }
            return addDocumentRequestSummary;
        } else {
            List<Package> duplicatePackages = getPackageByNameAndVersion(name, version);
            if (!duplicatePackages.isEmpty()) {
                final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                        .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
                if (duplicatePackages.size() == 1) {
                    addDocumentRequestSummary.setId(duplicatePackages.get(0).getId());
                }
                return addDocumentRequestSummary;
            }
        }

        pkg.setCreatedBy(user.getEmail());
        pkg.setCreatedOn(SW360Utils.getCreatedOn());
        AddDocumentRequestSummary summary = new AddDocumentRequestSummary()
                .setRequestStatus(AddDocumentRequestStatus.FAILURE);

        if (CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())) {
            try {
                Release release = componentDatabaseHandler.getRelease(pkg.getReleaseId(), user);
                boolean updateStatus = packageRepository.add(pkg);
                if (updateStatus) {
                    release.addToPackageIds(pkg.getId());
                    componentDatabaseHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true);
                    summary.setMessage("Package successfully created with linked release");
                } else {
                    log.error("Failed to add package: {} ", printName(pkg));
                    return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE)
                            .setMessage("Failed to add package!");
                }
            } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
                log.error(String.format("Invalid release id %s while adding package %s ", pkg.getReleaseId(),
                        printName(pkg)), e);
                return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT)
                        .setMessage("Invalid Release Id");
            }
        } else {
            packageRepository.add(pkg);
            summary.setMessage(String.format(
                    "An Orphan Package with id: <%s> successfully created (without linked release)", pkg.getId()));
        }
        databaseHandlerUtil.addChangeLogs(PackageConverter.toThrift(pkg), null, user.getEmail(), Operation.CREATE,
                attachmentConnector, Lists.newArrayList(), null, null);
        return summary.setId(pkg.getId()).setRequestStatus(AddDocumentRequestStatus.SUCCESS);
    }

    public RequestStatus updatePackage(Package updatedPkg, User user) {
        removeLeadingTrailingWhitespace(updatedPkg);
        String packageId = updatedPkg.getId();
        String name = updatedPkg.getName();
        String version = updatedPkg.getVersion();
        if (CommonUtils.isNullEmptyOrWhitespace(name) || CommonUtils.isNullEmptyOrWhitespace(version)) {
            return RequestStatus.NAMINGERROR;
        }

        Package actualPkg = packageRepository.get(packageId);
        if (actualPkg == null) {
            throw new SW360Exception(String.format("Could not fetch package from database! id = %s", packageId), 404);
        }

        if (!SW360Utils.isWriteAccessUser(actualPkg.getCreatedBy(), user,
                SW360Utils.readConfig(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER))) {
            log.error("User {} does not have write access to package: {} ", user.getEmail(), packageId);
            return RequestStatus.ACCESS_DENIED;
        }

        if (updatedPkg.getPackageType() == null) {
            log.error("Invalid Package Type for package: '{}'", packageId);
            return RequestStatus.INVALID_INPUT;
        }

        if (CommonUtils.isNotNullEmptyOrWhitespace(updatedPkg.getReleaseId())
                && DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(updatedPkg.getReleaseId()),
                        packageRepository)) {
            log.error("Invalid linked release id {} for package: {} ", updatedPkg.getReleaseId(), packageId);
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

        preparePackage(updatedPkg);

        if (changeWouldResultInDuplicate(actualPkg, updatedPkg)) {
            return RequestStatus.DUPLICATE;
        }

        copyImmutableFields(updatedPkg, actualPkg);
        updatedPkg.setModifiedBy(user.getEmail());
        updatedPkg.setModifiedOn(SW360Utils.getCreatedOn());

        String actualReleaseId = CommonUtils.nullToEmptyString(actualPkg.getReleaseId());
        String newReleaseId = CommonUtils.nullToEmptyString(updatedPkg.getReleaseId());
        if (!newReleaseId.equals(actualReleaseId)) {
            try {
                DocumentResult resp = packageRepository.updateWithResponse(updatedPkg);
                if (CommonUtils.isNotNullEmptyOrWhitespace(actualReleaseId)) {
                    Release actualRelease = componentDatabaseHandler.getRelease(actualReleaseId, user);
                    Set<String> packageIds = CommonUtils.nullToEmptySet(actualRelease.getPackageIds());
                    if (resp.isOk()) {
                        if (packageIds.contains(packageId)) {
                            packageIds.remove(packageId);
                            actualRelease.setPackageIds(packageIds);
                            componentDatabaseHandler.updateRelease(actualRelease, user, ThriftUtils.IMMUTABLE_OF_RELEASE,
                                    true);
                        } else {
                            log.info("Linked pacakgeId: {} is not present in release: {}", packageId, actualReleaseId);
                        }
                    } else {
                        log.error("Failed to update package: {} with id: {}", printName(updatedPkg), packageId);
                        return RequestStatus.FAILURE;
                    }
                }
                if (CommonUtils.isNotNullEmptyOrWhitespace(newReleaseId)) {
                    Release newRelease = componentDatabaseHandler.getRelease(newReleaseId, user);
                    Set<String> packageIds = CommonUtils.nullToEmptySet(newRelease.getPackageIds());
                    if (resp.isOk()) {
                        if (!packageIds.contains(packageId)) {
                            newRelease.addToPackageIds(packageId);
                            componentDatabaseHandler.updateRelease(newRelease, user, ThriftUtils.IMMUTABLE_OF_RELEASE,
                                    true);
                        } else {
                            log.info("Linked pacakgeId: {} is already present in release: {}", packageId, newReleaseId);
                        }
                    } else {
                        log.error("Failed to update package: {} with id: {}", printName(updatedPkg), packageId);
                        return RequestStatus.FAILURE;
                    }
                }

            } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
                log.error(String.format("Invalid release id %s while adding package %s ", newReleaseId,
                        printName(updatedPkg)), e);
                return RequestStatus.INVALID_INPUT;
            }
        } else {
            packageRepository.update(updatedPkg);
        }
        databaseHandlerUtil.addChangeLogs(PackageConverter.toThrift(updatedPkg), PackageConverter.toThrift(actualPkg),
                user.getEmail(), Operation.UPDATE, attachmentConnector, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus deletePackage(String id, User user) {
        Package pkg = packageRepository.get(id);
        if (pkg == null) {
            throw new SW360Exception("Invalid null input!");
        }

        if (checkIfInUse(id)) {
            return RequestStatus.IN_USE;
        }

        if (!SW360Utils.isWriteAccessUser(pkg.getCreatedBy(), user,
                SW360Utils.readConfig(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER))) {
            log.error("User {} does not have write access to package: {} ", user.getEmail(), pkg.getId());
            return RequestStatus.ACCESS_DENIED;
        }

        RequestStatus status = cleanupPackageDependentFieldsInRelease(pkg, user);
        if (RequestStatus.SUCCESS.equals(status)) {
            packageRepository.remove(pkg);
            databaseHandlerUtil.addChangeLogs(null, PackageConverter.toThrift(pkg), user.getEmail(), Operation.DELETE,
                    attachmentConnector, Lists.newArrayList(), null, null);
            return status;
        }
        return status;
    }

    private boolean checkIfInUse(String packageId) {
        return projectRepository.getCountByPackageId(packageId) > 0;
    }

    private RequestStatus cleanupPackageDependentFieldsInRelease(Package pkg, User user) {
        String releaseId = pkg.getReleaseId();
        if (CommonUtils.isNotNullEmptyOrWhitespace(releaseId)) {
            try {
                Release release = componentDatabaseHandler.getRelease(releaseId, user);
                Set<String> packageIds = release.getPackageIds();
                if (CommonUtils.isNotEmpty(packageIds) && packageIds.contains(pkg.getId())) {
                    packageIds.remove(pkg.getId());
                    release.setPackageIds(packageIds);
                    return toPojo(componentDatabaseHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE,
                            true));
                }
            } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
                throw new SW360Exception(e.getWhy() != null ? e.getWhy() : e.getMessage(), e);
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
        destination.setCreatedOn(source.getCreatedOn());
        destination.setCreatedBy(source.getCreatedBy());
    }

    private void removeLeadingTrailingWhitespace(Package pkg) {
        if (pkg.getName() != null) {
            pkg.setName(pkg.getName().trim());
        }
        if (pkg.getVersion() != null) {
            pkg.setVersion(pkg.getVersion().trim());
        }
        if (pkg.getVcs() != null) {
            pkg.setVcs(pkg.getVcs().trim());
        }
        if (pkg.getDescription() != null) {
            pkg.setDescription(pkg.getDescription().trim());
        }
        if (pkg.getHomepageUrl() != null) {
            pkg.setHomepageUrl(pkg.getHomepageUrl().trim());
        }
        if (pkg.getPurl() != null) {
            pkg.setPurl(pkg.getPurl().trim());
        }
        if (pkg.getHash() != null) {
            pkg.setHash(pkg.getHash().trim());
        }
        pkg.setLicenseIds(DatabaseHandlerUtil.trimSetOfString(pkg.getLicenseIds()));
    }

    public Map<PaginationData, List<Package>> getPackagesWithPagination(PaginationData pageData) {
        return packageRepository.getPackagesWithPagination(pageData);
    }

    private static String printName(Package pkg) {
        if (pkg == null || CommonUtils.isNullEmptyOrWhitespace(pkg.getName())) {
            return "New Package";
        }
        return SW360Utils.getVersionedName(pkg.getName(), pkg.getVersion());
    }

    private static RequestStatus toPojo(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        if (thrift == null) {
            return null;
        }
        return RequestStatus.valueOf(thrift.name());
    }
}
