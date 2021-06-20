[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# SW360 Introduction
--------------------

## Overview
**SW360** is a software catalogue application designed to provide a central place for sharing information about software components used by an organization. It is designed to neatly integrate into existing infrastructures related to the management of software artifacts and projects by providing separate backend services for distinct tasks and a set of portlets to access these services. A complete deployment unit exists (vagrant box or docker container) that contains a complete configuration of all services and portlets.

SW360 comprises the following main use case areas:

- Component: Handling of information and processes related to components, e.g. name, vendor
- License: Handling of information regarding licenses, e.g. obligations, license texts etc
- Project: handling of project information providing a context for the use of components
- Vulnerability: Collecting Security Vulnerability Management Information and matching them with components stored in the component service

## Functionality
The SW360 is a software catalogue application which is:

- Based on the Open Source Liferay portal server
- Integrated with Fossology

 With SW360, you can

- Manage your components and projects
- Send source packages to the clearing tool Fossology
- Reuse cleared components and releases for your project
- Import cleared components with clearing reports and other documents
- Browse licenses and their obligations

In order to work with SW360, please note a fundamental setup in the data model when dealing with components:

- A component is a list of releases with metadata. vice versa, releases refer to a component.
- A vendor is separate from a component and releases. The link to the vendor is set at the release. (think of Sun and Oracle)
- A project refers to a number of releases of components accordingly, not components.

The following picture outlines these decisions:

![sw360 datamodel](ImagesIntro/sw360 datamodel.png)

## Important Links

| Item | URL | Remark |
| --- | --- | --- |
| Public Project Homepage | [<span style="color:red">&#8599;</span> https://www.eclipse.org/sw360/](https://www.eclipse.org/sw360/) | Main project homepage |
| Public Project GitHub Pages | [<span style="color:red">&#8599;</span> https://github.com/eclipse/sw360/](https://github.com/eclipse/sw360/) | Main project |
| Project Project Information at Eclipse | [<span style="color:red">&#8599;</span> https://projects.eclipse.org/projects/technology.sw360](https://projects.eclipse.org/projects/technology.sw360) | Some background info about Eclipse sw360 |
| Public Project Homepage SW360Antenna | [<span style="color:red">&#8599;</span> https://github.com/eclipse/antenna](https://github.com/eclipse/antenna) | Antenna connects with sw360 to exchange information right from build time |
| Public Project sw360vagrant at GitHub | [<span style="color:red">&#8599;</span> https://github.com/sw360/sw360vagrant](https://github.com/sw360/sw360vagrant) | Vagrant setup for sw360 |
| Public Project sw360chores at GitHub | [<span style="color:red">&#8599;</span> https://github.com/sw360/sw360chores](https://github.com/sw360/sw360chores) | Docker setup for sw360 |
| Public Project sw360slides at GitHub | [<span style="color:red">&#8599;</span> https://github.com/sw360/sw360slides](https://github.com/sw360/sw360slides) | Main slide deck of sw360 published as git repository |





<!---

# Table of Contents

1. [SW360 Introduction](SW360 Introduction/SW360 Introduction.md)
2. [User Guide](User Guide/User Guide.md)
3. [Administrative Guide](Administrative Guide/Administrative Guide.md)
4. [Best Practices](SW360 Best Practices/SW360 Best Practices.md)
5. [RESTful API](SW360 RESTful API/SW360 RESTful API.md)
6. [FAQ](SW360 User FAQ/SW360 User FAQ.md)

--->