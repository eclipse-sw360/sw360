#!/usr/bin/env bash

# Copyright Bosch Software Innovations GmbH, 2017.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-1.0
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

# call as:
# $ ./dockerized-migration-runner.sh [--build|--save|--load] [migrationScript [migrationScript [...]]]
#   where
#     --build   implies that the docker image gets build
#     --save    implies that the generated docker image gets saved to "migration-runner.tar"
#     --load    implies that the generated image gets loaded from the tar
# e.g.:
# $ ./dockerized-migration-runner.sh --build
# $ ./dockerized-migration-runner.sh 003_rename_release_contacts_to_contributors.py 004_move_release_ecc_fields_to_release_information.py

set -e

cd "$(dirname "${BASH_SOURCE[0]}")"

case "$1" in
    "--build" )
        echo "build docker image for migration"
        docker build -t sw360/migration-runner \
               --rm=true --force-rm=true \
               - < ./dockerized-migration-runner.Dockerfile
        shift ;;
    "--save" )
        docker save -o "migration-runner.tar" "sw360/migration-runner"
        exit 0 ;;
    "--load" )
        docker load -i "migration-runner.tar"
        shift ;;
esac

for scriptpath in "$@"; do
    scriptname="$(basename $scriptpath)"
    if [ ! -e "$scriptname" ]; then
        echo "migration script \"$scriptname\" not found"
        continue
    fi

    # run docker image
    docker run -it --net=host \
           -v "$(pwd)":/migrations \
           sw360/migration-runner \
           "./$scriptname"
done
