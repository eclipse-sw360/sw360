[//]: # (Copyright Siemens AG, 2021. Part of the SW360 Portal Project)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)

# SW360 RESTful API
-------------------

## Introduction
Using the Web interface makes sense for some use cases, for some other cases the tool integration is more useful. The SW360 software offers a RESTful API. It has been initially developed by a colleague of the BT division - an excellent example of how Inner Source works for projects. Now it has been integrated to the official main project as component that can be deployed along with a SW360 solution.

## Methods of Authentication

1. OAuth workflow involving consumer / client secret and user token using user name and password from LDAP / Exchange accounts (very early)
2. Access key obtained in the SW360 UI
3. OAuth workflow involving consumer token / client secret and signed Java Web Tokens involving user authentication from OpenID Connect service for the first token and then using the OAuth refresh tokens.

API Documentation is available on the instances deployed:

- `https://<my_sw360_server>/resource/docs/api-guide.html`

## Brief Specs
| | |
| --- | --- |
| Implementation Technology	| Java-based Spring-framework based |
| REST Flavor |	Hypermedia-driven |
| Authentication | Now: Token by user token store. Previously: Spring Security using JWT and SW360 user management. Note that technically, both ways are possible |
|  More Technical Information | [<span style="color:red">&#8599;</span> https://github.com/eclipse/sw360/wiki/Dev-REST-API](https://github.com/eclipse/sw360/wiki/Dev-REST-API) |
