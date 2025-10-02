
# Copyright Helio Chisisni de Castro, 2023. Part of the SW360 Portal Project.
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH, 2021.
# Copyright Cariad SE, 2025. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

#--------------------------------------------------------------------------------------------------
# Thrift
# Ubuntu Noble image
FROM ubuntu@sha256:985be7c735afdf6f18aaa122c23f87d989c30bba4e9aa24c8278912aac339a8d AS build

ARG BASEDIR="/build"
ARG DESTDIR="/"
ARG THRIFT_VERSION="0.20.0"

RUN rm -f /etc/apt/apt.conf.d/docker-clean
RUN apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    bison \
    build-essential \
    ca-certificates \
    cmake \
    curl \
    flex \
    libevent-dev \
    libtool \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

COPY ./third-party/thrift/install-thrift.sh install_thrift.sh

RUN --mount=type=cache,target=/var/cache/deps \
    ./install_thrift.sh

FROM scratch
COPY --from=build /usr/local/bin/thrift /usr/local/bin/thrift
