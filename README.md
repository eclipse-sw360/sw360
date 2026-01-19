# SW360

[![Website](https://img.shields.io/badge/website-SW360-blue)](https://eclipse.dev/sw360/)

SW360 is a software component catalogue application designed to provide a central hub for managing software components and their metadata.

Visit the [official project homepage](https://eclipse.dev/sw360/) for more information.

<img width="1280" alt="homeImage" src="https://github.com/user-attachments/assets/3c2e6712-97a7-4637-80b5-915cdd3af1e8" />
<br></br>


[![Eclipse Public License 2.0](https://img.shields.io/badge/license-EPL--2.0-green.svg "Eclipse Public License 2.0")](LICENSE)
[![SW360 Build and Test](https://github.com/eclipse-sw360/sw360/workflows/SW360%20Build%20and%20Test/badge.svg)](https://github.com/eclipse-sw360/sw360/actions?query=workflow:"SW360+Build+and+Test")
[![Slack Channel](https://img.shields.io/badge/slack-sw360chat-blue.svg?longCache=true&logo=slack)](https://join.slack.com/t/sw360chat/shared_invite/enQtNzg5NDQxMTQyNjA5LThiMjBlNTRmOWI0ZjJhYjc0OTk3ODM4MjBmOGRhMWRmN2QzOGVmMzQwYzAzN2JkMmVkZTI1ZjRhNmJlNTY4ZGI)
[![Changelog](https://badgen.net/badge/changelog/%E2%98%85/blue)](https://github.com/eclipse/sw360/blob/master/CHANGELOG.md)


[![GitHub release (latest by date)](https://img.shields.io/github/v/release/eclipse/sw360)](https://github.com/eclipse/sw360/releases/latest)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/9485/badge)](https://www.bestpractices.dev/projects/9485)

### SW360 Portal

A software component catalogue application.

SW360 is a Backend server with a REST API to maintain your projects / products and the software components within.

It can manage SPDX files for maintaining the license conditions and maintain license information.

### Introduction

It is comprised of one frontend (portal) part, backend (services) part and additionally a REST API:

* Backend: Tomcat-based thrift services for being called by different applications.
* Database: we store software components and metadata about them in CouchDB.
* Rest: this REST API provides access to project resources for external integration.

The reference platform is the Ubuntu server 22.04 (which is an LTS version).

### Project structure

This is a multi module maven file. please consider that we have the following modules:

* backend: For the thrift based services.
* libraries: For general stuff that is reused among the above, for example, couchdb access.
* scripts: Auxiliary scripts to help build, deploy and config system
* rest: For the REST API which contains an authorization and resource server.

### Issues

If you run in any issues with documentation or software, please be kind and report to our
[GitHub issues area](https://github.com/eclipse/sw360/issues).

### Deployment

It is recommended to use the Docker-based setup,
[described here](https://github.com/eclipse/sw360/blob/main/README_DOCKER.md).

If you intend to install in a bare metal machine or use in your own virtualized system, [bare metal instructions are provided here](https://www.eclipse.org/sw360/docs/deployment/baremetal/deploy-natively/).

### Development

If you intend to develop over SW360, few steps are needed as equal you need have base
requirements

* Base build requirements
  * Java 21
  * Maven 3.8.7
  * pre-commit
  * thrift 0.20.0 runtime
  * Python environment ( to [pre-commit](https://pre-commit.com/) ) - SW360 use Eclipse formatting rules
  through [Spotless maven plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven)

If you can't install thrift 0.20 runtime, you will need the following requirements:

* C++ dev environment
* cmake
Then run the current build script:

```bash
./scripts/install-thrift.sh
```

#### Local Building

**Step 1**: Prepare source code

```bash
git clone https://github.com/eclipse-sw360/sw360.git
cd sw360
pip install pre-commit
pre-commit install
```

**Step 2**: Build the code

```bash
mvn package -P deploy \
    -Dhelp-docs=false \
    -DskipTests \
    -Djars.deploy.dir=deploy \
    -Drest.deploy.dir=webapps \
    -Dbackend.deploy.dir=webapps
```

If you want to run the tests, we need start a local couchdb server and Docker is required:

### License

SPDX-License-Identifier: EPL-2.0

This program and the accompanying materials are made
available under the terms of the Eclipse Public License 2.0
which is available at [https://www.eclipse.org/legal/epl-2.0/](https://www.eclipse.org/legal/epl-2.0/)
