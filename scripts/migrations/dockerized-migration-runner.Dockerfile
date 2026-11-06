# Copyright Bosch Software Innovations GmbH, 2017.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# Python 3.15 Slim
FROM python:3.15-slim@sha256:8c13a3f37a6a5b71498228e902c4b9f51c1f7900d49fabf71f9a650c6120e0d0

RUN set -x \
    && pip install pycouchdb \
    && mkdir -p /migrations

WORKDIR /migrations
