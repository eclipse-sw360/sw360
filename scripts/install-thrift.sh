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

VERSION=0.13.0

BASEDIR="/tmp"
BUILDDIR="${BASEDIR}/thrift-$VERSION"

has() { type "$1" &> /dev/null; }

if has "thrift"; then
    if thrift --version | grep -q "$VERSION"; then
        echo "thrift is already installed at $(which thrift)"
        exit 0
    else
        echo "thrift is already installed but does not have the correct version: $VERSION"
        exit 1
    fi
fi

SUDO_CMD=""
if [ "$EUID" -ne 0 ]; then
   if has "sudo" ; then
       SUDO_CMD="sudo "
   fi
fi

set -x

if [[ ! -d "$BUILDDIR" ]]; then
    echo "-[shell provisioning] Extracting thrift"
    if [ -e "/vagrant_shared/packages/thrift-$VERSION.tar.gz" ]; then
        tar -xzf "/vagrant_shared/packages/thrift-$VERSION.tar.gz" -C $BASEDIR
    else
        if has "apt-get" ; then
            $SUDO_CMD apt-get update
            $SUDO_CMD apt-get install -y curl
        elif has "apk" ; then
            $SUDO_CMD apk --update add curl
        else
            echo "no supported package manager found"
            exit 1
        fi

        TGZ="${BASEDIR}/thrift-$VERSION.tar.gz"

        curl -z $TGZ -o $TGZ http://archive.apache.org/dist/thrift/$VERSION/thrift-$VERSION.tar.gz
        tar -xzf $TGZ -C $BASEDIR

        [[ "$1" != "--no-cleanup" ]] && rm "${BASEDIR}/thrift-$VERSION.tar.gz"
    fi
fi

cd "$BUILDDIR"
if [[ ! -f "./compiler/cpp/thrift" ]]; then
    echo "-[shell provisioning] Installing dependencies of thrift"

    if has "apt-get" ; then
        $SUDO_CMD apt-get update
        $SUDO_CMD apt-get install -y build-essential libboost-dev libboost-test-dev libboost-program-options-dev libevent-dev automake libtool flex bison pkg-config g++ libssl-dev
    elif has "apk" ; then
        $SUDO_CMD apk --update add g++ make apache-ant libtool automake autoconf bison flex
    else
        echo "no supported package manager found"
        exit 1
    fi

    echo "-[shell provisioning] Building thrift"
    if [[ ! -f "./Makefile" ]]; then
        ./configure --without-java --without-cpp --without-qt4 --without-c_glib --without-csharp --without-erlang \
                    --without-perl --without-php --without-php_extension --without-python --without-py3 --without-ruby \
                    --without-haskell --without-go --without-d --without-haskell --without-php --without-ruby \
                    --without-python --without-erlang --without-perl --without-c_sharp --without-d --without-php \
                    --without-go --without-lua --without-nodejs --without-cl
    fi
    make
fi

echo "-[shell provisioning] Installing thrift"
$SUDO_CMD make install

if [[ "$1" != "--no-cleanup" ]]; then
    $SUDO_CMD rm -rf "$BUILDDIR"
fi
