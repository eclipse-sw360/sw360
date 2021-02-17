#!/bin/bash
# Copyright (c) Bosch.IO GmbH 2021.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

# Usage ./buildContainer.sh <version>

if [ -z "$1" ]
  then
    echo "No version supplied"
    exit 1
fi

echo "Script assumes that you are already logged into docker hub for pushing the new image."
echo "If not, please run 'docker login --username=<yourhubuser> --password-stdin' prior to running the script."

cp ../scripts/install-thrift.sh .
docker build -t eclipse/sw360buildenv:$1 --rm=true --force-rm=true .
sudo docker push eclipse/sw360buildenv:$1
sudo docker push eclipse/sw360buildenv
rm -f install-thrift.sh

echo "Please update Jenkinsfile to new version and push the change."
