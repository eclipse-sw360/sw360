#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2017.
# Part of the SW360 Portal Project.
#
# All rights reserved. This configuration file is provided to you under the
# terms and conditions of the Eclipse Distribution License v1.0 which
# accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php
#
# initial author: maximilian.huber@tngtech.com
# -----------------------------------------------------------------------------

cd "$(dirname $0)/.."

failure=false

while read file ; do
    if ! head -15 $file | grep -q 'SPDX-License-Identifier:' $file; then
        echo "WARN: no 'SPDX-License-Identifier' in  $file"
    fi
    if head -15 $file | grep -q 'http://www.eclipse.org/legal/epl-v10.html'; then
        continue # epl found
    fi
    if head -15 $file | grep -q 'http://www.eclipse.org/org/documents/edl-v10.php'; then
        continue # edl found
    fi
    if head -15 $file | grep -q 'Modifications applied by Siemens AG'; then
        continue
    fi

    echo "$(tput bold)ERROR: neither epl-1.0 nor edl-1.0 are specified in $file$(tput sgr0)"
    failure=true
done <<< "$(git ls-files \
    | grep -Ev '\.(csv|rdf|ent|dtd|lar|png|gif|psd|ico|jpg|docx|gitignore|cert|jks)' \
    | grep -Ev '(LICENSE|NOTICE|README|CHANGELOG)' \
    | grep -v 'id_rsa')"

if [ "$failure" = true ]; then
    echo "test failed"
    exit 1
fi
