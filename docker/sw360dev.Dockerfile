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

FROM maven:3.6.3-openjdk-11

COPY --from=gosu/assets /opt/gosu /opt/gosu
COPY scripts/entrypoint.sh /usr/local/bin/entrypoint.sh

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    git-core \
    locales \
    && rm -rf /var/lib/apt/lists/*

RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.utf8

COPY scripts/install-thrift.sh /install-thrift.sh

RUN chmod +x /usr/local/bin/entrypoint.sh \
    && /opt/gosu/gosu.install.sh \
    && rm -fr /opt/gosu \
    && /install-thrift.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
