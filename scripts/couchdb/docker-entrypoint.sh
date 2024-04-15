#!/bin/bash
# SPDX-License-Identifier: EPL-2.0
# SPDX-FileCopyrightText: © CouchDB Developers <dev@couchdb.apache.org>, 2024 Siemens AG
#
# Adopted from https://github.com/apache/couchdb-docker/blob/58910ed097489dc588b2a87592406f8faa1bdadf/3.3.3/docker-entrypoint.sh

set -em

# first arg is `-something` or `+something`
if [ "${1#-}" != "$1" ] || [ "${1#+}" != "$1" ]; then
	set -- /opt/couchdb/bin/couchdb "$@"
fi

# first arg is the bare word `couchdb`
if [ "$1" = 'couchdb' ]; then
	shift
	set -- /opt/couchdb/bin/couchdb "$@"
fi

if [ "$1" = '/opt/couchdb/bin/couchdb' ]; then
	touch /opt/couchdb/etc/local.d/docker.ini

	if [ "$(id -u)" = '0' ]; then
	  # Fix file permissions
		find /opt/couchdb \! \( -user couchdb -group couchdb \) -exec chown -f couchdb:couchdb '{}' +
		find /opt/couchdb/data -type d ! -perm 0755 -exec chmod -f 0755 '{}' +
		find /opt/couchdb/data -type f ! -perm 0644 -exec chmod -f 0644 '{}' +
		find /opt/couchdb/etc -type d ! -perm 0755 -exec chmod -f 0755 '{}' +
		find /opt/couchdb/etc -type f ! -perm 0644 -exec chmod -f 0644 '{}' +
	fi

	if [ ! -z "$NODENAME" ] && ! grep "couchdb@" /opt/couchdb/etc/vm.args; then
		echo "-name couchdb@$NODENAME" >> /opt/couchdb/etc/vm.args
	fi

	if [ "$COUCHDB_USER" ] && [ "$COUCHDB_PASSWORD" ]; then
		# Create admin only if not already present
		if ! grep -Pzoqr "\[admins\]\n$COUCHDB_USER =" /opt/couchdb/etc/local.d/*.ini /opt/couchdb/etc/local.ini; then
			printf "\n[admins]\n%s = %s\n" "$COUCHDB_USER" "$COUCHDB_PASSWORD" >> /opt/couchdb/etc/local.d/docker.ini
		fi
	fi

	if [ "$COUCHDB_SECRET" ]; then
		# Set secret only if not already present
		if ! grep -Pzoqr "\[chttpd_auth\]\nsecret =" /opt/couchdb/etc/local.d/*.ini /opt/couchdb/etc/local.ini; then
			printf "\n[chttpd_auth]\nsecret = %s\n" "$COUCHDB_SECRET" >> /opt/couchdb/etc/local.d/docker.ini
		fi
	fi

	if [ "$COUCHDB_ERLANG_COOKIE" ]; then
		cookieFile='/opt/couchdb/.erlang.cookie'
		if [ -e "$cookieFile" ]; then
			if [ "$(cat "$cookieFile" 2>/dev/null)" != "$COUCHDB_ERLANG_COOKIE" ]; then
				echo >&2
				echo >&2 "warning: $cookieFile contents do not match COUCHDB_ERLANG_COOKIE"
				echo >&2
			fi
		else
			echo "$COUCHDB_ERLANG_COOKIE" > "$cookieFile"
		fi
		chown couchdb:couchdb "$cookieFile"
		chmod 600 "$cookieFile"
	fi

	if [ "$(id -u)" = '0' ]; then
		chown -f couchdb:couchdb /opt/couchdb/etc/local.d/docker.ini || true
	fi

	# if we don't find an [admins] section followed by a non-comment, display a warning
        if ! grep -Pzoqr '\[admins\]\n[^;]\w+' /opt/couchdb/etc/default.d/*.ini /opt/couchdb/etc/local.d/*.ini /opt/couchdb/etc/local.ini; then
		# The - option suppresses leading tabs but *not* spaces. :)
		cat >&2 <<-'EOWARN'
*************************************************************
ERROR: CouchDB 3.0+ will no longer run in "Admin Party"
       mode. You *MUST* specify an admin user and
       password, either via your own .ini file mapped
       into the container at /opt/couchdb/etc/local.ini
       or inside /opt/couchdb/etc/local.d, or with
       "-e COUCHDB_USER=admin -e COUCHDB_PASSWORD=password"
       to set it via "docker run".
*************************************************************
EOWARN
		exit 1
	fi

	if [ "$(id -u)" = '0' ]; then
		export HOME=$(echo ~couchdb)
		exec setpriv --reuid=couchdb --regid=couchdb --clear-groups "$@"
	fi
fi

exec "$@"
