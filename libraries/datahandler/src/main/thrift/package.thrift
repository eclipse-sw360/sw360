/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "sw360.thrift"
include "components.thrift"
include "users.thrift"
include "vendors.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.packages
namespace php sw360.thrift.packages

typedef sw360.CycloneDxComponentType CycloneDxComponentType
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.RequestSummary RequestSummary
typedef sw360.RequestStatus RequestStatus
typedef sw360.SW360Exception SW360Exception
typedef sw360.PaginationData PaginationData
typedef users.User User
typedef vendors.Vendor Vendor
typedef components.Release Release

struct Package {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "package",
    4: required string name,
    5: required string version,
    6: required string purl, // package url
    7: required CycloneDxComponentType packageType, // required field in CycloneDX specifications
    8: optional string releaseId,
    9: optional set<string> licenseIds,
    10: optional string description,
    11: optional string homepageUrl,
    12: optional string hash,
    13: optional string vcs, // version control system
    14: optional string createdOn,
    15: optional string createdBy,
    16: optional string modifiedOn,
    17: optional string modifiedBy,
    18: optional PackageManager packageManager,
    19: optional Vendor vendor, // Supplier or Manufacturer
    20: optional string vendorId,
    30: optional Release release // only used for Linked packages UI in project details page.
}

enum PackageManager {
    ALPINE = 0,
    ALPM = 1,
    APK = 2,
    BITBUCKET = 3,
    CARGO = 4,
    COCOAPODS = 5,
    COMPOSER = 6,
    CONAN = 7,
    CONDA = 8,
    CPAN = 9,
    CRAN = 10,
    DEB = 11,
    DOCKER = 12,
    DRUPAL = 13,
    GEM = 14,
    GENERIC = 15,
    GITHUB = 16,
    GITLAB = 17,
    GOLANG = 18,
    GRADLE = 19,
    HACKAGE = 20,
    HEX = 21,
    HUGGINGFACE = 22,
    MAVEN = 23,
    MLFLOW = 24,
    NPM = 25,
    NUGET = 26,
    OCI = 27,
    PUB = 28,
    PYPI = 29,
    RPM = 30,
    SWID = 31,
    SWIFT = 32,
    YARN = 33,
    YOCTO = 34
}

service PackageService {
    /**
     * Get Package by Id
     */
    Package getPackageById(1: string packageId) throws (1: SW360Exception exp);

    /**
     * Get Packages by list of Package id
     */
    list<Package> getPackageByIds(1: set<string> packageIds) throws (1: SW360Exception exp);

    /**
     * Get Packages with associated release object by list of Package id
     */
    list<Package> getPackageWithReleaseByPackageIds(1: set<string> packageIds) throws (1: SW360Exception exp);

    /**
     * Get All Packages for package portal landing page and empty search
     */
    list<Package> getAllPackages();

    /**
     * Get All Orphan Packages for search / link to release
     */
    list<Package> getAllOrphanPackages();

    /**
     * global search function to list packages which match the text argument
     */
    list<Package> searchPackages(1: string text, 2: User user);

    /**
     * search packages in database that match subQueryRestrictions
     */
    list<Package> searchPackagesWithFilter(1: string text, 2: map<string, set<string>> subQueryRestrictions);

    /**
     * global search function to list orphan packages which match the text argument
     */
    list<Package> searchOrphanPackages(1: string text, 2: User user);

    /**
     * Get list of all the Package by list of release id
     */
    set<Package> getPackagesByReleaseIds(1: set<string> releaseIds) throws (1: SW360Exception exp);

    /**
     * Get list of all the Package by release id
     */
    set<Package> getPackagesByReleaseId(1: string releaseId) throws (1: SW360Exception exp);

    /**
     * Add package to database with user as creator, return package id
     */
    AddDocumentRequestSummary addPackage(1: Package pkg, 2: User user) throws (1: SW360Exception exp);

    /**
     * Update Package
     */
    RequestStatus updatePackage(1: Package pkg, 2: User user) throws (1: SW360Exception exp);

    /**
     * Delete Package by Id
     */
    RequestStatus deletePackage(1: string pacakgeId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Details of all packages with pagination
     **/
    map<PaginationData, list<Package>> getPackagesWithPagination(1: PaginationData pageData);

    /**
     * total number of packages in the DB
     **/
    i32 getTotalPackagesCount();
    
    /**
     * list of packages which match the `name`
     */
    list<Package> searchByName(1: string name);
    
    /**
     * list of packages which match the `packageManager`
     */
    list<Package> searchByPackageManager(1: string pkgManager);
    
    /**
     * list of packages which match the `version`
     */
    list<Package> searchByVersion(1: string version);
    
    /**
     * list of packages which match the `purl`
     */
    list<Package> searchByPurl(1: string purl);

}
