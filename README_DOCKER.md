# SW360 Docker

## Building

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

    ```ini
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

    ```ini
    PROXY_ENABLED=true
    PROXY_HTTP_HOST=<your_http_proxy_ip>
    PROXY_HTTPS_HOST=<your_https_proxy_ip>
    PROXY_PORT=<your_port>
    ```

## Running the image

* Run the resulting image:

    ```sh
    docker-compose -env-file scripts/docker-config/default.docker.env up
    ```

* With custom env file

    ```sh
    docker-compose --env-file <myenvfile> up
    ```

    You can add **-d** parameter at end of line to sgtart in daemon mode and see the logs with the following command:

    ```sh
    docker logs -f sw360
    ```

## Extra configurations

SW360 image runs without internal web server and is assigned to be SSL as default, so using port **8080** directly will cause CSS to be scrambled, since internally redirections will be passed to inexistent localhost port **443**. 

Here's some extra configurations that can be useful

* *Nginx* config for reverse proxy and X-Frame issues on on host machine ( not docker )

    For nginx, assuming you are using default config for your sw360, this is a simple configuration for root web server under Ubuntu.

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

    ***WARNING*** - X-frame is enabled wide open for development purposes. If you intend to use the above config in production, remember to properly secure the web server.

* Make http only **port 8080** default

    Modify the following line on your portal-ext.properties to http:

    ```ini
    web.server.protocol=http
    ```

* Liferay by default for security reasons do not allow redirect for unknown ips/domains, mostly on admin modules, so is necessary to add your domain or ip to the redirect allowed lists in portal-ext.properties.
A not proper redirect can see in logs

    **IP based** - The list of ips is separated by comma
    ```
    redirect.url.security.mode=ip
    redirect.url.ips.allowed=127.0.0.1,172.17.0.1,...

    ```

    **Domain based** - The list domains is separated by comma
    ```
    redirect.url.security.mode=domain
    redirect.url.domain.allowed=exampler.com,*.wildcard.com

    ```
