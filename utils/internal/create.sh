#!/bin/bash

if [ -z "$1" ]; then
  PROJECT_ROOT="$(pwd)/../../"
else
  PROJECT_ROOT="$1"
fi

printf '#!/bin/bash\n'

find $PROJECT_ROOT/backend $PROJECT_ROOT/rest -name "pom.xml" -exec bash -c '
    if grep -q "<packaging>war</packaging>" {}; then
        artifact=$(echo "\${project.groupId}:\${project.artifactId}:\${project.version}:war" | mvn -N -q -DforceStdout help:evaluate -f {} -Dstyle.color=never 2>/dev/null | sed -r "s/\x1B\[[0-9;]*[mK]//g")
        echo "mvn --no-transfer-progress dependency:get -Dartifact=$artifact -Dtransitive=false -DremoteRepositories=gitlab-maven::::https://code.siemens.com/api/v4/projects/708/packages/maven"
    fi
' \;
