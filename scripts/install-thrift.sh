#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
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

BASEDIR="/tmp"
BUILDDIR="${BASEDIR}/thrift-0.9.3"

if [[ ! -d "$BUILDDIR" ]]; then
    echo "-[shell provisioning] Extracting thrift"
    if [ -e /vagrant_shared/packages/thrift-0.9.3.tar.gz ]; then
        tar -xzf /vagrant_shared/packages/thrift-0.9.3.tar.gz -C $BASEDIR
    else
        apt-get update
        apt-get install -y curl

        TGZ="${BASEDIR}/thrift-0.9.3.tar.gz"

        curl -z $TGZ -o $TGZ http://ftp.fau.de/apache/thrift/0.9.3/thrift-0.9.3.tar.gz
        tar -xzf $TGZ -C $BASEDIR

        [[ "$1" != "--no-cleanup" ]] && rm "${BASEDIR}/thrift-0.9.3.tar.gz"
    fi
fi

cd "$BUILDDIR"
if [[ ! -f "./compiler/cpp/thrift" ]]; then
    echo "-[shell provisioning] Installing dependencies of thrift"
    apt-get update
    apt-get install -y libboost-dev libboost-test-dev libboost-program-options-dev libevent-dev automake libtool flex bison pkg-config g++ libssl-dev

    echo "-[shell provisioning] Building thrift"
    if [[ ! -f "./Makefile" ]]; then
        ./configure --without-test --without-erlang --without-python --without-cpp --without-php --without-haskell --without-go
    fi
    make
fi

echo "-[shell provisioning] Installing thrift"
make install

[[ "$1" != "--no-cleanup" ]] && rm -rf "$BUILDDIR"

exit 0
