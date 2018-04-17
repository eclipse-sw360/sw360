# Copyright (c) Bosch Software Innovations GmbH 2016.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-1.0
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

FROM maven:3.5.0-jdk-8-alpine
MAINTAINER Maximilian Huber <maximilian.huber@tngtech.com>

RUN set -x \
 &&  apk --update add su-exec git \
     wget g++ make apache-ant libtool automake autoconf bison flex \
 && rm -rf /var/cache/apk/* \
 && cd /tmp \
 && wget -q 'https://github.com/apache/thrift/archive/0.9.3.tar.gz' -O thrift.tar.gz \
 && tar xzf thrift.tar.gz && rm thrift.tar.gz && cd thrift* \
 && ./bootstrap.sh \
 && ./configure --prefix=/usr \
        --with-java  \
        --without-cpp --without-qt4 --without-c_glib --without-csharp --without-erlang --without-perl --without-php \
        --without-php_extension --without-python --without-ruby --without-haskell --without-go --without-d \
        --without-haskell --without-php --without-ruby --without-python --without-erlang --without-perl \
        --without-c_sharp --without-d --without-php --without-go --without-lua --without-nodejs \
 && make \
 && make install \
 && rm -rf /tmp/thrift*

CMD /bin/bash
