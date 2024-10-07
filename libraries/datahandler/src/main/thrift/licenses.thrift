/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 * With contributions by TOSHIBA CORPORATION, 2021.
 * With contributions by Toshiba Software Development (Vietnam) Co., Ltd., 2021.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "users.thrift"
include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.licenses
namespace php sw360.thrift.licenses

typedef users.User User
typedef users.RequestedAction RequestedAction
typedef sw360.RequestStatus RequestStatus
typedef sw360.DocumentState DocumentState
typedef sw360.CustomProperties CustomProperties
typedef sw360.RequestSummary RequestSummary
typedef sw360.Quadratic Quadratic
typedef sw360.SW360Exception SW360Exception
typedef i32 int


enum ObligationLevel {
    ORGANISATION_OBLIGATION = 0,
    PROJECT_OBLIGATION = 1,
    COMPONENT_OBLIGATION = 2,
    LICENSE_OBLIGATION = 3,
}

enum ObligationType {
    PERMISSION = 0,
    RISK = 1,
    EXCEPTION = 2,
    RESTRICTION = 3,
    OBLIGATION = 4
}

enum ObligationElementStatus {
    UPDATED = 0,
    DEFINED = 1
    UNDEFINED = 2
}

struct Obligation {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "obligation",
    4: required string text,
    5: optional set<string> whitelist,
    6: optional bool development,
    7: optional bool distribution,
    10: optional string title,
    11: optional map<string, string> customPropertyToValue,

    // These two are a quick fix to receiving booleans in PHP not working at the moment
    15: optional string developmentString,
    16: optional string distributionString,

    // information from external data sources
    19: optional map<string, string> externalIds,
    21: optional string comments,
    22: optional ObligationLevel obligationLevel,
    23: optional ObligationType obligationType,
    300: optional map<string, string> additionalData,
    97: optional string node,

}

struct LicenseType {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "licenseType",
	5: required i32 licenseTypeId,
    6: required string licenseType;
}

struct ObligationNode {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "obligationNode",
    4: optional string nodeType,
    5: optional string nodeText,
    6: optional string oblElementId
}

struct ObligationElement {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "obligationElement",
    4: required string langElement,
    5: required string action,
    6: required string object,
    7: optional ObligationElementStatus status
}

struct LicenseObligationList {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "licenseObligationList",
    4: required string licenseId,
    5: optional map<string, Obligation> linkedObligations
}

struct License {
	 1: optional string id,
	 2: optional string revision,
	 3: optional string type = "license",
	 4: optional string shortname, // Short name of the license
	 5: required string fullname,
	 6: optional LicenseType licenseType,
	 7: optional string licenseTypeDatabaseId,
     8: optional string externalLicenseLink,
     10: optional string note,

    // information from external data sources
     9: optional map<string, string> externalIds,
    300: optional map<string, string> additionalData,

    // Additional informations
	12: optional string reviewdate,

	17: optional Quadratic OSIApproved = sw360.Quadratic.NA,
	18: optional Quadratic FSFLibre = sw360.Quadratic.NA,

    20: optional list<Obligation> obligations,
    21: optional set<string> obligationDatabaseIds,
    22: optional string obligationListId,
    25: optional string text,

    30: optional bool checked = true;

    90: optional DocumentState documentState,

	200: optional map<RequestedAction, bool> permissions
}

