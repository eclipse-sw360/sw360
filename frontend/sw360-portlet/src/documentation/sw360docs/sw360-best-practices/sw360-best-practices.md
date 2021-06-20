[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# SW360 Best Practices
-----------------------

## SW360 Usage and Handling of Components
The above mentioned data model has consequences for the usage of SW360:

- If you create a component entry, most likely you will go ahead with a release entry, otherwise, the component stays an empty shell
- Uploading source packages / actual software as attachment makes sense at the release, not at the component
- If you have created a component and release entry, you can go ahead and assign a vendor to a release.

This very clear approach enables a number of issues, please keep the following goals in mind:

- Duplicate entries need to be removed
- Separating vendor from components names and release tags brings clarity to component naming
- Interaction with other systems is a must today. As such we need to support the CPE standard which also implement this 3-parts separation
- Having the clear modeling of data enables better search and filtering abilities of the component catalogue.

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
