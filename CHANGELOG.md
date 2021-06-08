# Changelog

This is the changelog file of the sw360 project. It starts with the first release being provided at eclipse/sw360. For older releases, please refer to the first project location:

https://github.com/sw360/sw360portal/releases

## sw360-13.4.0-M1

This tag covers many corrections and bug fixes after the 13.3 release. Th eproductive use of 13.3 has revealed a number of issues resulting from the big persistence layer switch.

This release provides also features, however, some smaller news are there, for example, new REST endpoints or new functions in the UI.

### Migrations

For this version, no database migration is necessary.

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
abdul.kapti@siemens-healthineers.com
jaideep.palit@siemens.com
kumar.ravindra@siemens.com
michael.c.jaeger@siemens.com
nam1.nguyenphuong@toshiba.co.jp
smruti.sahoo@siemens.com
yosuke.yamada.no@hitachi.com
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features

* `3089008c` feat(rest): Support map of release id to usage as request body in addition to previous array of release id for
* `df2f6dad` feat(VirusScanSchedulerService): Scheduler Service for deletion of attachment from local FS
* `276650a9` feat(ObligationHelpTextforProject): Provide the different obligation help text from the Projects Screen
* `ec37c480` feat(moderations): Pagination in requests tab for moderations
* `0d739556` feat(obligationlevelhelptext):Provide info text for different obligation Level
* `83282112` feat(ProjectUI): feature to add License Info to linked releases from License Clearing tab
* `afdac6f5` feat(ProjectVersion): Added the project version in the search Project filter
* `4b1a1b3f` feat(ProjectUI): Fixed copy of projects removes linked subprojects
* `d44b63ba` feat(ProjectGroupFilter):Filter the projects in Advanced Search based on Projects Group
* `4140a8ad` feat(rest): Added new endpoints to update attachment info of Project, Component, Release
* `96443359` feat(rest): Added rest endpoint to update project-release-relationship information of linked releases in a project
* `756190b4` feat(ProjectUI): feature to display the source files linked with the licenses

### Corrections

* `ef27ad5d` fix(rest): Auto-set release clearing state
* `debfe70d` Fix: Rest interface can not handle licenses which do not exist in the database #534
* `2d56d0b4` fix: Wrong error handling when deleting multiple components #851 nam1.nguyenphuong@toshiba.co.jp
* `9a31049d` fix(script): Build failure of sw360dev.Dockerfile and compileWithDocker.sh
* `9f32b882` fix(readmeossdownload): Null pointer while downloading readme_oss
* `f0aa5cbf` fix(ui/rest): Issue fetching releases by external ids and null value in external id breaks the release view
* `baaa9f42` fix(search): search releases while linking to project
* `00083ea8` fix(backend): Issues with boolean and timestamp field deserialization and get attachment info REST

## sw360-13.3.0-M1

