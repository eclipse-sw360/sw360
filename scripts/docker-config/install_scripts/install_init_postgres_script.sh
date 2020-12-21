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
# This script installs postgresql ,creates database and user for liferay,
# configures to be accessible from host machine.
# -----------------------------------------------------------------------------

install_init_postgres() {
  install_postgres
  configure_postgres
  init_db_and_user
}

init_db_and_user() {
  psql -U postgres -c "CREATE DATABASE lportal;"
  psql -U postgres -c "CREATE USER liferay WITH ENCRYPTED PASSWORD 'liferay';"
  psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE lportal to liferay;"
  psql -U postgres -c "ALTER USER postgres PASSWORD 'postgrespwd';"
}

configure_postgres() {
  sed -i "s/local   all             postgres                                peer/local   all             postgres                                trust/" /etc/postgresql/10/main/pg_hba.conf
  sed -i "s/host    all             all             127.0.0.1\/32            md5/host    all             all             0.0.0.0\/0               md5/" /etc/postgresql/10/main/pg_hba.conf
  sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '*'/" /etc/postgresql/10/main/postgresql.conf
  /etc/init.d/postgresql restart
}

install_postgres() {
  DEBIAN_FRONTEND=noninteractive apt-get install postgresql-10 -y --no-install-recommends
}

install_init_postgres
