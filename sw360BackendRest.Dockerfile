#
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

FROM tomcat:9.0.50-jdk11-openjdk-slim

COPY ./scripts/sw360BackendRestDockerConfig/etc_sw360/ /etc/sw360/

COPY ./webapps/* /usr/local/tomcat/webapps/
