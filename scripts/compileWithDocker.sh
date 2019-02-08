#!/usr/bin/env bash
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-1.0
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
tempdir=$(mktemp -d)
mkdir -p "$tempdir/scripts"
cp "$DIR/scripts/install-thrift.sh" "$tempdir/scripts"
pushd $tempdir
docker build \
    -f "$DIR/sw360dev.Dockerfile" \
    -t sw360/sw360dev \
    --rm=true --force-rm=true \
    $tempdir
popd
rm -r $tempdir

docker run -i \
    -v "$DIR":/sw360portal \
    -w /sw360portal \
    --net=host \
    sw360/sw360dev \
    su-exec $(id -u):$(id -g) \
    mvn package -DskipTests