This tag is applied to have the migration from cloudant to ektorp in one single step. Ektorp is a Java library which provides an object oriented interface to the (REST-based) access to couchdb. It has been used in sw360 from day 1. Now we concluded to replace ektorp: it does not support paging; having our server growing larger and lager and serving more and more users, receiving results sets from a couchdb view without paging is a pain. And it did not look like it will be supported, because the ektorp project looks calm now (last commit to master in 2017). Among the available options for replacing ektorp, we choose the java-client from the open source project cloudant (version 2.19.1, see https://github.com/cloudant/java-cloudant). It supports paging and offers potentially other interesting features (caching, compatibility with MongoDB, etc.).

### Migrations

For this version, no database migration is necessary.

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
smruti.sahoo@siemens.com
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Infrastructure

* `0e22d55e` feat(components-pagination): paginated view response for components
* `fd95a2cf` feat(cloudant): Migrating from ektorp to cloudant java client

## sw360-13.2.0-M1

The reason for this tag is to have the last release before the ektorp framework to the new cloudant framework for access to the couchdb. This upcoming change will touch a large number of places in the code and thus a last release before this larger change will be merged.

As per notable feature there is the new UI in the admin area to issue the OAuth client credentials for the OAuth legacy workflow for the REST API. Another feature is the storing of all attachments (at upload) also to a configurable location in the file system. This helps anti virus software to scan these instead of requesting them from the couchdb. Note that files are stored at the configured path with `user_mail/document_id` folder structure to quickly track down origin of viruses and malware.

This milestone tag also chovers changes to the build infrastructure on the eclipse servers to prepare future releases.

### Migrations

For this version, no database migration is necessary.

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
abdul.kapti@siemens-healthineers.com
jaideep.palit@siemens.com
kumar.ravindra@siemens.com
kouki1.hama@toshiba.co.jp
lars.geyer-blaumeiser@bosch.io
michael.c.jaeger@siemens.com
smruti.sahoo@siemens.com
```

### Features Summary

* `d8021733` feat(AttachmentVirusScan): Store attachment to File System asynchronously, handle saving of large multi-part attachments. Fix test cases
* `5c77743f` feat(AttachmentVirusScan):Store the attachment to local file system for virus scan.
* `d97146a3` feat(REST): Added new Rest API endpoint for reading clearing request
* `50f576a2` feat(OAuthClient): Create, update, delete OAuthClient from UI
* `d4017345` feat(PredefinedTags):Predefined tags per group in the Projects Tag field.
* `0c7fc59a` feat(UI): added button for copying document id to clipboard

### Corrections

* `d19d08d0` fix(rest): Added support for pagination and retrival using multi value `projectRelevance` param
* `3419b4a6` fix(search): Removing support for `_fti` hook based lucene search for couchdb 1.x
* `f783240a` fix(rest): Fix status code when moderation request is created as a part of an API call
* `4f2c2121` fix(moderaion):add CommonUtils.addAll(moderators, dbcomponent.getModerators());
* `8b867c19` fix(build): Fix issue with overwriting of patchlevel variable in pom.xml
* `b9a38744` fix(test): Use test databases in maven test phase
* `c68b4d4a` fix(OrtIntegration): Fix client to perform case insensitive search of component.
* `784fbafc` fix(script): Utility script to recompute clearing state of release
* `ce69b3bd` bug(eclipse): Quickfix for maven flatten pom problem

### Infrastructure, Docs and Refactorings

* `958a8a77` chore(tag) changing back pom.xml shapshot version tag

## sw360-13.1.0-M1

This time: client libraries. This release among other things brings the client libraries taken over from the sw360antenna project and moved them into the sw360 code base. The client libraries enable Java applications to communicate with a sw360 server via REST calls. Other notable contributions include:

* Support for CouchDB 3.x
* Massive speedup of SPDX import by switching to streaming based parsing for license information for large files
* Single container setup (see `Dockerfile` in project root) for super easy deployment of sw360
* More UI improvement on sorting and filtering in list views
* Supporting multiple templates for the project clearing reports
* REST: Manage used attachments for license info generation and better querying of vulnerabilities

### Migrations

For this version, no database migration is necessary.

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
abdul.kapti@siemens-healthineers.com
jaideep.palit@siemens.com
kumar.ravindra@siemens.com
kouki1.hama@toshiba.co.jp
lars.geyer-blaumeiser@bosch.io
michael.c.jaeger@siemens.com
smruti.sahoo@siemens.com
Stephanie.Neubauer@bosch.io
```

### Features Summary

* `0b7818de` feat(MyProjectsUI):Add additional filter to MY PROJECTS homepage based on clearing state
* `9e98dd3f` feat(ChangeLog): Highlight changes between old and new revision of Document
* `eef05a1b` feat(ProjectUI): Sort & Filter for ClearingStatus TreeView table
* `b6cd9df7` feat(Issue Template):Update issue templates for bug and feature
* `fb15708c` feat(ui):Rename and Re-arrange the Tabs under Projects section
* `98aa0859` feat(ProjectUI): Release Filter based on attachment availability
* `29308987` feat(clearingreport): Feature to select template for Project Clearing report
* `fc024b45` feat(ReleaseUi): Add other / detected license in release
* `04139347` feat(ui-rest): Provide option in attachment usage to include/exclude concluded licenses during LicenseInfo Generation
* `1f995bfa` feat(rest): Filter for get project vulnerabilities endpoint
* `1d771d30` feat(rest): Added endpoint to get changel og by document id
* `68ce3cf8` feat(ui): Display Id in summary page of project, component and release
* `5f2a4089` feat(http-support): add http support library for sw360
* `502d9087` feat(sw360Docker): Single container Docker for SW360
* `948924f0` client(test): add failsafe plugin

### Corrections

* `7091c4b6` fix(spdxtools):Use toArray(new Node[0]) for shorter code and better performance
* `8b4ebc00` fix(version): Increase minor version to ensure proper version sequence
* `31909cce` fix(pom): Fix indentation of profile
* `eefcf17f` fix(excelexport): Projects with linked releases excel export error
* `2ed2ad80` fix(LicenseInfo): Optimized loading of license info, source code download, Clearing report page
* `034f291c` fix(mergeComponentRelease): Attachments not linked properly from source component/Release
* `c3830559` fix(spdx): import large spdx rdf files
* `b08d2f44` fix(datahandler): Modified ektorp queryView call to support CouchDB 3
* `d9756e6a` fix(Rest): Create/Update Release with name same as component name

### Infrastructure, Docs and Refactorings

* `306c2080` chore(eclipse): Change Jenkinsfile to run release or commit count builds
* `948c7bac` chore(eclipse): Build on eclipse ci for deployment of java artifacts
* `ba666266` refactor(client): missing license headers
* `867372bd` refactor(http): Change http mockito to same version
* `ba72cb7d` test(client): fix mockito dependency
* `cfa8d512` refactor(pom): move version of purl to parent pom
* `31a239eb` doc(client): add documentation of the sw360 data model
* `cd3ac486` doc(http-support): Add site to http support
* `4670fffe` refactor(client): Remove all antenna mentions
* `b89e04ce` refactor(client): remove antenna http support and switch to sw360
* `834c1c79` refactor(client): Refactor package name
* `7a6f295c` refactor(client): Add dependencies to poms

## sw360-13.0.0-M1

We tagged this release, because there are persons testing the current master and not seeing the migration script on the database required. The migration on the database came in because of changes on the obligations. After the major work on the obligations data model in the previous release, more work on the UI made a migration script necessary (number `042`). Please note that per our versioning convention, the database migration script makes the tag `13.0` not `12.1`.

### Migrations

For existing installations, a data migration is required. Please go to the readme file in `scripts/migrations` to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the `DRYRUN` variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in reverse order of commit appearance):

