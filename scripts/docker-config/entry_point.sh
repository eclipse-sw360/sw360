#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH 2021
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is executed on startup of Docker container.
# (execution of docker run cmd) starts couchdb and tomcat.
# -----------------------------------------------------------------------------

set -e

wait_couchdb() {
  # Wait for it to be up
  until curl --noproxy couchdb -s http://couchdb:5984 >/dev/null 2>&1; do
      sleep 1
  done

  # Check if database is already created
  error=$(curl --noproxy couchdb --head http://admin:password@couchdb:5984/_bla | head -n 1 | cut -d' ' -f2)
  [ ! "$error" == "404" ] && return

  # Couchdb docker no cluster
  curl --noproxy couchdb -X PUT http://admin:password@couchdb:5984/_users
  curl --noproxy couchdb -X PUT http://admin:password@couchdb:5984/_replicator
  curl --noproxy couchdb -X PUT http://admin:password@couchdb:5984/_global_changes
}

start_sw360() {
  cd /app/sw360/tomcat/bin/
  rm -rf ./indexes/*
  ./startup.sh
  tail_logs
}

stop_sw360() {
  echo "###############################################################################################################"
  echo "# Stopping SW360 server"
  echo "###############################################################################################################"
  
  /app/sw360/tomcat/bin/shutdown.sh
  rm /app/sw360/tomcat/webapps/*.war
}

tail_logs()
{
  tail -f --lines=500 /app/sw360/tomcat/logs/catalina.out &
}

# We catch the container end and call the termination
trap 'stop_sw360' SIGTERM TERM SIGINT INT EXIT WINCH SIGWINCH

wait_couchdb
start_sw360

# Wait main container shut down
wait $!
