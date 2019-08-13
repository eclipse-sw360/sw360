[![Build Status](https://travis-ci.org/eclipse/sw360.svg?branch=master)](https://travis-ci.org/eclipse/sw360)

### SW360 Portal

A software component catalogue application - designed to work with FOSSology.

SW360 is a server with a REST interface and a liferay portal application
to maintain your projects / products and the software components within.

It can manage SPDX files for checking the license conditions and maintain
license information.

### Introduction

It is comprised of one frontend (portal) part, backend (services) part and additionally a REST API:

* Frontend: Liferay-(Tomcat-)based portal application using portlets.
* Backend: Tomcat-based thrift services for being called by different applications.
* Database: we store software components and metadata about them in couchdb.
* Rest: this REST API provides access to project resources for external integration.

The reference platform is the Ubuntu server 16.04 (which is a LTS version). However, it
runs well on other OSes (see below).

### Project structure

This is a multi module maven file. please consider that we have the following modules:

* frontend: for portlets, themes and layouts, the liferay part.
* backend: for the thrift based services.
* libraries: for general stuff that is reused among the above, for example, couchdb access.
* scripts: for deploying either inside the vagrant or on your development machine.
* rest: for the REST API which contains an authorization and resource server.

### Required software

* Java 1.8.X
* CouchDB, at least 1.5
* Liferay Portal CE 7.2.0 GA1
* Apache Tomcat 9.0.X

In addition, the Liferay instance must provide the following dependecies via OSGi:

* Apache Commons Codec 1.12
* Apache Commons Collections4 4.1
* Apache Commons CSV 1.4
* Apache Commons IO 2.6
* Apache Commons Lang 2.4
* Apache Commons Logging 1.2
* Google Gson 2.8.5
* Google Guava 21.0
* Jackson Annotations 2.9.8
* Jackson Core 2.9.8
* Jackson Databind 2.9.8

In order to build you will need:

* A git client
* Apache Maven 3.6.X
* Apache Thrift 0.11.0

http://maven.apache.org/download.html#Installation

Then, you must install Apache Tomcat, CouchDB. And, Java of course.

The software is tested with

* Maven 3.6.1
* Apache Tomcat 9.0.17
* Liferay 7.2.0 GA1
* CouchDB 1.5 / 1.5.1
* Java 1.8.X
* Tested with debian 8, debian 9, ubuntu 16.04, macosx 10.8 - 10.14
* We run Liferay with PostgreSQL 9.X, as the Lifera requires, but HSQL (as of the bundle) runs also OK.

### PROBLEMS

Running with the tested software shows no problems if you encounter some please report them at: 

https://github.com/eclipse/sw360/issues

### Deployment

There is a vagrant project for one-step-deployment. See the project wiki for details:

https://github.com/eclipse/sw360/wiki

Apart from the vagrant way, the software can be deployed using sw360chores:

https://github.com/sw360/sw360chores

### Commands

Most commands are using maven which is a dependency to build SW360.

#### Compiling, testing and deploying

Actually, there is a hierarchy of maven files, in general

1. to clean everything up
  - `mvn clean`

2. to run all targets including build the .war file at the end
  - `mvn install`

  this needs a couchdb running on the host on port 5984

3. to install without running the tests
  - `mvn install -DskipTests`

For deployment run the command
```
mvn install -Dbase.deploy.dir=<SOME_ABSOLUTE_PATH> -P deploy
```
which copies the artifacts depending on their type to the following folders:
  - backend: `<SOME_ABSOLUTE_PATH>/tomcat`
  - rest: `<SOME_ABSOLUTE_PATH>/tomcat`
  - frontend: `<SOME_ABSOLUTE_PATH>/liferay`
  - libraries: `<SOME_ABSOLUTE_PATH>/liferay`

You may also specify the paths using these properties:
  - backend artifacts: `backend.deploy.dir`
  - rest artifacts: `rest.deploy.dir`
  - liferay artifacts (frontend, libraries): `liferay.deploy.dir`
Be aware that you have to deploy the liferay artifacts in the Liferay auto-deploy folder.
On the other hand you must not deploy rest and backend artifacts to the auto-deploy folder.

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
