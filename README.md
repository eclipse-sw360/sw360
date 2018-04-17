[![Stories in Ready](https://badge.waffle.io/sw360/sw360portal.png?label=ready&title=Ready)](https://waffle.io/sw360/sw360portal)

[![Build Status](https://travis-ci.org/sw360/sw360portal.svg?branch=master)](https://travis-ci.org/sw360/sw360portal)

### sw360portal

A software component catalogue application - designed to work with FOSSology.

SW360 is a liferay portal application to maintain your projects / products and
the software components within. It can send files to the open source
license scanner FOSSology for checking the license conditions and 
maintain license information.

### Introduction

It is comprised of one frontend (portal) part, backend (services) part and additionally a REST API:

* Frontend: Liferay-(Tomcat-)based portal application using the Alloy UI framework.
* Backend: Tomcat-based thrift services for being called by different applications.
* Database: we store software components and metadata about them in couchdb.
* Rest: this REST API provides access to project resources for external clients.
        Please note the state of the REST API is experimental and its may exposed by breaking changes.

The reference platform is the Ubuntu server 14.04 (which is a LTS version). However, it
runs well on other OSes (see below).

### Project structure

This is a multi module maven file. please consider that we have the following modules:

* frontend: for portlets, themes and layouts, the liferay part.
* backend: for the thrift based services.
* libraries: for general stuff that is reused among the above, for example, couchdb access.
* importers: for provisioning tasks.
* scripts: for deploying either inside the vagrant or on your development machine.
* rest: for the REST API which contains an authorization and resource server.

### Required software

* Java 1.8.X
* CouchDB, at least 1.5
* Liferay Portal CE 6.2 GA5
* Apache Tomcat 7.0.X or 8.0.X

In order to build you will need:

* A git client
* Apache Maven 3.0.X
* Apache Thrift 0.9.3

http://maven.apache.org/download.html#Installation

Then, you must install Apache Tomcat, CouchDB. And, Java of course.

The software is tested with

* Maven 3.0.4 / 3.0.5
* Apache Tomcat 8.0.26 / 7.0.54 / 7.0.61
* Liferay GA5
* CouchDB 1.5 / 1.5.1
* OpenJDK Java 1.8.0_45 (64-bit) 
* Tested with windows 7 SP1, ubuntu 14.04, macosx 10.8, 10.9 10.10
* We run Liferay with PostgreSQL 9.3, but HSQL (as of the bundle) runs also OK.

### PROBLEMS

Running with the tested software shows no problems if you encounter some please report them at https://github.com/siemens/sw360portal/issues.


### Deployment

There is a vagrant project for one-step-deployment. See the project wiki for details.

Apart from the vagrant way, the software can be deployed using the provided scripts.

### Commands
Most commands are using maven which is a dependency to build SW360. Additionally
there is rake-support which wraps parts of maven and adds docker support for
compilation as well as fpm support for building **.deb** and **.rpm** packages.

#### Compiling, testing and deploying

Actually, there is a hierarchy of maven files, in general

1. to clean everything up
  - `mvn clean`

2. to run all targets including build the .war file at the end
  - using maven: `mvn install`
  - using rake wrapper around maven: `MAVEN_PARAMETERS="" rake compile`

  this needs a couchdb running on the host on port 5984

3. to install without running the tests
  - using maven: `mvn install -DskipTests`
  - using rake wrapper around maven: `rake compile`

For deployment run the command
```
mvn install -Pdeploy
```
which copies the war files to the liferay auto deploy folder (if `LIFERAY_PATH` is set).
Otherwise one has to specify the absolute path to the deploy folder in the following way:
```
mvn install -Pdeploy \
    -Ddeploy.dir=/ABSOLUTE/PATH/TO/DEPLOY/FOLDER
```
It is even better to also pass the path to the webapps folder, thus allowing maven to deploy the backend services directly via the native tomcat hot deploy mechanism.
This is done in the following way:
```
mvn install -Pdeploy \
    -Ddeploy.dir=/ABSOLUTE/PATH/TO/DEPLOY/FOLDER \
    -Dwebapps.dir=/ABSOLUTE/PATH/TO/WEBAPPS/FOLDER
```
  
#### Packaging
The packaging mechanisms are able to produce **.deb**, **.rpm** and **.tar.gz**
packages for the war files of SW360, which will be deployed to the tomcat
containing SW360. These packages can be created via the command
```
rake package
```
which will compile SW360 and use fpm to create the packages which will be placed
in this folder.
By default this will be done within an docker container and this behaviour can
be modified using the environmental variable `DOCKERIZE`.

If one, for example, wants to build only the debian package without
dockerization, one could use
```
DOCKERIZE=false rake package:deb
```

**Note:** the debian and rpm packages depend on the package `sw360_dependencies`
which has to be built somewhere else.

### Liferay Configuration

You should provide below property configuration based on his/her liferay deployment
environment as found in the master pom.xml file.

Please note that you should run the Liferay installation procedures as found on the
Liferay documentation.

### War file packaging

As backend services are supposedly being deployed in an application Server.
So to avoid conflicts for servlets api (in case of tomcat, tomcat-servlet-api-x.x.x-jar)
are excluded from the WAR file while packaging. Using below configuration,

```
            <plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-war-plugin</artifactId>
				<version>2.1.1</version>
				<configuration>
					<webResources>
						<resource>
							<directory>${basedir}/src/main/java</directory>
							<targetPath>WEB-INF/classes</targetPath>
							<includes>
								<include>**/*.properties</include>
								<include>**/*.xml</include>
								<include>**/*.css</include>
								<include>**/*.html</include>
							</includes>
						</resource>
					</webResources>
					<packagingExcludes>
        					    WEB-INF/lib/tomcat-servlet-api-7.0.47.jar
         		 	</packagingExcludes>
				</configuration>
            </plugin>
```

### License


SPDX Short Identifier: http://spdx.org/licenses/EPL-1.0

SPDX-License-Identifier: EPL-1.0

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v10.html
