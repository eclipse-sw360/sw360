#!/bin/sh

# -----------------------------------------------------------------------------
# Copyright Helio Chissini de Castro, 2022
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is executed on startup of Docker container.
# (execution of docker run cmd) starts couchdb and tomcat.
# -----------------------------------------------------------------------------


# Set default versions

CLUCENE_VERSION=${CLUCENE_VERSION:-2.1.0}
export CLUCENE_VERSION

THRIFT_VERSION=${THRIFT_VERSION:-0.17.0}
export THRIFT_VERSION

MAVEN_VERSION=${MAVEN_VERSION:-3.8.6}
export MAVEN_VERSION

LIFERAY_VERSION=${LIFERAY_VERSION:-7.4.3.18-ga18}
export LIFERAY_VERSION

LIFERAY_SOURCE=${LIFERAY_SOURCE:-liferay-ce-portal-tomcat-7.4.3.18-ga18-20220329092001364.tar.gz}
export LIFERAY_SOURCE
