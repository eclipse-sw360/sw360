#!/bin/bash

# -----------------------------------------------------------------------------
# Copyright Helio Chissini de CAstro, 2022
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
# -----------------------------------------------------------------------------

# Loop through duplicated unversionated jarfiles

shared_dir="${SHARED_DIR:-$PWD}"

for file in "$shared_dir"/*.jar; do
    base=$(basename "$file")
    no_version=${base%%-[0-9]*.jar}.jar
    if [[ -f "$shared_dir/$no_version" && "$shared_dir/$no_version" != "$file" ]]; then
        rm "$shared_dir/$no_version"
        ln -s "$file" "$shared_dir/$no_version"
    fi
done
