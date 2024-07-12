#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2013-2016.
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# script automatically generating keys for password-free login onto
# the vagrantbox
#
#
# initial author: birgit.heydenreich@tngtech.com
# -----------------------------------------------------------------------------

set -e
set -x

BASEDIR="${BASEDIR:-/tmp}"
THRIFT_VERSION=${THRIFT_VERSION:-0.19.0}
UNINSTALL=false

has() { type "$1" &> /dev/null; }

processThrift() {
  VERSION=$3

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
    "${BASEDIR}/thrift/"

  if [ "$1" == true ]; then
    # shellcheck disable=SC2046
    make -j$(nproc)
  fi

  echo "-[shell provisioning] Executing make $2 on thrift"
  $SUDO_CMD make "$2"
  $SUDO_CMD mkdir -p /usr/share/thrift
  $SUDO_CMD touch "/usr/share/thrift/${THRIFT_VERSION}"
}

installThrift() {
  if has "thrift"; then
      if thrift --version | grep -q "$THRIFT_VERSION"; then
          echo "thrift is already installed at $(which thrift)"
          exit 0
      else
          echo "thrift is already installed but does not have the correct version: $THRIFT_VERSION"
          echo "Use '$0 --uninstall' first to remove the incorrect version and then try again."
          exit 1
      fi
  fi

  processThrift true "install" "$THRIFT_VERSION"
}

uninstallThrift() {
  if has "thrift"; then
      VERSION=$(thrift --version | cut -f 3 -d" ")
      echo "Uninstalling thrift version $VERSION"
      processThrift false "uninstall" "$VERSION"
  else
      echo "thrift not installed on this machine."
      exit 1
  fi
}

for arg in "$@"
do
  if [ "$arg" == "--uninstall" ]; then
    UNINSTALL=true
  else
    echo "Unsupported parameter: $arg"
    echo "Usage: $0 [--no-cleanup] [--uninstall]"
    exit 1
  fi
done

SUDO_CMD=""
if [ "$EUID" -ne 0 ]; then
   if has "sudo" ; then
       SUDO_CMD="sudo "
   fi
fi

if [ "$UNINSTALL" == true ]; then
  uninstallThrift
else
  installThrift
fi
