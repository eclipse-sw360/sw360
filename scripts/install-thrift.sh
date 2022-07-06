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

BASEDIR="${BASEDIR:-/tmp}"
CLEANUP=true
TARBALL=false
THRIFT_VERSION=${THRIFT_VERSION:-0.16.0}
UNINSTALL=false

has() { type "$1" &> /dev/null; }

processThrift() {
  set -x
  VERSION=$3
  BUILDDIR="${BASEDIR}/thrift-$VERSION"

  if [[ ! -d "$BUILDDIR" ]]; then
      echo "-[shell provisioning] Extracting thrift"
      if [ -e "/vagrant_shared/packages/thrift-$VERSION.tar.gz" ]; then
          tar -xzf "/vagrant_shared/packages/thrift-$VERSION.tar.gz" -C "$BASEDIR"
      elif [ -e "/deps/thrift-$VERSION.tar.gz" ]; then
          tar -xzf "/deps/thrift-$VERSION.tar.gz" -C "$BASEDIR"
      else
          TGZ="${BASEDIR}/thrift-$VERSION.tar.gz"

          curl -z "$TGZ" -o "$TGZ" "http://archive.apache.org/dist/thrift/$VERSION/thrift-$VERSION.tar.gz"
          tar -xzf "$TGZ" -C "$BASEDIR"

          [[ $CLEANUP ]] && rm "${BASEDIR}/thrift-$VERSION.tar.gz"
      fi
  fi

  cd "$BUILDDIR"
  if [[ ! -f "./compiler/cpp/thrift" ]]; then
      echo "-[shell provisioning] Installing dependencies of thrift"

      echo "-[shell provisioning] Building thrift"
      if [[ ! -f "./Makefile" ]]; then
          ./configure --without-java --without-cpp --without-qt4 --without-c_glib --without-csharp --without-erlang \
                      --without-perl --without-php --without-php_extension --without-python --without-py3 --without-ruby \
                      --without-haskell --without-go --without-d --without-haskell --without-php --without-ruby \
                      --without-python --without-erlang --without-perl --without-c_sharp --without-d --without-php \
                      --without-go --without-lua --without-nodejs --without-cl --without-dotnetcore --without-swift --without-rs
      fi
      if [ "$1" == true ]; then
        # shellcheck disable=SC2046
        make -j$(nproc)
      fi
  fi

  echo "-[shell provisioning] Executing make $2 on thrift"
  $SUDO_CMD make "$2"

  if [ "$TARBALL" = true ]; then
    make DESTDIR="$PWD"/thrift-binary install
    cd thrift-binary || exit 1
    tar cfz /thrift-bin.tar.gz .
  fi

  if [ "$CLEANUP" = true ]; then
      $SUDO_CMD rm -rf "$BUILDDIR"
  fi
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
  if [ "$arg" == "--no-cleanup" ]; then
    CLEANUP=false
  elif [ "$arg" == "--uninstall" ]; then
    UNINSTALL=true
  elif [ "$arg" == "--tarball" ]; then
    TARBALL=true
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
