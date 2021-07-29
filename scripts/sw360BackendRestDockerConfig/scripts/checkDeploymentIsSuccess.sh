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
# This script checks deployment status of backend and rest.
# -----------------------------------------------------------------------------

check_deployment_status() {
    sleep 30s
    response=$(curl --max-time 360 --connect-timeout 360 http://127.0.0.1:8080/resource/health)
    echo "$response"
    if [[ "$response" != *"\"status\":\"UP\""* ]]; then
        echo "Error occured while deloying backend."
        exit 4;
    fi
}

check_deployment_status
