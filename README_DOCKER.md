# SW360 Docker

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
    or it can be https://cvepremium.circl.lu for testing purposes only.


    The script will build multiple intermediary images.
    Subsequent builds will only build the differences

    To configure couchdb, create a file containing the necessary credentials.

    A template of this file can be found in:
    `scripts/docker_config/default_secrets`

    Example:
    ```ini
    COUCHDB_URL=http://couchdb:5984
    COUCHDB_USER=admin
    COUCHDB_PASSWORD=password
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

    By default couchdb, postgres and sw360 have their own storage volumes:

    **Postgres**
    ```yml
    - postgres:/var/lib/postgresql/data/
    ```

    **CouchDB**
    ```yml
    - couchdb:/opt/couchdb/data
    ```

    **sw360**
    ```yml
    - etc:/etc/sw360
    - webapps:/app/sw360/tomcat/webapps
    - document_library:/app/sw360/data/document_library
    ```
    There is a local mounted as binded dir volume to add customizations
    ```yml
    - ./config:/app/sw360/config
    ```

    If you want to override all configs, create a docker env file  and alter for your needs.

    Then just rebuild the project with **-env env_file** option


## Networking

This composed image runs under a single default network, called **sw360net**

So any external docker image can connect to internal couchdb or postgresql through this network


## Running the image first time

* Run the resulting image:

    ```sh
    docker-compose up
    ```

* With custom env file

    ```sh
    docker-compose --env-file <envfile> up
    ```

    You can add **-d** parameter at end of line to start in daemon mode and see the logs with the following command:

    ```sh
    docker logs -f sw360
    ```

## Fossology
For docker based approach, is recommended use official [Fossology docker image](https://hub.docker.com/r/fossology/fossology/)

This is the steps to quick perform this:

```sh
# Create Fossology database on internal postgres
docker exec -it sw360_postgresdb_1 createdb -U sw360admin -W fossology

# Start Fossology container connected to sw360 env
docker run \
    --network sw360net \
    -p 8081:80 \
    --name fossology \
    -e FOSSOLOGY_DB_HOST=postgresdb \
    -e FOSSOLOGY_DB_USER=sw360admin \
    -e FOSSOLOGY_DB_PASSWORD=sw360admin \
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

By default, docker image of sw360 runs without internal web server and is assigned to be on port 8080. This is configured on *portal-ext.properties*

Here's some extra configurations that can be useful to fix some details.

### Customize portal-ext

The config file __portal-ext.properties__ overrides a second file that can be created to add a custom configuration with all data related to your necessities.

This file is called __portal-sw360.properties__

To add your custom configs, create this file under config dir on project root like this ( or with your favorite editor):

```sh
cd <sw360_source>
mkdir config
cat "company.default.name=MYCOMPANY" > config/sw360-portal-ext.properties
```

Docker compose will treat config as a bind volume dir and will expose to application.


### Make **HTTPS** default

Modify the following line on your custom __portal-sw360.properties__ to https:

```ini
web.server.protocol=https


### Nginx config for reverse proxy and X-Frame issues on on host machine ( not docker )

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
