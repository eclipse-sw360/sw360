#!/usr/bin/env bash
#
# Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Generates a new JWT signing keystore for the SW360 authorization server.
#
# Usage:
#   ./generateJwtStore.sh [output-file]
#
# The keystore password is read from the JWT_SECRET_KEY environment variable.
# If not set, the insecure default 'sw360SecretKey' is used.
#
# The generated file contains one RSA-2048 keypair under the alias 'jwt',
# which matches what KeyManager and SecurityConfig expect.
#
# After generating, copy the file to /etc/sw360/jwt-keystore.jks on every
# Authorization Server node (or mount the same Docker volume).
#
# Example:
#   JWT_SECRET_KEY='my-strong-password' ./generateJwtStore.sh /etc/sw360/jwt-keystore.jks
#

set -euo pipefail

PASSWORD="${JWT_SECRET_KEY:-sw360SecretKey}"
OUT="${1:-jwt-keystore.jks}"

if [ "${PASSWORD}" = "sw360SecretKey" ]; then
    echo "WARNING: Using insecure default password 'sw360SecretKey'." >&2
    echo "         Set JWT_SECRET_KEY to a strong secret for production." >&2
fi

keytool -genkeypair \
    -alias jwt \
    -keyalg RSA \
    -keysize 2048 \
    -dname "CN=jwt, L=Bengaluru, S=Karnataka, C=IN" \
    -validity 3650 \
    -keypass "${PASSWORD}" \
    -keystore "${OUT}" \
    -storepass "${PASSWORD}"

echo "Wrote keystore to: ${OUT}"
echo "  alias    : jwt"
echo "  algorithm: RSA-2048"
echo "  validity : 3650 days"
echo "  password : (from JWT_SECRET_KEY)"
