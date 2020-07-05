# Changelog

This is the changelog file of the sw360 project. It starts with the first release being provided at eclipse/sw360. For older releases, please refer to the first project location:

https://github.com/sw360/sw360portal/releases

## sw360-9.0.0-M1

This release brings new features which also change the data model. Therefore, a major version step is justified. Major new features are:

* Support for custom field layout using the referring Liferay mechanism, fully transparent with the REST API using additional data fields
* Changelog for changed data for projects, components and releases
* Reordered sections in detail view
* A completely new view *Clearing Status* offering tree and list view

Besides bug fixes and features correct bugs so far. In order to fix a bug with the REST API, note that a migration script should be executed. It corrects a missing value for the status of the release which is also now automatically set when creating releases using the REST API (and not only in the Web user interface).

### Features

* `a2e65103` feat(PreferenceUI): Enabled Email notification for CR
* `49311164` feat(ProjectUI): Custom Field for Additional data in Project
* `1d3423cb` feat(ReadmeOss): Filter linked project from ReadmeOss based on selection
* `03000401` feat(ProjectUI): Unified view of Linked Project and Releases, Clearing status, Linked Release Hierarchy. View includes tree view and list view
* `89714248` feat: Support custom fields in additionalData group in Component and Release page
* `04b57fd1` feat(ChangeLogs): Revision history of Document

### Corrections

* `fb09ccee` fix(MigrationScript): Update releases with empty clearingState to default value - NEW_CLEARING
* `7bbd4235` fix(ClearingStatus): Tree View Release name getting truncated, Sort for Project mainline state and Clearing State in List View fixed , added search filter
* `4c7b0e72` fix(ClearingStatusView): Fixed uneven button height, console error related to createVulnerabilityTable, takes lot of time to load Clearing status list view for large dataset.
* `d1c3731f` fix(ChangelogBasicInfoUI): Applied generic style and reordered the metadatas for Basic Info.
* `78bff1ba` fix(UI): Clearing Request and Obligations fixes
* `79f5c9d4` fix(mergeComponent): Prevent multiple releases with same name and version for a component, which may occur during merge component
* `a4b44107` fix: Resolve conflict
* `cd4cba10` fix(Changelog): Fixed missing fields like componentType in Changelog history
* `3ee65c9b` fix(REST): Added default value(NEW_CLEARING) for Clearing status while create and update Release
* `67875856` fix(spreadsheet-export): Project spreadsheet export returns blank spreadsheet
* `b91b9e2d` fix(ui): Release overview from component details
* `cd29922a` fix(UI): Clearing Request bug fix and improvements

### Infrastructure

* `79850290` chore(deps): Bump spring-security-core in /frontend/sw360-portlet
* `e1aabab1` chore(deps-dev): Bump dom4j in /backend/src/src-licenseinfo

## sw360-8.2.0-M2

Although there are only few commits listed below for this release, the change from 8.1 to 8.2 is huge: sw360 supports now an UI which can be extended with different languages.

With the initial pull request, the English and Vietnamese languages is supported. More languages can be supported. For this, a translation file must be added. Please see `README_LANG.md` in the root level of the project directory for more details.

Many thanks to the colleagues at Toshiba for providing this big feature to the community.

### Features

* `8bd91be` feat: SW360 support multi-language update after review                                                  
* `994ad5c` feat: SW360 support multi-language                                                  

### Corrections

* `ae45236` fix(mergeUI): Provided fix for error message on merge component, release, vendor.

## sw360-8.1.0-M1

A version upgrade is justified, because of a number of new features have been integrated: FOSSology scans can be now triggered over the SW360 REST API. By this feature, an upload, for example from sw360antenna, could also trigger the FOSSology scan right away. It requires FOSSology being integrated with sw360.

Another new endpoint is the query for SHA1 values of a file to check if that attachment is actually already found at some release. With this endpoint, one would not need to search for release names and version before making a new entry, but just search for the source code attachment using its SHA1 value to check if an upload has been performed already.

