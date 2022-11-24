[![Eclipse Public License 2.0](https://img.shields.io/badge/license-EPL--2.0-green.svg "Eclipse Public License 2.0")](LICENSE)
[![Build Status](https://travis-ci.org/eclipse/sw360.svg?branch=master)](https://travis-ci.org/eclipse/sw360)
[![Slack Channel](https://img.shields.io/badge/slack-sw360chat-blue.svg?longCache=true&logo=slack)](https://join.slack.com/t/sw360chat/shared_invite/enQtNzg5NDQxMTQyNjA5LThiMjBlNTRmOWI0ZjJhYjc0OTk3ODM4MjBmOGRhMWRmN2QzOGVmMzQwYzAzN2JkMmVkZTI1ZjRhNmJlNTY4ZGI)
[![Changelog](https://badgen.net/badge/changelog/%E2%98%85/blue)](https://github.com/eclipse/sw360/blob/master/CHANGELOG.md)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/eclipse/sw360)](https://github.com/eclipse/sw360/releases/latest)

### SW360 Portal

A software component catalogue application.

SW360 is a server with a REST interface and a Liferay CE portal application
to maintain your projects / products and the software components within.

It can manage SPDX files for maintaining the license conditions and maintain
license information.

### Introduction

It is comprised of one frontend (portal) part, backend (services) part and additionally a REST API:

* Frontend: Liferay-CE-(Tomcat-)based portal application using portlets.
* Backend: Tomcat-based thrift services for being called by different applications.
* Database: we store software components and metadata about them in CouchDB.
* Rest: this REST API provides access to project resources for external integration.

The reference platform is the Ubuntu server 22.04 (which is an LTS version).

### Project structure

This is a multi module maven file. please consider that we have the following modules:

* frontend: For portlets, themes and layouts, the liferay part.
* backend: For the thrift based services.
* libraries: For general stuff that is reused among the above, for example, couchdb access.
* scripts: Auxiliary scripts to help build, deploy and config system
* rest: For the REST API which contains an authorization and resource server.

### Issues

If you run in any issues with documentation or software, please be kind and report to our [Github issues area](https://github.com/eclipse/sw360/issues).

### Deployment

Is recommended using the docker based setup, [described here](https://github.com/eclipse/sw360/blob/main/README_DOCKER.md).

If you intend to install in a bare metal machine or use in your own virtualizaed system, [bare metal instructions are provided here](https://www.eclipse.org/sw360/docs/deployment/baremetal/deploy-natively/).


#### Compiling, testing

Please refer to [SW360 main documentation website](https://www.eclipse.org/sw360/docs/).


### License

SPDX-License-Identifier: EPL-2.0

This program and the accompanying materials are made
available under the terms of the Eclipse Public License 2.0
which is available at https://www.eclipse.org/legal/epl-2.0/
