#!/bin/bash
#
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH, 2021.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

cd /build || exit 1

if [[ ! -f "./Makefile" ]]; then
    ./configure --without-java --without-cpp --without-qt4 --without-c_glib --without-csharp --without-erlang \
                --without-perl --without-php --without-php_extension --without-python --without-py3 --without-ruby \
                --without-haskell --without-go --without-d --without-haskell --without-php --without-ruby \
                --without-python --without-erlang --without-perl --without-c_sharp --without-d --without-php \
                --without-go --without-lua --without-nodejs --without-cl --without-dotnetcore --without-swift --without-rs || exit 1
fi

make -j$(nproc) && make install

# Pack fo next docker stage
make DESTDIR="$PWD"/thrift-binary install
cd thrift-binary || exit 1
tar cfz /thrift-bin.tar.gz .
