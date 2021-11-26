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

  # Couchdb docker no cluster
  curl --noproxy couchdb -X PUT http://admin:password@couchdb:5984/_users
  curl --noproxy couchdb -X PUT http://admin:password@couchdb:5984/_replicator
  curl --noproxy couchdb -X PUT http://admin:password@couchdb:5984/_global_changes
}

start_sw360() {
  # Init internal couchdb
  #/etc/init.d/couchdb restart

  cd /app/sw360/tomcat/bin/
  rm -rf ./indexes/*
  ./startup.sh
  tail_logs
}

stop_sw360() {
  /app/sw360/tomcat/bin/shutdown.sh
  tail -f --lines=500 /app/sw360/tomcat/logs/catalina.out &
  sleep 20
  pkill -9 -f tail
  pkill -9 -f tomcat
  rm -rf /app/sw360/tomcat/webapps/*.war

  echo "###############################################################################################################"
  echo "# SW360 server has stopped successfully."
  echo "# Logged into sw360 container."
  echo "# In order to save state of sw360 create an image(using docker commit) of the current running container."
  echo "# Execute 'sh /app/entry_point.sh' to start sw360 again."
  echo "# Enter 'exit' to log out."
  echo "###############################################################################################################"
}

tail_logs()
{
  tail -f --lines=500 /app/sw360/tomcat/logs/catalina.out &
}

# We catch the container end and call the termination
trap 'stop_sw360' SIGTERM TERM SIGINT INT EXIT WINCH SIGWINCH

# Copy etc scripts if not here yet
if [ ! -f /etc/sw360/sw360.properties ]; then
  cp -av /etc_sw360/* /etc/sw360/
fi

wait_couchdb
start_sw360

# Wait main container shut down
wait $!
