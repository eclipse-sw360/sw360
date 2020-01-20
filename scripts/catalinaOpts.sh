#!/usr/bin/env bash

# -----------------------------------------------------------------------------
#
# Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# script for setting debugging options while deploying SW360
#
# author: daniele.fognini@tngtech.com
# -----------------------------------------------------------------------------

set -e
exec 2>/dev/null

debugPort=
devel=
while getopts p:d opt; do
   case ${opt} in
      p) debugPort="$OPTARG";;
      d) devel="yes";;
   esac
done

CATALINA_OPTS=""
if [[ "${debugPort}" =~ ^[0-9]+$ ]]; then
   CATALINA_OPTS+="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${debugPort} "
fi
if [[ -n "${devel}" ]]; then
   CATALINA_OPTS+="-Dorg.ektorp.support.AutoUpdateViewOnChange=true "
fi

echo "CATALINA_OPTS=\"${CATALINA_OPTS}\""
