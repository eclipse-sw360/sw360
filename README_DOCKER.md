# Steps to use SW360 Docker

* Install Docker and docker-compose.
* Build docker compose

    You will need to have a recent docker and docker-compose installed.

    Build is done by the script:

    ```sh
    docker_build.sh
    ```

    The script will download all dependencies in the deps folder.

    Docker compose for sw360 relies on environment file to setup postgres, couchdb ands sw360 containers.
    The default environment file is under `scripts/docker-config/default.docker.env`

    The config file looks like this:

    ```sh
    # scripts/docker-config/default.docker.env
    POSTGRES_USER=liferay
    POSTGRES_PASSWORD=liferay
    POSTGRES_DB=lportal
    POSTGRES_DATA_DIR=./data/postgres
    COUCHDB_USER=admin
    COUCHDB_PASSWORD=password
    COUCHDB_CREATE_DATABASE=yes
    COUCHDB_DATA_DIR=./data/couchdb
    SW360_DATA=./data/sw360
    ```

    By default, data for postgres, couchdb and sw360 document will be persisted under `data` on current directory. If you want to override this pass a `SW360_ENV` to a copy of the above env file, with your modifications like this:

    ```sh
    SW360_ENV=<mynew_env> ./docker_build.sh
    ```

* Proxy setup

    To build under proxy system, add this options on your custom env file:

    ```sh
    PROXY_ENABLED=true
    PROXY_HTTP_HOST=<your_http_proxy_ip>
    PROXY_HTTPS_HOST=<your_https_proxy_ip>
    PROXY_PORT=<your_port>
    ```

* Running the image

    To run the resulting compose just do:

    ```sh
    docker-compose up
    ```

    or with your custom env file

    ```sh
    docker-compose --env-file <myenvfile> up
    ```

* *Nginx* config for reverse proxy and X-Frame issues

    For nginx, assuming you are using standard 8080 localhost port for you sw360, this is a simple configuration for root webserver under Ubuntu.

    ```nginx
        location / {
            resolver 127.0.0.11 valid=30s;
            proxy_pass http://localhost:8080/;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header Host $http_host;
            proxy_set_header X-Forwarded-Proto https;
            proxy_redirect off;
            proxy_read_timeout 3600s;
            proxy_hide_header X-Frame-Options;
            add_header X-Frame-Options "ALLOWALL";
        }
    ```

    ***WARNING*** - Cross frame is enable and open for development purposes. If you intend to use the above config in production, remember to properly secure the webserver.
