# SW360 Docker

> **WARNING**: This readme refers to main branch. [This is the Docker documentation for stable 18.x series](https://github.com/eclipse-sw360/sw360/blob/releases/18/README_DOCKER.md)

## Table of Contents

[Building](#building)

[Running the Image](#running-the-image-first-time)

[Extra Configurations](#configurations)

## Building

* Install Docker recent version
* Build images

    Build is done by the script:

    ```sh
    ./docker_build.sh
    ```

    If you want to specify [CVE-Search](https://github.com/cve-search/cve-search) host at build time, run as follows:

    ```sh
    ./docker_build.sh --cvesearch-host <HOST_URL>
    ```

    The `<HOST_URL>` above should be `http://<YOUR_SERVER_HOST>:<PORT>` style, 
    or it can be [https://cvepremium.circl.lu](https://cvepremium.circl.lu) for testing purposes only.

    The script will build multiple intermediary images.
    Subsequent builds will only build the differences

    To configure couchdb, create a file containing the necessary credentials.

    A template of this file can be found in:
    `config/couchdb/default_secrets`

    Example:

    ```ini
    COUCHDB_URL=http://couchdb:5984
    COUCHDB_USER=sw360
    COUCHDB_PASSWORD=sw360fossie
    ```

    To pass your file during build export a variable called **SECRETS** pointing to your file

* Proxy during build stage

    Docker will detect if you configured proxy environment variables.

    It's suggested though to configure docker system wide ( require super user privileges )

  * systemd based
    If you are using a regular systemd based docker:
    * Create the following file **http_proxy.conf** on the directory `/etc/systemd/system/docker.service.d/`

    ```ini
    [Service]
    Environment="HTTP_PROXY=<your_proxy>"
    Environment="HTTPS_PROXY=<your_proxy>"
    Environment="NO_PROXY=<your_proxy>"
    ```

    * Do a regular systemctl daemon-reload and systemctl restart docker

* Volumes

  By default couchdb and sw360 have their own storage volumes:

  **CouchDB**

  ```yml
  - couchdb → /opt/couchdb/data
  ```

  **sw360**

  ```yml
  - etc → /etc/sw360
  - webapps → /app/sw360/tomcat/webapps
  ```

  There is a local mounted as binded dir volume to add customizations

  **sw360**

  ```yml
  - ./config/sw360 -> /app/sw360/config
  ```

  **couchdb**

  ```yml
  - config/couchdb/sw360_setup.ini → /opt/couchdb/sw360_setup.ini
  - config/couchdb/sw360_log.ini → /opt/couchdb/etc/local.d/sw360_log.ini
  - logs/couchdb → /opt/couchdb/log
  ```

  If you want to override all configs, create a docker env file  and alter for your needs.

  Then just rebuild the project with **-env env_file** option

## Networking

This composed image runs under a single default network, called **sw360net**

So any external docker image can connect to internal couchdb  through this network

## Running the image first time

* Run the resulting image:

    ```sh
    docker compose up
    ```

* With custom env file

    ```sh
    docker compose --env-file <envfile> up
    ```

    You can add **-d** parameter at end of line to start in daemon mode and see the logs with the following command:

    ```sh
    docker logs -f sw360
    ```

### Post setup configuration

* Please read this page after you have initial screen:
 [SW360 Initial Setup Configuration](https://eclipse.dev/sw360/docs/deployment/)

## Fossology

For docker based approach, is recommended use official [Fossology docker image](https://hub.docker.com/r/fossology/fossology/)

This is the steps to quick perform this:

```sh
# Start Fossology container connected to sw360 env
docker run \
    --network sw360net \
    -p 8081:80 \
    --name fossology \
    -e FOSSOLOGY_DB_HOST=<your_db_host> \
    -e FOSSOLOGY_DB_USER=<your_db_user> \
    -e FOSSOLOGY_DB_PASSWORD=<your_db_password> \
    -d fossology/fossology
```

This will pull/start the fossology container and made it available on the host machine at port 8081

### Configure Fossology

* **On Fossology**
  * Login on Fossology
  * Create an API token for the user intended to be used
* **On sw360**
  * Go to fossology admin config
  * Add the host, will be something like: `http(s)://<hostname>:8081/repo/api/v1/`
  * Add the id of folder. The default id is **1** (Software Repository). You can get the ID of the folder you want from the folder URL in Fossology
  * Add your obtained Token from Fossology

## Configurations

By default, docker image of sw360 runs without internal web server and is assigned to be on port 8080.

Here's some extra configurations that can be useful to fix some details.

### CouchDB

CouchDB in compose runs with one standard admin user in a single node setup, user **sw360** and password **sw360fossy**

To modify the entries and setup, you have two possible options:

* Modify `config/couchdb/docker.ini` in main source tree
* Create a new `.ini` file, add to `config/couchdb/` folder and add as a mounted volume file in docker compose

For logging, they are now file based on local source folder `logs/couchdb` and the base configuration is in `config/couchdb/log.ini`.

You can find [CouchDB configuration docs here](https://docs.couchdb.org/en/stable/config/index.html)
