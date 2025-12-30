#!/bin/bash

mkdir -p /tmp/webapps

docker compose \
    -f docker-compose.yml  \
    -f scripts/docker-compose-dev.override.yml \
    up
