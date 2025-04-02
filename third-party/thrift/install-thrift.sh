#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2013-2016.
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Copyright Cariad SE, 2024.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# initial author: birgit.heydenreich@tngtech.com
# -----------------------------------------------------------------------------

set -ex

BASEDIR="${BASEDIR:-$(mktemp -d)}"
THRIFT_VERSION=${THRIFT_VERSION:-0.20.0}

has() { type "$1" &> /dev/null; }

processThrift() {
  VERSION=${THRIFT_VERSION}

  echo "-[shell provisioning] Extracting thrift"
  [ -d "$BASEDIR/thrift" ] && rm -rf "$BASEDIR/thrift"
  mkdir -p "$BASEDIR/thrift"
  if [ -f "/var/cache/deps/thrift-$VERSION.tar.gz" ]; then
      tar -xzf "/var/cache/deps/thrift-$VERSION.tar.gz" -C "$BASEDIR/thrift" --strip-components=1
  else
      curl -L "http://archive.apache.org/dist/thrift/$VERSION/thrift-$VERSION.tar.gz" | tar -xz -C "$BASEDIR/thrift" --strip-components=1
  fi

  mkdir -p "${BASEDIR}/build"
  cd "${BASEDIR}/build" || exit 1
  echo "-[shell provisioning] Building thrift"
  cmake \
    -DBUILD_JAVA=OFF \
    -DBUILD_CPP=OFF \
    -DBUILD_C_GLIB=OFF \
    -DBUILD_JAVASCRIPT=OFF \
    -DBUILD_NODEJS=OFF \
    -DWITH_OPENSSL=OFF \
    -DBUILD_PYTHON=OFF \
    -DBUILD_TESTING=OFF \
    -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
    "${BASEDIR}/thrift/"

  # -DCMAKE_POLICY_VERSION_MINIMUM=3.5 is added to fix the cmake error:
  # CMake Error at CMakeLists.txt:20 (cmake_minimum_required):
  #  Compatibility with CMake < 3.5 has been removed from CMake.

  make -j"$(nproc)"

  DESTDIR="${DESTDIR:-$BASEDIR/dist/thrift-$VERSION}" make install
}

processThrift
