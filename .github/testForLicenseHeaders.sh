#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2017.
# Copyright Bosch.IO 2020.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# initial author: maximilian.huber@tngtech.com
# -----------------------------------------------------------------------------

set -e

cd "$(dirname "$0")/.." || exit 1

failure=false

while read -r file ; do
    if ! head -15 "$file" | grep -q 'SPDX-License-Identifier:' $file; then
        echo "WARN: no 'SPDX-License-Identifier' in  $file"
    fi
    if head -15 "$file" | grep -q 'https://www.eclipse.org/legal/epl-2.0/'; then
        continue # epl found
    fi
    if head -15 "$file" | grep -q 'SPDX-License-Identifier: EPL-2.0'; then
        continue # edl found
    fi

    echo "$(tput bold)ERROR: no EPL-2.0 licensing is specified in $file$(tput sgr0)"
    failure=true
done <<< "$(git ls-files \
    | grep -Ev '\.(csv|rdf|ent|dtd|lar|png|gif|psd|ico|jpg|docx|gitignore|dockerignore|cert|jks|spdx|rdf|MockMaker|json)' \
    | grep -Ev '(LICENSE|NOTICE|README|CHANGELOG|CODE_OF_CONDUCT|CONTRIBUTING|Language|Language_vi)' \
    | grep -v .pre-commit-config.yaml \
    | grep -v 'id_rsa' \
    | grep -v '.versions' \
    | grep -v 'default_secrets' \
    | grep -Ev '*/asciidoc/*')"

if [ "$failure" = true ]; then
    echo "test failed"
    exit 1
fi
