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
* SW360 UI and REST can be accessed from `http://localhost:8090` . Open SW360 UI and perform [initial configurations](https://github.com/eclipse/sw360/wiki/Deploy-Liferay7.3). Save image of the container once all configurations done, using `docker commit`
*  Docker commit and Clean Exit
    * Press `ENTER` in the terminal from where `docker run` was executed .Wait for some time, it will initiate shutdown procedure for tomcat.
    *  Tomcat shutdown logs will start loading, wait till it's completed.
    * `docker commit container-id sw360configured`  -  create docker image from container
    * `docker stop old_container_id`  - stop old container
    * Start new configured container - `docker run -it -p 8090:8080 -p 5985:5984 -p 5435:5432 sw360configured`