service LicenseService {

    /**
     * Get a single license by providing its ID, filled with license type, risks and obligations containing obligations and whitelists
     * filtered for the given organisation
     **/
    License getByID(1:string id, 2: string organisation) throws (1: SW360Exception exp);

    /**
      * Get a single license by providing its ID, filled with license type and obligations containing obligations and whitelists
      * filtered for the given organisation, risks are not set,
      * user's moderation request with status pending or in progress applied
      **/
    License getByIDWithOwnModerationRequests(1:string id, 2: string organisation, 3: User user);

    /**
     * get licenses for ids filled with license types, risks and obligations containing obligations
     * whitelists filtered for organisation
     **/
    list<License> getByIds(1:set<string> ids, 2: string organisation);

    /**
     * Add a new obligation object to database, return id
     **/
    string addObligations(1:Obligation obligations, 2: User user);

    /**
     * Update a existed obligation object to database, return id
     **/
    string updateObligation(1:Obligation obligation, 2: User user);

    /**
    * Add an existing obligation to a license or generate moderation request if user has no permission
    **/
    RequestStatus addObligationsToLicense(1: set<Obligation> obligations, 2: License license, 3: User user);

    /**
     * Add a new license type object to database
     **/
    RequestStatus addLicenseType(1:LicenseType licenseType, 2: User user);

    /**
     * Update given license,
     * user is used to check permissions, requesting user's department is used to set whitelists
     **/
    RequestStatus updateLicense(1: License license, 2: User user, 3: User requestingUser);

     /**
      * update license in database if user has permissions, additions and deletions are the parts of the moderation request
      * that specify which properties to add to and which to delete from license
      **/
    RequestStatus updateLicenseFromModerationRequest(1: License additions, 2: License deletions, 3: User user, 4: User requestingUser);

    /**
     * Update the whitelisted obligations for an organisation, generate moderation request if user has no permissions
     **/
    RequestStatus updateWhitelist(1: string licenseId, 2: set<string> obligationsDatabaseIds, 3: User user);

    /**
     * delete license from database if user has permissions,
     * otherwise create moderation request
     **/
    RequestStatus deleteLicense(1: string licenseId, 2: User user);

    /**
     * Get a list of summaries filled with license types for all licenses
     **/
    list<License> getLicenseSummary();

    /**
     * Get a list of export summaries for all licenses
     **/
    list<License> getLicenseSummaryForExport();

    /**
     * download license for api
     **/
    binary downloadExcel(1:string token) throws (1: SW360Exception exp);

    /**
     * get report data stream
     **/
    binary getLicenseReportDataStream() throws (1: SW360Exception exp);

    /**
     * get a list of all full license documents filled with obligations, risks and license types,
     * obligations and risks themselves are not filled
     **/
    list<License> getDetailedLicenseSummaryForExport(1: string organisation);

    /**
      * get a list of full documents with ids in identifiers, filled with obligations, risks and license types,
      * obligations and risks themselves are also filled, obligation whitelists are filtered for organisation
      **/
    list<License> getDetailedLicenseSummary(1: string organisation, 2: list<string> identifiers);

    /**
     * bulk add for import of license archive, returns input license types if successful, null otherwise
     **/
    list<LicenseType> addLicenseTypes(1: list <LicenseType> licenseTypes, 2: User user);

    /**
     * bulk add for import of license archive, returns input licenses if successful, null otherwise
     **/
    list<License> addLicenses(1: list <License> licenses, 2: User user);

    /**
     * bulk add for import of license archive, returns input licenses if successful, null otherwise
     **/
    list<License> addOrOverwriteLicenses(1: list <License> licenses, 2: User user);

    /**
     * bulk add for import of license archive, returns input obligations if successful, null otherwise
     **/
    list<Obligation> addListOfObligations(1: list <Obligation> obligations, 2: User user);

    /**
     * get complete list of license types
     **/
    list<LicenseType> getLicenseTypes();

    /**
     * get complete list of licenses, completely filled
     **/
    list<License> getLicenses();

    /**
     * get complete list of filled obligations
     **/
    list<Obligation> getObligations();

    /**
     * get license types with id in ids
     **/
    list<LicenseType> getLicenseTypesByIds( 1: list<string> ids);

    /**
     * get filled obligations with id in ids
     **/
    list<Obligation> getObligationsByIds( 1: list<string> ids);

    /**
     * get obligations with license id
     **/
    list<Obligation> getObligationsByLicenseId( 1: string id);

    LicenseType getLicenseTypeById( 1: string id);

    /**
     * return filled obligation
     **/
    Obligation getObligationsById( 1: string id);

    list<CustomProperties> getCustomProperties(1: string documentType);

    RequestStatus updateCustomProperties(1: CustomProperties customProperties, 2: User user );

   /**
    * removes all licenses, license types, obligations, obligations, risks, risk categories from db
    **/
    RequestSummary deleteAllLicenseInformation(1: User user);

    RequestSummary importAllSpdxLicenses(1: User user);

    RequestSummary importAllOSADLLicenses(1: User user);
    /**
     * delete obligation from database if user has permissions
     **/
    RequestStatus deleteObligations(1: string id, 2: User user);

    /**
     * delete license type from database if user has permissions
     **/
    RequestStatus deleteLicenseType(1: string id, 2: User user);

    /**
     * Check license type is in using by license
     **/
    int checkLicenseTypeInUse(1: string id);

    /* Add a new Obligation Element object to database, return id
    **/
    string addObligationElements(1:ObligationElement obligationElement, 2: User user);

    /**
     * get complete list of filled obligation elements
     **/
    list<ObligationElement> getObligationElements();

    /**
     * Add a new Obligation Node object to database, return id
     **/
    string addObligationNodes(1:ObligationNode obligationNode, 2: User user);

    /**
     * get complete list of filled obligation nodes
     **/
    list<ObligationNode> getObligationNodes();

    /**
     * get ObligationNode by id
     **/
    ObligationNode getObligationNodeById( 1: string id);

    /**
     * get ObligationElement by id
     **/
    ObligationElement getObligationElementById( 1: string id);

    /**
     * add ObligationNode from jsonString input
     **/
    string addNodes(1:string jsonString, 2: User user)

    /**
     * build obligation text after addNodes
     **/
    string buildObligationText(1: string nodes, 2: string level)

    // Search functions

    /**
     * global search function to list obligation elements which match the text argument
     */
    list<ObligationElement> searchObligationElement(1: string text);

    /**
     * convert Text to Node in obligation
     **/
    string convertTextToNode(1:Obligation Obligation, 2: User user);
}
