# SW360 Docker

### Table of Contents

[Building](#building)

[Running the Image](#running-the-image)

[Extra Configurations](#extra-configurations)


## Building

* Install Docker and docker-compose.
* Build docker compose

    You will need to have a recent docker and docker-compose installed.

    Build is done by the script:

    ```sh
    docker_build.sh
    ```

    The script will download all dependencies in the deps folder.

    Docker compose for sw360 are configured with default entries on docker-compose.yml.
    
    The config entries that can be modifiled:

    ```ini
    # Postgres
    POSTGRES_USER=liferay
    POSTGRES_PASSWORD=liferay
    POSTGRES_DB=lportal
    # Couchdb
    COUCHDB_USER=admin
    COUCHDB_PASSWORD=password
    COUCHDB_CREATE_DATABASE=yes
    ```

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
    There's a local mounted as binded dir volume to add customizations
    ```yml
    - ./config:/app/sw360/config
    ```

    If you want to override all configs, create a docker env file  and alter for your needs.

    Then just rebuild the project with **-env_file** option


## Networking

This composed image runs unde a single ndefault network, called **sw360net**

So any external docker image can connect to internal couchdb or postgresql through this network


## Running the image

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
docker exec -it sw360_postgresdb_1 createdb -U liferay -W fossology

# Start Fossology container connected to sw360 env
docker run \
    --network sw360net \
    -p 8081:80 \
    -name fossology \
    -e FOSSOLOGY_DB_HOST=postgresdb \
    -e FOSSOLOGY_DB_USER=liferay \
    -e FOSSOLOGY_DB_PASSWORD=liferay \
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
  * Add the id of folder. The default id is **1** (Software Repository). You can get the ID of the folder you want from the folder URL in FOssology
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

Docker compose with treat config as a bind volume dir and will expose to application.


### Make **HTTPS** default

Modify the following line on your custom __portal-sw360.properties__ to https:

```ini
web.server.protocol=https
```

### CSS layout looks wrong or using non standard ports

If you do not use an external web server with redirection ( see below ), you may find the main CSS theme scrambled ( not properly loaded ) or you are using a different port

This happens because current Liferay used version try to access the theme using only canonical hostname, without the port assigned, so leading to an invalid CSS url.

To fix, you will need to change __portal-sw360.properties__ ( as described above ) with the following extra values:

```ini
# For different hostname redirection
web.server.host=<your ip/host of docker>
# For HTTP non standard 80 port
web.server.http.port=<your_http_port>
# For HTTPS non standard 443 port
web.server.https.port=<your_https_port>
```

This will tell liferay where is your real host instead of trying to guess the wrong host.


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


### Liferay Redirects

Liferay by default for security reasons do not allow redirect for unknown ips/domains, mostly on admin modules, so is necessary to add your domain or ip to the redirect allowed lists in custom __portal-sw360.properties__.
    
A not proper redirect can see in logs

**IP based** - The list of ips is separated by comma

```
redirect.url.security.mode=ip
redirect.url.ips.allowed=127.0.0.1,172.17.0.1,...
```

**Domain based** - The list domains is separated by comma
    
```
redirect.url.security.mode=domain
redirect.url.domain.allowed=example.com,*.wildcard.com
```