```
smruti.sahoo@siemens.com
jaideep.palit@siemens.com
abdul.mannankapti@siemens-healthineers.com
michael.c.jaeger@siemens.com
external.Martin.Idel@bosch.io
```

### Features Summary

* `1246c023` feat(SplitComponent): Add tooltip for releases of component with SourceCodeDownloadUrl and BinaryDownloadUrl during split feature
* `2eafe3d5` feat(rest): Accept 'downloadurl' in request body as 'sourceCodeDownloadurl' as an alternative to original value 'sourceCodeDownloadurl' for Create and update Release APIs
* `ccf05247` feat(ChangeLogs): Fixed issue related to null to empty string or collection conversion
* `20be42db` fix(rest): Fixed update Project API issue - unexpected changes in some fields like moderators, contributors, etc
* `cf4bdcfa` feat(UtilityScript): Script for couchdb 2.x to update a field(String) in project document to a new value
* `ea009aed` feat(Obligation): Add License Obligation from License Database based on licenses found in accepted attachments in Release and its LicenseInfo attachmentUsage in Project
* `a6cf31a3` feat(projectEdit): Project creators and moderators can edit few fields in a closed project
* `2496f037` feat(ClearingReport): Added hyperlink to release document in project clearing report, Changed Font Style (Arial) and Font Size (9) for table content
* `bd07d53e` feat(CR-UI): Added Advaced filter for CR & fix # of components count

### Corrections

* `c18b42b9` fix(clearingreport): Error while downloading clearing report
* `3ff60a09` fix(ExportSpreadSheet): Fixed ClassNotFoundException while export Spreadsheet
* `431e1673` fix(ClearingReport): Fixed null pointer issue for replace text in Clearing report
* `3ff60a09` fix(ExportSpreadSheet): Fixed ClassNotFoundException while export Spreadsheet
* `431e1673` fix(ClearingReport): Fixed null pointer issue for replace text in Clearing report

### Infrastructure

* `71348b4f` chore(deps): Upgrade dependencies (LibreOffice et al)

## sw360-12.0.0-M1

