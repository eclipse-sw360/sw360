[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# SW360 User FAQ
----------------
Here we answer some of the most frequently asked questions about SW360:

##### **Q**: Who should be listed as Moderator?

**A**: Moderator are persons who need to review changes done on certain items (project, component, release or attachment) by persons who do not have the user right to actually do these changes. For BT moderators are the persons with the role 'Software Clearing Site Representative'.



##### **Q**: Who should be listed as Contributor?

**A**: By default only the owner (or creator) of an item (project, component, release) is allowed to modify this item. Often it is useful that additional people are allowed to edit an item. These additional people (software architects, developers, additional experts) should get listed as contributors.



##### **Q**: I have changed a project, component, release or attachment, but SW360 does not show the changes?

**A**: It might be that you have tried to change something that needs to be review by someone else. In such cases a so called 'Moderation Request' is generated. A Moderator needs to approve your changes. Go to the Home view an check the box 'My Task Submissions', the project, component, or release should be listed there.



##### **Q**: What should I enter in the field 'Visibility'.

**A**: Visibility controls which group of people is allowed to see a project. The default setting is 'Everyone', i.e. everyone within an organization can see the project and all its releases.



##### **Q**: How can I change the 'Clearing State' of a release?

**A**: There is no direct way to do it. If there is no clearing report available, the clearing state will be 'New'. If a clearing report available it will be 'Clearing report available'. If at least one clearing report has been approved, the clearing state will be 'Approved'.



##### **Q**: I can't find a specific release inside my project – what can I do?

**A**: You can sort each column by clicking on the column name, i.e. you can sort the entries by name, project origin, clearing state, mainline state or project mainline state – normally that helps finding a certain release.



##### **Q**: I can't delete my component called 'Tom's Test Component'.

**A**: Do not use special characters like single or double quotes. To be able to delete such a component or release you'll first have to rename it…



##### **Q**: What is Copyleft Effect?

**A**: **Copyleft** effect is the reverse idea of **copyright**. Goal is that software licensed under such license is always free and can never get a privatized software asset. The user gets the freddom to run, copy, modify and distribute the software, but it is not possible to add any further restrictions. This implies that **modified software** must also be free and becomes available to the community.



##### **Q**: Different Classification of the Open Source Licenses.

**A**: There are hundreds of OSS licenses, the following table will give a biref overview about the most common OSS licenses, the risks and the obligations that need to be fulfilled when using them:

| | License Class | License Name(s) | Risks | Obligations |
| --- | --- | --- | --- | --- |
| <span style="color:white;font-size:2em;">&#9899;</span> | **White Licenses** | MIT, BSD (except for BSD-4-Clause), BSL-1.0, CPOL-1.02, MsPL, zLib, Apache-1.1, Apache-2.0 (if no code changes are done) | **low risk** | Display license text; display copyrights |
| <span style="color:yellow;font-size:2em;">&#9899;</span> | **Yellow Licenses** | CDDL-1.0, CPL-1.0, EPL-1.0, eCos License, MPL, NPL | **medium risk** - because of limited copyleft effect | 	Display license text; display copyrights; all changes of the component code must become OSS as well; possible license incompatibility with red licenses |
| <span style="color:red;font-size:2em;">&#9899;</span> | **Red Licenses** | SleepyCat, Aladdin Free Public License; Berkeley DB liceses | **very high - do not use -** because of nearly unlimited copy left effect | Before thinking about components licensed under these license, get in contact with your software clearing experts! |
| <span style="color:red;font-size:2em;">&#9899;</span> | **Red Licenses** | GPL-2.0, GPL-3.0, LGPL-2.1, LGPL-3.0, AGPL | **high risk** - because of copyleft effect | Display license text; display copyrights; take care about copyleft effect - get in contact with your software clearing experts; all distributions must clearly state that (L)GPL license code is used |
