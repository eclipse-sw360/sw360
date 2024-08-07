#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2024.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# script to build couchdb server with nouveau plugin enabled.
#
#
# initial author: mishra.gaurav@siemens.com
# -----------------------------------------------------------------------------

set -e
set -x

BASEDIR="${BASEDIR:-/tmp}"
NOUVEAU_PATH="/opt/couchdb/lib/nouveau-*"

nouveauExists() { ls $NOUVEAU_PATH 1> /dev/null 2>&1; }

has() { type "$1" &> /dev/null; }

processCouchdb() {
  MOZ_PACKAGE_NAME="libmozjs-*-dev"

  MOZ_PACKAGE=$(dpkg-query -W -f='${Package}\n' $MOZ_PACKAGE_NAME | head -n 1)

  echo "-[shell provisioning] Installing build dependencies"

  $SUDO_CMD DEBIAN_FRONTEND=noninteractive apt-get install -y \
    $MOZ_PACKAGE \
    build-essential \
    curl \
    debhelper \
    dh-exec \
    devscripts \
    erlang-dev \
    erlang-reltool \
    erlang-src \
    erlang \
    git \
    help2man \
    libcurl4-openssl-dev  \
    libicu-dev \
    libssl-dev \
    lsb-release \
    pkgconf \
    python3 \
    python3-venv \
    po-debconf \
    wget

  echo "-[shell provisioning] Cloning couchdb"
  [ -d "$BASEDIR/couchdb" ] && rm -rf "$BASEDIR/couchdb"
  mkdir -p "$BASEDIR/couchdb"
  git clone --single-branch --branch main --depth 1 https://github.com/apache/couchdb-pkg.git "$BASEDIR/couchdb/couchdb-pkg"
  git clone --single-branch --branch main --depth 1 https://github.com/apache/couchdb.git "$BASEDIR/couchdb/couchdb"

  # Get the version of the package
  MOZ_PACKAGE_VERSION=$(dpkg-query -W -f='${Version}\n' $MOZ_PACKAGE_NAME | head -n 1)
  # Extract the major version
  MOZ_VERSION=$(echo $MOZ_PACKAGE_VERSION | awk -F'.' '{print $1}')

  pushd "$BASEDIR/couchdb/couchdb"
  echo "-[shell provisioning] Configuring couchdb with nouveau plugin enabled"
  ./configure --enable-nouveau --spidermonkey-version $MOZ_VERSION
  popd

  pushd "$BASEDIR/couchdb/couchdb-pkg"
  echo "-[shell provisioning] Building couchdb packages"
  make build-couch
  make "$DISTRO-$CODENAME"
  popd

  echo "-[shell provisioning] Installing couchdb"
  $SUDO_CMD dpkg -i "$BASEDIR/couchdb/couchdb/couchdb_*.deb" || $SUDO_CMD apt-get --fix-broken install -y
}

installCouchdb() {
  if nouveauExists; then
      echo "couchdb with nouveau is already installed"
      exit 1
  fi

  processCouchdb
}

for arg in "$@"
do
  echo "Unsupported parameter: $arg"
  echo "Usage: $0"
  exit 1
done

SUDO_CMD=""
if [ "$EUID" -ne 0 ]; then
   if has "sudo" ; then
       SUDO_CMD="sudo "
   fi
fi

installCouchdb
