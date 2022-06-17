[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# User Management Roles
-------------------

Every user can create records and edit own created records. However, to change records of others, approval is required. Approval in SW360 is a so called moderation request. A moderation request is a set of proposed changed not applied to record immediately, but will be routed to;

- The creator of the record
- The moderators for a record
- The clearing admins of the same group in SW360.

Then, the proposed changes can be approved by them.

## General SW360 Roles and Access

There are two main types of roles. The first type are general roles on the system that apply in the default case:

1. **User** - A user is the default, in order to apply modifications, a user can pose moderation requests, except for the data items that a user has created.
2. **Clearing Expert** - Member of the clearing team. Has the rights to work on the projects of the own group and to edit licenses. Can also work on clearing requests.
3. **Clearing Admin** - A clearing admin has the rights to work on the projects of the own group and to edit licenses.
4. **ECC Admin** - The only users who can edit (or approve as moderation request) ECC classifications.
5. **Secuirty Admin** - The only users to edit relevance for security vulnerabilities.
6. **SW360 Admin** - An admin has full rights on all (visible!) data items. Can elevate permissions of other users.

In addition there are ACL-style roles, meaning that per data item access settings can be made:

1. **Creator** - A creator can modify in addition to the user's read abilities, a user can be creator of a data item.
2. **Moderator** - A creator can define moderators for a data item. Moderators can change a data item as a creator can.
3. **Contributor** (Component) - Is a contributor to a component, project, similar (but not the same) to a moderator. In addition to moderator, this role has been added to identify contributors (or that contributors get the fame). In contrast, the contributor cannot delete data items.
4. **Project Owner** - A user who owns the project.
5. **Lead Architect** (Project) - Is a contributor, just named differently to identify the responsible person. an architect refers to the person who has that role of the project or product. This role has been added to identify architects to have a contact person for technical questions.
6. **Project Responsible** (Project) - Is a contributor, just named differently to identify the responsible person.
7. **Security Responsible** - Users responsible for the security of the project.

