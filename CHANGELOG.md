# Changelog

This is the changelog file of the sw360 project. It starts with the first
release being provided at eclipse/sw360. For older releases, please refer to
the first project location:

https://github.com/eclipse-sw360/sw360/releases

## sw360-20.0.0-beta
This is a beta release for the next major version 20.0.0 of SW360. The release
includes numerous features, corrections, and improvements over the previous
release 19.2.0.

This release serves as a preview of the upcoming major version 20.0.0 for
testing and should not be used in production environments.

Highlight of the changes includes:
* Various vulnerabilities and security fixes.
* More endpoints created for the support of new UI project.
* Improvements on KeyCloak sync and user management.

### Credits

The following GitHub users have contributed to the source code since the last
release (in alphabetical order):

```
> Achal Jhawar <35405812+achaljhawar@users.noreply.github.com>
> bibhuti230185 <bibhuti230185@gmail.com>
> Bibhuti Bhusan Dash <bibhuti230185@gmail.com>
> deo002 <oberoidearsh@gmail.com>
> dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
> Farooq Fateh Aftab <farooq-fateh.aftab@siemens.com>
> Gaurav Mishra <mishra.gaurav@siemens.com>
> harshitg927 <121371860+harshitg927@users.noreply.github.com>
> Himanshu A Garode <himanshu2006garode@gmail.com>
> Kaushlendra Pratap <kaushlendra-pratap.singh@siemens.com>
> Keerthi B L <keerthi.bl@siemens.com>
> Mohamed Hanafy <mohamed.hanfy.dev@outlook.com>
> Nikesh kumar <kumar.nikesh@siemens.com>
> nikesh <kumar.nikesh@siemens.com>
> pranayh24 <pranayheda24@gmail.com>
> Rajnish Kumar <22it3036@rgipt.ac.in>
> Rudra Chopra <prabhuchopra@gmail.com>
> Sameed Ahmad <141239852+sameed20@users.noreply.github.com>
> sathwik-y <sathwik.yellapragada@gmail.com>
> suvrat1629 <suvrat1629@gmail.com>
```

Please note that also many other persons usually contribute to the project with
reviews, testing, documentations, conversations or presentations.

### Features
* `080b277bb` feat(importCDX): enhance importer VCS sanitization
* `c87d2c6b2` feat(vuln): pagination on vulnerabilities endpoint
* `d588c924d` feat(project): use DB side pagination
* `46cc985bd` feat(component): use DB side pagination
* `13a9c716a` feat(datahandler): prepare for paginated queries
* `0ba6dd02e` feat(docs): add other response types in docs
* `e4103eb3e` feat(keycloak): set externalId on sync
* `85986c781` feat(Keycloak): Enhance user synchronization with batch processing and retry logic
* `e01a4e9f6` feat(core): introduce quick search functionality for vulnerabilities
* `59f5c49fd` feat(config): add old UI configs
* `44e6f563f` feat(config): move more configs to DB
* `64158b1bd` feat(rest): new Security user role.
* `7be4e0675` feat(Release): Need createdBy field for list of releases under a component
* `b25398586` feat(Release) : Automate_check_for_Source_Code_Download_URL_1650
* `178743477` feat(rest): endpoint to get fossology connection configuration data.
* `310434d5e` feat(obligation): add field comparators
* `a75e59bbb` feat(rest) : QuickFilter for Obligation page
* `bcf5141a7` feat(rest) : Completed code for advance-search for packages
* `3b929a059` feat(Rest): Advance search for packages
* `3ca1d5b6e` feat(rest): add SBOM file validation for SPDX and CycloneDX formats
* `bbb4c6c01` feat(rest): endpoint to get src file list for the licences.
* `24d9d7df8` feat(keycloak): allow thrift loc to be configured
* `1480c0c75` feat(rest): add additional fields to clearing request endpoint.
* `c4b541310` feat(rest): getting license info from release attachment's content id.
* `d1a51acfa` feat(rest): download users endpoint in CSV format
* `b9be6bace` feat(test): add test for invalid /mergeComponent
* `93928eeab` feat(component): validate merge selection
* `2086cf14d` feat(Rest): adding filter search in license clearing get endpoint.
* `d7a6e4d28` feat(ECC): Add field containsCryptography in Release ECC-Backend
* `35aa150eb` feat(rest): fossology attachment configs to API
* `85e406126` feat(rest): added AttachmentCleanUpControllerTest
* `eae223d9a` feat(rest): added search API integration tests
* `f5493594f` feat(rest): added tests for ecc rest endpoints

