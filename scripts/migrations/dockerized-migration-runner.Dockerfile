# Copyright Bosch Software Innovations GmbH, 2017.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# Python 3.10 Bookworm
FROM python:3.10-bookworm@sha256:2c7bb615d8d39334f857260dc012e27070f04bea5d653e687f5f6fb643637e15

RUN set -x \
    && pip install pycouchdb \
    && mkdir -p /migrations

WORKDIR /migrations
