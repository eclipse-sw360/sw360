#!/bin/bash
#
# Copyright Bosch Software Innovations GmbH, 2019.
# Copyright Helio Chissini de CAstro, 2022
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
# -----------------------------------------------------------------------------

set -eo pipefail

[ -d libs ] && rm -rf ./libs
mkdir -p libs
[ -d slim-wars ] && rm -rf slim-wars
mkdir -p slim-wars

for i in *.war; do
  i=${i%.war}
  echo "Repacking $i ..."
  [ -d "$i" ] && rm -rf "$i"
  mkdir -p "$i"
  cd "$i" && (unzip -q ../"$i.war") || exit 1
  mv WEB-INF/lib/* ../libs/
  rmdir WEB-INF/lib
  zip -q -r ../slim-wars/"$i.war" .
  cd ..
  rm -rf "$i" "$i.war"
done