A third new feature is the ability to agree on a clearing job for the software components of a project or product. A project owner can now send to a clearing expert a request to perform the clearing of software components right from SW360.

### Features

* `bb9f2ba` feat(REST): Trigger FOSSology process and check status
* `99e23dc` feat(ObligationUI): Added new status fields for Obligation
* `d025c4a` feat(rest): Attachement sha1 improvement
* `9a53e7b` feat(ProjectUI): Project Clearing Report

### Corrections

* `7bd1fd5` fix(UI/REST): Remove Trailing and leading whitespace for all fields in component, release and project
* `a2a4b16` fix(components): components listing limited to 200 entries both in UI and excel spreadseet
* `0de1db1` fix(vulerability): vulnerability view breaks at backslash in description
* `83e6f28` fix(REST): Updated upload attachment documentation

### Chores

* `1fc2e0b` Add pull request tempalte and .github folder (11 days ago) <Stephanie.Neubauer@bosch.io>

## sw360-8.0.1-M1

There is some small but very substantial bug in 8.0.0, which prevents the user from creating records in special conditions. Therefore, version 8.0.0 is deprecated and replaced by version 8.0.1.

### Corrections

* `c20fa46` fix(component/release): Add component and release error in UI

## sw360-8.0.0-M1

It is not really that we like to ignore minor releases, but release 8 is coming because:

* changes in the DB for external id handling, pls see migration script: `scripts/migrations/016_update_byExternalIds_component_view.py`
* changes in the Thrift API, allowing for SPDX BOM import pls see: `libraries/lib-datahandler/src/main/thrift/projects.thrift`

And as a larger, very important feature, there is the SPDX BOM import there in a first version, adding two modes:

* Import a project with linked releases from a SPDX BOM file
* Import a list of components and releases from a SPDX BOM file

Moreover a very important feature or fix has been provided for ensuring that malformed REST requests do not lead to failure in the application. Previously, providing wrong typed references (for example: linking releases to a project) was accepted by the application and can lead to malfunction then. The following list lists the detailed changes since 7.0.1:

### New Features

* `712ba79` feat(rest): validate the linked document ids in the payload before updating it in the DB
* `f90fcc4` feat(bomImport): implement SPDX BOM import for projects and releases
* `24999ce` feat(AddProjectReleaseRelation): add a project release relation for source code snippets
* `48de678` feat(REST): Patch Releases to Project

### Corrections

* `d34d454` fix(ReleaseUI): fixed reload report in FOSSology Process
* `336534a` fix(REST): fixed search component by external id
* `bc28c54` fix(EditReleaseUI): Fixed missing functionality of button to delete release to release relation
* `e437a5b` fix(spreadsheet-export): fixed the secuence of values based on headers
* `4c0d5c9` fix(thrift): add should return ID on duplicate
* `1d65e70` fix(html): fix minor bugs and styling
* `b7a83d6` fix(ui): saving attachment usage issue for source code bundle and others

## sw360-7.0.1-M1

After tagging 7.0.0, we found two bugs to be corrected to provide a sound SW360. Therefore, here a new tagged version of sw360. Everyone should use 7.0.1-M1 instead of 7.0.0.

Adding rolling version since last tag will prepare automated tagging with incrementing patch level, retaining manual tagging for major and minor version only.

### Corrections

