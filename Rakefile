# Copyright (c) Bosch Software Innovations GmbH 2016.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-1.0
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

ROOT=File.dirname(__FILE__)

TARGET_PREFIX=ENV['TARGET_PREFIX'] || "/opt/sw360/deploy"
TARGET_NAME="sw360"
DEPENDENCIES_PACKAGE="sw360_dependencies"

MAVEN_PARAMETERS=ENV['MAVEN_PARAMETERS'] || "-DskipTests"

DEPLOY_DIR=File.expand_path(ENV['DEPLOY_DIR'] || File.join(ROOT, "deploy"))

TMP_DIR_PREFIX="/tmp/sw360_package_tmp_dir_"
TMP_DIR=TMP_DIR_PREFIX << `date +"%FT%Hh%Mm%Ss"`.strip

DEV_CONTAINER_NAME = "sw360dev"
DOCKERIZE=(ENV['DOCKERIZE'] || "true") == "true"

print "########################################################################\n"
print "# DEPLOY_DIR is set to: #{DEPLOY_DIR}\n"
print "# MAVEN_PARAMETERS are set to: #{MAVEN_PARAMETERS}\n"
print "# DOCKERIZE is set to: #{DOCKERIZE}\n"
print "########################################################################\n\n"

task :default => :compile

desc "clean up"
task :clean do
  sh "mvn clean"
end

desc "build the docker container needed for the dockerized commands"
task :build_docker_image do
  cmd = "cat #{DEV_CONTAINER_NAME}.Dockerfile"
  sh cmd << " | docker build -t sw360/#{DEV_CONTAINER_NAME} --rm=true --force-rm=true -"
end

task :maybe_build_docker_image do
  if DOCKERIZE
    Rake::Task["build_docker_image"].invoke
  end
end

def runInDocker(dockercmd, moreVolumes="")
    volumes="-v #{ROOT}:/sw360portal #{moreVolumes}"
    workdir="-w /sw360portal"
    chroot="su-exec $(id -u):$(id -g)"
    sh "docker run -i #{volumes} #{workdir} --net=host sw360/#{DEV_CONTAINER_NAME} #{chroot} #{dockercmd}"
end
def maybeRunInDocker(cmd, dockercmd=cmd, moreVolumes="")
  if DOCKERIZE
    runInDocker(dockercmd, moreVolumes)
  else
    sh cmd
  end
end

desc "use maven to compile SW360 (use DOCKERIZE=true to run this within docker)"
task :compile => [:maybe_build_docker_image] do
  maybeRunInDocker("mvn install #{MAVEN_PARAMETERS}")
end

desc "use maven to deploy SW360 (use DOCKERIZE=true to run this within docker)"
task :deploy => [:maybe_build_docker_image] do
  maybeRunInDocker("mvn install -P deploy #{MAVEN_PARAMETERS} -Ddeploy.dir=#{DEPLOY_DIR}",
                   "mvn install -P deploy #{MAVEN_PARAMETERS} -Ddeploy.dir=#{TMP_DIR}",
                   "-v #{DEPLOY_DIR}:#{TMP_DIR}")
end

namespace :package do
  # the following bash command evaluates to the version of the maven projekt
  $getVersion = "printf 'VERSION=${project.version}\n0\n' | mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate | grep '^VERSION' | awk -F\"=\" '{print $2}'"

  def package(format)
    args =                " -v $(#{$getVersion})"
    args = args <<        " -n #{TARGET_NAME}"
    args = args <<        " -t #{format}"
    args = args <<        " -d #{DEPENDENCIES_PACKAGE}"
    args = args <<  " --prefix #{TARGET_PREFIX}"
    args = args << " -s dir -C #{TMP_DIR}"
    sh "fpm -f #{args} ."
  end

  task :getWars do
    mkdir_p TMP_DIR
    maybeRunInDocker("mvn install -P deploy #{MAVEN_PARAMETERS} -Ddeploy.dir=#{TMP_DIR}")
  end

  desc "generate a debian package, which deploys the war files to #{TARGET_PREFIX}"
  task :deb => ["getWars"] do
    package("deb")
  end
  desc "generate a RPM package, which deploys the war files to #{TARGET_PREFIX}"
  task :rpm => ["getWars"] do
    package("rpm")
  end
  desc "generate a .tar.gz archive of all war files"
  task :tar => ["getWars"] do
    sh "tar cvzf #{TARGET_NAME}-$(#{$getVersion}).tar.gz #{TMP_DIR} --transform='s/#{TMP_DIR.gsub('/','\/')}//g'"
  end

  task :all => [:deb, :rpm, :tar]
end
desc "generate all packages (currently .deb, .rpm and .tar.gz)"
task :package => "package:all"