This release something special because it brings a lot, really a lot of changes in the database model, more specifically it is a refactoring of the licenses and obligation objects. Following corrections:

* Risks are dropped and migrated to obligations
* Term "todo" is eliminated and we aim at consistently use "obligation"

Then there are two new dimensions of obligations, first obligation level

* Organisation obligations: obligations that apply for all projects of the sw360 instance.
* Project obligations: obligations that apply for a specific project, for example, obligations need to be applied to software which is delivered on a device without display.
* Component obligations: obligations that apply to a release to be more precise, for example IP issues coming when using a particular release.
* License obligations: obligations which come from using software under a license.

Second, the obligations have types:

* Permissions
* Restrictions
* Obligations (finally)
* Risks (for example patent litigation clauses)
* Exceptions (for example classpath exception with GPL)

So that involves a lot of changes to the data model, and resulting a lot of migrations. We apologize in advance for the 18 migrations scripts to execute. But it will be easier to have individual migration scripts for particular changes instead of having a large one. Please refer to scripts/migrations/README.md for further details. Please note that in general, all scripts have a `DRYRUN` variable which is set to `True` by default and needs to be set to `False` to apply actually changes to the database.

Besides, this release has also some other changes, including:

* changing download URL into two attributes: binary download URL and source code download URL
* New REST Endpoint: Search!
* if you ant to write clients using REST: Pagination for some of the major listings!

### Migrations

For existing installations, a data migration is required. Please go to the readme file in `scripts/migrations` to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the `DRYRUN` variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in order of appearance):

```
<abdul.mannankapti@siemens.com>
github dependabot ;-)
<jaideep.palit@siemens.com>
<michael.c.jaeger@siemens.com>
<smruti.sahoo@siemens.com>
<Stephanie.Neubauer@bosch.io>
```

### Features Summary

* `596ed7bb` feat(ProjectListUI): Added clearing state filter in Project List
* `693dc596` feat(rest): New search resource endpoint and get releases for multiple projects
* `a2577cf0` feat(rest/ui): Project vulnerability enpoint update & added new projectrating in UI
* `c1b1e33b` feat(ui-rest): Changes in Release information, change title "Download URL" to "Source Code Download URL", add new data filed "Binary Download URL", added new field in excel sheet
* `99b3f816` feat(ProjectTodo): Remove ProjectTodo and UI changes for Obligation and ProjectTodo
* `7b9b73a7` feat(projecttodo): Migration Scripts
* `cb890218` feat(ProjectTodo): Renamed type to obligationType ,Changed required licenseIds to optional, Added optional ObligationLevel obligationLevel in ObligationStatusInfo
* `04020bef` feat(CR-UI): Enhancement & Bug fixes
* `1d6d2b32` feat(licensemodel): Drop Risk and Risk category and merge it with Obligation
* `3ac3ba23` feat(ProjectObligation): Changes in Project Obligation Data Model, renamed linkedObligations to linkedObligationStatus in ProjectObligation struct, renamed struct ProjectObligation to struct ObligationList
* `c009f2c8` feat(obligation): Rename product obligation to project obligation
* `fcfec496` feat(LicenseDataModel): Merge LiceneObligation with Obligation
* `c5e4e1e6` feat(ui): Allow access to merge/split of component and release based on user role configured in properties (6 weeks ago) <jaideep.palit@siemens.com>
* `af625d7b` feat(ProjectUI): Added 2 new fields in Project Obligation
* `5b837649` feat(Project-UI): Added new field in Advanced Search for Projets
* `ff4a9af4` feat(LicenseInfoObligation): Rename Obligation in LicenseInfo.thrift to ObligationAtProject, added null check in change log for merge release
* `6c13cc93` feat(ObligationDataModel): Changes in Obligation data model, Renamed struct Obligations to struct Obligation in License.thrift, Renamed existing obligationType to obligationLevel, Created new obligationType field which has Permission,Risk,Exception,Restriction as options, Fixed adding obligation in licenses tab
* `067b731f` feat(rest): Adding pagination while listing projects and listing project releases

### Corections

