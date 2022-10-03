FROM maven:3.8-eclipse-temurin-11

ARG BASEDIR="/build"
ARG THRIFT_VERSION

COPY ./scripts/install-thrift.sh build_thrift.sh

RUN --mount=type=tmpfs,target=/build \
    --mount=type=cache,mode=0755,target=/var/cache/deps,sharing=locked \
    ./build_thrift.sh
