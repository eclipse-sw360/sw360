# Steps to use SW360 Docker

* Install Docker.
* Build Docker file
    1. Execute `docker build -t sw360 .` from inside base folder - `sw360`, `-t sw360` is the tag name given to final image created, `.` is to mention build context to Docker.
    2. Files need to be placed in `/etc/sw360`, can be placed in `sw360/scripts/docker-config/etc_sw360`
    3. It might be required to set proxy in case docker is not able to fetch dependencies during building.
       * Set proxy and `active=true` in `sw360/scripts/docker-config/mvn-proxy-settings.xml`
       * Set proxy and place `config.json` in `~/.docker/` directory
       * `Note : ` In case of failure at any step due to network issue or java core dump during mvn building of sw360 , Execute build command again and it will continue from where it failed.
* Run SW360 Docker
    * `docker run -it -p 8090:8080 -p 5985:5984 -p 5435:5432 sw360`
       * `sw360` - tag name of final image
       * `-p <host_machine_port>:<docker_container_port>` - for port forwarding.
       * UI can be accessed - `http://localhost:8090`
       * CouchDB can be accessed on port `5985` Ex - `curl -X GET http://localhost:5985/sw360db/{doc_id}`
       * Postgresql can be accessed on port `5435` Ex - `psql -h localhost -p 5435 -U postgres` , `Default pwd` - `postgrespwd`
* Perform initial configuration of SW360
  * SW360 UI and REST can be accessed from `http://localhost:8090`. 
  * Open SW360 UI and perform [initial configurations](https://github.com/eclipse/sw360/wiki/Deploy-Liferay7.3). 
* Create new image of the running and configured SW360 container once all configurations done, using `docker commit`
    * Press `ENTER` in the terminal from where `docker run` was executed .Wait for some time, it will initiate shutdown procedure for tomcat. Tomcat shutdown logs will start loading, wait till it's completed.
    * Open another terminal window and type `docker ps` in order to get the container ID of the running SW360 container. The result should look like
```
CONTAINER ID   IMAGE     COMMAND                  CREATED        STATUS        PORTS                                                                    NAMES
b5a5b75dd81b   sw360     "/bin/sh -c './entry…"   16 hours ago   Up 16 hours   0.0.0.0:5435->5432/tcp, 0.0.0.0:5985->5984/tcp, 0.0.0.0:8090->8080/tcp   upbeat_jemison
```
   * Create a new docker image from the state of your running SW360 container with `docker commit CONTAINER ID sw360configured`
   * Check if the new image `sw360configured`is available with `docker images`. You should have an output like
```
REPOSITORY            TAG       IMAGE ID       CREATED          SIZE
sw360configured       latest    d41cfb273751   48 seconds ago   6.86GB
...
```
   * Now you can stop the old SW360 container with `docker stop CONTAINER ID`
   * And start newly configured container with `docker run -it -p 8090:8080 -p 5985:5984 -p 5435:5432 sw360configured`
