#!/usr/bin/env bash

set -eu

BRANCH_NAME="${TRAVIS_BRANCH:=unknown}"
LOCAL_TEST_DATA_PATH="target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated"

mkdir -p "$HOME/.m2/repository"

# Lein

mkdir -p $HOME/.lein

# GAE dev server test data

cd gae-dev-server

mvn clean install

mkdir -p "${LOCAL_TEST_DATA_PATH}"

cp -v "${HOME}/.cache/local_db.bin" "${LOCAL_TEST_DATA_PATH}"

mvn appengine:devserver_start

# Check API code

cd ../api

lein do clean, check, test :all

cd ../gae-dev-server

mvn appengine:devserver_stop

cd ..

# Check nginx configuration

docker run \
       --rm \
       --volume "$PWD/nginx/":"/conf" \
       --entrypoint /usr/local/openresty/bin/openresty \
       openresty/openresty:1.11.2.3-alpine-fat -t -c /conf/nginx.conf

# Build docker images if branch is `develop`

if [[ "${BRANCH_NAME}" != "develop" ]] && [[ "${BRANCH_NAME}" != "master" ]]; then
    echo "Skipping docker build"
    exit 0
fi

cd nginx

docker build -t "${PROXY_IMAGE_NAME:=akvo/flow-api-proxy}" .

cd ..

cd api

lein uberjar

docker build -t "${BACKEND_IMAGE_NAME:=akvo/flow-api-backend}" .
