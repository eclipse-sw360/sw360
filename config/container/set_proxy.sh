#!/bin/bash

# Copyright BMW CarIT GmbH, 2021.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

if [ -n "$HTTP_PROXY" ]; then
    IFS=: read -r PROXY_HTTP_HOST PROXY_PORT <<< "${HTTP_PROXY/*\/\//}"
    PROXY_HTTPS_HOST=$PROXY_HTTP_HOST
    PROXY_ENABLED=true

    export PROXY_ENABLED PROXY_HTTP_HOST PROXY_HTTPS_HOST PROXY_PORT

    [ ! -d /root/.m2 ] && mkdir -p /root/.m2
    envsubst </etc/mvn-proxy-settings.xml > /root/.m2/settings.xml
fi
