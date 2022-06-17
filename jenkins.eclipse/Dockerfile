# Copyright (c) Bosch.IO GmbH 2021.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# This file is the basis for building SW360 in the Eclipse Foundation Jenkins Infrastructure.
# The image has to be published to Docker Hub with the tag eclipse/sw360buildenv.
# To publish the image, you need write access to the repository in the Docker Hub eclipse organization.
# Versioning is done by a single number, e.g., eclipse/sw360buildenv:1
# 
# Build and push it from the jenkins-eclipse folder with
# $ docker build -t eclipse/sw360buildenv:<version> --rm=true --force-rm=true .
# $ docker login --username=yourhubuser --password-stdin
# $ sudo docker push eclipse/sw360buildenv
# 
# See the Jenkinsfile for usage within the Jenkins environment.

FROM maven:3.6.3-jdk-11

ADD install-thrift.sh /install-thrift.sh
RUN /install-thrift.sh

CMD /bin/bash
