#!/usr/bin/env bash
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"

tempdir=$(mktemp -d)
mkdir -p "$tempdir/scripts"
cp "$DIR/scripts/install-thrift.sh" "$tempdir/scripts"
cp "$DIR/docker/entrypoint.sh" "$tempdir/scripts"

pushd $tempdir
docker build \
    -f "$DIR/docker/sw360dev.Dockerfile" \
    -t sw360/sw360dev \
    --rm=true --force-rm=true \
    $tempdir
popd
rm -r $tempdir

docker run -i \
    -v "$DIR":/sw360portal \
    -w /sw360portal \
    -e LOCAL_USER_ID=`id -u` \
    -e MAVEN_CONFIG=/sw360portal/.m2 \
    sw360/sw360dev \
    mvn clean package -P deploy -Dbase.deploy.dir=/sw360portal/_deploy \
         -Dliferay.deploy.dir=/sw360portal/_deploy \
         -Dbackend.deploy.dir=/sw360portal/_webapps \
         -Drest.deploy.dir=/sw360portal/_webapps \
         -DskipTests
