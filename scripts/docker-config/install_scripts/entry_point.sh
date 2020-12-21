#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is executed on startup of Docker container.
# (execution of docker run cmd) starts couchdb, postgres and tomcat.
# -----------------------------------------------------------------------------

start_sw360() {
  /etc/init.d/couchdb restart
  /etc/init.d/postgresql restart
  cd /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/bin/
  rm -rf ./indexes/*
  ./startup.sh
  tail_logs
}

stop_sw360() {
  /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/bin/shutdown.sh
  tail -f --lines=500 /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/logs/catalina.out &
  sleep 20
  pkill -9 -f tail
  pkill -9 -f tomcat
  cd /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/webapps/
  rm -rf *.war
  /etc/init.d/couchdb stop
  /etc/init.d/postgresql stop
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
  tail -f --lines=500 /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/logs/catalina.out &
  read -r user_input
  pkill -9 -f tail
}

start_sw360
stop_sw360
