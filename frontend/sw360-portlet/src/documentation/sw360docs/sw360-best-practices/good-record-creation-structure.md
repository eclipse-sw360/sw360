[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# Record Creation
---------------------------

## How to Create (Component) Entries?
In order to have a clean and useful catalogue, data hygiene is very important. The main goal is to have clean component / release datasets that allow for versatile use and seamless integration with other systems (see the Handling of Components above). When creating a component, please consider the following rules:

- What is the name of the vendor, the name of the component and what is the release designator?
- For the Vendor
    - Does a CPE entry exist?
        - Look here: [<span style="color:red">&#8599;</span> https://nvd.nist.gov/cpe.cfm](https://nvd.nist.gov/cpe.cfm) or [<span style="color:red">&#8599;</span> http://scap.nist.gov/specifications/cpe/dictionary.html](http://scap.nist.gov/specifications/cpe/dictionary.html)
        - Use the same writing as found in the CPE dictionary
    - A CPE does not exist?
        - Who is the copyright holder: an organization?
            - Use this organization name without "inc", "Gmbh", etc.
        - A person
            - Look at the CPE dictionaries for example
            - They use first name last name with "_", for example "Wedge_Antilles
- For a component
    - Again, does a CPE entry exist?
    - Separate Component name from release designation
- For a release
    - Do not repeat the component name
    - Use the release designation as provided by the software package
    - Avoid prefixes, such as "version", "v" etc
- For special cases:
    - If you upload a part of a release software package, create a **separate** release for this
    - For example "2.0-MODIFIED"
    - Consider that leaving items out from a software release is actually a modification

## How to Create Vendors
In order to have a vendor record in the sw360, then choosing a name is important. The vendor in SW360 is the real "manufacturer" independently from where you download it.

There are different cases:

1. COTS:

	- Obvious case: use vendor short name in CPE style and long name for the actual company name (Apple Inc. vs. Apple)

	- You could even search for an existing vendor entry in the CPE dictionary to get existing vendor naming rules and use this as short name.

	- Consider the following link: [<span style="color:red">&#8599;</span>  https://nvd.nist.gov/products/cpe/search](https://nvd.nist.gov/products/cpe/search)

	- Vendor is actually entity that is contract partner, but is confusing: for Microsoft products, there could be a Microsoft certified solution partner which is the vendor, this must mapped differently in the SW360.

	- **General rule**: Vendor is meant to be manufacturing party not distributing / delivering party.

2. Freeware

	- Problem is that freeware has an author, but also different "vendors" in terms of where it could be downloaded from. This is difficult because different download Web site may involve different licensing conditions.

3. OSS:

	- Community name, e.g. zlib project for zlib.

	- Or the org name of the github orgname or sourceforge group name

	- Do not use "Github" or "Sourceforge" as vendor

	- However, foundations, publishing the software would be a vendor, e.g. "Apache", "Eclipse"

	- But eclipse has a github organization anyway, for example

	- With single author projects should you take the author name. A "john_doe" from John Doe as short name.

Note that very release has its own vendor. as a consequence:

- There could be a release from one Web page and one release downloaded from another Web page. If there is different licensing or sources involved, this could be a solution.

## Naming a Vendor

Each release of a component has a vendor or community. Having unambiguous vendor names is very helpful for managing 3rd party software components.

Required information:

- **Full name** - The full name of the company, organization or person.
- **Short name** - A good short name, compatible to CPE (see section 8.3)
- **URL** - The URL of the organization or a URL where we can get more information about a person.

### How to find a (good) vendor name?

Some guidelines

- If there is a company (Microsoft, Oracle, Pivotal, etc.) behind the component, that's most probably the right vendor name.
- If there is an well known open source community (Apache, Eclipse, etc.) behind the component, that's is the right vendor name.
- If there is only a single person developing the component, then this is the vendor.
- If there is a GitHub organization name or person name available, use this one.
- **No vendor names are**: 'Open Source Software', 'NuGet Gallery', 'CodePlex', 'Codeguru', 'Stack Overflow', 'CodeProject', etc. as these or only platform, where vendors can offer the projects and these name do not help to identify projects.

### Examples

#### Microsoft

Full name = Microsoft Corporation

Short name = Microsoft

URL = [<span style="color:red">&#8599;</span> www.microsoft.com](https://www.microsoft.com/en-in/)

#### Apache

Full name = Apache Software Foundation

Short name = Apache

URL = [<span style="color:red">&#8599;</span> http://www.apache.org/](http://www.apache.org/)

#### Constantin Titarenko

Full name = Constantin Titarenko

Short name = constantin_titarenko (Note the underscore!)

URL = [<span style="color:red">&#8599;</span> https://github.com/titarenko](https://github.com/titarenko)

## How to determine the CPE?

The Common Platform Enumeration (CPE) is used to have an unambiguous identification of a specific component release. This information is especially needed to find matching security vulnerability information.

### Syntax of a CPE Entry

The syntax of a CPE entry is defined as:

`cpe:<CPE-Version>:<part>:<vendor>:<product>:<version>:<update>:<edition>:<language>`

**CPE-Version** refers to the CPE naming format version. We will always use version 2.3

**part** refers to the type of the component (a = application, o = operating system, h =hardware device)

**vendor** refers to the vendor or author of the component. Only small letters are allowed.

**product** refers to the name of the product. Only small letters are allowed.

**version** refers to the version of the product.

**update** refers to the updates of this specific version

**edition** and **language** can be used to specify more details


Non-existing or unknown party can get replaced by the placeholder '*'.


**Note**: only small letters are allowed. Spaces have to be replaced by underlines '_'.

### Examples

**Microsoft .Net Framework, version 1.0 SP2**

`cpe:2.3:a:microsoft:.net_framework:1.0:sp2:*:*:*:*:*:*`


**Apache ActiveMQ, version 4.0**

`cpe:2.3:a:apache:activemq:4.0:*:*:*:*:*:*:*`


**Apache log4net, version 1.2.9 beta**

`cpe:2.3:a:apache:log4net:1.2.9_beta:*:*:*:*:*:*:*`


**Oracle Java Runtime, version 1.7.0, update 51**

`cpe:2.3:a:oracle:jre:1.7.0:update_51:*:*:*:*:*:*`