* `a2dd35de` fix(lucene): fix parameter allow leading wildcard to true
* `9ac6e93e` fix(ModerationRequest): Fixed Moderation Request not opening when associated attachment deleted
* `51ab6e0b` fix(ProjectListUI): Fixed sorting of project clearing state in Project List page
* `0d525531` fix(Report): Fixed Clearing report to show project, component, organisation obligation
* `92d00ab1` fix(Obligation): Expand/Collapse all columns including comment using single leftmost toggle button for a row, Remove truncate for Obligation Text, Added expand collapse column feature for comments
* `5a1422e6` fix(obligations):cover null pointer case if file with obligations is missing
* `51860a0f` fix(moderation): Project moderation fix
* `2f9a6879` fix(UserSearch): Fixed search user functionality

### Infrastructure

* `d04911b8` chore(deps-dev): Bump junit in /backend/src/src-attachments
* `4a3e8904` chore(deps-dev): Bump junit in /backend/src/src-licenseinfo
* `4f3c3ea8` chore(deps): Bump junit from 4.12 to 4.13.1
* `ca348628` typo(rest): fix patchComponent in releasecontroller is patchRelease

## sw360-11.0.0-M1

The changes for this release incorporate a larger jump from the previous release, because it changes the sw360 infrastructure to the following versions:

* From Couchdb 1.X to Couchdb 2.X and Couchdb Lucene 2.1
* From Java 8 to Java 11 - tested with the OpenJDK
* From Liferay Community Edition 7.2.1 to 7.3.3
* From thrift 0.11 to 0.13

