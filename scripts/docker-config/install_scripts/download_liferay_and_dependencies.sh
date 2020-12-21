#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script downloads liferay and OSGi modules.
# -----------------------------------------------------------------------------

download_liferay_and_dependencies() {
  wget https://sourceforge.net/projects/lportal/files/Liferay%20Portal/7.3.3%20GA4/liferay-ce-portal-tomcat-7.3.3-ga4-20200701015330959.tar.gz/download -O liferay-ce-portal-tomcat-7.3.3-ga4.tar.gz
  tar -xzf liferay-ce-portal-tomcat-7.3.3-ga4.tar.gz
  cd /app/build/sw360/liferay-ce-portal-7.3.3-ga4/deploy
  wget https://search.maven.org/remotecontent?filepath=commons-codec/commons-codec/1.12/commons-codec-1.12.jar -O commons-codec-1.12.jar
  wget https://search.maven.org/remotecontent?filepath=org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar -O commons-collections4-4.4.jar
  wget https://search.maven.org/remotecontent?filepath=org/apache/commons/commons-csv/1.4/commons-csv-1.4.jar -O commons-csv-1.4.jar
  wget https://search.maven.org/remotecontent?filepath=commons-io/commons-io/2.6/commons-io-2.6.jar -O commons-io-2.6.jar
  wget https://search.maven.org/remotecontent?filepath=commons-lang/commons-lang/2.4/commons-lang-2.4.jar -O commons-lang-2.4.jar
  wget https://search.maven.org/remotecontent?filepath=commons-logging/commons-logging/1.2/commons-logging-1.2.jar -O commons-logging-1.2.jar
  wget https://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.8.5/gson-2.8.5.jar -O gson-2.8.5.jar
  wget https://search.maven.org/remotecontent?filepath=com/google/guava/guava/21.0/guava-21.0.jar -O guava-21.0.jar
  wget https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-annotations/2.11.3/jackson-annotations-2.11.3.jar -O jackson-annotations-2.11.3.jar
  wget https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-core/2.11.3/jackson-core-2.11.3.jar -O jackson-core-2.11.3.jar
  wget https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-databind/2.11.3/jackson-databind-2.11.3.jar -O jackson-databind-2.11.3.jar
  wget https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.20/commons-compress-1.20.jar -O commons-compress-1.20.jar
  wget https://repo1.maven.org/maven2/org/apache/thrift/libthrift/0.13.0/libthrift-0.13.0.jar -O libthrift-0.13.0.jar
}

download_liferay_and_dependencies
