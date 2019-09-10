## Changelog

This is the changelog file of the sw360 software. It starts with the first release being provided at eclipse/sw360. For older releases, please refer to the first project location:

https://github.com/sw360/sw360portal

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

SPDX Short Identifier: http://spdx.org/licenses/EPL-1.0

SPDX-License-Identifier: EPL-1.0

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html