Accordingly, also the vagrant project has changed: the current latest master of sw360/sw360vagrant builds with Java-11-based versions of sw360 (onwards from commit `0269392` at https://github.com/sw360/sw360vagrant).

Apart from the changes to the infrastructure, a number of nice new features are introduced, including:

* A Japanese language file for SW360
* Multiple values for external ids for the same keys
* A completely new health check service for better monitoring of an sw360 installation
* Improvements on project handling

For corrections and further changes on the infrastructure, please refer to the listed commits below.

### Migrations

For existing installations, a data migration is required. Please go to the readme file in `scripts/migrations` to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package.

### Credits

The following users have contributed to the source code since the last release (in order of appearance):

```
albert.he@sap.com
smruti.sahoo@siemens.com
michael.c.jaeger@siemens.com
kouki1.hama@toshiba.co.jp
oliver.heger@bosch.io
Stephanie.Neubauer@bosch.io
jaideep.palit@siemens.com
kouki1.hama@toshiba.co.jp
nam1.nguyenphuong@toshiba.co.jp
abdul.mannankapti@siemens.com
```

And many thanks to all the other contributions in presentation, issues, discussions!

### Features

* `60f82182` feat(ProjectReleaseRelation): Added new Field comment, createdOn, createdBy in ProjectReleaseRelation
* `c4342f38` feat(ui): Added link to project button from project detail view
* `137b46a7` feat(language): add Japanese properties
* `4d4184d3` feat(ProjectUI):Added Expand/Collapse All and Search in AttachmentUsageTable
* `1b4f2362` feat(project-report): Layout and content update in project report
* `1ee05b59` feat(ComponentUI): Added new filters in Advance search
* `30ce4db1` feat(ExternalIds): Change file permission
* `2442e1f2` feat(ExternalIds): Change file permission and fix typo of special character
* `753d3889` feat(ExternalIds): Add comments to Component, Release and Project rest APIs
* `0049dd3b` feat(ExternalIds): Add byExternalIds views migration scripts, and upgrade to new version 11.0.0-SNAPSHOT
* `bb2f2950` feat(ExternalIds): 1. Handle EscapeXml for external id value, 2. Fix "Upon update of existing project or Component or Release without any change in External IDs"
* `6ec67338` feat(REST): Whitelisting field in REST API response
* `5ee02f75` feat(EditProjectUI): Release table in edit project page should be sorted and omit vendor name
* `c0bf7132` feat(CRView): Clearing Request Comments enhancements

### Corrections

* `73894c08` fix(resource-server): Resolve logback conflict
* `40f4a3aa` fix(ui): Prevent resubmission of form for Project, Component, Vendor, Moderation inorder to prevent loss of data.
* `6b484677` fix(ui): Fixed download license disclosure error upon selection of corrupted attachment
* `b8446dc1` fix(license): Fixed the NullPointerException and addressed code duplication
* `a92d2677` fix(byExternalIdView): Fixed the byExternalIds view not working if the value is number
* `f62a685f` fix(Language_ja): change datatables.lang's URL
* `d65be244` fix(ImportSPDXBOM): Set Default value[Default_Category] to categories field of Component if found null or empty
* `fda56f18` Fix: 'Download license archive' button in Admin>'import & Export' page is not working #906
* `56eb7074` Fix: import spdx information #927 #915 and change quotes
* `538b1aa7` fix(license): Fixed the license loading issue

### Infrastructure

* `c0685187` chore(script): Added support to uninstall the current thrift version
* `14b1a4af` chore(deps): Bump jackson-databind in /backend/src/src-fossology
* `4f7234cc` chore(java): Support for Liferay 7.3.3 GA4
* `d4c6983c` chore(java): Fixed Deployment issues
* `5d484ee1` chore(java): Updated Spring version
* `d247a0ff` chore(java): updating test deps for java 11
* `3a5958b6` chore(java): migrating to openjdk java version 11
* `c5f82e0e` chore(logging): Added a library containing the log4j2 classes
* `aa6d5ae0` chore(logging): Fixed test failures caused by NoClassDefFound errors
* `fe659050` chore(logging): Updated OSGi package imports
* `68b91bcc` chore(logging): More tweaks of logging dependencies
* `e4060da6` chore(logging): Switched logging configuration to log4j2 format
* `ebc8f852` chore(logging): Upgraded from log4j 1 to log4j 2
* `7866a852` chore(logging): Removed unused dependency to logback
* `465fc5fa` chore(couchdb): Support CouchDb Lucene 2.1.0 with CouchDb 2.1.2 and backward compatibility
* `bbabafd7` chore(Portlet): Rename Moderation portlet to Requests
* `f512b867` chore(changelog): fixing formatting
* `a6d07505` chore(release): changing pom file for 10.0.0-SNAPSHOT


## sw360-10.0.0-M1

Again, another data model change, new major version. Please see the script `018_remove_unwanted_field_from_clearing_request.py` in the directory `scripts/migrations` to change the data model accordingly. The script is necessary for existing clearing request records; not executing the script will lead to malfunction of the sw360 application.

The update improves also runtime stability, because the escaping when displaying quotes has been improved: previously, special characters such as quotes have compromised the rendering of the page. Now, the content is rendered in a correct way.

Further improvements include:

* The ability to split releases from a component and assign this release to another component. This is the opposite case of merging components. On one hand it can undo mistaken component merges. On the other hand, user input, creating a release at the wrong component, can be corrected now.
* a new REST endpoint to request all details from a larger list in one REST call.
* a new REST endpoint to delete attachments from the REST API

More features include the ability to search for IDs when linking releases to projects or enhancements to the clearing request structures.

### Features

* `ef6170e1` feat(attachments): Evaluate check status before deletion.
* `ea6d31ad` feat(CRUI): Modifications in Clearing Request table in moderation tab.
* `90dbdb52` feat(attachments): Updated REST documentation.
* `220f991f` feat(attachments): ComponentController can now delete attachments.
* `5f504aef` feat(attachments): ReleaseController now supports deleting attachments.
* `2930cea5` feat(attachments): Added function to prepare deleting attachments.
* `8adb9147` feat(attachments): Implemented ThriftAttachmentServiceProvider.
* `daa3b3fb` feat(attachments): Introduced ThriftServiceProvider interface.
* `5783cc3b` feat(rest): Whitelisting project 'state' and 'phaseOutSince' field.
* `fc0c7e43` feat(ui/search): Search using rel. id and added link to the release in the search result.
* `2ec0e6d9` feat(REST): Added new allDetails Parameter to List Projects and List Releases API to fetch records with all details.
* `c0bf7132` feat(CRView): Clearing Request Comments enhancements.
* `8528ecfe` feat(SplitComponentUI): Move Component data like releases and attachments from Source Component to Target Component.

### Corrections

* `9c01170b` fix(escapeXML): Added missing escapeXML, to prevent js script execution and rendering break due to single or double quotes, Added missing escapeXml to merge-split Component, merge releases, license details view, list-details-edit view of project, component and release.
* `887533ba` fix(ProjectModeration): Fixed isWriteActionAllowedOnProject check for project update, Fixed incorrect value for Visibility in Edit Project view which has existing moderation request.

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