### Corrections
* `d07f0d922` fix(rest): add documentation for license types usage in admin view.
* `37c9a5951` fix(resource): no config read at init
* `1e63f38dc` fix(test): disable ssl health endpoint not used
* `999eccda1` fix(xss): test for null value for strip
* `c75442858` fix(spring): upgrade to 3.5.3 from 3.3.3
* `82e16b696` fix(rest): add license type usage check and restructure delete API response
* `8fe11c797` fix(rest): add vendor existence validation in getReleases endpoint
* `18ac76e0c` fix(rest): handle missing component ID with 404 response.
* `176a70f56` fix(release): throw appropriate exceptions
* `de970cafd` fix(rest): add endpoint to  merge two releases.
* `34ff1494e` fix(controller): fix access for SECURITY_USER
* `7722ae9b0` fix(component): skip should accept URLs
* `3831b8a06` fix(Rest): Only admin users can delete license types in the admin license tab.
* `a6dec7574` fix(svm): SVMSyncHandler dont return loop
* `547611a75` fix(rest): fix permission check
* `48893d23a` Fix(Rest): Add quick search for license type.
* `f1ec624ad` fix(bug): Fixed pagination at projects table (#3069)
* `6f6eb2021` Add proper self-link with project ID in licenseClearing endpoint (#3135)
* `7a2680b80` fix(rest) : Missing request param for downloadlicenseinfo report
* `5432c35cd` fix(components): read id for ComponentDTO
* `aa2ca47ef` fix(component): ComponentDTO for /splitcomponent
* `a249b7ef1` fix(component): read list of attachments for merge
* `ab5c62292` fix(rest): improve error messages for invalid SBOM file imports
* `4e26b0553` fix(cloudant): upgrade to 0.10.3 to fix gson issue
* `f55dd3b5f` fix(components): allow field createdBy
* `43c5d1de9` fix(deps): add com.sun.mail:jakarta.mail:2.0.1
* `348337a8f` fix(spdx): fix deps for spdx-library v2
* `c8a756b10` fix(sw360UserGroup): add missing CLEARING_EXPERT
* `cdc2b5dcd` fix(Security) : KeyCloak integration #3087
* `f0f6ac7d6` fix(backend): fix FossologyConfig
* `2bfa0ae41` fix(fossology): fetch download timeout from ConfigContainer repository
* `68236f17d` fix(docs): update scripts/utilities/README.md Documentation (#3066)
* `586bdc3bb` fix(project): return updated releases
* `bdf7648f8` fix(docs): fix OpenAPI docs /fossology/saveConfig

### Infrastructure
* `cce5b2cf9` chore(release): 20.0.0 beta release
* `4461e9ee1` chore(deps): bump org.dom4j:dom4j from 2.1.4 to 2.2.0
* `0f9a61592` chore(deps): bump step-security/harden-runner from 2.12.1 to 2.12.2
* `d52f78f2c` chore(deps): bump github/codeql-action from 3.29.1 to 3.29.2
* `2f76f4fc9` chore(deps): bump org.apache.maven.plugins:maven-gpg-plugin
* `3ddcf3e74` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `d1a9ce73a` chore(deps): bump keycloak.version from 26.2.5 to 26.3.0
* `852f097f8` chore(deps): bump tomcat from `d2f9bdc` to `5ea8fbd`
* `49d03be83` chore(deps): bump maven from `d9f3089` to `615bd38`
* `8bd566560` perf(vuln): use views instead of mango query
* `7ca79f030` chore(rest): paginate users endpoint on DB
* `4ee6294b9` docs(controller): responses for /licensetype/usage
* `e72f8207a` chore(deps): bump maven from `3a4ab32` to `d9f3089`
* `1724114c4` chore(deps): bump github/codeql-action from 3.29.0 to 3.29.1
* `94d5ee4fd` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `dd8fe8dec` chore(deps): bump org.springframework.security:spring-security-oauth2-authorization-server
* `b7fc0e8a5` chore(deps): bump log4j2.version from 2.24.3 to 2.25.0
* `3d4f3d68b` chore(deps-dev): bump net.bytebuddy:byte-buddy from 1.17.5 to 1.17.6
* `384f0c4d7` chore(deps): bump spring-security.version from 6.5.0 to 6.5.1
* `df8addc43` chore(deps): bump docker/setup-buildx-action from 3.10.0 to 3.11.1
* `ea9e7ab95` chore(deps): bump tomcat from `f55695f` to `d2f9bdc`
* `64ef2aacf` chore(deps): bump org.wiremock:wiremock from 3.13.0 to 3.13.1
* `dccbe71fd` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `930c7b33d` chore(deps): bump springdoc-openapi-stater-common.version
* `ee35897ba` chore(deps): bump jackson.version from 2.19.0 to 2.19.1
* `76464f7ba` chore(deps): bump github/codeql-action from 3.28.19 to 3.29.0
* `388c0b024` chore(deps): bump step-security/harden-runner from 2.12.0 to 2.12.1
* `d4814d4dd` chore(deps): bump org.springframework:spring-web from 6.2.7 to 6.2.8
* `72787f9c2` chore(deps): bump org.codehaus.mojo:build-helper-maven-plugin
* `849b1bbca` chore(deps): bump com.ibm.cloud:cloudant from 0.10.3 to 0.10.4
* `0bd9c6bd3` chore(deps): bump github/codeql-action from 3.28.18 to 3.28.19
* `98be9010b` chore(deps): bump maven from `933900d` to `3a4ab32`
* `5bb6a9c5c` chore(deps): bump tomcat from `8058582` to `f55695f`
* `09ababc26` chore(deps): bump ossf/scorecard-action from 2.4.1 to 2.4.2
* `d6f94b768` chore(deps): bump docker/build-push-action from 6.17.0 to 6.18.0
* `39d02c7ac` chore(deps): bump keycloak.version from 26.2.4 to 26.2.5
* `2f537d19a` chore(deps): bump io.github.git-commit-id:git-commit-id-maven-plugin
* `24f1e19f8` chore(deps): bump org.mockito:mockito-core from 5.17.0 to 5.18.0
* `6c06523db` chore(deps): bump org.apache.httpcomponents.client5:httpclient5
* `c2bfc63c2` chore(deps): bump spring-security.version from 6.4.5 to 6.5.0
* `9b3c09f76` chore(mail): update MR email to include docname
* `5dd802ff4` chore(mail): added more information to the mails
* `57f5de1a2` chore(deps): bump actions/dependency-review-action from 4.7.0 to 4.7.1
* `cfcc346f6` chore(deps): bump docker/build-push-action from 6.16.0 to 6.17.0
* `c7bc2e410` chore(deps): bump github/codeql-action from 3.28.17 to 3.28.18
* `4719c400a` chore(deps): bump tomcat from `7edbb52` to `8058582`
* `e51667a87` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `61e34e9b6` chore(deps): bump org.json:json from 20250107 to 20250517
* `67e95b77a` chore(deps): bump springframework.version from 6.2.6 to 6.2.7
* `c9252e8b1` chore(deps): bump org.springframework.security:spring-security-oauth2-authorization-server
* `ce650279f` chore(deps): bump maven from `2e3824a` to `933900d`
* `facca5607` chore(deps): bump tomcat from `74925aa` to `7edbb52`
* `b670b4958` chore(deps): bump actions/dependency-review-action from 4.6.0 to 4.7.0
* `aaf3821a4` chore(deps): bump org.apache.commons:commons-text from 1.13.0 to 1.13.1
* `20596228d` chore(deps): bump org.apache.commons:commons-csv from 1.13.0 to 1.14.0
* `2ca22b9c0` chore(deps): bump org.apache.httpcomponents.client5:httpclient5
* `7eccc398e` chore(deps): bump keycloak.version from 26.2.2 to 26.2.4
* `5f54415ad` chore(deps): bump springdoc-openapi-stater-common.version
* `5bb711b65` chore(deps): bump com.google.code.gson:gson from 2.12.1 to 2.13.1
* `5fcadd5f0` chore(deps): bump github/codeql-action from 3.28.16 to 3.28.17
* `2e88f3a74` chore(deps): bump commons-io:commons-io from 2.18.0 to 2.19.0
* `bfe0f1a85` chore(deps): bump org.apache.maven.plugins:maven-failsafe-plugin
* `84dcd3432` chore(deps): bump org.wiremock:wiremock from 3.12.1 to 3.13.0
* `d19d4aa71` chore(deps): bump keycloak.version from 26.2.1 to 26.2.2
* `114126423` chore(deps): update to spdx-tools:2.0.1
* `6cc01fa3e` chore(deps): bump org.spdx:java-spdx-library from 1.1.1 to 2.0.0
* `b67e732d6` chore(deps): bump maven from `887820a` to `2e3824a`
* `6fc499912` chore(deps): bump tomcat from `0c14861` to `74925aa`
* `81c6940e4` chore(deps): bump spring-security.version from 6.4.4 to 6.4.5
* `c0c32f235` chore(deps): bump org.apache.commons:commons-collections4
* `eec18988a` chore(deps): bump jackson.version from 2.18.3 to 2.19.0
* `42ee8c79f` chore(deps): bump keycloak.version from 26.2.0 to 26.2.1
* `0423e184c` chore(deps): bump step-security/harden-runner from 2.11.1 to 2.12.0
* `7d505b27e` chore(deps): bump github/codeql-action from 3.28.15 to 3.28.16
* `9dc05434b` chore(deps): bump docker/build-push-action from 6.15.0 to 6.16.0
* `b70e6e036` chore(authorizationserver): remove unused vars
* `5431f4203` chore(deps): bump springframework.version from 6.2.5 to 6.2.6
* `eb02b9a35` chore(deps): bump org.mockito:mockito-core from 5.16.1 to 5.17.0
* `c2684b619` chore(deps): bump com.google.guava:guava from 33.4.0-jre to 33.4.8-jre
* `2964da147` chore(deps-dev): bump net.bytebuddy:byte-buddy from 1.15.11 to 1.17.5
* `76cdbacdf` chore(deps): bump tomcat from `1374a56` to `0c14861`
* `a69bf2f49` chore(deps): bump maven from `f1e4a85` to `887820a`
* `bc42ed8cb` chore(deps): bump joda-time:joda-time from 2.13.0 to 2.14.0
* `7e84f9f35` chore(deps): bump org.wiremock:wiremock from 3.12.0 to 3.12.1
* `bc395fbe1` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `f85d5db5f` chore(deps): bump keycloak.version from 26.1.4 to 26.2.0
* `f25a3c9b9` chore(deps): bump commons-codec:commons-codec from 1.17.1 to 1.18.0
* `c1cd81235` chore(deps): bump actions/setup-java from 4.7.0 to 4.7.1
* `85654d998` chore(deps): bump github/codeql-action from 3.28.13 to 3.28.15

## sw360-19.2.0
This minor release includes numerous features, corrections, and improvements
across the SW360 project since the 19.1.0 release.

Highlight of the changes includes:
* Various vulnerabilities and security fixes.
* Unified/simplified REST API error response with Exceptions.
* New endpoint to get and update SW360 config (also making it possible to
  update on fly).
* Multitude of REST API endpoint improvements and additions.
* `linux/amd64` and `linux/arm64` multi-arch docker image support.

### Credits

The following GitHub users have contributed to the source code since the last
release (in alphabetical order):

```
> Akshit Joshi <akshit.joshi@siemens-healthineers.com>
> Bibhuti Bhusan dash bibhuti230185 <bibhuti230185@gmail.com>
> dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
> duonglq-tsdv <duong1.lequy@toshiba.co.jp>
> Farooq Fateh Aftab <farooq-fateh.aftab@siemens.com>
> Gaurav Mishra <mishra.gaurav@siemens.com>
> Helio Chissini de Castro <heliocastro@gmail.com>
> hoangnt2 <hoang2.nguyenthai@toshiba.co.jp>
> Keerthi B L <keerthi.bl@siemens.com>
> mishraditi <aditimishra91924@gmail.com>
> Mohamed Hanafy <mohamed.hanfy.dev@outlook.com>
> Nikesh kumar <kumar.nikesh@siemens.com>
> Rudra Chopra <prabhuchopra@gmail.com>
> Sameed <sameed.ahmad@siemens-healthineers.com>
> Shi Qiu <shi1.qiu@toshiba.co.jp>
> Shushant <148479955+Shushant-Priyadarshi@users.noreply.github.com>
> Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
```

Please note that also many other persons usually contribute to the project with
reviews, testing, documentations, conversations or presentations.

### Features
* `2d51a3097` feat(exception): replace deprecated exception
* `f133b896d` feat(Configurations): Add new endpoints that allow to GET/UPDATE SW360 configurations
* `5fa3afec1` feat(version): generate OpenAPI doc version on fly
* `d8f6b01d8` feat(Department): Add new endpoints: - Get/Update department members - Get importing department's log file list and content
* `edec79367` feat(addNewComponentType) : Added new component type COTS-Trusted Supplier
* `e464254be` feat(rest): Added tests for upload and download components
* `d8393a319` feat(rest): Added endpoints to fetch schedule service status
* `a1a01c89d` feat(rest) : Endpoint for export SBOM at project detail page
* `f15fd779a` feat(script): read host, user and pass as args
* `8d5a77ee7` feat(rest): new rest endpoint for edit obligation
* `65db380b9` feat(project): add new values to project state field
* `8c17597a4` feat(exportCDX): update CycloneDX exporter dependency from v1.4 to v1.6
* `a84a42b48` feat(rest): Count of attachments used in different projects.
* `f40e72c3c` feat(rest): create new endpoint for bulk delete function
* `927da5a54` feat(rest) : Search for vendors added.
* `45a53b4e2` feat: Add multiarch for docker image
* `6373bed28` feat(rest) : Comment added to reuse methods for Duplicateobligation functionality
* `e764a5823` feat(rest): endpoint to merge vendor.
* `4bab8d07a` feat(User): Add 2 new endpoints: - Allow Admin user to update user - List all existing department
* `2d0664f2f` feat(rest) : Advanced Search for project page
* `f15ccd798` feat(rest): standardize POST response to include created entity ID
* `c273f1925` feat(rest): create new endpoint to delete ModerationRequests by id.
* `be7606f32` feat(rest): create new enpoint to upload component csv file.
* `068385703` feat(api): complete advance search for components

### Corrections
* `1b92b5135` fix(spdx): add null and empty field checks for SPDX documents
* `2d1ace631` fix(ci): set min version of CMake to 3.5
* `1cb9e8f4e` fix(test): fix test cases for correct exceptions
* `4cba33716` fix(controller): fix further changes after rebase
* `eb73f32c4` fix(Obligations): includes ObligationLevel in get all obligations responses
* `b0d1be0d0` fix(security): remove WebSecurityCustomizer
* `991eb8f0a` fix(xss): ignore essential headers from XSS filter
* `00d3cb129` fix(project): set fields getLicenseObligationData
* `ef153bce9` fix(obligation): fix obligation patch
* `5f6796ee6` fix(rest) : Advancesearch(AdditionalData) for project page with value based search
* `9daf29b74` fix(Project): Resolve issue with embedded type in project release response when length is 0
* `e415d05a4` fix: Set docker main and development image
* `9038d8dd2` fix: Adjust copyrights and licenses properly
* `72dbb8c72` fix(projectService): fix user role check
* `18193631b` fix(rest): Add license information linking for project releases.
* `5336aea47` fix(script): fix addUnsafeDefaultClient.sh script
* `00b552d58` fix(SPDXDocument): Fix bug add SPDX document always return fail
* `d4c0f913c` fix(Token): Fix bug authentication by user token not working
* `5b3535a9b` fix(project): add more null checks for attachments
* `0e9052f23` fix(project): null check at /summaryAdministration
* `840fa9740` fix: Adjust sw360 container build for external thrift
* `e378da720` fix(Admin): fix OAuth Client deserialization and database operations
* `cb52c1ad6` fix(Rest): Create new endpoint to activate the department manually.
* `4adc4a268` fix(rest) : Add licenseInfoHeaderText in summaryAdministration api response
* `cadc213e9` fix(rest) : Moderation update overwrites previous fields
* `d3aeefc6d` fix(Attachment): Make get attachment endpoints of component/release/project consistent - Allow updating project/component/release with attachment data (in a consistent way)
* `48f9159bb` fix(Rest): new endpoint will help to get the package details by projectId.
* `fbea70a91` fix(rest): Added packageIds in project create and update APIs.
* `886ad473c` fix(Rest): Updated the REST endpoint to schedule the upload of release component attachments.
* `975e30f49` fix(importCDX): Add logging for null metadata in sbom.
* `41ea54857` fix(licenseinfo): Corrected the Open Source title in TEXT format to match DOCX format
* `6ba3bf675` fix(rest): Prevent stored XSS
* `5365f10b8` fix(component): add null check for release merge
* `b91d3ad10`  fix(rest): Added code to get obligation releaseView data in project.
* `bbd7a4361` fix(Rest): License overview is not updating in summary page.
* `eeb3c86d4` fix(rest): fix doc for ModerationRequestController
* `663ac8377` fix(rest): Validate comment message while create a moderation request.
* `6dbec3601` fix(rest): adding additional fields to attachmentUsage endpoint.
* `325cf0ef5` fix(deps): Deprecate old commmons-lang library
* `75d3748cc` fix(cloudant): fix structure of elemMatch query
* `aadf18948` fix(report): refactor /reports endpoint
* `20d02c954` fix(doc): fix OpenAPI docs for report controller
* `73726c45e` fix(moderation): fix moderation creation
* `1cd3739bd` fix(rest) : modified attachment info in response to the moderation request rest api
* `1e1c5c1d0` fix(rest): Added code for for updating multiple project attachments.
* `c8b27567f` fix(rest) : Closed Project functionalities not uniform with respect to UI and REST

### Infrastructure
* `57827d8ed` chore(deps): bump org.jacoco:jacoco-maven-plugin from 0.8.12 to 0.8.13
* `9bfa90129` chore(deps): bump com.tngtech.jgiven:jgiven-maven-plugin
* `30d5f61ab` chore(deps): bump org.apache.maven.plugins:maven-surefire-plugin
* `40a22ede4` chore(deps): bump poi.version from 5.4.0 to 5.4.1
* `fad1b859a` chore(deps): bump step-security/harden-runner from 2.11.0 to 2.11.1
* `f73f40dc4` chore(deps): bump actions/dependency-review-action from 4.5.0 to 4.6.0
* `9f208baf0` chore(rest): rework exceptions
* `b14bf4058` chore(deps): bump github/codeql-action from 3.28.12 to 3.28.13
* `5387e3fcd` chore(deps): bump maven from `70591cb` to `f1e4a85`
* `87806a5ae` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `5a3acda61` chore(deps): bump springdoc-openapi-stater-common.version
* `b15710833` chore(deps): bump org.apache.httpcomponents.client5:httpclient5
* `0cded8b31` chore(deps): bump org.ow2.asm.version from 9.7.1 to 9.8
* `d2de95f47` chore(deps): bump httpcore5.version from 5.3.2 to 5.3.4
* `b0e52e4f6` chore(deps): bump org.mockito:mockito-core from 5.15.2 to 5.16.1
* `2a1ea1952` chore(deps-dev): bump com.tngtech.jgiven:jgiven-junit
* `350a8db21` chore(deps): bump com.google.guava:failureaccess from 1.0.2 to 1.0.3
* `b4b475444` chore(deps): bump org.apache.maven.plugins:maven-compiler-plugin
* `197ed98b4` chore(deps): bump springframework.version from 6.2.4 to 6.2.5
* `8c87ab4ed` chore(deps): bump actions/cache from 4.2.2 to 4.2.3
* `403020e2b` chore(deps): bump actions/upload-artifact from 4.6.1 to 4.6.2
* `4809763e4` chore(deps): bump github/codeql-action from 3.28.11 to 3.28.12
* `3ac6ea7df` chore(deps): bump org.springframework.security:spring-security-crypto
* `64a8742a7` doc(sbom): add allowable SBOM export types
* `40c061cdf` chore(controller): fix typo in endpoint name
* `dfe68e180` chore(deps): bump docker/login-action from 3.3.0 to 3.4.0
* `712d613ed` chore(deps): bump org.springframework.security:spring-security-oauth2-authorization-server
* `1b05c7add` chore(deps): bump com.ibm.cloud:cloudant from 0.10.0 to 0.10.2
* `1ac13a85a` chore(deps): bump keycloak.version from 26.1.3 to 26.1.4
* `dff3a99d9` chore(deps): bump springframework.version from 6.2.3 to 6.2.4
* `fc4910ec0` chore(deps): bump org.cyclonedx:cyclonedx-core-java
* `38e0f199a` chore: Add push docker tag capability
* `4e424695b` refactor(rest): enhance logging and error handling in FossologyRestClient
* `79beaf846` chore(deps): bump docker/build-push-action from 6.13.0 to 6.15.0
* `ac0cf9887` chore(deps): bump docker/metadata-action from 5.6.1 to 5.7.0
* `341fad29b` chore(deps): bump docker/setup-buildx-action from 3.9.0 to 3.10.0
* `c442800bd` chore(deps): bump github/codeql-action from 3.28.10 to 3.28.11
* `1b2c6f8f8` chore(deps): bump tomcat from `0530899` to `1374a56`
* `344b6995f` chore(deps): bump slf4j.version from 2.0.16 to 2.0.17
* `9e584c0a6` chore(deps): bump com.google.code.gson:gson from 2.11.0 to 2.12.1
* `a61d51f79` chore(deps): bump org.assertj:assertj-core from 3.27.2 to 3.27.3
* `be2535da9` chore(deps): bump jackson.version from 2.18.2 to 2.18.3
* `d7869d252` refactor: Fix Thrift to 0.20.0 and split from main docker
* `4ee5a62ff` chore(deps): bump org.apache.velocity:velocity-engine-core
* `5666c6846` chore(deps): bump actions/upload-artifact from 4.6.0 to 4.6.1
* `0e5871fad` chore(deps): bump ossf/scorecard-action from 2.4.0 to 2.4.1
* `30c482edd` chore(deps): bump docker/build-push-action from 6.13.0 to 6.15.0
* `6a431c40c` chore(deps): bump actions/cache from 4.2.0 to 4.2.2
* `d2c4d0bdc` chore(deps): bump keycloak.version from 26.1.1 to 26.1.3
* `188e56fc2` chore(deps): bump github/codeql-action from 3.28.9 to 3.28.10
* `b8c0fd5a9` chore(deps): bump springframework.version from 6.2.2 to 6.2.3
* `01e7f3846` chore(deps): bump spring-security.version from 6.4.2 to 6.4.3
* `3a3f9d902` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `562ab7e42` chore(deps): bump tomcat from `46e15fe` to `0530899`
* `7e0291da8` issue#954
* `31bef0cb6` chore(deps): bump net.minidev:json-smart from 2.5.1 to 2.5.2
* `1b9662cf4` chore(deps): bump tomcat from `c147f0e` to `46e15fe`
* `600b67c3a` chore(deps): bump step-security/harden-runner from 2.10.4 to 2.11.0
* `cdc2f4058` chore(deps): bump org.wiremock:wiremock from 3.10.0 to 3.12.0
* `c9dce5aef` chore(deps): bump ubuntu from `80dd3c3` to `7229784`
* `953221903` chore(deps): bump maven from `a330654` to `70591cb`
* `d38bdd326` chore(deps): bump github/codeql-action from 3.28.8 to 3.28.9
* `975b92433` chore(deps): bump docker/setup-buildx-action from 3.8.0 to 3.9.0
* `b5709ce00` chore(deps): bump org.jetbrains:annotations from 26.0.1 to 26.0.2
* `a64b21d45` chore(deps): bump commons-logging:commons-logging from 1.3.4 to 1.3.5
* `d8b75c575` chore(deps): bump keycloak.version from 26.0.7 to 26.1.1
* `de15ed514` chore(deps): bump poi.version from 5.3.0 to 5.4.0
* `9d2150a9a` chore(deps): bump springdoc-openapi-stater-common.version
* `264ad75cd` chore(deps): bump org.json:json from 20240303 to 20250107
* `72b0dc9dd` chore(deps): bump org.apache.httpcomponents.client5:httpclient5
* `cb1cc7478` chore(deps): bump actions/setup-java from 4.6.0 to 4.7.0
* `6ed457616` chore(deps): bump github/codeql-action from 3.28.5 to 3.28.8
* `ed92b9b9b` chore(deps): bump maven from `8472bdb` to `a330654`
* `bf6d206d6` chore(deps): bump tomcat from `846c66e` to `c147f0e`
* `c1fbd1bf8` chore(deps): bump com.ibm.cloud:cloudant from 0.9.3 to 0.10.0
* `9c20b870e` chore(deps): bump springframework.version from 6.2.1 to 6.2.2
* `95bd3db56` chore(deps): bump tomcat from `935ff51` to `846c66e`
* `b2dd95270` chore(deps): bump maven from `b89ede2` to `8472bdb`
* `cc1ff1153` chore(deps): bump docker/build-push-action from 6.12.0 to 6.13.0
* `bbf5c570d` chore(deps): bump github/codeql-action from 3.28.1 to 3.28.5
* `9dd4ff409` chore(deps): bump docker/build-push-action from 6.10.0 to 6.12.0
* `dfd4ab2b0` chore(deps): bump step-security/harden-runner from 2.10.2 to 2.10.4
* `226b3f8a1` chore(deps): bump org.apache.commons:commons-csv from 1.12.0 to 1.13.0
* `3658fb2e6` chore(deps): bump httpcore5.version from 5.3.1 to 5.3.2
* `b259a6e55` chore(deps): bump actions/upload-artifact from 4.5.0 to 4.6.0
* `0d4b2abbc` chore(deps): bump github/codeql-action from 3.28.0 to 3.28.1
* `5f002e04c` chore(clients): remove version from clients pom.xml
* `ea7ca60bb` chore(deps): bump org.mockito:mockito-core from 5.14.2 to 5.15.2
* `f07a61e2e` chore(deps): bump org.assertj:assertj-core from 3.27.0 to 3.27.2
* `230334e90` chore(deps): bump org.json:json from 20240303 to 20241224
* `2e4b511b1` chore(deps): bump org.apache.velocity:velocity-engine-core
* `8f65bccab` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `f4fb51b20` chore(deps): bump org.cyclonedx:cyclonedx-core-java from 9.0.5 to 10.1.0
* `dfc187d9f` chore(deps-dev): bump net.bytebuddy:byte-buddy from 1.15.10 to 1.15.11
* `fc812cc7c` chore(deps): bump github/codeql-action from 3.27.9 to 3.28.0
* `b4e02713d` chore(deps): bump com.google.guava:guava from 33.3.1-jre to 33.4.0-jre
* `c737350a9` chore(deps): bump org.assertj:assertj-core from 3.26.3 to 3.27.0
* `0043dec55` chore(deps): bump com.squareup.okhttp3:okhttp from 4.10.0 to 4.12.0
* `d35afa4d2` test(components): add test for component filter
* `05472fd45` chore(deps): bump org.jboss.logging:jboss-logging
* `c3d456d49` chore(deps): bump com.tngtech.jgiven:jgiven-maven-plugin
* `ecea61e71` chore(deps): bump org.apache.maven.plugins:maven-failsafe-plugin
* `ab17c1f93` chore(deps): bump org.springframework.security:spring-security-oauth2-authorization-server
* `3f5fd9ff7` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `a5d4a5b3f` chore(deps): bump actions/setup-java from 4.5.0 to 4.6.0
* `8e2234e8f` chore: Move dependabot to weekly interval
* `08857345c` chore(deps): bump maven from `85d505f` to `b89ede2`
* `608975d62` chore(deps): bump org.apache.commons:commons-text from 1.12.0 to 1.13.0
* `f59e3b037` chore(deps): bump log4j2.version from 2.24.2 to 2.24.3
* `0e58f511b` chore(deps): bump springframework.version from 6.2.0 to 6.2.1
* `6e09df793` chore(deps): bump spring-security.version from 6.4.1 to 6.4.2
* `5259d8c72` chore(deps): bump docker/setup-buildx-action from 3.7.1 to 3.8.0
* `fcb603df2` chore(deps): bump actions/upload-artifact from 4.4.3 to 4.5.0
* `318da8768` chore(deps): bump org.projectlombok:lombok from 1.18.36 to 1.18.38

## sw360-19.1.0
This minor release includes numerous features, corrections, and improvements
across the SW360 project since the 19.0.0 release.

Highlight of the changes includes:
* Various vulnerabilities and security fixes.
* Multiple new REST API endpoints.
* Improvements on SBOM and CDX import.

### Credits

The following GitHub users have contributed to the source code since the last
release (in alphabetical order):

```
> Afsah Syeda <afsah.syeda@siemens-healthineers.com>
> Akshit Joshi <akshit.joshi@siemens-healthineers.com>
> Arun Azhakesan <arun.azhakesan@siemens-healthineers.com>
> dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
> duonglq-tsdv <duong1.lequy@toshiba.co.jp>
> Gaurav Mishra <mishra.gaurav@siemens.com>
> Helio Chissini de Castro <heliocastro@gmail.com>
> hoangnt2 <hoang2.nguyenthai@toshiba.co.jp>
> Keerthi B L <keerthi.bl@siemens.com>
> nikesh kumar <kumar.nikesh@siemens.com>
> Rudra Chopra <prabhuchopra@gmail.com>
> Sameed <sameed.ahmad@siemens-healthineers.com>
> Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
> StepSecurity Bot <bot@stepsecurity.io>
> tuannn2 <tuan2.nguyennhu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with
reviews, testing, documentations, conversations or presentations.

### Features
* `2133694fa` feat(rest) : Export Project Create Clearing Request
* `36df4a611` feat(spdx): Add API for feature SPDX Document tab
* `719165516` feat(rest): endpoint to get license info header text.
* `c64470ff8` feat(rest): Add documentation for new clearing size parameter.
* `e02307383` feat(rest) : Rest end point for project ECC Export Spreadsheet
* `9cd8646c1` feat(Component): Add new endpoint that allows user to subscribe and unsubscribe to a component
* `a3edc6cee` feat(Release): Add new endpoint for release subscription
* `8d6315f31` feat(FossologyTrigger): stop repetitive entries of attachment.
* `3a48426c9` feat(ImportCDX):Handle redirection of VCS URLs in SBOM
* `be8d94046` feat(rest): Create new api's in schedule tab.
* `f41b8927d` feat(importCDX): Add functionality to configure release creation when importing SBOM to an existing project
* `ddec17e5d` feat(rest): Add size parameter to clearing request.
* `be032e39c` feat(importCDX): enhance CDX importer to sanitize VCS URLs for non-GitHub domains
* `646c4e1bb` feat(Project): Create new endpoint that allow to duplicate project with network
* `68c1fb737` feat(Release): Add new endpoint to check cyclic links between releases
* `9b32525a3` feat(Project): Add new endpoint that allow to compare project network with default network
* `108ba6700` feat(Project): Add new endpoint to fetch linked releases of linked projects
* `067f9135b` feat(Release): Add new endpoint that allow to get linked releases of release
* `466a8c6d7` feat(Project): Create new endpoint that allow to get linked releases in dependency network of a project
* `75e3bc899` feat(rest): Add endpoint to handle updation of clearing requests.
* `7bcedef6a` feat(rest): endpoint to remove orphaned obligations from project.
* `fa17c2fed` feat(rest): delete a vendor by id.
* `453eff793` feat: Add default user/pwd to couchdb connection
* `e81031333` feat: Add default admin user if database is empty
* `f98db4ff4` feat(rest): Add pagination to get clearing requests endpoint and fix 403 forbidden error
* `33012fdc2` feat(REST):fetch releases that are in NEW_CLEARING state and have a SRC/SRS attachment using parameter isNewClearingWithSourceAvailable
* `2621657cd` feat: Add logging to identify releases with corrupted attachments during license generation
* `73d0576c7` feat(rest): endpoint to get list of obligations depending upon obligation level.
* `24b71c5e6` feat: Update README.md with openssf scorecard badge

### Corrections
* `802013389` fix(openapi)!: add health endpoint to openapi
* `b39c71b5b` fix(Cloudant): Fix Cloudant document creation error by setting id and rev to null instead of empty string during Java object conversion
* `da677a677` Revert "fix(importCDX): Resolved unnecessary update of component fields"
* `8f9859955` fix(docs): fix OpenAPI docs
* `8164a1f48` fix(rest): Fixed the reference to wrong db for oauthclients
* `4918ecd85` fix(test): Remove unused invalid entries
* `7c4b647e9` fix(test): Remove unused invalid entries
* `ac410370c` fix: Enable back client library
* `c41cdedfc` fix: Ignore SECURITY.md on license check
* `ffd83c62f` fix(Project): Add missing properties in network response
* `849284e3b` fix(Project): Unset unnecessory data before store network into database
* `87bdf001e` fix(test): enable unauthorized request test
* `519496118` fix(Project): Fix vulnerability: Information exposure through an error message
* `48eb7437e` fix(User): Fix XSS vulnerability due to a user-provided value
* `89e67b7e9` fix(Rest): component attachment deletion while updating externalIds
* `c35e05fbd` fix: Create sw360oauthclients database
* `9cfb2c16d` fix(rest): Enhance the acceptRequest method to see the proposed changes in project/component/release pages.
* `342145702` fix: Restore target for Dockerfile
* `e18227af9` fix: Remove spotless dead code
* `ec6d2bc18` fix: Adjust pinned dependencies on Dockerfile
* `73e682053` fix: Update POI code to modern version
* `a2734ca50` fix(StepSecurity): Apply security best practices

### Infrastructure
* `8a0793ed5` chore(deps): bump org.apache.maven.plugins:maven-gpg-plugin
* `06426f8bb` chore(deps): bump keycloak.version from 26.0.6 to 26.0.7
* `385a8bc74` chore(deps): bump tomcat from `7ebc6c3` to `935ff51`
* `d24a5c32a` chore(deps): bump github/codeql-action from 3.27.6 to 3.27.9
* `e38177ad1` chore(deps-dev): bump com.tngtech.jgiven:jgiven-junit
* `7277d0815` chore(deps): bump org.apache.maven.plugins:maven-javadoc-plugin
* `e424549f5` chore(deps): update wiremock to 3.10.0
* `e35110da8` chore(deps): use updated wiremock
* `c5cbf16f4` chore(deps): bump org.apache.httpcomponents.client5:httpclient5
* `d59b81243` chore(deps): bump actions/cache from 4.1.2 to 4.2.0
* `e15aa510c` chore(deps): bump maven from `9ae8f00` to `85d505f`
* `97c483c04` chore(deps): bump net.minidev:json-smart from 2.4.10 to 2.5.1
* `862a08e73` chore(deps): bump maven from `f401172` to `9ae8f00`
* `e0bec4851` chore(deps): bump commons-io:commons-io from 2.17.0 to 2.18.0
* `668953ad0` chore(deps): bump org.mockito:mockito-core from 2.28.2 to 5.14.2
* `684e0703c` chore(deps): bump maven from `5a44dff` to `f401172`
* `b80aaa302` chore(deps): bump tomcat from `2ade2b0` to `7ebc6c3`
* `39bb1e985` chore(deps): bump ubuntu from `35b7fc7` to `80dd3c3`
* `f24cbc910` chore(deps): bump github/codeql-action from 3.27.5 to 3.27.6
* `0db57d021` chore(deps): bump ubuntu from `278628f` to `35b7fc7`
* `db32f3bb8` chore: Remove cache from java-setup action
* `03dda4438` chore(deps): bump org.codehaus.mojo:versions-maven-plugin
* `2a4c3c3a6` chore(deps): bump org.apache.maven.plugins:maven-assembly-plugin
* `92f05513f` chore(deps): bump org.apache.maven.plugins:maven-resources-plugin
* `1c3aefe32` chore(deps): bump jackson.version from 2.18.1 to 2.18.2
* `6d5b60f67` chore(deps): bump org.springframework.security:spring-security-oauth2-authorization-server
* `360f63268` chore(deps): bump docker/build-push-action from 6.9.0 to 6.10.0
* `75b9565a2` chore(deps): bump org.apache.maven.plugins:maven-dependency-plugin
* `8589b49b9` chore(deps-dev): bump com.github.tomakehurst:wiremock-jre8
* `b4362b73d` chore(deps): bump org.apache.commons:commons-lang3 from 3.12.0 to 3.17.0
* `c0f95baab` chore(deps): Fix Maven warning for deprecation values
* `067a3025e` chore(deps): bump org.apache.commons:commons-csv from 1.10.0 to 1.12.0
* `41da93540` chore(deps): Move versions to supperpom
* `2dfa4afdb` chore(deps): bump org.keycloak:keycloak-core from 26.0.5 to 26.0.6
* `90c1a4724` chore(deps): bump log4j2.version from 2.24.1 to 2.24.2
* `a2beaa41e` chore(deps-dev): bump net.bytebuddy:byte-buddy from 1.10.18 to 1.15.10
* `cca5c12a9` chore(deps-dev): bump org.ow2.asm:asm-commons from 7.1 to 9.7.1
* `ec4e041f6` chore(deps): bump springframework.version from 6.1.14 to 6.2.0
* `bb9225664` chore(deps): bump org.apache.maven.plugins:maven-enforcer-plugin
* `c4b75cf53` chore(deps): bump com.google.guava:guava from 32.0.0-jre to 33.3.1-jre
* `c3c75c7df` chore(deps): bump spring-security.version from 6.3.3 to 6.4.1
* `bca5bc337` chore(deps): bump github/codeql-action from 3.27.4 to 3.27.5
* `df9bf4801` chore(deps): bump actions/dependency-review-action from 4.4.0 to 4.5.0
* `eaf13a8d6` chore(deps): bump docker/metadata-action from 5.5.1 to 5.6.1
* `9bf808d70` chore(deps): bump org.apache.maven.plugins:maven-failsafe-plugin
* `a11f1830f` chore(deps): Update apache.commons-compress
* `3658d3970` chore(deps): bump org.apache.commons:commons-text from 1.10.0 to 1.12.0
* `6cd1da38b` chore(deps): bump com.tngtech.jgiven:jgiven-maven-plugin
* `36398cfbb` Update security.md file
* `ce6aa331c` Create SECURITY.md
* `a2a88dc79` chore(deps): bump step-security/harden-runner from 2.10.1 to 2.10.2
* `12bd1bf81` chore(deps): bump org.projectlombok:lombok from 1.18.34 to 1.18.36
* `4d336c6ad` chore(deps): bump jackson.version from 2.17.1 to 2.18.1
* `cce753580` chore(deps-dev): bump nl.jqno.equalsverifier:equalsverifier
* `6098b6723` chore(deps): bump com.github.package-url:packageurl-java
* `40ec24f69` chore(deps): bump tomcat from `a09d4c1` to `2ade2b0`
* `965ac8dc2` chore(deps): bump ubuntu from `99c3519` to `278628f`
* `49c3e574f` chore(deps): bump maven from `440a97a` to `5a44dff`
* `a91c6249c` chore(deps): bump httpcore5.version from 5.2.5 to 5.3.1
* `f2b202b7a` chore(docs): update the KeyCloak doc for 26.0.5
* `8f9492422` chore(deps): bump keycloak.version from 25.0.6 to 26.0.5
* `6239843ef` chore(deps): Adjust Maven dependency declarations
* `9fa14d2e3` chore: Remove pre-commit checkstyle in favour of maven solution
* `3f7153601` chore: Remove mave source plugin duplcation
* `3608ef514` chore(deps): bump jakarta.servlet:jakarta.servlet-api
* `1f7225b07` chore(deps): bump github/codeql-action from 3.27.3 to 3.27.4
* `952a11afd` chore(deps): bump com.ibm.cloud:cloudant from 0.9.1 to 0.9.3
* `dbf82f199` chore(deps): bump com.jcraft:jsch from 0.1.54 to 0.1.55
* `c972c7fc3` chore(deps): bump github/codeql-action from 3.27.1 to 3.27.3
* `6985820ec` chore: Update oudated migration Docker
* `ed71926a6` chore(deps): bump org.codehaus.mojo:build-helper-maven-plugin
* `1d148bf15` chore(deps): bump org.apache.maven.plugins:maven-scm-plugin
* `c72a1e2bb` chore(deps): bump tomcat from `7e26fc3` to `a09d4c1`
* `78bd70065` chore(deps): bump org.dom4j:dom4j from 2.1.3 to 2.1.4
* `dcfdc9e41` chore(deps): bump org.apache.maven.plugins:maven-jar-plugin
* `cc2f51ab2` chore(deps): bump com.google.guava:failureaccess from 1.0.1 to 1.0.2
* `a5ce63316` chore(deps): bump github/codeql-action from 3.27.0 to 3.27.1
* `01b30091c` chore(rest): reformat ModerationRequestService
* `56ab42369` chore(deps): bump com.google.code.gson:gson from 2.10.1 to 2.11.0
* `f2b110dd0` chore(deps): bump org.apache.maven.plugins:maven-source-plugin
* `29fdca6fb` chore(deps): bump org.apache.maven.plugins:maven-surefire-plugin
* `4d34c09d2` chore(deps): bump commons-io:commons-io from 2.16.1 to 2.17.0
* `a4be46a19` chore: update OpenAPI docs for ProjectController
* `7478bd81a` chore: fix OpenAPI docs for VendorController
* `e892e5ed4` chore: fix OpenAPI docs for DatabaseSanitationController
* `b330354f4` chore: fix OpenAPI docs for EccController
* `671f39337` chore: fix OpenAPI docs for UserController
* `f88c820b9` chore: fix openapi docs for LicenseController
* `d5068fdee` chore: fix swagger docs of ScheduleAdminController
* `4a88eba4c` chore(deps): bump tomcat from `e19f9ca` to `7e26fc3`
* `e84e66b03` chore(deps): bump org.springframework.security:spring-security-oauth2-authorization-server
* `038e12a64` chore(deps): bump org.jetbrains:annotations from 26.0.0 to 26.0.1
* `d026717e0` chore(deps): bump log4j2.version from 2.19.0 to 2.24.1
* `0bbf1392f` chore(deps): bump org.sonatype.plugins:nexus-staging-maven-plugin
* `c41a3d0dd` chore: Remove unused dead code
* `c120a4cef` chore(deps): bump org.glassfish.jaxb:jaxb-runtime from 2.3.9 to 4.0.5
* `34ab188c0` chore(deps): bump version.keycloak from 25.0.4 to 26.0.5
* `4bd5a97fd` chore(deps): bump poi.version from 4.1.2 to 5.3.0
* `bb84e6eb0` chore(deps): bump docker/build-push-action from 5.4.0 to 6.9.0
* `5901e9bac` chore(deps): bump ossf/scorecard-action from 2.3.3 to 2.4.0
* `b3de287b9` chore: Update pre-commit with latest versions
* `d4c57b195` chore: Extend gitignore
* `047bff839` chore(deps): bump org.json:json from 20231013 to 20240303
* `06a65cdc1` chore: Remove duplicate entries for vscode workspace
* `75971bd42` chore(scorecard): Update permissions on workflows
* `416c9a4e7` chore: Remove dead code from actions
* `0be1b1889` chore: No need validate for any of .github files
* `1f3193529` chore: Remove unmaintained and disabled workflow
* `f95b3b5da` chore(scorecard): Remove broad permissions allowance.
* `0f7167b7d` chore(deps): Update json
* `0ea6cfb3e` chore(scorecard): Create initial codeql.yml setup

## sw360-19.0.0-M1
This tag covers many corrections, bug fixes and features after the 18.1 release.
Version 19.0.0 is also the first release without the Front-end integrated, but
as a separate [sw360-frontend](https://github.com/eclipse-sw360/sw360-frontend)
project.

Major changes in the release includes:
* Removal of Liferay and related libraries, OSGi framework
* Unification of various backend packages from src and svd
* Support for Java 21 and Apache Tomcat 11.0.0
* Replace couchdb-lucene with couchdb-nouveau

### Credits

The following GitHub users have contributed to the source code since the last
release (in alphabetical order):

```
> afsahsyeda <afsah.syeda@siemens-healthineers.com>
> Akshit Joshi <akshit.joshi@siemens-healthineers.com>
> Gaurav Mishra <mishra.gaurav@siemens.com>
> Helio Chissini de Castro <helio.chissini.de.castro@cariad.technology>
> hoangnt2 <hoang2.nguyenthai@toshiba.co.jp>
> Keerthi B L <keerthi.bl@siemens.com>
> Nikesh Kumar <kumar.nikesh@siemens.com>
> Rudra Chopra <prabhuchopra@gmail.com>
> Sameed <sameed.ahmad@siemens-healthineers.com>
> Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
> tuannn2 <tuan2.nguyennhu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with
reviews, testing, documentations, conversations or presentations.

### Features
* `c167bcca9` feat(rest): Endpoint to add comment on a clearing request
* `cd97b6154` feat(rest): Create new endpoint for schedule CVE and schedule attachment deletion.
* `00d70bcc5` feat(rest): get releases used by vendor
* `31b720b9e` feat(rest) : Rest end point for generate source code bundle
* `062a89290` feat(rest): saveUsages in project page
* `9751a2e1a` feat(Project): Add new endpoint for project's license clearing tree view (New GUI)
* `546d35b73` feat(Project): Import SPDX as dependency network
* `a18b053f5` feat(rest): Create new endpoint to download component template in csv format.
* `144ea5b81` feat(rest) : Move GenerateLicenseInfoFile rest end point to SW360reportcontroller
* `61ec9ac39` feat(REST): Exclude release version from license info
* `295f1cbff` feat(rest): fetch group list in project add and edit page.
* `e9ec8d8a7` feat: Make Java 21 default
* `cb99fc678` feat(ImportSBOM):Change naming convention of imported components
* `441fa7d85` feat(Project): Create new endpoint to serve list view of dependency network (New GUI)
* `7b4c534e3` feat(cloudant): use IBM SDK
* `09586fad6` feat(ektorp): remove ektorp from search handlers
* `af0262112` feat(lucene): nouveau integration
* `a019b468b` feat(keycloak-spis): Added the custom keycloak SPIs
* `3c453670d` feat(couchdb): Enable use of latest CouchDB with nouveau
* `8fdd93c86` feat(rest): endpoint to update a vendor.
* `bff430140` feat: Add CODEOWNERS to the repository
* `90ad3ea1c` feat(rest): Add additional fields in get clearingrequest endpoints.
* `771b965b2` feat(ComponentPortletandImportCDX): Validate VCS URL and sanitize GitHub Repo URLs during CDX import
* `99d0c80ed` feat(api): postpone moderation request action
* `af15a09e3` feat(rest): includeAllAttachments parameter in licenseInfo endpoint
* `66cac90c6` feat(CycloneDX): Make methods compatible with cyclonedx upgrade and update jackson version
* `9a15832c0` feat(rest): Endpoint to get comments of a Clearing Request.
* `ffbf1b183` feat(project): endpoint for vulnerabilitySummary page.
* `0d6908ab2` feat(project): Add necessary library dependencies required by rest code
* `acb1e54ea` feat(vscode): Add base Eclipse java formatter config file
* `a29d5b0c2` feat: Generate provenance and SBOMs on Docker images
* `8b6aa42cf` feat(docs): Remove old asciidocs support
* `fd0546244` feat: Update to Ubuntu 24.04 (Noble)
* `8f971f765` feat(rest): new endpoint for releases of linked projects.
* `5bd4cae83` feat(obligation): rest endpoint to update license obligations of the project.
* `3c40f09f2` feat(License): Add API Listing LicenseType and Add pageble for licenses, obligations
* `204ce2f02` feat: Add scorecard

### Corrections
* `9452b2b89` fix(cloudant): fix attachment creation
* `5bdef6d51` fix(pom): fixed the java version in kc module pom.xml
* `48e0f6c8c` fix(ImportCDX): VCS sanitization failing on characters like colon
* `dc18109b8` fix(Project): Fix project handler test with dependency network feature
* `5702dc595` fix(clearingState): making fossology report download configurable.
* `3f10b6856` fix(build): add the missing excludeReleaseVersion
* `69fcc6c9f` fix(servlet): complete migration javax to jakarta
* `3cad1c4aa` fix(UI): Add lang attribute to ReadmeOSS.html for generated license info.
* `77b801825` fix(keycloak-spi): Added the README.md
* `e43c3422a` fix(nouveau): fix nouveau query result
* `442ac94c7` fix(test): fix test cases with cloudant SDK
* `41e3d4605` fix(nouveau): extend nouveau connector as cloudant
* `cbcffd979` fix(cloudant): fix query builders
* `ced70a0e4` fix(cloudant): fix views
* `57f5b6908` fix(REST): Patch Release is causing the clearing state to be updated to NEW even if a Clearing is existing
* `5c4810a56` fix(backend): fix dependency for backend core
* `f0719b97a` fix(rest): Resolved null value returning for svm tracking status.
* `fe05d9f29` fix(rest): Update search API to return 200 status with empty results array when no match found
* `b0c11a1fb` fix(GenerateLicenseInfo): Generate License Info failing for releases having the same CLX
* `d6f630021` fix(rest): Ensure visibility field is case-insensitive
* `6a1408f50` fix(doc): fix OpenAPI doc for Search endpoint
* `83796a935` fix(rest): add requestClosedOn field in get clearingRequest_by_id endpoint
* `45a8137f3` fix: Update docker documentation to reflect current status
* `9dc2d6835` fix(rest): Enable back authorization and resource server with up to dat springboot
* `c493d83bf` fix(couchdb): Move setup data for single file and update compose to use as read only
* `c15e36cd8` fix(docker): Use Tomcat with Ubuntu 24.04 (Noble)
* `d655adc64` fix(rest): Add null check for linkedProject field if it is empty
* `77bdbf7f6` fix(rest): Add null check for linkedProject field to prevent Internal Server Error on GET request to fetch the linked projects of a project
* `5943127c6` fix(rest): Add code to update user details when creating a moderation request.
* `9777923f8` fix(docker): Reinstate docker builds
* `0265205b0` fix(docker): Update docker build to fit Ubuntu Noble and improved caching
* `293e025cf` fix(rest): Added JWT token convert to fix the issue with authorities
* `540f9baf1` fix(rest): Added the Oidc user info customizer and token customizer
* `1fb7bcf97` fix(rest): Add null check for linkedProject field to prevent Internal Server Error on GET request to fetch the linked projects of a project
* `3f6ae983b` fix(importCDX):Improve error message when PURL is invalid
* `3dfbb5538` fix(rest): Fix internal server error with 500 status code for link project to projects endpoint
* `f0e149422` fix(rest): Fixing pagination for endpoint '/packages'.
* `0d88cacc7` fix(rest) : Non uniform link format in attachmentUsage end point
* `fea2d4eda` fix(rest): Fixed the swagger issue
* `01218278d` fix(backend) : Product clearing report generated has strange numbering issue fix
* `da95be6e7` fix(rest): Added modifiedBy field in get package_by_id endpoint.
* `82ad83e70` Revert "fix(rest): Fixed the swagger issue"
* `cc38d07df` fix(rest): Fixed the swagger issue
* `51fabdfc2` fix(rest):Added code to resolve the server error while fetching a summaryAdministraion endpoint.
* `b262c4c82` fix(rest): Fixing the rest test cases
* `308ce540b` fix(rest): Added a missed field in package endpoint for allDetails.
* `8f0560c04` fix: Only publish test report on failures
* `f48e6d27b` fix: Thrift cache location
* `b69720c91` fix: Update thrift build to fix github caching
* `89f47fe05` fix(test): Proper build tests now without jump folders
* `4dd4f8aa7` fix: Remove wrong placed copyrights on commit template
* `f8dcd79f2` fix(test): Disable rest test to avoid chicken and egg integration
* `7ce112133` fix(github): restore pull_request_template.md

### Infrastructure
* `4e883a5a1` chore(deps): bump org.springframework:spring-context
* `7dd44a5fd` chore: Add maven validation on build
* `d086e9a71` chore(deps): bump org.keycloak:keycloak-core
* `2d90a9a00` chore(deps): bump org.keycloak:keycloak-core
* `bfd296052` chore(maven): deploy keycloak listeners
* `c71b0d5c4` chore(maven): segregate war and jar deploy dirs
* `d9b3edf25` chore: Add Tomcat 11 default for Docker
* `872c74ef1` chore(nouveau): catch exception for nouveau query
* `824504564` chore(docker): update compose with dockerhub image
* `3fc2e0976` chore(couchdb-lucene): remove third-party/couchdb-lucene
* `111a0fe88` chore(refactor): Refactored the models by adding Lombok
* `e3dccf3ee` chore: Reduce couchdb log level on docker compose
* `e3f3dab7e` chore: Update the license header checkfor CODEOWNERS
* `af056ef15` chore: Properly set components servlet as war file
* `27fddd182` refactor: Use the correct thrift image
* `56b63f065` refactor: Remove dead code comments
* `7b3fe9233` chore: CouchDB setup can't be read only
* `442970d4c` chore: Add color coding for sw360 project
* `30b6114f8` refactor(backend): Adjust component test call
* `9a09353af` refactor(backend): Disable ComponentImportTestUtils
* `a0369e0a3` refactor(backend): Allow test properties be configurable
* `b7d9941dd` refactor(backend): Fix licenseinfo test
* `2f24d0b3e` chore: Disable logging on disk for couchdb and configure authorization server
* `bc759edb4` refactor(backend): Restore webapps install
* `a9cff25ea` chore: Fix version dependencies
* `a81fe91dc` refactor(backend): Remove invalid recursive add-build-configuration process
* `a973a70f4` refactor(backend): Disable usage of Handlers by importer
* `2019328a3` refactor(backend): Adjust dependencies for subprojects
* `a5df30cbb` refactor(backend): Move svc-common to service-core
* `2e9b67182` refactor(backend): Create licenses-core shared library
* `d1f88af5c` refactor(backend): Move vulnerabilities shared classes to core
* `eaeb4e0e8` refactor(backend): Unify source tree
* `eec9f1557` chore(vscode): Increase memory requirements for language server
* `9dbbaf958` chore: Update README_DOCKER with proper commands
* `1bb1ce228` chore: Update couchdb user and password for scripts
* `86be40d49` chore: Ignore vscode directory
* `d1e1269b2` chore: Remove dead code
* `e8d6398cc` chore(docker): Fix syntax warnings
* `09517affc` ci(docker): Use correct thrift docker context
* `f10c1b0bb` refactor(docker): Adjust CouchDB configurations
* `714e16eac` ci: Minor quality control fixes
* `406b2eec2` chore: Remove pom duplicates
* `828c05a63` build(deps): bump urllib3 in /.github/actions/clean_up_package_registry
* `612bce6b7` refactor: Remove liferay deploy dir
* `0462eec98` refactor(project): Remove OSGI bundle plugin
* `51af9238f` refactor(libraries): Remove OSGI bundle from importers
* `d66d6f6db` refactor(libraries): Remove OSGI bundle from exporters
* `a305f5f08` refactor(libraries): Remove OSGI bundle from CommonIO
* `0507602ba` refactor(datahandler): Remove OSGI bundle
* `063c294e1` refactor(project): Remove log4j-osgi-support
* `8505587a3` chore: Remove unused buildnumber plugin
* `1eb27eb2c` refactor: Remove liferay build references
* `41e6951ea` chore: Remove unused spotless plugin
* `e2719816b` chore: Remove unused flatten plugin
* `2e04e949d` chore: Place enforcer plugin in correct place
* `712f7c057` refactor: Versioning update
* `474323658` chore: Update gitignore and ide settings
* `8d493bcd3` build(deps): bump requests in /.github/actions/clean_up_package_registry
* `f754535e4` chore: Ignore templates to check license
* `02824ef71` chore(project): Minor clenaups
* `8b68eff39` refactor(docker): Modernize docker without liferay
* `447c89c68` refactor(project): Adjust dependencies for Java 17 and Liferay removal
* `f7dc1d0f9` build(deps): bump certifi in /.github/actions/clean_up_package_registry
* `f8b201838` build(deps): bump org.springframework:spring-web from 6.1.5 to 6.1.6
* `83da48abc` chore(upgrade): skipped rest auth server test cases until its fixed
* `d31c5bd60` chore(upgrade): Resolving src-licenseinfo module test cases.
* `8a2688883` chore(upgrade): Added a patch for the java 17 related changes w.r.t couchdb-lucene
* `aa9422126` chore(upgrade): Authorization upgrade
* `a2a30f552` chore(upgrade): Upgrade to Java 17
* `d8d8ef585` chore(upgrade): Remove liferay
* `62829f44c` refactor(java): Disable some tests to easy migration
* `0cfdeada8` ci(java): Update to Java 17 as default and enforce it


## sw360-18.1.0-M1
This tag includes important corrections and fixes following the 18.0 pre-release. It is also the final tag with Liferay, as SW360 will use the SW360-frontend project (https://github.com/eclipse-sw360/sw360-frontend) starting from the next release.

### Migrations

For existing installations, a data migration is required with PR 1963. Please go to the readme file in scripts/migrations to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

Note: For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the DRYRUN variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
> Afsah Syeda <afsah.syeda@siemens-healthineers.com>
> Aftab, Farooq Fateh (ext) <farooq-fateh.aftab.ext@siemens-energy.com>
> Anupam Ghosh <anupam.ghosh@siemens.com>
> Akshit Joshi <akshit.joshi@siemens-healthineers.com>
> Eldrin <eldrin.sanctis@siemens.com>
> Gaurav Mishra <gmishx@gmail.com>
> Helio Chissini de Castro <heliocastro@gmail.com>
> Jens Viebig <jens.viebig@vitec.com>
> hoangnt2 <hoang2.nguyenthai@toshiba.co.jp>
> Keerthi B L <keerthi.bl@siemens.com>
> Nikesh kumar <kumar.nikesh@simens.com>
> rudra-superrr <rudra.chopra@siemens.com>
> sameed.ahmad <sameed.ahmad@siemens-healthineers.com>
> tuannn2 <tuan2.nguyennhu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features
* `4bfabe486` feat(rest) : Remove mail-request parameter and read from config file
* `96863d14c` feat(REST): Search package by purl and version
* `684d90117` feat(REST): Create clearing request for a project and move the preferred clearing date limit field out of Liferay"
* `fe044d00` feat(project): Added release field for licenseObligation get endpoint
* `70837b27` feat(rest): filter attachment usages in project
* `ea94202b` feat(license): Update Whitelist
* `af155858` feat(CR): Update clearing request state from AWAITING RESPONSE to PENDING INPUT
* `2bd2b2fd` feat(vscode): Add workspace java settings
* `8ceba8fb` feat(docker): Add test build using docker
* `944a7164` feat(rest): added pagination for vulnerability tracking status page.
* `70391d07` feat(rest): add license obligations to a project.
* `4f65386f` feat(obligation): endpoint to list license obligation table data
* `5fcb3533` feat(rest) : endpoint to list license obligations from license database.
* `240c73f3` feat(CR): Create a new Clearing Request state Sanity Check to perform sanity check before accepting a project
* `4bc56326` Revert "feat(CR): Disable Clearing Request creation for the projects which have linked releases without SRC type attachment"
* `71d3a470` Feat(User): Create new endpoints to Create/Revoke/List rest api token
* `d4820efc` feat(Rest) : Download license clearing report end point.
* `14fda713` feat(api): new endpoint /mySubmissions
* `cec7f4b7` feat(docker): Improve output of check_image script.
* `d7699485` feat(docker): Revamp docker build setup
* `2ddf76f0` feat(user): Enable API user endpoint by default
* `36a41cef` feat(Obligation): adding obligation type data in license obligation table.
* `44219a39` feat(rest) : Pagination for vulnerability tracking status
* `b925c0ab` Revert "feat(UI): enhanced date filter for open and closed clearing requests tab"
* `a3038447` feat(UI): enhanced date filter for open and closed clearing requests tab
* `9f9a1ffa1` feat(UI): Add an info button in the create CR page
* `b98d346a4` feat(UI): Add clearing type column in closed clearing request tab
* `b6aa50650` feat(Project): - Extract license from all releases in dependency network when download license information of a project - Generate source code bundle from all releases in dependency network when download Generate source code bundle for a project
* `49f5486fa` feat(rest): endpoint to link sourceProject to list of projects.
* `1ab14350b` feat(CR): Disable Clearing Request creation for the projects which have linked releases without SRC type attachment
* `bcd600c26` feat(User): Add new endpoints to get/update requesting user profile
* `3cb73c19f` feat(rest): Create new endpoint to unschedule all services.
* `83a2b3a28` feat(license): Listing obligations by license
* `8a9c407e8` feat(license): Fix Update License isChecked
* `89a75f815` feat(project): Update ghactions workflows deps
* `849e10a0c` feat(obligation): Add api listing obligations by ObligationLevel
* `3ec2cb129` feat(rest) : Rest end point for releases by lucene search
* `7ccba71d5` feat(project): Setup Sonatype publishing
* `c0fb731c4` feat(license): Create API Export License
* `141e24bab` feat(Release):Upload Source Code Attachment to Releases through a Scheduled Service
* `c7c33c78f` feat(rest): adding pagination for listing vendors endpoint.
* `c805ff90f` feat(rest) : Adding or Modifying fields to project summaryadminastration page
* `6a89beabc` feat(Script): Delete MR's for a specific user
* `adc862038` feat(license): Create new api update license

### Corrections
* `dfabecd2c` fix(importCDX) : Fix package's linked release updation when an SBOM is imported
* `3de514387` fix(project): adding project owner field in project get endpoint.
* `c31464972` fix(api): throw 409 if last moderator
* `219792b1` fix(importCDX): Resolve incorrect package/release count in import summary
* `6d9f3620` fix(rest): Create a new endpoint for dataBaseSanitation.
* `ae997be2` fix(project): Update outdated Github actions
* `cb02b200` fix(sw360): changing mkdocs version
* `0c9523fb` fix(REST): Improve error message handling for CycloneDX sbom import using REST API
* `df735e9b` fix(Release): Updating the license overview in the summary page
* `e5ac9278` fix(SRCUploadService): Source upload should work for release versions having alphanumeric characters
* `fa42d204` fix(api): provide typeMasks name as Optional type
* `6e36abbb` fix(api): check project modifier before embedding
* `3beff049` fix(Project): Fix bug Expand Next Level and Collapse All button are hidden when click on sort icon
* `5112980f` fix(urlEncoding): url encoding.
* `fe0a4408` fix(Release): Add embedded other licenses in release response
* `d4a8be84` fix(importCDX): Packages without VCS in SBOM having VCS in SW360 are not getting linked to project
* `8af9bd5e` fix(importCDX): Add check for existing comps and package using case-insensitive comparison of vcs and purl
* `ee3ed068` fix(Liferay): Fix bug cannot access oauth client page when import lar file
* `edc9320c` fix(rest) : attachment usage type fix in response
* `49be7428` fix(importSBOM): Remove the invalid characters appearing in import summary message for invalid packages list
* `5a726764` fix(rest): create endpoint for search by userName using lucene search.
* `ff068133` fix(rest): Added releaseId in recentRelease and release mySubscription.
* `87a14f7a` fix(Rest): Added status for mysubsciption in component.
* `d28843c2` fix(docker): Fix broken binaries context inclusion
* `16475d70` fix(rest) : create new endpoint for cleanup attachment.
* `0950a2ca` fix(script): update modifiedBy/modifiedOn project fields.
* `67696a9f` fix(department): Division by zero caused by bad default value for interval
* `9703661d` fix(rest): Added primaryRole and secondaryDepartmentRoles fields for user endpoint.
* `fba0d8e5` fix(rest): Added modifiedBy field in project search by id.
* `178813e5f` fix(docker): Adjust local naming for docker images
* `b55372562` fix(thrift): Add proper version to build
* `34765dd80` fix(thrift): Follow link download step
* `ef5cc0142` fix(database): Restore reading environment database vars
* `8aaf95734` fix(UI) : Issue fix for vulnerability not displaying for project
* `c63023c4d` fix(release): modify the externalId query parsing
* `6a6cb33b5` fix(docker): We have been using wrong Java version
* `625ffcfa1` fix(release): revert external id query parsing
* `222879a9e` fix(rest): error handling when user dont have sufficient import permission
* `d619c5121` fix(Table): Fix error of hiding attachment table content when clicking sort
* `ef83441df` fix(moderator): show message when only moderator choose remove me option.
* `590a2b3ad` fix(docker): Remove deletion that invalidate image
* `2fe147f09` fix(rest): create new enpoint to check server connection.
* `47d14b158` fix(script): Fix migration script not working with python3
* `0d535c386` fix(config): Correct file number
* `0f9d9b85a` fix(rest): create a new endpoint for fossology in admin tab.
* `5b9f10921` fix(script): Fix incorrect numbering for migration scripts
* `0f9d31974` fix(couchdb): Add config entry to disable couchdb cache
* `451948a79` fix(javadoc): Remove invalid link reference
* `05c2445fa` fix(lib): Add meta information to enable publish
* `b5f6cb469` fix(importCDX): Update failed component creation error message
* `6e1964a40` fix(rest-fossology): applied changes for upload endpoint
* `5a83fe2c9` fix(RequestsPortlet): Unable to reopen CR, Open Components to display open releases, clearing progress to show percentage
* `2fdd5f4c5` fix(Rest): Allowing search for releases using externalIds
* `d9fce216f` Fix(package): Fix issues api for package - Cannot unlink orphan packages from the project - Cannot link a package to a release without any package - Handle message when package with same purl already exists
* `02d84be81` fix (rest) : rest api created for component search by lucene search

### Infrastructure
* `e71c5e53f` Revert "build(deps): bump org.apache.commons:commons-compress"
* `42ed65ee` chore(deps): Update json to version 20240303
* `cd53eed2` refactor(deps): Update new codebase library
* `8fca0929` chore(license): Ignore checks under templates
* `73ea0cf3` chore(templates): Second batch of bug report template updates
* `f375af4f` chore(templates): Update outdated bug/issue templates
* `a28f3ce3` build(deps): bump idna in /.github/actions/clean_up_package_registry
* `2d907549` build(deps): bump org.apache.commons:commons-compress
* `4d87a2bb` build(deps): bump org.bitbucket.b_c:jose4j in /rest/resource-server
* `7ee06367` build(deps): bump org.springframework.security:spring-security-core
* `737a1320` ci(docker): Use external action to reduce maintenance
* `d9341ee28` chore(package-portlet): package-portlet enabled for default installation
* `ecb30a34d` Update build status
* `7d3511146` build(deps): bump com.jayway.jsonpath:json-path from 2.8.0 to 2.9.0
* `8b5428d92` docs(api): add OpenAPI docs for /vulnerabilities
* `27dc3d8bb` docs(api): add OpenAPI docs for releases
* `72a99c897` docs(project): response codes DELETE /projects
* `1c3f70f8e` chore(javadoc): Fix javadoc entries as requirements to publish in sonatype


## sw360-18.0.0-M1
This tag covers many corrections/bug after the 17.0 release and multiple new endpoints to support sw360 UI project.

### Migrations

For existing installations, a data migration is required with PR 1963. Please go to the readme file in scripts/migrations to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the DRYRUN variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
> Abdul Kapti <abdul.kapti@siemens-healthineers.com>
> afsahsyeda <afsah.syeda@siemens-healhtineers.com>
> Anupam Ghosh <anupam.ghosh@siemens.com>
> Dinesh Ravi <dineshr93@gmail.com>
> Eldrin Sanctis <eldrin.sanctis@siemens.com>
> Gaurav Mishra <gmishx@gmail.com>
> Helio Chissini de Castro <heliocastro@gmail.com>
> hoangnt2 <hoang2.nguyenthai@toshiba.co.jp>
> Keerthi B L <keerthi.bl@siemens.com>
> Kouki Hama <kouki1.hama@toshiba.co.jp>
> Le Tien <tien1.le@toshiba.co.jp>
> Muhammad Ali <alimuhammad@siemens.com>
> Nguyen Nhu Tuan <tuan2.nguyennhu@toshiba.co.jp>
> Nikesh kumar <kumar.nikesh@simens.com>
> rudra-superrr <rudra.chopra@siemens.com>
> Shi Qiu <shi1.qiu@toshiba.co.jp>
> Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
> Tien Le <tien1.le@toshiba.co.jp>
> tuannn2 <tuan2.nguyennhu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features
* `e9a9f308b` feat(rest): Adding pagination for ecc info of releases for a project.
* `c0db06a68` feat(rest) : Adding pagination for listing users endpoint.
* `acc553b14` feat(rest): endpoint to get attachmentUsages for a project.
* `bb0d01fd5` feat(rest): endpoint to get license clearing count for a project.
* `bc5ae7d1b` feat(rest) : Add enableSvm field in response to projects api
* `130ed2585` feat(importCDX): enhanced sw360 CDX importer
* `754ba96a7` feat(CreateCRandRequestsPortlet):Added new field Clearing Type for CR and Additional columns in Open CR table
* `b89bde7b9` feat(Rest): Modifying the document for search endpoint
* `ec750b824` feat(bug) : Download release attachment fail issue fix
* `f629a0d3f` feat(rest) : End point for export vendor spreadsheet
* `930ef1d13` feat(docker): Add option to specify cvesearch.host at build time
* `f4febd954` feat(release): Fix response api get single release with costDetails
* `70141590c` feat(rest): API to get vulnerability tracking status
* `03aaa6985` feat(Rest): New endpoint allow load assessment summary information of release
* `8c2e71b85` feat(ui):enable to bulk delete component/releases for admin SW360
* `266aeac3d` feat(REST): Add restricted project counter for component and release usedBy API
* `ca0ef31f2` feat(rest):Update API Create Release with Cost Detail
* `1974005e2` feat(ui): Added collapse and expand icon for all the tabs
* `16dae1a4a` feat(rest): API to get vulnerability tracking status
* `afe118d96` feat(Rest): New endpoint allow load SPDX license info from attachment of release (ISR, CLX, CLI)
* `a330fde1e` feat(rest): Update release with attachment info
* `ba6c743f5` feat(ui) : Add changelogs for license pages
* `d369c73e3` feat(rest): Update API create Release with Moderator, Contributor, CpeId
* `c9c37b94d` feat(rest): Update API create Release with LinkedRelease
* `d7b52f53e` feat(rest): Add Information Vendor to response Get release detail
* `4449e6017` feat(liferay): Export private pages include package portlet
* `7c57b8081` feat(rest): Add information user change status attachment when edit component by API
* `d25d35ce3` feat(Project): New configuration make project and releases relationship more flexible
* `de4125bb4` feat(debug): Add Tomcat manager to docker
* `fd13d1943` feat(rest): listing license clearing info of a project.
* `cc9291d68` feat(CycloneDX): support CPE in import and export
* `42f44107f` feat(rest): Update Component with attachment
* `d8c594628` feat(REST): New endpoint to write SPDX license info into release
* `d356bc022` feat(UI): Package Portlet Signed-off-by: akapti <abdul.kapti@siemens-healhtineers.com>
* `6aa0b8d7e` feat(rest) : asynchronous end point for report download
* `4d4c863ad` feat(RequestsPortlet): Added On Hold value for request status and Next/Last 15 days filter
* `fd159f302` feat(Components): Add a new field VCS/Repository URL for components
* `be9e5f5bb` feat(rest): New Endpoint create attachment
* `34e2d9e77` feat(Rest): Rest API allow to re-generate fossology report
* `f4432c98b` feat(rest):Adding new fields to get list of project vulnerability
* `efbe761f5` feat(ExportSpreadsheet): Add project and release ID to the exported excel
* `b7740902b` feat(ProjectObligationsEdit): Save comment and status fields on edit
* `a7bc2969c` feat(rest):New end point for my components
* `a4e7f6808` feat(REST): New endpoint split components
* `447143b8e` feat(rest): To list linked projects of sub-projects.
* `ed7f4e237` feat(Department): New function for Department Management
* `662a05977` feat(rest): new endpoint merge component
* `1bf157600` feat(UI/REST): CycloneDX SBOM Importer & Exporter
* `e8f6e6b26` feat(rest): update response API Get a single release
* `57b02aa29` feat(REST): Update response endpoint get attachments by release
* `410184928` feat(ECC):Added pagination to ECC release list
* `b6d58b979` feat(ui): add note filed in license page
* `f14f9b0e4` feat(rest): update response API Listing users
* `c27a2fe35` feat(rest): update response API Listing vendors
* `9bd7869f4` feat(update): update response api get single component
* `90c59acb4` feat(rest): modify moderation requests
* `8e71c959c` feat(ci): Use actions java setup instead of standard packages
* `037acd41b` feat(ci): Use actions java setup instead of standard packages
* `a7af308fa` feat(ci): Update build and test to accept dispatch
* `669d6f98b` feat(rest) : api to get count of projects
* `1c4b223f8` feat(update):update response api get attachment by component
* `e6374e820` feat(api): create new endpoint import bom for component
* `462675325` feat(api): create new endpoint update vulnerabilities of a release
* `4dbc8705a` feat(api): create new endpoint update vulnerabilities of a component
* `bc368f203` feat(REST): Endpoint for Download Attachment Bundle of Release
* `764a24c6c` feat(api): Endpoint get release overview by component
* `391c006e6` feat(REST): Endpoint for Download Attachment Bundle of Component
* `96a032814` feat(api): endpoint get vulnerabilities of a component
* `d10048956` feat(rest): new endpoint `/moderationrequest`
* `e682a50fa` feat(spdx): Added support for pasring of SPDX-2.3 (ISR) generated via fossology
* `b7710e630` feat(lucene): Modify pom to generate proper war from couchdb lucene
* `53236b590` feat(libs): Add couchdb-lucene as third party
* `84e098774` feat(project): Prepare to introduce thirdparty libraries
* `c80f75908` feat(rest):Components with all details Rest Api doc updated
* `b32e90154` feat(REST):Endpoint for sbom import
* `249f48f49` feat(SPDX): Making new tab in component release pages for showing SPDX/SPDX Lite data #1240
* `9d566af03` feat(rest):New end point for my components
* `53c8d85da` feat(clearing): Improved cloud backend clearing
* `2e0732a2b` feat(rest): Added basic username and password based authentication
* `4f171a659` feat(rest): optimize fetch project
* `729207997` feat(EditCR): Admin will be able to reassign/edit the Requesting User of CR
* `56096f24a` feat(ProjectUI):ExternalIds and Additional Data fields in Export Excel
* `7b84b0e4f` feat(api): get vulnerabilities from relase by api
* `aafc95808` feat(rest) : Update data without moderation request And This features' a configurable setting
* `73ba7012d` feat(docker): Use main Maven docker image
* `d6555a370` feat(rest): endpoint for linked projects.
* `e20d7bf06` feat(rest): new endpoint /releases/recentReleases
* `c5aea6f4e` feat(rest): newendpoint /components/recentComponents.
* `d707d7b53` feat(rest): new endpoint `/projects/myprojects`
* `0f95fd368` feat(project): Added Email functionality for individual project spreadsheet export
* `ff92cd956` feat(ProjectUi): Enable Release with only one non-approved CLI for 'Adding License Infor To Release' and 'Displaying Obligations' (#1764)
* `f5daadb6e` feat(Search): Added restricted search (#1797)
* `df0a6a123` feat(ui): Add banner to broadcast messages (#1830)
* `d4cd90f67` feat(Project): Added Vulnerability Summary Tab in Projects.
* `ca1da16fe` feat(ProjectsUI):Changed Expand All To Expand Next Level and added alert message
* `b682060ae` feat(Advance Search): Provided an 'Exact Match' checkbox in Advance Search that inserts (") around  search keyword
* `b0ccdc480` feat(ci): Add thrift binary to cache
* `95009d35f` feat(project): Add pre-commit and spotless
* `eabbb0053` feat(svm): Publish SVM codes to Community

### Corrections
* `5e48f83b2` fix(importCDX): Remove view BY_VCS_LOWERCASE and BY_PURL_LOWERCASE
* `e94d9c729` fix(Moderation): Fix bug could not open Release and Component moderation request
* `45b317d86` fix(rest): adding additional fields to rest response for linked projects.
* `4e329b464` fix(license): Update Response api for single license and Add rest-docs api create license
* `d261f70e5` fix(rest): Added new endpoint for LicenseType in admin tab
* `41d735f9a` fix(package): Can't link project to package
* `1debd1e2c` fix(REST):Get Component failing for names with space
* `521835e38` fix(UI) : Added code to import the upload license in admin tab
* `f748c7cba` fix(package): Create package by API can't link release
* `eb7efb3f9` fix(rest): create new endpoint for import OSADL information in admin tab.
* `23242daaf` fix(importCDX): Resolved unnecessary update of component fields
* `976f0ffeb` fix(rest): Added new rest endpoint for upload license in admin tab.
* `fbd924cdd` fix(Package): Can't create a new Package by API
* `08962f93c` fix(rest): New endpoint to download archive files in admin page
* `f49e6d372` fix(db): clean-up closed moderation requests.
* `c0dbccfd5` fix(lar): Remove old lar files that are usable only for old releases
* `67f8d9f5a` fix(OAuthClient): Fix Can't show OAuth Client page
* `de67119ae` fix(rest): create new endpoint for import spdx information in admin tab
* `c2a9ee24a` fix(GUI): Fix edit dependency network GUI broken
* `91b768595` fix(rest): Removed the copyright text from RESTAPI docs
* `9d37c4993` fix(rest) : Modifications to improve GET result speed
* `3bf53a1a3` fix(docker): Use the recommend fix fro Liferay x Java
* `5a1ba6145` fix(UI): Added code to load moderation documents in request tab
* `5fcb2b303` fix(sw360Build): build failing because of invalid CEN header
* `7c8f8d24b` fix(rest):new endpoint to delete all license information in admin tab
* `9f7859184` fix(lucene): Use old javax.servlet
* `5f1e3d0ce` fix(OSADL): Add missing obligation rules
* `b70be52a6` fix(rest): API create component's businessUnit is always set as the user's department
* `ec4c39e06` fix(doc): remove merge conflict notation
* `7b0938a6b` fix(Export): component.visibility.restriction.enabled option works incorrectly with the Export feature in Cyclone DX
* `c0df9334d` fix(Rest): Allowing search for components without encoding
* `475b4b437` fix(UI):word correction of Initial use scan in attachment type
* `f29de2b1c` fix(rest): show linked project in project summay tab
* `f237ecdb6` fix(Rest): Allowing search for externalIds without encoding
* `5cdd364eb` fix(rest): fix test rest api of component
* `ded850d7c` fix(UI): Sorting release verions in drop down menu when inspecting component.
* `c4079d9a0` fix(Rest): Add clearing information in release response
* `04c64a580` fix(rest): Missing moderators field when creating component using API
* `9d79b2896` fix(rest): Added endpoint url for summary and administration page info
* `81b6ca3a7` fix(readme): Add Information of python2 to python3 change when running file migration scripts
* `b02f90ec2` fix(update): Update the migration readme file
* `3f7349f78` fix(db): Data Quality and reduction of storage.
* `42ffce89d` fix(test): Add missing test deps
* `b6303eccc` fix(deps): Normalize spring-web and spring-boot versions
* `bada732d8` fix(rest): Add Description field for myProjects and myComponents in homepage.
* `19325b333` fix(lucene): Restore original portlet naming and configs
* `e3df30e53` fix(docker): Restore document_properties volume
* `ed87dde0b` fix(docker): Enable cache deps downloads in volume
* `5ce0d0788` fix(license): Fix attachment type when importing SBOM
* `262d0cad6` fix(EditCR): Not able to edit CR
* `2bcb6185e` fix(docker): Remove clucene build
* `bd39f4fa5` fix(sw360): Add sw360 adapt patch by default
* `53236b590` feat(libs): Add couchdb-lucene as third party
* `9811edf59` fix(update): Updating sw360.propertiy file is missing when using SPDX Document Tab
* `e1dd33f43` fix(rest): added endpoint for release subscriptions
* `6479d8894` fix(libs): Normalize json versions
* `c2844e30c` fix(deps): Revert httpcore and fix httpcore and spring-boot
* `d4e4ac764` fix(deps): Update to current httpcore5 release
* `96387f9c7` fix(cache): Key was invalid due file not exists and hash attempt fail
* `43a07df64` fix(ci): Cache now use the right naming
* `b42ea4fd3` fix(versions): Normalize javax.activation version
* `61c0a1b7c` fix(docker): Remove couchdb-clucene from the slim jars
* `fdbc8c360` fix(docker): Adjust config defaults
* `c923fa847` fix(component_gui): Can not load component detail page with long additional data text
* `d8b9d77f6` fix(UserSearch) : Modified the user search operation (#1858)
* `b54169f09` fix(projectExporter): Added Project visibility in project exporter spreadsheet
* `8c454efb6` fix(config): Couchdb configs not like double commas
* `b7d2f7a46` fix(docker): Update docker-compose.yml
* `470b70788` fix(docker): Move to the new Github org
* `2a9bffa4f` fix(rest): componentType field  will show when allDetails true in release
* `59ebfdcf0` fix(UI): Attachments tab not loading in UI
* `f81243c40` fix(docker): Fix couchdb default setting
* `48f688e1b` fix(docker): Change maven version from 3.8.7 to 3.8.8
* `cd4293f97` fix(api): deletion project returns 500 error and API doc of link release to release makes ambiguous
* `dd6f60218` fix(rest): disable URI encoding in search by external ids in release
* `aecc19141` fix(rest): endpoint api/projects does not return all projects
* `5514b4e38` fix(language): Fix the properties file and add some other needed files for Chinese language support
* `3ff6f65bd` fix(rest): Added endpoint for mysubscriptions for component
* `d6da8a919` fix(rest): Added endpoint for release subscriptions
* `2a0395256` fix(ProjectUI):External Id not visible in Vulnerability Tracking Status
* `c596d6094` fix(vulnerability): apache commons-text vulnerability CVE-2022-42889 #1864
* `949288618` fix(project): Update Apache commons-text
* `27d5fc011` fix(db): Deactivate email notification of user not belonging to a domain.
* `1af7ecb1a` fix(LiferaySetup):Website not loading after removing BannerMessage Field
* `63e2fef1d` fix(rest): Api endpoint /components/usedBy returns 500 when component not have any release.
* `e66363183` fix(UI): Error when creating/editing duplicate project/component/release
* `c28bac8e8` fix(ui): support Vietnamese language in Obligation page
* `2d20226e7` fix(Script): Modified script such that it removes trailing and leading whitespaces of components and releases and additonally  link releases of duplicate components
* `3b7269f6d` fix(vulnerability): Script to repair release vulnearability relations
* `92b18eaaf` fix(UI): Added EnableSVM field in project exportspreadsheet
* `9f277825b` fix(attachmentUploadModal):Progress bar will be visible only after clicking on the upload button
* `0a59109e9` fix(rest): Reuse centrally created thrift client
* `92f3c42f0` fix(pre-commit): Do not run clean/build
* `3cc8b6293` fix(ci) : Change maven version from 3.8.7 to 3.8.7
* `27e14e70b` fix(UI) : Added Created on in project export
* `799d2f789` fix(UI): ISR which will make the SW360 Release status as Scan Available
* `09c126967` fix(UI): unset few field while create a duplicate in project and component
* `5d9c3024b` fix(UI):remove deactivated users from moderators list
* `790c7ae9d` Fix(REST): Add COTS details information when fetch a single release that has component type COTS
* `3cd88e009` Fix(Search): Fix bug can not show result with special character and can not search Obligation
* `776c9b3ff` Fix(Rest): Add more information in get components response (support New GUI)
* `ce6f9e616` Fix(Project): Can't disable CR based on Japanese group
* `d80822818` Fix(Project GUI): Fix bug missing obligation text when change status or comment of component, project, organisation obligation in project edit
* `67dff9e27` Fix (REST): Fix bug do not set businessUnit automatically when create component. Get all components always return visibility EVERYONE.
* `b6bfa4258` Fix(REST): Fix bug update project without vendor information will remove vendor of project

### Infrastructure
* `265fb1953` ci(fix): Ignore requirements.txt files in testForLicenseHeaders
* `6be2c6f79` build(deps): bump org.json:json from 20230227 to 20231013
* `d630785b1` ci(fix): Missing code checkout on clean workflow
* `0b713d8af` refactor(docker): Improve docker build and deployment
* `9f71e11a7` docs(openapi): add OpenAPI doc for Obligation, Package and Report
* `c315c0b7a` docs(openapi): add OpenAPI doc for License and Moderation
* `8194286af` docs(openapi): add OpenAPI doc for ComponentController
* `05a27600a` Update build_and_test.yml
* `5410eefc5` Update README.md
* `606d9b353` Update README_DOCKER.md
* `1a534db4d` docs(openapi): add docs for attachment
* `98e10d47f` docs(openapi): add docs for vendor, user, search
* `ac8e9d10f` docs(rest): generate OpenAPI docs for Project
* `561687678` chore(docker): update maven version
* `c3492c322` chore(deps): bump guava from 31.1-jre to 32.0.0-jre
* `a9821a634` chore(thrift): Prevent datahandler recompile all the times
* `1d15e7741` chore(deps): Update thrift version
* `f5c86b9d9` chore(deps): bump jose4j from 0.7.9 to 0.9.3 in /rest/resource-server
* `1d3cd248b` chore(rest): Making endpoints configurable
* `cd6d5cfed` ci(cache): Give GH_ACTIONS permissions to reach cache
* `21833c85c` ci(cache): Give GH_ACTIONS permissions to reach cache
* `baaa882f8` ci(docker): Improve cache mechanism
* `ae2b667f7` ci(secrets): Fix the new secrets loading mechanism
* `651c67680` ci(cache): Fix thrift cache miss
* `f6d40b3e6` ci(project): Update pre-commit and ci hooks
* `5081686ca` chore(action): Cache maven dependencies
* `8937ec88b` Update(Vulnerability): Improve the function of API to delete vulnerability and relation of vulnerability with release
* `b41273dec` chore(migration) Avoid null pointer on script 048_add_component_businessunit.py
* `a49191fb7` upd(ci): Reduce the intermediary docker builds for Midnight daily


## sw360-17.0.0-M1
This tag covers many corrections/bug fixes after the 16.0 release.

This release provides features, muliple bug fixes for release 16.0, for example, new REST endpoints, improved docker script and fixes related to liferay-7.4.

### Migrations

For existing installations, a data migration is required. Please go to the readme file in scripts/migrations to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the DRYRUN variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
Abdul Kapti <abdul.kapti@siemens-healthineers.com>
afsahsyeda <afsah.syeda@siemens-healhtineers.com>
Anupam Ghosh <anupam.ghosh@siemens.com>
dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
Eldrin <eldrin.sanctis@siemens.com>
Gaurav Mishra <gmishx@gmail.com>
Helio Chissini de Castro <heliocastro@gmail.com>
Jaideep Palit <jaideep.palit@siemens.com>
Kouki Hama <kouki1.hama@toshiba.co.jp>
Muhammad Ali <alimuhammad@siemens.com>
Nikesh kumar <kumar.nikesh@simens.com>
rudra-superrr <rudra.chopra@siemens.com>
Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
tuannn2 <tuan2.nguyennhu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features
* `a20704c7` update(lang): add chinese lang property file
* `f9d23047` feat(MailApi): enable control over trusting the email host
* `3707569d` feat(rest): new param (allDetails) added in component call to get more details of component
* `65011f18` feat(UI): Applying sorting on release versions in drop down when inspecting a component.
* `c8597b45` feat(SPDX): Upgrade tools-java library to support SPDX 2.3 format
* `684d3c6a` feat(ProjectUI):License Info In the Spreadsheet Exported from Project License Clearing
* `111d5876` feat(RESTapi): created new endpoint for clearingrequest and modified existing endpoint payload
* `8cb48cd8` feat(AdminUI):Changed the title of the button in Edit Client modal to 'Update' from 'Edit' and set the validity to 'Days' by default in OauthClient
* `e6a81fe0` Feat (Vulnerability): Improve add/update/delete vulnerability APIs implement add/update/delete vulnerability by GUI
* `e9b035f2` feat(buildsystem): Rearrange dependencies and deployments
* `b4c14975` feat(CRUI):Change CR state 'On Hold' to 'Awaiting Response' & edit PreferredClearingDate
* `35d9e021` feat(docker): Move deps script outside docker build
* `db5176ab` feat(deps): Update shared slim script to have a txt file with libraries
* `4596f06d` feat(SPDX): Use new SPDX library (#1496)
* `d6ba4c07` feat(docker): Improve docker size and build time
* `467edfba` feat(UI):Made the table header collapsable in wherever possible
* `1550e909` feature(ui) : select your group in Project page by grid
* `3b4e36c7` feat(search): allow searching for external ids
* `27869c8a` feat(ProjectUI): Load License info header text based on project group

### Corrections
* `93363bd7` fix(dependencies): Update okhttp and httpclient versions
* `35ea249b` Fix(Vulnerability GUI): Fix bug cannot load vulnerability view page
* `b131a5bc` fix(ProjectUI):Stale data displayed after using the Group filter in Project Advance Search
* `2cd58b9f` Modified the check so that searchQuery is considered when submitSearch is empty
* `01eecf3a` fix(ProjectUI): Changing Project group should update CR
* `4ca47851` fix(REST): Save otherLicenseIds while patching Release - 1735
* `e97c8188` fix(UI): Added new column in exprot spreadsheet in project tab
* `ae77534c` Fix(Obligation): Fix bug can not add/update Admin Obligation and import OSADL
* `06b741b0` fix(SPDX): import SPDX licenses with new SPDX library (tools-java 1.0.4)
* `5d86c067` fix(moderation_request): Added a check that if documentId is null then ignore
* `ac308a5d` upd(CI/CD): Build and test only during the PR.
* `7da2858a` upd(docker): Fix wrong branch
* `42cce1a6` upd(docker): Publish push to main commits
* `d0432233` fix(script): Script to remove trailing and leading whitespaces from component names
* `4f7fd085` fix(ProjectUI): Multiple alerts when there are same linked projects
* `f6c22e52` fix(PreferencesUI):Read Access has to be checked before Generating token
* `920d1281` fix(docker): Deploy libraries in correct place
* `1564ab79` upd(doc): Update docker documentation related to redirects
* `5c9e7845` upd(docker): Improve docker build and github actions
* `7bcb75db` upd(docker): Improve github actions pipeline
* `03e665ec` fix(docker): add missing dependencies
* `b679b883` fix(UI): Unresponsive UI & top align session message
* `68f171f5` fix(UI): Added code to show the project list in component tab
* `8312a8e6` fix(UI): Text field is blank while ExportSpread in licenses
* `05b9c5f0` fix(User): CountryId does not exists while creating user with new Organization
* `e0059eec` upd(docker): Push sw360 docker image to registry
* `429b6b73` fix(UI): Default behaviour of write access checkbox restore
* `5ffcda69` fix(Project and Component UI): Formatting issues and the type of files that can be uploaded in Import SBOM MOdal are limited to rdf now-783
* `32ea05fe` upd(buildsystem): Move away build-configuration
* `8c09cfa1` upd(deps): Update jackson versions
* `b7757326` Fix(ProjectUI): Fix bug when editing obligations in a project.
* `46e2b73d` fix(CouchDbView): Improve couchdb view performance
* `282298e0` fix(Docs): Fixed REST and MkDocs generated issue
* `9a1dcb48` fix(ecc): Reset Ecc Fields when Component type is changed.
* `a5ece957` upd(sanitize): Remove lib prefix from datahandler
* `ca8b2efc` "fix(rest): Added code for to Update the REST-API documentation for Definition of Manufacturer on project level
* `14103917` fix(ComponentUI):HTML encoded character in Vendor field
* `01448d74` fix(scripts): Sanitize scripts
* `13753dbf` upd(ghactions): Fail fast with the license checker without setting a full blown system
* `b365744e` fix(bnd): Restore original bundle
* `8682aa42` fix(docker): Dependencies need to be deployed
* `aa4b625e` upd(docker): Move versions to separate file and update dep script
* `9d3e9b3f` fix(versions): Update commons lang to correct last version
* `7ee69887` fix(SBOM): Fixed Component type is not being set when components are created by importing SBOM
* `db359094` fix(ecc): Script to change ECC status in Release
* `daa15a90` upd(thrift): Use only provided tarball to generate resources
* `932987bc` fix(maven): Update commons-logging to equal versions
* `d9f594ec` fix(maven): Update commons-codec to equal versions
* `41450708` fix(liferay): Use unique versions for same dependencies
* `5acd4ecb` fix(maven): Use unique versions for same dependencies
* `de429b3f` bug(docker): Fix share location of jar files
* `5e0a30cd` fix(ui): Fixed lar file to add missing widgets(Oauth Client & License Types)
* `adb4f930` fix(ecc): Script to cleanup ECC information in release
* `d0ead7d1` fix(rest): Added component type tag in release api
* `f0f308e4` upd(maven): Update maven build infra
* `2db4244f` fix(UI): Do not copy specific external id while cloning release
* `b8190e25` fix(UI): Disable write access from UI
* `9f5e1ddd` fix(CrUi): fix the critical CR creation issue
* `a6f8fa65` fix(ProjectUI): fixed Release filter bug in AttachmentUsage tab
* `77e0ec1d` fix(ui): Generate portlet X url inside portlet Y
* `33908857` fix(report): Nullpointer downloading report
* `e1dd21fc` fix(jenkins): Update old eclipse jarsigner
* `f35c6244` fix(deps): Fixed wrong dependency download
* `7ba948c4` fix(docker): Fix double called shutdown script
* `d2d8011f` bug(docker): Fix invalid commited docker props
* `9cddc708` upd(Docker): Upgrade docker and versions for new Liferay
* `3a0d8c38` fix(AdminUI): Prevent license type duplication with case insensitive check
* `17a82169` fix(ui): cannot link Component with closed project
* `6d0a20ef` fix(REST): fixed release update issue for releases with invalid licenses

### Infrastructure
* `a2b75597` fix(doc) : update migration Readme
* `b7048928` upd(README): Update with new information
* `e130c068` chore(deps): bump spring-security-core in /frontend/sw360-portlet
* `6b8c6e7d` Update githubactions.yml
* `8602a169` WIP
* `e7e9858f` chores(liferay): updated liferay kernel and theme
* `9e64374c` chores(upgrade): Updated default country Id of liferay
* `f19f0203` chores(upgrade): Fixed the ui issues
* `71145b2a` chores(upgrade): Updated default country Id of liferay
* `a7fd29d7` chores(upgrade): Fixed the ui issues
* `822597c2` Updated versions in bnd file according to Liferay 7.4.3.18 GA18
* `9efff9ff` chores(upgrade): Upgrade Liferay to 7.4.3.18 GA18
* `32bc4839` chore(rel): Changing back to 16.1.0-SNAPSHOT

## sw360-16.0.0-M1
This tag covers many corrections/bug fixes after the 15.0 release.

This release provides features, muliple bug fixes for release 15.0, for example, new REST endpoints, new integration test suite.

### Migrations

For existing installations, a data migration is required. Please go to the readme file in scripts/migrations to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the DRYRUN variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
Abdul Kapti <abdul.kapti@siemens-healthineers.com>
Alberto Pianon <alberto@pianon.eu>
Anupam Ghosh <anupam.ghosh@siemens.com>
dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
Helio Chissini de Castro <helio.chissini-de-castro@bmw.de>
hoangnt2 <hoang2.nguyenthai@toshiba.co.jp>
Jaideep Palit <jaideep.palit@siemens.com>
Kouki Hama <kouki1.hama@toshiba.co.jp>
Pham Van Hieu <hieu1.phamvan@toshiba.co.jp>
Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
Tran Vu Quan <quan1.tranvu@toshiba.co.jp>
tuan99123 <tuan2.nguyennhu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features
* `1f6db6db` upd(mockito): Update the deprecated old mockito-all to mockito-core
* `29d019b6` feat(ui): Ability to filter for active users
* `0d0de03c` feat(ReleaseUi):Display AssessmentSummary info from CLi in Release details page
* `709a5ec9` feat(UI): ModifiedOn & MOdifiedBy fields for Project/COmponent/Release
* `73fe7e68` feat(export): Enable mailing for exported spreadsheet for components
* `bbc37a93` feat(ProjectUI): added filter for linked release/projects table
* `a9053df2` feat(ProjectUI): AttachmentUsages - Added option to filter for releases without source attachments
* `f7aebb1e` feat(rest): Add upload description to trigger fossology process
* `26226fbb` feat(exportExcel): Send an email to user with download link once export completed
* `860e420d` feat(exportExcel): Generate and save excel to file system, Download generated file with token
* `07b54e93` feat(UI): Display Licenses from Scanner findings in ISR attachments
* `9511adb7` feat(obligation): add function Edit/Duplicate/Changelog for Obligation
* `830f463a` feat(ui) : Strengthen sw360 admin privileges about Read and Write
* `7dd31343` feat(compose): Common network adn Fossology decoupling
* `5974152f` feat(ProjectUI): Disabled CR based on project Group
* `0f2e4c14` feat(rest): Get Project Vulnerability by external id and release id
* `3dfe2bbc` feat(projectUi): Update some fields in a Project in closed state
* `440a6fda` feat(docker): Overhaul SW360 docker
* `0dc962d0` feat(script): Addition to update project field starting with some value
* `e5516c21` feature(docker): Run sw360 as non-priv user
* `cec73056` feature(docker): Use volumes with tomcat
* `33481c32` feature(docker): Add fossology on the mix
* `4036a822` feat(project): Added  vendor for project

### Corrections
* `00271e79` Fix (Component): Fix bug component list sorting
* `3eb27362` fix(closedproject): Fixed issue w.r.t. editing close project
* `8911a4c4` fix(project): Added write permissions for closed project
* `1bef35d3` update(ghactions): Improve gh actions process
* `bcdfad6b` update(docker): Docker to use latest Ubuntu LTS
* `728acb20` fix(export): Added missing ECC AL column and release vendor in project export
* `8efc4871` fix(rest): Added release main licenses in the response
* `5f5bca8a` fix(ISR):Fixed source file not found in ISR & Total files count mismatch
* `b4f0b870` Fix (Release): Fixed vulnerability can't be deleted when it is linked with a deleted release
* `f8052466` fix(UI): fix Some long sentence can't show property in License Obligation
* `8ead75c3` fix(ui): Display url, email, text  of Additional Data for Component and Release
* `bafd477f` fix(CR-UI): fixed the count mismatch in Open Components column of CR table
* `e776a969` fix(excel-export): fixed project filter issue while exporting excel
* `bcc2d89c` fix(Obligation): Save Admin Level Obligation based on Obligation topic
* `1bec6af2` fix bug Invalid GitHub action #1519
* `9bc9b9bb` Fix(License): Fix bug one license cound add only 10 obligations
* `4b7197b4` Fix(REST): fix visibility of Project Rest API
* `aef08989` fix(docker): Add better proxy documentation to docker-compose
* `534ee6f7` fix(ui): Fixed Obligation count in project view
* `cac1b13e` fix(thrift): Updated thrift configuration to adopt configurable max message size and max framesize
* `2fab647b` typo in the docker run command
* `8d1ddfc3` fix(compoent-visibility): Moderation request for clearing admin
* `d92ecace` fix(ui) : modify translation for search function
* `3792db20` fix(ModerationRequestUI): Fixed project Moderation Request UI is not loading
* `1c0dd050` fix(Dockerfile): Make Dockerfile more consistent
* `a8c2334e` fix(merge): Optimized code to check for write permission of release and components before starting to merge
* `9bbb49ba` fix(modReq): Fixed moderation request for release with version overwrite
* `d1fd4307` fix(ReleaseClearingState): ClearingState not changing to New from Scan Available
* `cbec94a4` fix(api): Correct the ECC status when release is created by API
* `f0f9ff62` fix(docker): Added missing license
* `6fb1f415` fix(docker): Add Document Library as volume to enable keep custom settings
* `fde1f460` fix(docker): Add proper missing clucene config
* `b719f989` fix(docker): Add better proxy handling
* `11e24172` fix(docker): Get liferay from github releases
* `6bddc2bf` fix(docker): Reduce first bootstrapping
* `5df8eb4a` fix(docker): Update README_DOCKER.md
* `0e917987` fix(docker): Update documentation with CSS issue
* `e1a21e07` fix(docker): Update documentation with CSS issue
* `cfe7e413` fix(docker): Improve documentation and persist porta-ext.properties
* `e335c374` fix(docker): README update and cert ignore for curl
* `ab23d0cc` fix(docker): Thrift builds now under tmpfs
* `ff9409fd` fix(docker): Improve build speed and build layers size
* `5467abf9` Update docker base using Eclipse Temurin
* `681eb0c4` fix(ui): Restrict visibility of each component/release like Project
* `0b06f3ee` fix(ui): Fixed pagination of component list with search params
* `f14298a4` Fix search function with key is empty

### Infrastructure
* `7332bec0` chore(dependencies): spring vulnerbility - cve-2022-22970,cve-2022-22971
* `3efa3a56` (chores): updated README.md and download_dependencies.sh files
* `7541ec8d` chore(deps): bump spring-security-core in /frontend/sw360-portlet
* `a17efda8` chore(deps): bump gson from 2.8.6 to 2.8.9
* `18763b51` chore(deps): bump jackson-databind from 2.11.3 to 2.12.6.1
* `2502b58d` (chores): fix security vulnerabilities
* `ce57d9b5` Update information about port redirection
* `ea798093` Update README_DOCKER with typos fixing
* `a7a75336` chore(rel): Changing back to 15.1.0-SNAPSHOT

## sw360-15.0.0-M1

This tag covers many corrections/bug fixes after the 14.0 release.

This release provides features, muliple bug fixes for release 14.0, for example, new REST endpoints, new integration test suite.

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
Anupam Ghosh <anupam.ghosh@siemens.com>
dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
Gaurav Mishra <gmishx@gmail.com>
He, Albert <albert.he@sap.com>
Jaideep Palit <jaideep.palit@siemens.com>
ravi110336 <kumar.ravindra@siemens.com>
Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features

* `9807d381` feat(ui): Added new Clearing State and Attachment Type
* `77f06a6e` feat(ci): Remove dependency of commonsIO from client
* `be212373` feat(ci): Fixed Attachment test cases
* `bf43f889` feat(ci): Fixed Release test cases
* `790c935f` feat(ci): Fixed component test cases
* `32ae085f` feat(ci): Run Client Integration Test for rest api on DB
* `77f49ec2` feat(ui): Added new column for ECCN in ECC status tab of project details view
* `8ed3c68d` feat(AttachmentTypeUI):Add a new attachment type Security Assessment.
* `2e593adf` feat(client): Added Java Client Apis for vulnerability endpoints.

### Corrections

* `2b562699` fix(ci): Fixed vulnerability IT testcases
* `854c6453` fix(release): Fixed mainline state is empty when creating a release by ui or rest
* `be26f6ca` fix(ci): Fixed Project Client Testcases
* `e06eb192` fix(ci): Fixed License Testcases
* `2261b62f` fix(script): Fixed deployment status check after spring boot updat
* `02ecfe6f` Fix default config not working issue
* `30e404bd` Fix component list sorting error
* `f6337094` fix(rest): Optimize rest api for get project by tag, type, group

### Infrastructure

* `376d5b94` chore(deps): bump log4j-core from 2.17.0 to 2.17.1
* `4fc46d41` chore(deps): bump log4j-core from 2.16.0 to 2.17.0
* `c386b4c6` log4j version upgrade to 2.16.0(log4j-vulnerability)
* `b8ebd682` chore(rel): Changing back to 14.1.0-SNAPSHOT
* `0368ae99` chore(readme): Update release badge to latest


## sw360-14.0.0-M1

This tag covers many corrections/bug fixes after the 13.4 release.

This release provides features, muliple bug fixes for release 13.4, for example, new REST endpoints, new functions in the UI and changelog enable/disable from sw360.properties.

### Migrations

For existing installations, a data migration is required. Please go to the readme file in scripts/migrations to see more information:

https://github.com/eclipse/sw360/blob/master/scripts/migrations/README.md

For running the migrations scripts, you will need python and the couchdb package. Please note that you will need to change manually in the python file: the DRYRUN variable and the couchdb URL (if that is not on localhost or requires password or both).

### Credits

The following github users have contributed to the source code since the last release (in alphabetical order):

```
Abdul Kapti <abdul.kapti@siemens-healthineers.com>
Anupam Ghosh <anupam.ghosh@siemens.com>
Jaideep Palit <jaideep.palit@siemens.com>
Kouki Hama <kouki1.hama@toshiba.co.jp>
Michael C. Jaeger <michael.c.jaeger@siemens.com>
ravi110336 <kumar.ravindra@siemens.com>
Shi Qiu <shi1.qiu@toshiba.co.jp>
Smruti Prakash Sahoo <smruti.sahoo@siemens.com>
Tran Vu Quan <quan1.tranvu@toshiba.co.jp>
```

Please note that also many other persons usually contribute to the project with reviews, testing, documentations, conversations or presentations.

### Features

* `e1923ac3` feat(UI): import OSADL obligation information and update screen of Adding new obligation
* `2b6b9a9d` feat(UI): CLI file clean up assistant
* `3702de56` feat(rest): Added rest api to create duplicate of project
* `8ff2748f` feat(RestAPI):Update the attachment status with the approver/rejecter Name and Group.
* `e3d8122a` feat(ProjectUI): Add new values to Obligation status
* `3bab5e99` feat(ui): Display,update vulnerability for linked projects in project details view
* `8d1f96ff` feat(log): Added output processing of the change log
* `a873ad83` feat(ReleaseUI): License to SourceFile Mapping
* `533ace69` feat(rest): Add Rest API for linking release to release

### Corrections

* `ea72ce63` fix(ui): Fixed redirect page from Release Edit page to Release Details page
* `ce9d9550` fix(changelog):Fixed the file permission issue for sw360 changelog.
* `9ef38314` fix(rest): Change base url of health api from /actuator to /
* `843f1f8d` fix(rest): Get component by name case insensitive
* `96a59335` fix(rest): Create duplicate project clearing state should always be open and not copied
* `fc1f1e39` fix(sw360ChangeLog):Configure the sw360ChangeLog path.
* `d27527d3` fix(docker): Fixed cannot upload attachment more than 1 MB by Rest Api
* `46e6eb18` fix(views): Optimize views for components
* `2e8a9cc8` fix(views): Optimize views for releases
* `21682a3a` fix(views): Optimize views to load large projects
* `65719867` fix(rest): Fixed hateoas link not showing correct protocol
* `0ed91d75` fix(ui): Links in ReadmeOss as HTMl are not rendered properly
* `edeb13d2` fix(ui): fix the bug that attachments usages in project cannot show other line
* `5bff785f` fix(rest): Update project vulnerabilities
* `0202f9df` fix(rest): Fixed projects loading issue in REST
* `62d8887b` fix(UI):Component details not shown for the Security Admin Role.
* `1db9afda` fix(rest): Added new parameter luceneSearch to Get Project List Api, to get project list based on lucene search
* `3305fc6b` fix(Japanese) : Update and modify Japanese translations
* `2f85cf70` fix(projects): Fixed thrift timeout by optimizing projects loading
* `aa8574eb` fix(upgradeVersion): Updated resource server properties for Spring 2.X
* `a0f1861b` fix(upgrade version): fixed the test cases failure issue when generating the rest docs.
* `033d912a` fix(upgradeVersion): Fixed Test case for authorization server with spring boot version upgrade   * Refactored code and removed commented lines
* `71bf74bc` fix(upgradeVersion):Upgrade version.
* `2e98d07d` fix(RestAPI):500 Internal server error from releases API.
* `eb6192bc` fix(ui): Cleanup moderation request on deleting project/release/component
* `57e08173` fix(ui): Changes in External urls in Project are not registered in Moderation Request. Closed Moderation Request doesnot show Proposed changes
* `8b5ffecc` fix(Rest):make SW360 REST API Get Releases by Name Case-Insensitive.
* `97a72951` fix(DBTestsFail): Migrating databasetest.properties to couchdb-test.properties.
* `6c3c51ec` fix(log): Fix indentation issue in source code.
* `4ab50904` fix(MyProjectErrorMessage):update the error message in UI for the project which is not accessible.
* `d2f22b80` fix(ui): Fixed js error while  merge component/release with null additional data
* `9c4d2f0d` fix(rest): Added exception processing for authorization
* `af443442` fix(script): add password and user in couchdb-lucene.ini
* `318d0923` fix(docker):Update couchdb3.1 ubuntu20.04 liferay7.3.4 postgresql12
* `5ec1df6a` fix(ci) added new files to license check script
* `26dc7333` fix(ui): Fixed create/update users with uppercase email or externalid
* `db1c1a97` fix(ui): User should be able to edit group of project


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
