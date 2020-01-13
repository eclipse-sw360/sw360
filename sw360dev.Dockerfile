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
# $ docker run -i -v $(pwd):/sw360portal -w /sw360portal --net=host sw360/sw360dev su-exec $(id -u):$(id -g) mvn package -DskipTests

FROM maven:3.5.0-jdk-8-alpine
MAINTAINER Maximilian Huber <maximilian.huber@tngtech.com>

ADD scripts/install-thrift.sh /install-thrift.sh
RUN set -x \
 &&  apk --update add su-exec git \
 && /install-thrift.sh \
 && rm -rf /var/cache/apk/*

CMD /bin/bash