* `0dcd109` fix(ProjectUI: fixed blank / non-responsive screen on project
* `da677b5` fix(ui): fix issue #762

### Infrastructure

* `a37e24d` chore(readme): adding some more badges
* `f1a7c63`feat(chore): adding rolling versions based on commit count

## sw360-7.0.0-M1

The main reason for release version 7 is to have the license upgrade from EPL-1.0 to EPL-2.0. All contributing parties have submitted their consent by e-mail and on most cases also approved the referring pull request (https://github.com/eclipse/sw360/pull/756).

Another change which justifies a major version jump is the required view update in the couchdb. Please see https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md for more information when migrating from an older version. The view update allows users to configure the `My Projects` portlet.

### New Features

* `9b92795` feat(docs): relicensing from EPL-1.0 to EPL-2.0
* `66a4126` feat(Component/ReleaseUI): Added button to remove selected vendor for component and release
* `860aa3e` feat(ProjectMigration): script to migrate a project field to new value
* `bd99641` feat(REST): Add parameter to GET release by name
* `322c45d` feat(WelcomePageUI): display configurable content for guidelines on welcome page
* `abac231` feat(fossology-pull-report): Added the button to pull the already generated report from fossology
* `062c899` feat(HomePageUI): Listing of MyProjects is made configurable
* `9849cb0` feat(licenseinfo): Added filter to exclude releases based on selected relationship

### Corrections

* `2a52475` fix(ProjectUI): Show proper error msg ,when loading of project fails due to access or dependency not found
* `752bd78` fix(ProjectUI): fixed 'Project is temporarily unavailable' issue due to obligation feature
* `b32afd5` fix(ReleaseUI/REST): prevent cyclic link in release
* `0d2647d` fix(licenseinfo): White page while downloading license disclosure

### Infrastructure

* `d22aaaf` test: add script to start temporary couchdb with docker
* `df54014` chore(cleanup): drop unused and outdated code related to the codescoop integration

## sw360-6.0.0-M1

This release covers as the biggest change the new integration with the FOSSology REST API. It replaces the previous integration using an ssh login. It requires a migration of the couchdb database. More information about the scripts can be found in `scripts/migrations/README.md`.

Apart from changing the integration with FOSSology from ssh to the REST API, the entire data structure has been changed to be tool agnostic: A data structure for external tool requests replaces the info for the FOSSology upload. In future, albeit not supported today by the UI, also other tools could be integrated using the same data structure.

*Warning* Although the was much care for migrating existing data. It may happen with old datasets where source code attachments have been transferred to FOSSology using the ssh integration, the migration fails. For those datasets, the data must be changed manually. For example, just remove the status values.

*Warning* Migrations run per default in dry run mode, meaning that no changes are written to the database. After you have reviewed the changes (and checked that the scripts runs), you must change the `DRY_RUN` variable accordingly to `False`.

Two notable more features are provided by this release:

* Management for project obligations
* Merging release and vendor records added

### New Features

* `653a7e3` feat(ProjectUI): added project obligation logic on change of accepted license file
* `648755a` feat(REST): Added parameter to GET project by Group and Tag
* `8eae7d3` feat(rest): get attachmentUsages for a project
* `b8549de` feat(REST): linked release hierarchy is included in the response
* `1bc03f9` feat(Project-UI) License Obligation tracker at Project Level
* `1f506f2` feat(Rest): New end points for project/component/release usage summary
* `176557a` feat(moderation): Moderation requests to all clearing admin irrespective of their group
* `82977a0` feature(merge): add wizard for merging vendors
* `e476f39` feat(rest): Added support to add role category fields while creating project
* `86afeef` feat(Projects): enabled Project/Release mainline state change only for clearing admins
* `578f53c` feat(fossology-rest): replaced ssh communication to fossology with REST
* `d19f658` feat(external-tool-request): added general datastructure for external tool requests
* `71535e6` feat(Authorization): Added support to read keystore from central configuration
* `43bd667` feature(release): add release merge wizard

### Corrections

* `ca88b44` fix(ProjectUI): Added options to generate ReadmeOSS for main project only or main project with subprojects.
* `51bc423` fix(rest): Error getting component/project with unknown creator
* `7814e7e` fix(ProjectUI): Obligation view for changes in linked release attachment
* `255f54e` fix(ui): Added missing tooltip
* `00c3110` fix(businessrules): NPE in clearingStateSummaryComputer
* `6bb0cc2` fix(project): Keep release mainline state as it is while cloning project
* `7b488d5` fix(projectUI): NPE in SW360Utils.getApprovedClxAttachmentForRelease
* `7181861` fix(LicenseInfo): NPE in ProjectPortlet.prepareLicenseInfo and downloadLicenseInfo
* `7df48da` fix(rest): License information generation based on attachment usages from rest.
* `466185e` fix(project): prevent cyclic link in linked projects
* `dcc4192` fix(projectUI): NPE in ProjectDatabaseHandler.setReleaseRelations
* `6f02ae7` fix(component): incorrect release edit link in component edit page
* `20211c9` fix(component): component merge not working
* `e1921d7` Fix(Project UI): Removed 'Unknown' from Project Clearing Team dropdown
* `16c3452` fix(REST): added support for createdComment field for uploadAttachements
* `2e0d776` fix(Project/Admin): Set to default text feature is not working correctly for Obligation
* `aa71a06` fix(Componnet): ComponentType field should be mandatory
* `c7a0737` fix(links): Fixed the incorrect links

## sw360-5.1.0-M1

This release contains a number of corrections after the Liferay Portal 7.2 CE GA1 based release has been rolled out. Therefore it contains mostly corrections for the UI. In addition to these, also the REST API endpoints were further improved. The report generation has been improved: Now, external Ids can be added to the generated documents.

Because it contains many corrections, every 5.0.0-M1 installation should be updated to this release.

### New Features

* `c86c97b`	feat(License Disclosure): Change order of listed items in disclosure documents
* `82a45cf`	feat(license-disclosure): External Ids incorporated in the license disclosure
* `5b554ae`	feature(table-filter): add filter box, fix print

### Corrections

* `9b02a75`	fix(components): Recompute aggrated fields on save
* `17d90ee`	fix(DownloadLicenseInfo): Corrected license selection based on attachment selection on attachmentusage
* `d6d8540`	fix(EditRelease UI): Removed duplicate field 'Licenses' from edit release
* `b9be0e4`	fix(licenseDisclosure): Added acknowledgements in TEXT and Docx format of License Disclosure
* `b123c48`	fix(LicenseDisclosureDocument): Ordering and formating license disclosure document.
* `97008f3`	fix(merge): allow merging of complex fields, style improvements
* `cd4c788`	fix(merge): fix update conflict on component merge
* `c6b3838`	fix(merge): Some fields were not merged
* `1e6f424`	fix(Release-UI): Vertical scrollbar for link release to project popup
* `20fb3d2`	fix(ui): Added missing search box
* `dcd681b`	fix(vendor): fix view name used when editing vendors
* `abc6404`	fix(vulnerability): Vulnerability tab loading issue
* `dc0b9d6`	fix(fossology): fossology and fossolgy
* `4fe4d4f`	fix(Rest-API): Corrected 'createdBy' field value for Project and Component
* `eb15c85`	fix(Rest-API): Small fix around ProjectClearingState during create and update project
* `fae1c99`	fix(Rest-Component): Corrected all components by type rest end point
* `f7d204e`	fix(REST: Project) : Fixed error response for create project from rest
* `a2750bf`	fix(rest): Fixed get component API having default vendor id as empty

### Infrastructure

* `d9ff676`	chore(pom): change snapshot version from 6.0.0-SNAPSHOT to 5.1.0-SNAPSHOT
* `e59f8b3`	chores(config): Fix friendly URL for license page
* `81600f4`	chores(merge): Retain owner as moderator
* `a80b82c`	chores(pom): Update to next development version

## sw360-5.0.0-M1

This release is the first release using the Liferay Portal 7.2 CE GA1 release. The codebase of the portal project has been updated from the previously used Liferay 6.2 version. As this represents a huge change also to related areas (pom files, etc.), the sw360 5.0.0 is bascially a sw360 4.0.1 with the newer Liferay. The following commits have been applied:

### New Features

* `35165e6` feat(auth): script to add the unsafe default client directly to DB
* `4fd501c` feat(thrift): add timeout for thrift client

### Test, Documentation and Infrastructure

* `3c4d3ed` chores(all): Upgrade to Liferay 7.2 (Part I)
* `6657e79` chores(configuration): Update Liferay configuration
* `7fbd42e` chores(all): Upgrade to Liferay 7.2 (Part II)
* `52592bf` chores(build): add build plugin
* `7d9e30e` chores(deploy): add new deploy profile
* `1d5bff2` chores(liferay): Feedback from Liferay 7 review
* `36ae2c1` chores(build): Fix deploy profile
* `918d054` chores(configuration): allow external files
* `` chores(changelog): initial commit

### Corrections

* `deb868c` fix(tests): Use configured couch db url
* `da1f0b8` fix(search): make search logic consistent
* `1d830ee` fix(project): fix compare if no version is set
* `0c2a341` fix(Components): Fix naming component error (name's component contain...
* `c7f03c8` fix(rest): fix broken logic in updateProject  
* `be90070` fix(rest): auth server is broken due to LifeRay api change

## sw360-4.0.1-M1

This release fixes a small issue at the project creation. It was added to have a good working sw360 4 release.

### Corrections

* `c7f03c8` fix(rest): fix broken logic in updateProject

## sw360-4.0.0-M1

Proudly announcing a new milestone release for SW360. We have many additions since the last release 3.3.0 in November 2018. The main reason why it turned into the 4.0.0, a major version change, was the change on the database model. According to our versioning guidelines, we have major release jumps when the DB changes. Please see below for details about how to deal with the change.

The release has the following new features:

* A first release of the project / product approval report, listing and maintaining obligations resulting from OSS use.
* Support for integration for single-sign-on and identity management server keycloak
* A lot of enhancements (new endpoints) to the REST API
* More management for using attachments (license information, source code)
* Starting to parse and show SPDX information in the Web UI
* Integration of Codescoop`s OSS library OSMAN.

### Comitters

There are many different contributors which lead to new releases a release, for example doing presentations and promoting sw360. If you count the committers who have commited since 3.3.0-M1 and this release, the credits for code go to the following persons:

```
akapti
alexbrdn
aratib
blaumeiser-at-bosch
bs-jokri
bs-matil
dreh23
greimela-si
hemarkus
henrik
imaykay
kallesoranko
lepokle
maierthomas
maxhbr
mcjaeger
nutanv1contr
smrutis1
sweetca
```

### Changes

#### New Features

Larger areas of improvements include the work on the REST API (see individual changes below) and on the reporting for projects, which includes now a project clearing report.

* `4b12200` feat(attachment-usage): Restrict users to change the attachment usage without any WRITE access
* `68f28f7` feat(attachment-usages): Take over the attachment usages from the original project, while cloning one.
* `29ba68d` feat(client-management): added support to dynamically manage oauth clients
* `4722f04` feat(codescoop): osman integration
* `923d236` feat(default vendor): added possibility to save a default vendor for components
* `e21d358` feat(duplicates): added support to prevent duplicate projects/components/releases
* `ca45db7` feat(homepage): show accepted releases in MyProjects portlet
* `093bc8a` feat(licenseinfo): Add version string to file name
* `fe58767` feat(licenseinfo): Use property for controlling license info generation
* `111e99d` feat(licenseInfoMigration): added manual migration script for license infos
* `28d252e` feat(licenses): add support for project-only obligations
* `ed7e9f9` feat(project): Reporting Improvements
* `4f2166a` feat(project): Reporting Improvements
* `33397be` feat(Projects): Added new field "domain" to project summary
* `65fa6d5` feat(Projects): display `uploadedBy` & `Relation` in Attachment Usages
* `1944686` feat(releaselink): add release to project from release view
* `477019b` feat(report): add common rules table
* `b46cb4d` feat(report): add common rules table
* `67975c2` feat(report): fill development detail and additional requirements table
* `2acd46d` feat(REST-Doc): Updated the REST API Documentation
* `2cdaa1c` feat(Rest-Project) : Added possibility to update project from rest endpoint.
* `2faffb9` feat(rest): add keycloak support for sw360 rest api
* `834e676` feat(rest): Added CORS module to fix CORS problems with JS clients
* `a666bc3` feat(rest): Added CORS module: fixes after review
* `d9f6164` feat(rest): Added missing fields to REST API json
* `5438233` feat(rest): Allow to search only by externalId-Key (without specific value)
* `b35b265` feat(rest): make screenName auth case insensitive
* `e270a28` feat(rest): REST Authentification with ScreenName
* `390fb16` feat(rest): Search by externalIds endpoints for releases and components
* `381469f` feat(rest): Updated response for GET requests on resource lists if there are no resources available
* `6821256` feat(rest): Whitelist fields in REST API response
* `2c68620` feat(rest): Whitelisting Fields in the REST API Response
* `eb0c44d` feat(search-dialog): improved multi item search dialog
* `1dc69ad` feat(spdx-import): added functionality to view and use spdx information
* `2b788b7` feat(spreadsheet): Added component categories field on spreadsheet export of Project with linked releases
* `45ba41e` feat(sso-oauth): added possibility to get oauth access tokens when pre authenticated
* `94971ec` feat(subproject licenses): added possibility to take over license selection from subproject
* `8ca3200` feat(tabview): added better navigation support for tabview
* `3bb68c9` feat(thrift): add http proxy for thrift clients
* `26401da` feat(thrift): add new `additionalData` Field for generic data storage
* `ee7b374` feat(todoMigration): migration script for todos
* `a903ba4` feat(UI-attachment): Create attachment bundle zip container, even for only one attachment
* `9a59372` feat(UI-Project): Jump to edit release from ProjectDetails
* `19bd0fa` feat(UI:PageTitle): Show selected Project/Component Name in Browser Tab
* `2f7474f` feat(ui): Send to fossology error message.

#### Corrections

* `ba57b76` fix: Security changes in source code
* `aa9ccf3` fix(attachment): Multiple attachment upload stall issue
* `94fedc4` fix(Attachments-UI):Restrict user from adding attachements with same file name
* `31deb6f` fix(chores): updated documentation including licenses file
* `862915f` fix(component edit): fixed an issue where external id and attachment changes were not saved
* `d10022c` fix(cve-search): disable tests by assume statement and refactor
* `8908b66` fix(license-import): add missing dependency
* `0cf598a` fix(license-todo): Adding TODOs to License
* `8fee825` fix(licenseinfo): Exclude old commons-lang3 dependencies
* `8ac21e0` fix(licenseinfo): NPE at Generate License Report
* `be69470` fix(Project UI): Fixed "Set To Default Text" feature for project license info header
* `144a8ac` fix(Project): Only users with Admin access should be allowed to edit a closed project
* `be38717` fix(ReleaseLink): Remove self link from LinkedReleases hierarchy
* `8015cc8` fix(report): adding coverage if content exceeds the max number of characters in cell
* `2fc4bd3` fix(report): corrections to report
* `0525fde` fix(report): fix indentation and message text
* `fb70f43` fix(report): Fix merge error, fix rest payload
* `c8d15ac` fix(REST API): Attachmentupload endpoint documentation
* `b3615b3` fix(rest): do not answer with 404 if resource list is empty
* `12931ff` fix(rest): Download licenseinfo file error
* `026cb34` fix(rest): Hiding unwanted fields in project listing response in REST
* `4a1f90c` fix(scripts): add missing dependency to scripts/install-thrift.sh
* `06d113d` fix(sso-oauth): feedback from review
* `00368cf` fix(treetables): fix inconsistent indentation in treetables
* `8ddce65` fix(UI-Release): UI error on duplicate release creation
* `7db8c86` fix(ui): After removing a task from Home page, the task is back in the list when navigating back
* `01453cb` fix(ui): datepicker date and year selection is made available
* `8cad8ea` fix(UI): Deleting submitted task under My Task Submission section.
* `4f07ca5` fix(ui): Fix infinity loop by expanding empty projects in AttachmentUsage
* `c07932b` fix(user export): fix Nullpointer Exception on user export
* `166b03d` fix(user): migrate completly from getOpenId -> getScreenname
* `20ea660` fix(users): write screenname into externalID field
* `6acf644` fix(vendors): Remove vendorId and vendor of release in case of deletion
* `6453b69` fix(vul-scheduler): fixed an issue where vulnerabilities were stored in the wrong db
* `c94e999` fix(wsimport): remove projects from components that are created
* `c54ef0e` fix(wsimport): small fixes and some refactoring for wsimport
* `e3c47ba` fix(wsimport):download url for releases
* `3cca3b8` fix(documentation): Fixed link to issue tracker in eclipse org
* `71c6f6f` fix(rest): Fix self link for user resource
* `c2b5f90` fix(licenses): added log message and handle GPL-2.0+ case when converting licenses
* `bbf55aa` fix(wsimport): removed unnecessary check

#### Test, Documentation and Infrastructure

The most important part on the infrastructure part is the change of the thrift compile to version 0.11.0. This has an impact to all, because an update of the installed thrift compiler is required from the previous version for all machines where the sw360 projects needs to build. Note that also the ektrop lib has been updated as well as the webjars which include the Java script components for the Web UI.

* `7128acd` chore(common): Mail service sends notifications asynchronous
* `bdd45d2` chore(rm): Change Thrift Version in Readme
* `c4228b0` chore(thrift): update thrift version to 0.11.0
* `7089e19` chore(thrift): use install/fast make target
* `5ba0ebf` chore(ui): Auto resize textarea in project view and edit mode
* `517faaa` chore(ui): Display banner warning for IE
* `0864e14` chore(ui): Improve lucene search logic for project version
* `f51c4af` chore(ui): Some fixes for UI regarding search and filters
* `abf5be7` chore(vulnerabilities): Linked releases can be empty or null (rest create project)
* `b6da7ca` chores(developer): remove developer tag in pom.xml
* `d4d522d` chores(quick-deploy): add quick deploy for portlet
* `165f9ca` chore(REST): add documentation for Licenses in Releases
* `53ae7b0` refactor(db-bridge): updated ektorp library version to current 1.5.0
* `0632505` refactor(velocity): update to new version
* `6e8c349` refactor(webjars): update versions of webjars

#### Database Schema Updates

Because of changes in the couchdb schema you likely need to run a migration script. Please find more information here: `sw360/scripts/migrations/`, in summary, you will need to update in the database:

* Changes to the way how the selected licenses and resulting attachment usage information is stored leads to the need to execute `011_migrate_attachment_usages_license_info.py`.
* An identified for a todo was not used, but it was changed to title, so execute `012_migrate_todoid_to_title.py`.

## sw360-3.3.0-M1

### New Features

* `48741ac` feat(rest): Token Generate with API Keys implementation (9 days ago)
* `a20a225` feat(licenseImport): fix and improve
* `26e4c55` feat(rest): Add externalId endpoint (projects) to REST API
* `20e4472` feat(projects): add a flag to enable/disable displaying project's vulnerabilities
* `0a3a636` feat(wsimport): Whitesource import service
* `1386a75` feat(rest): Specify properties dynamically in GET /releases
* `7918a40` feat(rest) Added route DELETE /releases/{ID},{ID2} to delete releases
* `8d36000` feat(rest): Update REST Attachment endpoints and documentation
* `c55c5f7` feat(rest) Paging/Sorting for GET /components, response contains pagination...
* `dd7025a` feat(attachments): enable viewing/editing of attachment usages...
* `dc1be63` feat(rest) Route PATCH /release/{id} added to update an existing release
* `ed79f9a` feature: codescoop integration
* `590841b` feat(rest) Batch-Deletion for components
* `5933bb7` feat(rest) Route PATCH /component/{id} added to update an existing component
* `cc5a5a1` feat(search): Improve lucene search logic and handling of results
* `f9f6604` feat(licenses): allow to create unchecked licenses
* `897acbf` feat(rest) Specify required fields for components in GET /components
* `0ed834a` feat(rest): Read client id and client secret from configuration file
* `7fa5164` feat(ui): Add preferred external id keys for projects, components and releases
* `0eb74f9` feat(ui): Pagination of entries in project and component view
* `5581b19` feat(release): Add project mainline state to export spreadsheet (clearing status)

### Corrections

* `5ddf781` fix(license): fix problem in editing licenses
* `06ea9d0` fix(rest): GET /components response will contain all components...
* `942f263` fix(projects): prevent duplicate attachment usages from crashing project display
* `0f936d7` fix(wsimport): add lar file
* `f145f0b` fix(rest): Correct REST embedded User to prevent error messages
* `c2c0afe` fix(rest): Show attachments as embedded resource list
* `187756b` fix(projects): fix assertion exception when requesting attachment uses with empty...
* `ffd6884` fix(licenseInfo): remove the unnecessary license text input field from license info...
* `90791fc` fix(rest): Trying to sort components by an unsupported property causes NPE
* `74c6512` fix(projects): Enable phrase search for group and tag in projects
* `a4a4244` fix(components): Remove updateOnlyRequested condition
* `47045ad` fix(component): No update for all component properties if they are not in request
* `d7c6fec` fix(licenseinfo): Update merge handling for licenseInfo objects
* `02d1289` fix(search): impose the defined search limit on all searches by default
* `f844a42` fix(attachments): Set content encoding to identity only for gzip files
* `4b7a2f3` fix(attachments): Set content encoding to identity in case of gzip files
* `c19298b` fix(licenses): Validate obligation list in LicenseDatabaseHandler
* `7e75dfb` fix(moderation): Add external id map to moderation request
* `4f7b441` fix(releases): Show release summary if the search text is empty
* `f28df1b` fix(search): Remove special characters in lucene search
* `48ad171` fix(datahandler): Handle duplicate names in source code bundle generation...
* `a149ff9` fix(user): Change user id field to optional
* `1b7aa8a` fix(ui): Fix table styling for components and projects
* `54e5286` fix(users): handle external change of user email address by storing...
* `4bfaea3` fix(home): Truncate long document names in homepage datatables
* `b70bc7f` fix(test): replace "BLACK_HOLE_ADDRESS" with real one
* `e693af5` fix(test): add IOException to the expected ones in BlackHole test
* `a1f8433` fix(attachments): Allows to set attachments for source bundle generation

### General Clean Up and Infrastructure

* `7d41a20` chore(vulnerabilities): Improve handling of null values in lastUpdate
* `d553979` chore(licenseinfo): Add separate DOCX template for reports
* `56834d7` chore(rest): Change docs reference in HAL Browser
* `7ffab39` chore: move attachments db classes to common
* `c638bb8` chore(rakefile): drop Rakefile, since it is unused and partially does no longer work
* `3da002e` chore(moderation): Adjust footer length with datatable width (columns)
* `ba44539` chore(projects): Remove default value for clearingTeam in projects
* `e480824` chore(datahandler): Fix typo Repostitory to Repository
* `bc4128a` chore(components): Autoset ECC options should check if component is OSS
* `59cf17a` chore(projects): Change the file name of export spreadsheet
* `756d7b9` chore(rest): Add delivery start date to REST API guide
* `37b61cd` chore(docs): removing orphan architecture document to wiki
* `ae16c73` chore(config): Restore sw360.properties configuration file
* `1c156ce` chore(travis): Add travis configuration file to project
* `848c233` chore(config): Change sw360portal specific links because of new repository
* `69a4fd7` chore(git): restore .gitignore that's gone missing during move to eclipse repository

## License

This program and the accompanying materials are made
available under the terms of the Eclipse Public License 2.0
which is available at https://www.eclipse.org/legal/epl-2.0/

SPDX-License-Identifier: EPL-2.0
