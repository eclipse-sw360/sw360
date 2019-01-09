#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2013-2016.
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# All rights reserved. This configuration file is provided to you under the
# terms and conditions of the Eclipse Distribution License v1.0 which
# accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php
#
# script automatically generating keys for password-free login onto
# the vagrantbox
#
#
# initial author: birgit.heydenreich@tngtech.com
# -----------------------------------------------------------------------------

set -ex

VERSION=0.11.0

BASEDIR="/tmp"
BUILDDIR="${BASEDIR}/thrift-$VERSION"

has() { type "$1" &> /dev/null; }

SUDO_CMD=""
if [ "$EUID" -ne 0 ]; then
   if has "sudo" ; then
       SUDO_CMD="sudo "
   fi
fi

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
        $SUDO_CMD apt-get install -y libboost-dev libboost-test-dev libboost-program-options-dev libevent-dev automake libtool flex bison pkg-config g++ libssl-dev
    elif has "apk" ; then
        $SUDO_CMD apk --update add g++ make apache-ant libtool automake autoconf bison flex
    else
        echo "no supported package manager found"
        exit 1
    fi

    echo "-[shell provisioning] Building thrift"
    if [[ ! -f "./Makefile" ]]; then
        ./configure --with-java  \
                    --without-cpp --without-qt4 --without-c_glib --without-csharp --without-erlang --without-perl --without-php \
                    --without-php_extension --without-python --without-ruby --without-haskell --without-go --without-d \
                    --without-haskell --without-php --without-ruby --without-python --without-erlang --without-perl \
                    --without-c_sharp --without-d --without-php --without-go --without-lua --without-nodejs --without-cl
    fi
    make
fi

echo "-[shell provisioning] Installing thrift"
$SUDO_CMD make install

if [[ "$1" != "--no-cleanup" ]]; then
    $SUDO_CMD rm -rf "$BUILDDIR"
fi
