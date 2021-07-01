[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

#Tasks for Software Clearing Experts
-----------------------

<!---
## Tasks for Experts

###  Find a Project

Step-by-step:

1. Go to 'Projects'

2. Use the 'Advanced Search' (case insensitive prefix)

3. Done

### Add/Modify Export Control Information

Step-by-step:

1. Go to 'Components'

2. Search for the component…

3. Click on the component name

4. Select 'Release Overview'

5. Click on the version number

6. Click 'Edit Release'

7. Select 'ECC Details'

8. Enter/update the ECC Information

9. Click 'Update Release'

10. Done

![Information](ImagesBasic/Information.png) **Note**: changes to the export control information done by other users always requires the approval of one the assigned export control experts. You will find this as a task assigned to you.
--->

## Moderation Requests

Details: [<span style="color:red">&#8599;</span> https://github.com/sw360/sw360portal/wiki/Dev-Moderation-Requests](https://github.com/sw360/sw360portal/wiki/Dev-Moderation-Requests)

The concept of moderation is good for two things:

- To cope with a large number of potential edits on documents

- To allow every user to propose edits.

Allowing every user to edit opposed to propose edits would lead to a large number of changes, potentially, not making everyone happy. As such, the changes should be reviewed by an experienced person and can be then approved.

See [<span style="color:red">&#8599;</span> https://github.com/sw360/sw360portal/wiki/User-Workflows:-sw360](https://github.com/sw360/sw360portal/wiki/User-Workflows:-sw360)

The moderation is the basic way of applying changes if the document is not created by someone else. In sw360 the following person can edit documents right away (without moderation request):

- The creator of a document (document is a project entry, a release entry etc)

- Admins

- Clearing admins

- Moderators of this document

- Other special roles, such as project responsible

If the user who wishes to change a document and is not one of these, the moderator workflow kicks in. Then changes applied to the document are not really applied, but are sent to a moderator. Moderators are:

- The creator of a document (document is a project entry, a release entry etc)

- Admins

- Clearing admins

- Moderators of this document

The moderator can review, approve or decline the request. Then, the requesting user can delete the request. The moderator request workflow is shown below.

## Clearing Requests

There are two ways to create a clearing request.

 - From Clearing Status tab in project details page

 - Second is from the square check-box icon under the Actions column in the projects home page

Enter the required details to create the request. Then the clearing team will confirm on the agreed clearing date.
You can view the Open Clearing Requests under the Request tab.

## Get Clearing State Overview

##### Find all Projects with Open Components

Not yet possible. One idea for the future is that we can sort the projects view by the clearing status field (or parts of this field).

##### Get Clearing State Overview for a Single Project

Step-by-step:

1. Go to 'Projects'

2. Search for the project

3. Click on 'Clearing Status'

4. You'll see a list of all subcomponents, their type (OSS/COTS) and their different states.

##### Get Clearing State Overview for All Project (of a certain organization)

Step-by-step:

1. Go to 'Projects'

2. Apply filter (for organization)

3. Check the 'Clearing Status' column
