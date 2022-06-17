[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# Properties
---------------------------

**Frontend Properties**: All the sw360 frontend properties are mentioned in [sw360.properties](https://github.com/eclipse/sw360/blob/master/frontend/sw360-portlet/src/main/resources/sw360.properties) file.
For example;

 - Different categories for components,

    component.categories=[ "framework", "SDK", "big-data", "build-management", "cloud", "content", "database", "graphics", "http", "javaee", "library", "mail", "mobile", "network-client", "network-server", "osgi", "security", "testing", "virtual-machine", "web-framework", "xml"]

 - Dropdown for project type,

    project.type=[ "Customer Project", "Internal Project", "Product", "Service", "Inner Source" ]

 - API Token generation,

    rest.apitoken.generator.enable=false

 - Activation of portlets and components

**Backend Properties**: This, [sw360.properties](https://github.com/eclipse/sw360/blob/master/backend/src-common/src/main/resources/sw360.properties) file contains the sw360 backend properties. This file contains the common properties for the backend services and also holds the setting for the mail utility.

You can change these default values by mentioning it in the sw360.properties file, present in /etc/sw360 folder. This path is to be created by the admin. After changing the properties, server needs to be restarted in order to make the changes effective. If the properties file is not present in the required folder, the default values will be selected.