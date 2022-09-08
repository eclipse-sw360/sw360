[//]: # (Copyright Bosch.IO GmbH 2020)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)
# SW360 Data Model

The sw360 client is capable of interacting with your instance of [SW360](https://www.eclipse.org/sw360/).

Not all data that is represented in SW360 is captured and mapped by the sw360 client. 
In the following section there is an outline what data of SW360 the client maps.

#[[###]]# Projects
  
  | Variable Name            | Meaning                                                                    |
  |--------------------------|----------------------------------------------------------------------------|
  | Type                     | the name of the component                                                  |
  | Name                     | the name of the project                                                    |
  | Version                  | Current version of the project                                             |
  | Project Type             | What type of project. E.g. "Product"                                       |
  | Description              | Description of the project. E.g what it does and who is responsible for it.|
  | Created On               | Date of creation in SW360 instance                                         |
  | Business Unit            | Business Unit                                                              |
  | Clearing Team            | Clearing Team                                                              |
  | Visibility               | Which roles can see the project                                            |
  | Release Relation Network | Present dependencies of project and releases                               |
  | External Ids             | Additional information can be put here in a `Map<String,String>` format    |

#[[###]]# Licenses
 All licenses are stored separately from the components and releases.

  | Variable Name         | Meaning                       |
  |-----------------------|-------------------------------|
  | Text | The text of the license |
  | Short Name | The short name of the license. E.g. `Apache-2.0`, if possible the SPDX identifier |
  | Full Name | The full, long name of the license. E.g. `Apache Software License 2.0` |
 
#[[###]]# Components
 One component is an individual component with no version.
 The individual versions of a component are called "releases" and are saved as a different object.
 Each component has a list of its releases.

 | Variable Name         | Meaning                       |
 |-----------------------|-------------------------------|
 | Name | the name of the component E.g. Junit:Junit stored in the `Name` field. |
 | Component Type| Can be `Internal`, meaning it is proprietary, or `OSS`, meaning it is open source. |
 | Created On| Date of creation in SW360 instance |
 | Homepage| Link to the homepage of the OSS Component stored in the `Homepage` field. |
 
#[[###]]# Releases
 An individual release is a release of a component, meaning it always has a version and always has a component it belongs to.
 All known license and copyright information can be found here.

  | Variable Name         | Meaning                       |
  |-----------------------|-------------------------------|
  | Component ID | ID of the component this release belongs to. E.g. The ID of the Junit component. |
  | Name | Name of the release, which should equal the component name stored in the `Name` field. |
  | Version | Version of the release stored in the `Version` field. |
  | Download Url | Local link to the source archive stored in the `Download URL` field. I.e., the link to the local storage where the sources of the release are stored. |
  | Main License IDs | IDs of the final licenses in the `Main License Ids` field. The IDs have been extracted from the license information below and link to licenses in the License database. |
  | Purls | These are the corresponding release coordinates that are saved under an external ID as purls. It is a list of key-value pairs of Strings, with the purl type as key, e.g., `maven`, and the purl as value. There can be multiple coordinates. |
  | Overwritten License | The manually defined license of the release in a SPDX license expression represented as String in the additional data with `overwritten_license` as key. |
  | Declared License | The declared licenses of the release in a SPDX license expression represented as String in the additional data with `declared_license` as key. |
  | Observed License | The obsserved licenses of the release in a SPDX license expression  represented as String in the additional data with `observed_license` as key. |
  | Release Tag Url | This is a link to the OSS repos tag of the release used stored as external id with key `orig_repo`. |
  | Software Heritage ID | A release ID in software heritage in the format "swh:1:rel:*", stored as external id with key `swh`. |
  | Copyrights | Copyrights of the release, given in a String. Individual copyrights are separated by line. Stored in the additional data section with key `copyrights` |
  | Change Status | An enumeration with values `CHANGED` or `AS_IS` stored as additional data with key `change_status`. A changed component release should be stored as own release with an appendix to the original version, e.g. `1.2.3_modified_xyz`. Currently the implementation can only handle `AS_IS` components. |
  | Homeapge Url | Homepage of the release that is also one of the component, given in a String. Stored in the additional data section with key `homepage` |
  | Clearing State | State reflecting the clearing state of a curation processes with the values: `INITIAL`, `WORK_IN_PROGRESS`, `EXTERNAL_SOURCE`, `AUTO_EXTRACT`, `PROJECT_APPROVED`, `OSM_APPROVED`. This is stored in the additional data field with the key `clearingState`. The clearing states come with an additional boolean value, indicating whether new updates are allowed to overwrite existing data. This value is `false` for the states `OSM_APPROVED` and `PROJECT_APPROVED`.|
  | SW360 Clearing State | Can not be set with any analyzer, but is a reflection of the clearing state from the SW360 instance data model itself. The SW360 instance clearing state values are `NEW_CLEARING`, `SENT_TO_CLEARING_TOOL`, `REPORT_AVAILABLE`, `APPROVED`. This state is reflected in the SW360 instances UI in the `Clearing State`. |

#[[###]]## Used Attachments
 Attachments are used to store additional elements in files, like sources and known binaries.

  | Attachment Type       | Meaning                       |
  |-----------------------|-------------------------------|
  | Source File | A zipped folder with the sources of the component. Typically uploaded by the SW360 Uploader |
  | Binary | A known binary of the component, stores also e.g. the hash that can be used to search for the metadata based on a hash. Typically uploaded by the SW360 Uploader |
  | Clearing Report | A file with curated data and the approval state. If the status of this attachment is `Accepted`, the Clearing State of the component release is `Approved`. Typically uploaded by compliance office tooling. |
  
**Note**: Setting the `Clearing State` to `WORK_IN_PROGRESS` results in only updating release information. No attachment
is uploaded.

**Note**:
All string fields in this data model have a limit of 2147483647 - 1 in length, since this is the frame size of the binary protocol used with thrift.

**Note**:
In the current implementation, SW360 automatically sets the ECC (Export Control & Customs) status of a release that 
is to be updated or created to `APPROVED` if the following conditions are fulfilled:
* The release contains a valid `Download URL`, i.e., the URL has a correct format. It does not necessarily need to be 
reachable.
* The`Component Type` is of `OSS`. This is the default if not set otherwise.
* The SW360 user has sufficient writing rights. In terms of SW360 user groups, these are `ADMIN`, `CLEARING_ADMIN`, and
`CLEARING_EXPERT`. Alternatively, this holds also true if the release's `contributor` field (on the SW360 side)
contains the SW360 user's email.
