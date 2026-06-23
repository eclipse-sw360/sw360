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
# Prints the public key stored in the JWT signing keystore.
# Useful for diagnosing JWKS / token-validation mismatches.
#
# Usage:
#   ./printKeyPair.sh [keystore-file]
#
# The keystore password is read from JWT_SECRET_KEY (default: sw360SecretKey).
#
# Requires: keytool (JDK), openssl
#
set -euo pipefail
PASSWORD="${JWT_SECRET_KEY:-sw360SecretKey}"
IN="${1:-jwt-keystore.jks}"
keytool -list -rfc \
    -keystore "${IN}" \
    -storepass "${PASSWORD}" \
    -alias jwt \
    | openssl x509 -inform pem -noout -pubkey -text
