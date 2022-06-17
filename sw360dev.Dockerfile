# Copyright (c) Bosch Software Innovations GmbH 2016.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# This can be used to compile SW360 via:
# $ docker build -f sw360dev.Dockerfile -t sw360/sw360dev --rm=true --force-rm=true .
# $ docker run -i -v $(pwd):/sw360portal -w /sw360portal --net=host -u $(id -u):$(id -g) sw360/sw360dev mvn package -DskipTests

FROM maven:3.6.3-openjdk-11-slim
MAINTAINER Maximilian Huber <maximilian.huber@tngtech.com>

ADD scripts/install-thrift.sh /install-thrift.sh
RUN set -x \
 && apt-get update && apt-get install -y --no-install-recommends git \
 && /install-thrift.sh \
 && apt-get purge -y --auto-remove build-essential libboost-dev libboost-test-dev libboost-program-options-dev libevent-dev automake libtool flex bison pkg-config g++ libssl-dev \
 && apt-get install -y --no-install-recommends libfl2 \
 && apt-get -y clean \
 && rm -rf /var/lib/apt/lists/* /var/cache/debconf/*

CMD /bin/bash
